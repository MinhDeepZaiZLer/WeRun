package com.example.werun.ui.screens.history
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.model.RunData
import com.example.werun.data.repository.FirebaseRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: FirebaseRunRepository
) : ViewModel() {

    private val _runDataList = MutableStateFlow<List<RunData>>(emptyList())
    val runDataList: StateFlow<List<RunData>> = _runDataList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    init {
        loadRunHistory()
    }

    fun loadRunHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val runsFlow = repository.getUserRuns()
                runsFlow.collect { runs ->
                    _runDataList.value = runs
                    _totalDistance.value = repository.getUserTotalDistance().getOrDefault(0.0)
                    _totalDuration.value = repository.getUserTotalDuration().getOrDefault(0L)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _runDataList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper extension for Result
    private fun <T> Result<T>.getOrDefault(default: T): T = fold({ it }, { default })
}