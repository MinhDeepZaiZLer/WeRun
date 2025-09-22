package com.example.werun.ui.screens.run

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.werun.services.LocationTrackingService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Define the color palette based on the image for easy reuse
val lightGreen = Color(0xFFD0FD3E)
val lightGray = Color(0xFFF5F5F5)
val textBlack = Color(0xFF000000)
val textGray = Color(0xFF8A8A8E)
val chipBackground = Color(0xFFE8F5E9)

@Composable
fun RunScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var service by remember { mutableStateOf<LocationTrackingService?>(null) }
    var bound by remember { mutableStateOf(false) }
    var distance by remember { mutableStateOf(0.0) }
    var duration by remember { mutableStateOf(0L) }
    var averageSpeed by remember { mutableStateOf(0.0) }
    var isTracking by remember { mutableStateOf(false) }
    var hasRoute by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val b = binder as LocationTrackingService.LocationBinder
                service = b.getService()
                bound = true
                println("RunScreen: Service connected")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                bound = false
                service = null
                println("RunScreen: Service disconnected")
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
            coroutineScope.launch {
                svc.isTracking.collectLatest { tracking ->
                    isTracking = tracking
                }
            }
            coroutineScope.launch {
                svc.routePoints.collectLatest { points ->
                    hasRoute = points.isNotEmpty()
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = lightGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopStatusBar()
            Spacer(modifier = Modifier.height(64.dp))
            DistanceDisplay(distance = String.format("%.2f", distance / 1000))
            Spacer(modifier = Modifier.height(48.dp))
            StatsInfo(
                duration = formatDuration(duration),
                averageSpeed = averageSpeed,
                distance = distance
            )
            Spacer(modifier = Modifier.weight(1f))
            MusicPlayerControls()
            Spacer(modifier = Modifier.height(32.dp))
            ActionButtons(
                navController = navController,
                isTracking = isTracking,
                hasRoute = hasRoute,
                onStartStop = { shouldStart ->
                    service?.let { svc ->
                        if (shouldStart) {
                            svc.startRun()
                        } else {
                            svc.pauseRun()
                        }
                    }
                },
                onStop = {
                    if (hasRoute && distance > 0) {
                        showSaveDialog = true
                    } else {
                        service?.clearRoute()
                        // Could navigate back or show message
                    }
                }
            )
        }
    }

    // Save Run Dialog
    if (showSaveDialog) {
        SaveRunDialog(
            distance = distance,
            duration = duration,
            onSave = {
                service?.stopRun() // This will save to Firebase
                showSaveDialog = false
                // Navigate to history or main screen
            },
            onDiscard = {
                service?.clearRoute()
                showSaveDialog = false
                // Navigate back
            },
            onDismiss = {
                showSaveDialog = false
            }
        )
    }
}

@Composable
fun SaveRunDialog(
    distance: Double,
    duration: Long,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Run?") },
        text = {
            Column {
                Text("Distance: ${String.format("%.2f km", distance / 1000)}")
                Text("Duration: ${formatDuration(duration)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Do you want to save this run?")
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = lightGreen)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDiscard) {
                Text("Discard")
            }
        }
    )
}

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val remainingSecs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSecs)
    } else {
        String.format("%02d:%02d", remainingMinutes, remainingSecs)
    }
}

// Helper function to parse duration string back to milliseconds if needed
fun parseDurationToMs(durationStr: String): Long {
    return try {
        val parts = durationStr.split(":")
        when (parts.size) {
            2 -> {
                val minutes = parts[0].toLong()
                val seconds = parts[1].toLong()
                (minutes * 60 + seconds) * 1000
            }
            3 -> {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val seconds = parts[2].toLong()
                ((hours * 60 + minutes) * 60 + seconds) * 1000
            }
            else -> 0L
        }
    } catch (e: Exception) {
        0L
    }
}

@Composable
fun TopStatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(icon = Icons.Default.WbSunny, text = "32°C")

        // Progress/Battery indicator
        Row(
            modifier = Modifier
                .height(8.dp)
                .width(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(chipBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .background(lightGreen)
            )
        }

        StatusChip(icon = Icons.Default.GpsFixed, text = "GPS")
    }
}

@Composable
fun StatusChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color = chipBackground, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = textGray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = textBlack, fontSize = 14.sp)
    }
}

@Composable
fun DistanceDisplay(distance: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = distance,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = textBlack,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Distance (Km)",
            fontSize = 16.sp,
            color = textGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatsInfo(duration: String, averageSpeed: Double, distance: Double) {
    // Calculate pace (minutes per km) - fix the duration parsing
    val durationMs = parseDurationToMs(duration)
    val paceMinutesPerKm = if (distance > 0 && durationMs > 0) {
        val durationMinutes = durationMs.toDouble() / 60000 // Convert ms to minutes
        val distanceKm = distance / 1000 // Convert m to km
        durationMinutes / distanceKm
    } else {
        0.0
    }

    val paceDisplay = if (paceMinutesPerKm > 0) {
        val minutes = paceMinutesPerKm.toInt()
        val seconds = ((paceMinutesPerKm - minutes) * 60).toInt()
        "${minutes}'${String.format("%02d", seconds)}\""
    } else {
        "0'00\""
    }

    // Calculate calories (rough estimate: 0.75 calories per kg per km, assuming 70kg)
    val calories = ((distance / 1000) * 0.75 * 70).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(icon = Icons.Default.DirectionsRun, value = paceDisplay, label = "Avg Pace")
        StatItem(icon = Icons.Default.Timer, value = duration, label = "Duration")
        StatItem(icon = Icons.Default.LocalFireDepartment, value = "$calories kcal", label = "Calories")
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = textBlack
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = textGray
        )
    }
}

@Composable
fun MusicPlayerControls() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = lightGreen),
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause music",
                modifier = Modifier.size(28.dp),
                tint = textBlack
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sweet Disposition",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textBlack
                )
                Text(
                    text = "The Temper Trap",
                    fontSize = 12.sp,
                    color = textBlack.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next song",
                modifier = Modifier.size(28.dp),
                tint = textBlack
            )
        }
    }
}

@Composable
fun ActionButtons(
    navController: NavController,
    isTracking: Boolean,
    hasRoute: Boolean,
    onStartStop: (Boolean) -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Map Button
        OutlinedButton(
            onClick = { navController.navigate("map") },
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, lightGreen),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = "Open Map",
                tint = textBlack,
                modifier = Modifier.size(36.dp)
            )
        }

        // Start/Pause Button
        Button(
            onClick = { onStartStop(!isTracking) },
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) Color.Red else lightGreen
            )
        ) {
            Text(
                text = if (isTracking) "PAUSE" else if (hasRoute) "RESUME" else "START",
                color = if (isTracking) Color.White else textBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Stop Button (visible when there's a route)
        if (hasRoute) {
            Button(
                onClick = { onStop() },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_6")
@Composable
fun RunScreenPreview() {
    RunScreen(navController = rememberNavController())
}