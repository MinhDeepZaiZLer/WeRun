package com.example.werun.ui.screens.run

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.werun.services.LocationTrackingService
import com.mapbox.geojson.Point
import com.mapbox.geojson.LineString
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Define the color palette
val overlayBackground = Color.Black.copy(alpha = 0.3f)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var service by remember { mutableStateOf<LocationTrackingService?>(null) }
    var bound by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var routePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }
    var hasInitialZoom by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0L) }
    var averageSpeed by remember { mutableStateOf(0.0) }
    var showSaveDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val b = binder as LocationTrackingService.LocationBinder
                service = b.getService()
                bound = true
                // Sync initial data from service
                service?.let { svc ->
                    routePoints = svc.routePoints.value
                    distance = svc.distance.value
                    duration = svc.duration.value
                    averageSpeed = svc.averageSpeed.value
                    isRunning = svc.isTracking.value
                    userLocation = svc.currentLocation.value
                }
                println("MapScreen: Service connected, initial routePoints size: ${routePoints.size}")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                bound = false
                service = null
                println("MapScreen: Service disconnected")
            }
        }
        Intent(context, LocationTrackingService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        onDispose {
            if (bound) {
                context.unbindService(connection)
            }
        }
    }

    LaunchedEffect(service) {
        service?.let { svc ->
            coroutineScope.launch {
                svc.currentLocation.collectLatest { loc ->
                    userLocation = loc
                }
            }
            coroutineScope.launch {
                svc.routePoints.collectLatest { points ->
                    routePoints = points
                    println("MapScreen: Route points updated: ${points.size}")
                }
            }
            coroutineScope.launch {
                svc.isTracking.collectLatest { tracking ->
                    isRunning = tracking
                    println("MapScreen: Tracking status: $tracking")
                }
            }
            coroutineScope.launch {
                svc.distance.collectLatest { dist ->
                    distance = dist
                }
            }
            coroutineScope.launch {
                svc.duration.collectLatest { dur ->
                    duration = dur
                }
            }
            coroutineScope.launch {
                svc.averageSpeed.collectLatest { speed ->
                    averageSpeed = speed
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapViewContainer(
            userLocation = userLocation,
            routePoints = routePoints,
            hasInitialZoom = hasInitialZoom,
            onInitialZoom = { hasInitialZoom = true }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp)
        ) {
            TopStatusBar()
            Spacer(modifier = Modifier.weight(1f))
            StatsInfoOverlay(
                distance = distance,
                duration = duration,
                averageSpeed = averageSpeed,
                isRunning = isRunning
            )
            Spacer(modifier = Modifier.height(24.dp))
            MapActionButtons(
                navController = navController,
                isRunning = isRunning,
                hasRoute = routePoints.isNotEmpty(),
                onStartStop = { running ->
                    service?.let { svc ->
                        if (running) {
                            svc.startRun()
                        } else {
                            svc.pauseRun()
                        }
                    }
                },
                onStop = {
                    if (routePoints.isNotEmpty() && distance > 0) {
                        showSaveDialog = true
                    } else {
                        service?.clearRoute()
                        navController.popBackStack()
                    }
                },
                onReset = {
                    service?.clearRoute()
                }
            )
        }

        // Save Run Dialog
        if (showSaveDialog) {
            SaveRunDialog(
                distance = distance,
                duration = duration,
                onSave = {
                    service?.stopRun() // This will save to Firebase
                    showSaveDialog = false
                    navController.popBackStack()
                },
                onDiscard = {
                    service?.clearRoute()
                    showSaveDialog = false
                    navController.popBackStack()
                },
                onDismiss = {
                    showSaveDialog = false
                }
            )
        }
    }
}

@Composable
fun MapViewContainer(
    userLocation: Point?,
    routePoints: List<Point>,
    hasInitialZoom: Boolean,
    onInitialZoom: () -> Unit
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapView = this
                mapboxMap.loadStyle(Style.OUTDOORS) { style ->
                    location.apply {
                        enabled = true
                        pulsingEnabled = true
                    }

                    val routeSource = geoJsonSource("route-source") {}
                    style.addSource(routeSource)

                    val routeLayer = LineLayer("route-layer", "route-source")
                    routeLayer.lineColor("#FF4081")
                    routeLayer.lineWidth(5.0)
                    routeLayer.lineOpacity(0.8)
                    style.addLayer(routeLayer)
                }
            }
        },
        update = { view ->
            userLocation?.let { location ->
                if (!hasInitialZoom) {
                    view.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(location)
                            .zoom(18.0)
                            .build()
                    )
                    onInitialZoom()
                } else {
                    // More gentle camera update when following
                    view.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(location)
                            .zoom(16.0)
                            .build()
                    )
                }
            }

            view.mapboxMap.getStyle { style ->
                val routeSource = style.getSource("route-source") as? GeoJsonSource
                routeSource?.let { source ->
                    if (routePoints.size >= 2) {
                        val lineString = LineString.fromLngLats(routePoints)
                        source.geometry(lineString)
                    } else {
                        // Clear the route by setting empty LineString
                        val emptyLineString = LineString.fromLngLats(emptyList<Point>())
                        source.geometry(emptyLineString)
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun StatsInfoOverlay(
    distance: Double,
    duration: Long,
    averageSpeed: Double,
    isRunning: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(overlayBackground, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            icon = if (isRunning) Icons.Default.DirectionsRun else Icons.Default.Pause,
            value = if (isRunning) "RUNNING" else "PAUSED",
            label = "Status",
            color = if (isRunning) lightGreen else Color.Yellow
        )

        StatItem(
            icon = Icons.Default.Straighten,
            value = String.format("%.2f km", distance / 1000),
            label = "Distance",
            color = Color.White
        )

        StatItem(
            icon = Icons.Default.Timer,
            value = formatDuration(duration),
            label = "Duration",
            color = Color.White
        )

        if (averageSpeed > 0) {
            StatItem(
                icon = Icons.Default.Speed,
                value = String.format("%.1f km/h", averageSpeed), // averageSpeed is already in km/h from service
                label = "Speed",
                color = Color.White
            )
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, color: Color = textBlack) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun MapActionButtons(
    navController: NavController,
    isRunning: Boolean,
    hasRoute: Boolean,
    onStartStop: (Boolean) -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back to Run Screen Button
        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.size(70.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, lightGreen),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textBlack,
                containerColor = chipBackground
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back to Run Screen",
                tint = textBlack,
                modifier = Modifier.size(24.dp)
            )
        }

        // Start/Pause Button
        Button(
            onClick = { onStartStop(!isRunning) },
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color.Red else lightGreen
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        // Reset Button (enabled only when paused and has route)
        OutlinedButton(
            onClick = { onReset() },
            modifier = Modifier.size(70.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, if (hasRoute && !isRunning) lightGreen else Color.Gray),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textBlack,
                containerColor = chipBackground
            ),
            enabled = hasRoute && !isRunning
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = if (hasRoute && !isRunning) textBlack else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        // Stop & Save Button
        Button(
            onClick = { onStop() },
            modifier = Modifier.size(70.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop & Save",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen(navController = rememberNavController())
}