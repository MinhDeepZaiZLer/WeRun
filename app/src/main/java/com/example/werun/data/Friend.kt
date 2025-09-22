package com.example.werun.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Friend(
    val friendUid: String = "",
    val status: String = "pending", // pending, accepted, blocked
    val actionBy: String = "", // uid của người gửi lời mời
    @ServerTimestamp val since: Date? = null
)
