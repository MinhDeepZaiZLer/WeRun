package com.example.werun.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.User
import com.example.werun.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val uid = userRepository.currentUserId
                if (uid == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "User not authenticated") }
                    return@launch
                }
                val user = userRepository.getCurrentUser()
                Log.d("ProfileViewModel", "Loaded user: $user")
                _uiState.update {
                    it.copy(
                        user = user,
                        editedUser = user,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to load profile", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load profile: ${e.message}"
                    )
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, editedUser = it.user) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateField(field: String, value: String) {
        _uiState.update { state ->
            state.copy(
                editedUser = state.editedUser?.let { user ->
                    when (field) {
                        "fullName" -> user.copy(fullName = value)
                        "phoneNumber" -> user.copy(phoneNumber = value)
                        "dob" -> user.copy(dob = value)
                        "address" -> user.copy(address = value)
                        "gender" -> user.copy(gender = value)
                        "public" -> user.copy(public = value.toBoolean())
                        else -> user
                    }
                }
            )
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updatedUser = _uiState.value.editedUser ?: throw Exception("No user data to save")
                userRepository.updateUser(updatedUser)
                _uiState.update { it.copy(user = updatedUser, isEditing = false, isLoading = false) }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to save profile", e)
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to save profile: ${e.message}") }
            }
        }
    }
}

data class ProfileUiState(
    val user: User? = null,
    val editedUser: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null
)