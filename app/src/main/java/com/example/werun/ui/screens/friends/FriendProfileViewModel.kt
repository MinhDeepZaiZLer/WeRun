package com.example.werun.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.User
import com.example.werun.data.model.RunData
import com.example.werun.data.repository.FriendsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FriendProfileViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    private val runsRepository: com.example.werun.data.repository.FirebaseRunRepository
) : ViewModel() {

    private val _friend = MutableStateFlow<User?>(null)
    val friend: StateFlow<User?> = _friend.asStateFlow()

    private val _friendStats = MutableStateFlow(FriendStats())
    val friendStats: StateFlow<FriendStats> = _friendStats.asStateFlow()

    private val _recentRuns = MutableStateFlow<List<RunData>>(emptyList())
    val recentRuns: StateFlow<List<RunData>> = _recentRuns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadFriendProfile(friendId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = friendsRepository.getFriendById(friendId)
                if (result.isSuccess) {
                    _friend.value = result.getOrNull()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFriendStats(friendId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement with FirebaseRunRepository
                _friendStats.value = FriendStats(
                    totalRuns = 15,
                    totalDistance = 75000.0, // 75km
                    totalTime = 18000000L, // 5 hours
                    averageSpeed = 12.5,
                    bestTime = 1800000L, // 30 minutes
                    longestRun = 15000.0 // 15km
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadRecentRuns(friendId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement with FirebaseRunRepository
                _recentRuns.value = emptyList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.removeFriend(friendId)
                if (!result.isSuccess) {
                    // Handle error (e.g., set error state)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}