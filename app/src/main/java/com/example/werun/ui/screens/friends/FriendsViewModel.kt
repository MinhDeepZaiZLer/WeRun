package com.example.werun.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.FriendRequest
import com.example.werun.data.Friendship
import com.example.werun.data.User
import com.example.werun.data.repository.FriendsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<Friendship>>(emptyList())
    val friends: StateFlow<List<Friendship>> = _friends.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _suggestedFriends = MutableStateFlow<List<User>>(emptyList())
    val suggestedFriends: StateFlow<List<User>> = _suggestedFriends.asStateFlow()

    private val _nearbyRunners = MutableStateFlow<List<User>>(emptyList())
    val nearbyRunners: StateFlow<List<User>> = _nearbyRunners.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendsRepository.getFriends().collect { friendsList ->
                    _friends.value = friendsList
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFriendRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendsRepository.getFriendRequests().collect { requests ->
                    _friendRequests.value = requests
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSuggestedFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendsRepository.getSuggestedFriends().collect { suggestions ->
                    _suggestedFriends.value = suggestions
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNearbyRunners(currentLat: Double, currentLng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = friendsRepository.getNearbyRunners(currentLat, currentLng)
                if (result.isSuccess) {
                    _nearbyRunners.value = result.getOrNull() ?: emptyList()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendFriendRequest(toUserId: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.sendFriendRequest(toUserId)
                if (result.isSuccess) {
                    // Remove from suggested friends and nearby runners
                    _suggestedFriends.value = _suggestedFriends.value.filter { it.uid != toUserId }
                    _nearbyRunners.value = _nearbyRunners.value.filter { it.uid != toUserId }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.acceptFriendRequest(requestId)
                if (result.isSuccess) {
                    // Remove from friend requests
                    _friendRequests.value = _friendRequests.value.filter { it.id != requestId }
                    // Reload friends list
                    loadFriends()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun rejectFriendRequest(requestId: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.rejectFriendRequest(requestId)
                if (result.isSuccess) {
                    // Remove from friend requests
                    _friendRequests.value = _friendRequests.value.filter { it.id != requestId }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun removeFriend(friendUid: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.removeFriend(friendUid)
                if (result.isSuccess) {
                    loadFriends()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                val result = friendsRepository.searchUsers(query)
                if (result.isSuccess) {
                    _suggestedFriends.value = result.getOrNull() ?: emptyList()
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSearchResults() {
        _suggestedFriends.value = emptyList()
    }
}