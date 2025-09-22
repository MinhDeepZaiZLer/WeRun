package com.example.werun.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val loginSuccess: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginClicked(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginError = null) }

            try {
                val email = _uiState.value.username.trim()
                val password = _uiState.value.password

                if (email.isBlank() || password.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, loginError = "Email and password are required") }
                    return@launch
                }

                // Đăng nhập với Firebase Authentication
                auth.signInWithEmailAndPassword(email, password).await()
                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                onLoginSuccess() // Gọi callback khi đăng nhập thành công

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, loginError = e.message ?: "Login failed") }
            }
        }
    }
}