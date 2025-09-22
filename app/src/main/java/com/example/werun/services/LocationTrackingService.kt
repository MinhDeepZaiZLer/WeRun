package com.example.werun.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.werun.MainActivity
import com.example.werun.R
import com.example.werun.data.model.LocationPoint
import com.example.werun.data.model.RunData
import com.example.werun.utils.toLocationPoint
import com.example.werun.data.repository.FirebaseRunRepository
import com.google.android.gms.location.*
import com.mapbox.geojson.Point
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import javax.inject.Inject
import kotlin.math.*

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var firebaseRepository: FirebaseRunRepository

    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State flows for UI
    private val _currentLocation = MutableStateFlow<Point?>(null)
    val currentLocation: StateFlow<Point?> = _currentLocation.asStateFlow()

    private val _routePoints = MutableStateFlow<List<Point>>(emptyList())
    val routePoints: StateFlow<List<Point>> = _routePoints.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _averageSpeed = MutableStateFlow(0.0)
    val averageSpeed: StateFlow<Double> = _averageSpeed.asStateFlow()

    // Run session data
    private var startTime: Date? = null
    private var endTime: Date? = null
    private var currentRunId: String? = null
    private var locationPoints = mutableListOf<LocationPoint>()
    private var durationTimer: Job? = null
    private var lastLocation: Location? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds
        const val MIN_DISTANCE_THRESHOLD = 5.0 // 5 meters
    }

    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        setupLocationTracking()
        createNotificationChannel()
    }

    private fun setupLocationTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
    }

    private fun updateLocation(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)
        _currentLocation.value = point

        if (_isTracking.value) {
            // Check if location is accurate enough and different enough from last location
            if (location.accuracy <= 20 && shouldAddLocation(location)) {
                // Sử dụng location.toLocationPoint() thay vì point.toLocationPoint()
                val locationPoint = location.toLocationPoint()

                locationPoints.add(locationPoint)

                // Update route points for map display
                val currentRoutePoints = _routePoints.value.toMutableList()
                currentRoutePoints.add(point)
                _routePoints.value = currentRoutePoints

                // Calculate and update distance
                if (lastLocation != null) {
                    val additionalDistance = calculateDistance(lastLocation!!, location)
                    _distance.value += additionalDistance

                    // Update average speed
                    val durationInHours = _duration.value / 3600000.0 // Convert ms to hours
                    if (durationInHours > 0) {
                        _averageSpeed.value = (_distance.value / 1000) / durationInHours // km/h
                    }
                }

                lastLocation = location
            }
        }
    }

    private fun shouldAddLocation(newLocation: Location): Boolean {
        return if (lastLocation == null) {
            true
        } else {
            val distance = calculateDistance(lastLocation!!, newLocation)
            distance >= MIN_DISTANCE_THRESHOLD
        }
    }

    private fun calculateDistance(location1: Location, location2: Location): Double {
        val lat1 = Math.toRadians(location1.latitude)
        val lon1 = Math.toRadians(location1.longitude)
        val lat2 = Math.toRadians(location2.latitude)
        val lon2 = Math.toRadians(location2.longitude)

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1

        val a = sin(dlat / 2) * sin(dlat / 2) +
                cos(lat1) * cos(lat2) * sin(dlon / 2) * sin(dlon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return 6371000 * c // Earth radius in meters
    }

    @SuppressLint("ForegroundServiceType")
    fun startRun() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            _isTracking.value = true
            startTime = Date()
            locationPoints.clear()
            _distance.value = 0.0
            _duration.value = 0L
            _averageSpeed.value = 0.0
            lastLocation = null

            // Start location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Start duration timer
            startDurationTimer()

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    fun pauseRun() {
        _isTracking.value = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        durationTimer?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    @SuppressLint("ForegroundServiceType")
    fun resumeRun() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            _isTracking.value = true

            // Bật lại location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Bật lại duration timer
            startDurationTimer()

            // Bật lại foreground service
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    fun stopRun() {
        pauseRun()
        endTime = Date()

        // Save run to Firebase
        saveRunToFirebase()
    }

    fun clearRoute() {
        pauseRun()
        _routePoints.value = emptyList()
        locationPoints.clear()
        _distance.value = 0.0
        _duration.value = 0L
        _averageSpeed.value = 0.0
        startTime = null
        endTime = null
        currentRunId = null
        lastLocation = null
    }

    private fun saveRunToFirebase() {
        serviceScope.launch {
            try {
                val runData = RunData(
                    distance = _distance.value,
                    duration = _duration.value,
                    averageSpeed = _averageSpeed.value,
                    routePoints = locationPoints,
                    startTime = startTime,
                    endTime = endTime,
                    calories = calculateCalories(),
                    steps = estimateSteps()
                )

                val result = firebaseRepository.saveRun(runData)
                if (result.isSuccess) {
                    currentRunId = result.getOrNull()
                    println("Run saved successfully with ID: $currentRunId")
                } else {
                    println("Failed to save run: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                println("Error saving run: ${e.message}")
            }
        }
    }

    private fun calculateCalories(): Int {
        // Simple calorie calculation based on distance and average weight
        val averageWeight = 70.0 // kg
        val caloriesPerKm = averageWeight * 0.75 // Rough estimation
        return ((_distance.value / 1000) * caloriesPerKm).toInt()
    }

    private fun estimateSteps(): Int {
        // Rough estimation: average step length is about 0.78 meters
        return (_distance.value / 0.78).toInt()
    }

    private fun startDurationTimer() {
        durationTimer = serviceScope.launch {
            while (_isTracking.value) {
                delay(1000) // Update every second
                _duration.value += 1000
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows run tracking status"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("WeRun - Tracking Run")
        .setContentText("Distance: ${String.format("%.2f km", _distance.value / 1000)}")
        .setSmallIcon(R.drawable.ic_run) // Đảm bảo icon này tồn tại
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}