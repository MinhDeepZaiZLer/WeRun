package com.example.werun.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "Customer",
    @ServerTimestamp val createdAt: Date? = null,
    val address: String = "",
    val gender: String = "",
    val phoneNumber: String = "",
    val dob: String = "",
    val lastRunLat: Double? = null,
    val lastRunLng: Double? = null,
    val public: Boolean = false // Added for profile visibility
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "fullName" to fullName,
            "role" to role,
            "address" to address,
            "gender" to gender,
            "phoneNumber" to phoneNumber,
            "dob" to dob,
            "lastRunLat" to lastRunLat,
            "lastRunLng" to lastRunLng,
            "public" to public,
            "createdAt" to createdAt
        )
    }
}