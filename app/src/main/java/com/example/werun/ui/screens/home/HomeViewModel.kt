
package com.example.werun.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.User
import com.example.werun.data.repository.HomeRepository
import com.example.werun.data.repository.HomeStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val stats: HomeStats = HomeStats(),
    val error: String? = null,
    val motivationalMessage: String = "Push harder than yesterday!"
)

sealed class HomeUiEvent {
    object StartRun : HomeUiEvent()
    object OpenSettings : HomeUiEvent()
    object OpenMusic : HomeUiEvent()
    object RefreshStats : HomeUiEvent()
}

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadRunStats()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.StartRun -> handleStartRun()
            is HomeUiEvent.OpenSettings -> handleOpenSettings()
            is HomeUiEvent.OpenMusic -> handleOpenMusic()
            is HomeUiEvent.RefreshStats -> {
                loadUserData()
                loadRunStats()
            }
        }
    }



    private fun loadUserData() {
        viewModelScope.launch {
            try {
                repository.getCurrentUser()
                    .catch { e ->
                        Log.e("HomeViewModel", "User load error: ${e.message}", e)
                        _uiState.update { it.copy(error = e.message) }
                    }
                    .collect { user ->
                        _uiState.update { it.copy(user = user) }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "User data launch error: ${e.message}", e)
            }
        }
    }

    private fun loadRunStats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.getUserRunStats()
                    .catch { e ->
                        Log.e("HomeViewModel", "Stats load error: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Unknown error occurred"
                            )
                        }
                    }
                    .collect { stats ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                stats = stats,
                                motivationalMessage = getMotivationalMessage(stats)
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Stats launch error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun getMotivationalMessage(stats: HomeStats): String {
        return when {
            stats.consecutiveDays >= 7 -> "Amazing streak! Keep it up! 🔥"
            stats.consecutiveDays >= 3 -> "Push harder than yesterday!"
            stats.totalDistance > 10000 -> "Great distance covered!"
            stats.goalProgress >= 1f -> "Goal achieved! Ready for more?"
            stats.goalProgress >= 0.5f -> "Halfway there! Keep going!"
            else -> "Let's start your journey!"
        }
    }

    private fun handleStartRun() {
        // Navigation handled in UI layer
    }

    private fun handleOpenSettings() {
        // Navigation handled in UI layer
    }

    private fun handleOpenMusic() {
        // TODO: Implement music logic
    }
}
