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
    val dob: String = ""

)
