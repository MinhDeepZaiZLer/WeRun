package com.example.werun.data.repository

import com.example.werun.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    fun isAuthenticated(): Boolean = auth.currentUser != null

    fun logout() {
        auth.signOut()
        // Optional: Clear local data if needed (e.g., SharedPreferences)
    }

    /**
     * Fetches the current user's data from Firestore.
     * @return User object or null if not found or unauthenticated.
     * @throws Exception if Firestore operation fails.
     */
    suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return try {
            firestore.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            throw Exception("Failed to fetch user data: ${e.message}", e)
        }
    }

    /**
     * Updates the current user's data in Firestore.
     * @param user The updated User object.
     * @throws Exception if authentication fails or Firestore update fails.
     */
    suspend fun updateUser(user: User) {
        val uid = currentUserId ?: throw Exception("User not authenticated")
        try {
            firestore.collection("users").document(uid).set(user.toMap(), SetOptions.merge()).await()
        } catch (e: Exception) {
            throw Exception("Failed to update user data: ${e.message}", e)
        }
    }
}