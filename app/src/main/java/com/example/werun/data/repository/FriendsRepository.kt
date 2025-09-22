package com.example.werun.data.repository

import android.util.Log
import com.example.werun.data.User
import com.example.werun.data.FriendRequest
import com.example.werun.data.Friendship
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class FriendsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val friendsCollection = firestore.collection("friendships")
    private val friendRequestsCollection = firestore.collection("friend_requests")
    private val usersCollection = firestore.collection("users")

    fun getFriends(): Flow<List<Friendship>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = friendsCollection
            .whereEqualTo("userId1", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val friendships = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val friendship = doc.toObject(Friendship::class.java)?.copy(id = doc.id)
                        friendship
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(friendships)
            }

        awaitClose { listener.remove() }
    }

    fun getFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = friendRequestsCollection
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "pending")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(FriendRequest::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    fun getSuggestedFriends(): Flow<List<User>> = callbackFlow {
        val userId = currentUserId ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var requestsListener: ListenerRegistration? = null
        var usersListener: ListenerRegistration? = null
        val friendsListener = friendsCollection
            .whereEqualTo("userId1", userId)
            .addSnapshotListener { friendsSnapshot, _ ->
                requestsListener?.remove()
                requestsListener = friendRequestsCollection
                    .whereEqualTo("fromUserId", userId)
                    .addSnapshotListener { requestsSnapshot, _ ->
                        val friendIds = friendsSnapshot?.documents?.mapNotNull {
                            it.getString("userId2")
                        } ?: emptyList()

                        val requestedIds = requestsSnapshot?.documents?.mapNotNull {
                            it.getString("toUserId")
                        } ?: emptyList()

                        val excludeIds = friendIds + requestedIds + userId

                        usersListener?.remove()
                        usersListener = usersCollection
                            .limit(20)
                            .addSnapshotListener { usersSnapshot, error ->
                                if (error != null) {
                                    close(error)
                                    return@addSnapshotListener
                                }

                                val suggestions = usersSnapshot?.documents?.mapNotNull { doc ->
                                    try {
                                        val user = doc.toObject(User::class.java)
                                        if (user?.uid !in excludeIds) user else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                } ?: emptyList()

                                trySend(suggestions.take(10))
                            }
                    }
            }

        awaitClose {
            friendsListener.remove()
            requestsListener?.remove()
            usersListener?.remove()
        }
    }

    suspend fun getNearbyRunners(
        currentLat: Double,
        currentLng: Double,
        maxDistanceKm: Double = 10.0,
        limit: Long = 10
    ): Result<List<User>> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            // Query users with non-null lastRunLat/lng
            val usersSnapshot = usersCollection
                .whereNotEqualTo("lastRunLat", null)
                .whereNotEqualTo("lastRunLng", null)
                .limit(limit)
                .get()
                .await()

            val users = usersSnapshot.documents.mapNotNull { doc ->
                try {
                    val user = doc.toObject(User::class.java)
                    if (user?.uid == userId) null // Exclude current user
                    else user
                } catch (e: Exception) {
                    null
                }
            }

            // Filter by distance (Euclidean for simplicity)
            val nearbyUsers = users.filter { user ->
                user.lastRunLat != null && user.lastRunLng != null &&
                        calculateDistance(
                            currentLat,
                            currentLng,
                            user.lastRunLat!!,
                            user.lastRunLng!!
                        ) <= maxDistanceKm
            }.take(limit.toInt())

            Result.success(nearbyUsers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Simple Euclidean distance in km (approximate)
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        return earthRadius * sqrt(dLat * dLat + dLng * dLng)
    }

    suspend fun sendFriendRequest(toUserId: String): Result<String> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            // Check if request already exists
            val existingRequest = friendRequestsCollection
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("toUserId", toUserId)
                .whereIn("status", listOf("pending", "accepted"))
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("Friend request already exists"))
            }

            // Get sender user info
            val senderDoc = usersCollection.document(userId).get().await()
            val sender = senderDoc.toObject(User::class.java) ?: return Result.failure(Exception("Sender not found"))

            val friendRequest = FriendRequest(
                fromUserId = userId,
                toUserId = toUserId,
                fromUser = sender,
                status = "pending",
                createdAt = System.currentTimeMillis()
            )

            val docRef = friendRequestsCollection.add(friendRequest).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return Result.failure(Exception("Request not found"))

            if (request.toUserId != userId) {
                return Result.failure(Exception("Unauthorized"))
            }

            firestore.runTransaction { transaction ->
                // 🔹 Đọc trước tất cả documents cần thiết
                val user1Doc = transaction.get(usersCollection.document(request.fromUserId))
                val user2Doc = transaction.get(usersCollection.document(request.toUserId))

                val user1 = user1Doc.toObject(User::class.java)
                    ?: throw IllegalStateException("User1 not found")
                val user2 = user2Doc.toObject(User::class.java)
                    ?: throw IllegalStateException("User2 not found")

                // 🔹 Sau đó mới update request
                transaction.update(requestDoc.reference, "status", "accepted")

                // 🔹 Tạo friendship
                val friendship1 = Friendship(
                    userId1 = request.fromUserId,
                    userId2 = request.toUserId,
                    user1 = user1,
                    user2 = user2,
                    createdAt = System.currentTimeMillis(),
                    lastActivity = System.currentTimeMillis()
                )

                val friendship2 = Friendship(
                    userId1 = request.toUserId,
                    userId2 = request.fromUserId,
                    user1 = user2,
                    user2 = user1,
                    createdAt = System.currentTimeMillis(),
                    lastActivity = System.currentTimeMillis()
                )
                Log.e("FriendRequest", "👥 Writing friendship: userId1=${friendship1.userId1}, userId2=${friendship1.userId2}, auth=${userId}")

                transaction.set(friendsCollection.document(), friendship1)
                transaction.set(friendsCollection.document(), friendship2)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRequest", "❌ Error accepting friend request $requestId: ${e.message}", e)
            Result.failure(e)
        }
    }


    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            val requestDoc = friendRequestsCollection.document(requestId).get().await()
            val request = requestDoc.toObject(FriendRequest::class.java)
                ?: return Result.failure(Exception("Request not found"))

            if (request.toUserId != userId) {
                return Result.failure(Exception("Unauthorized"))
            }

            friendRequestsCollection.document(requestId)
                .update("status", "rejected")
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendUid: String): Result<Unit> {
        return try {
            val currentUserId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            // Find main friendship ref outside transaction
            val friendshipQuery = friendsCollection
                .whereEqualTo("userId1", currentUserId)
                .whereEqualTo("userId2", friendUid)
                .get()
                .await()

            val friendshipRef = friendshipQuery.documents.firstOrNull()?.reference
                ?: return Result.failure(Exception("Friendship not found"))

            // Find reverse friendship ref outside transaction
            val reverseQuery = friendsCollection
                .whereEqualTo("userId1", friendUid)
                .whereEqualTo("userId2", currentUserId)
                .get()
                .await()

            val reverseRef = reverseQuery.documents.firstOrNull()?.reference

            firestore.runTransaction { transaction ->
                transaction.delete(friendshipRef)
                reverseRef?.let { transaction.delete(it) }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("User not authenticated"))

            if (query.isBlank()) {
                return Result.success(emptyList())
            }

            // Search by full name or email
            val nameResults = usersCollection
                .whereGreaterThanOrEqualTo("fullName", query)
                .whereLessThan("fullName", query + '\uf8ff')
                .limit(10)
                .get()
                .await()

            val emailResults = usersCollection
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThan("email", query + '\uf8ff')
                .limit(10)
                .get()
                .await()

            val users = (nameResults.documents + emailResults.documents)
                .distinctBy { it.id }
                .mapNotNull { doc ->
                    try {
                        val user = doc.toObject(User::class.java)
                        if (user?.uid != userId) user else null
                    } catch (e: Exception) {
                        null
                    }
                }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFriendById(friendId: String): Result<User?> {
        return try {
            val userDoc = usersCollection.document(friendId).get().await()
            val user = userDoc.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLastActivity(friendshipId: String): Result<Unit> {
        return try {
            friendsCollection.document(friendshipId)
                .update("lastActivity", System.currentTimeMillis())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}