package com.example.werun.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Friend(
    val friendUid: String = "",
    val status: String = "pending", // pending, accepted, blocked
    val actionBy: String = "", // uid của người gửi lời mời
    @ServerTimestamp val since: Date? = null
)
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val fromUser: User = User(),
    val status: String = "pending", // pending, accepted, rejected
    val createdAt: Long = 0L
)

data class Friendship(
    val id: String = "",
    val userId1: String = "",
    val userId2: String = "",
    val user1: User = User(),
    val user2: User = User(),
    val createdAt: Long = 0L,
    val lastActivity: Long = 0L
)