package com.example.werun.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class RunData(
    @DocumentId
    val id: String = "",
    val userId: String = "", // Đảm bảo trường này tồn tại khi lưu dữ liệu
    val distance: Double = 0.0, // in meters
    val duration: Long = 0L, // in milliseconds
    val averageSpeed: Double = 0.0, // km/h, có thể tính từ distance và duration
    val routePoints: List<LocationPoint> = emptyList(),
    val startTime: Date? = null,
    val endTime: Date? = null, // Có thể tính từ startTime + duration
    @ServerTimestamp
    val createdAt: Date? = null, // Thời gian server tạo tài liệu
    val calories: Int = 0,
    val steps: Int = 0,
    val elevationGain: Double = 0.0, // in meters
    val weather: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false
) {
    // Tính averageSpeed nếu chưa có
    val computedAverageSpeed: Double
        get() = if (duration > 0 && distance > 0) {
            (distance / 1000) / (duration / 3600000.0) // km/h
        } else averageSpeed

    // Tính endTime nếu chưa có
    val computedEndTime: Date?
        get() = startTime?.let { Date(it.time + duration) }

    fun getFormattedDistance(): String {
        val km = distance / 1000.0
        return String.format("%.2f Km", km)
    }

    fun getFormattedTime(): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = duration / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun getFormattedPace(): String {
        val speed = if (computedAverageSpeed > 0) computedAverageSpeed else averageSpeed
        if (speed <= 0) return "--'--''"
        val paceMinutes = 60.0 / speed
        val minutes = paceMinutes.toInt()
        val seconds = ((paceMinutes - minutes) * 60).roundToInt()
        return String.format("%d'%02d''", minutes, seconds)
    }

    fun getFormattedDate(): String {
        return startTime?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } ?: ""
    }

    fun getDayOfWeek(): String {
        return startTime?.let {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(it)
        } ?: ""
    }

    fun getRunType(): String {
        return if (notes.isNotEmpty()) notes else "Morning Run"
    }
}

data class LocationPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L
)