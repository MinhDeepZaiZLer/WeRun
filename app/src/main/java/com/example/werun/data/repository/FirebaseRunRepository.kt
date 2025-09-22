package com.example.werun.data.repository

import com.example.werun.data.model.RunData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRunRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private val runsCollection = firestore.collection("runs")

    suspend fun saveRun(runData: RunData): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val runWithUserId = runData.copy(userId = userId)
            val documentRef = runsCollection.add(runWithUserId).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRun(runId: String, runData: RunData): Result<Unit> {
        return try {
            runsCollection.document(runId).set(runData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRun(runId: String): Result<Unit> {
        return try {
            runsCollection.document(runId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRun(runId: String): Result<RunData?> {
        return try {
            val document = runsCollection.document(runId).get().await()
            val runData = document.toObject(RunData::class.java)
            Result.success(runData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserRuns(limit: Int = 50): Flow<List<RunData>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            val snapshot = runsCollection
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING) // Thay đổi sang startTime
                .limit(limit.toLong())
                .get()
                .await()
            val runs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(RunData::class.java)?.copy(id = doc.id)
            }
            emit(runs)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getUserRunsCount(): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val snapshot = runsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTotalDistance(): Result<Double> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val snapshot = runsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val totalDistance = snapshot.documents.sumOf { doc ->
                doc.getDouble("distance") ?: 0.0
            }
            Result.success(totalDistance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTotalDuration(): Result<Long> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val snapshot = runsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val totalDuration = snapshot.documents.sumOf { doc ->
                doc.getLong("duration") ?: 0L
            }
            Result.success(totalDuration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}