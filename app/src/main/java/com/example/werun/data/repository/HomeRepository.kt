// ============================================
// 1. DATA LAYER - Repository
// ============================================
package com.example.werun.data.repository

import com.example.werun.data.User
import com.example.werun.data.model.RunData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

interface HomeRepository {
    fun getCurrentUser(): Flow<User?>
    fun getUserRunStats(): Flow<HomeStats>
    suspend fun getTodayRuns(): List<RunData>
    suspend fun getRecentRuns(limit: Int): List<RunData>
}

data class HomeStats(
    val totalDistance: Double = 0.0,
    val bestPace: String = "0:00",
    val consecutiveDays: Int = 0,
    val todayGoal: Double = 5000.0, // meters
    val goalProgress: Float = 0f,
    val todayDistance: Double = 0.0
)

class HomeRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : HomeRepository {

    private val userId: String?
        get() = auth.currentUser?.uid

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    override fun getUserRunStats(): Flow<HomeStats> = callbackFlow {
        val uid = userId
        if (uid == null) {
            trySend(HomeStats())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("runs")
            .whereEqualTo("userId", uid)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val runs = snapshot?.documents?.mapNotNull {
                    it.toObject(RunData::class.java)
                } ?: emptyList()

                val stats = calculateStats(runs)
                trySend(stats)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getTodayRuns(): List<RunData> {
        val uid = userId ?: return emptyList()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        return try {
            firestore.collection("runs")
                .whereEqualTo("userId", uid)
                .whereGreaterThanOrEqualTo("startTime", startOfDay)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(RunData::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRecentRuns(limit: Int): List<RunData> {
        val uid = userId ?: return emptyList()

        return try {
            firestore.collection("runs")
                .whereEqualTo("userId", uid)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(RunData::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateStats(runs: List<RunData>): HomeStats {
        if (runs.isEmpty()) return HomeStats()

        // Total distance (all time)
        val totalDistance = runs.sumOf { it.distance }

        // Best pace
        val bestPace = runs
            .filter { it.computedAverageSpeed > 0 }
            .minByOrNull { 60.0 / it.computedAverageSpeed }
            ?.getFormattedPace() ?: "0:00"

        // Consecutive days
        val consecutiveDays = calculateConsecutiveDays(runs)

        // Today's distance
        val todayDistance = getTodayDistance(runs)

        // Goal progress (5km default)
        val todayGoal = 5000.0
        val goalProgress = (todayDistance / todayGoal).toFloat().coerceIn(0f, 1f)

        return HomeStats(
            totalDistance = totalDistance,
            bestPace = bestPace,
            consecutiveDays = consecutiveDays,
            todayGoal = todayGoal,
            goalProgress = goalProgress,
            todayDistance = todayDistance
        )
    }

    private fun calculateConsecutiveDays(runs: List<RunData>): Int {
        if (runs.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val today = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val runDates = runs
            .mapNotNull { it.startTime }
            .map { date ->
                Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
            }
            .distinct()
            .sortedDescending()

        if (runDates.isEmpty()) return 0

        var consecutiveDays = 0
        var currentDate = today

        for (runDate in runDates) {
            if (runDate == currentDate) {
                consecutiveDays++
                calendar.time = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = calendar.time
            } else {
                break
            }
        }

        return consecutiveDays
    }

    private fun getTodayDistance(runs: List<RunData>): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        return runs
            .filter { it.startTime != null && it.startTime!! >= startOfDay }
            .sumOf { it.distance }
    }
}

