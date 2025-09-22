package com.example.werun.utils

import android.location.Location
import com.example.werun.data.model.LocationPoint

fun Location.toLocationPoint(): LocationPoint {
    return LocationPoint(
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.time
    )
}