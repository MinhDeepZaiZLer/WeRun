package com.example.werun.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val dob: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val passwordMismatchError: Boolean = false,
    val isLoading: Boolean = false,
    val signUpError: String? = null,
    val signUpSuccess: Boolean = false
)

class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // --- Event Handlers for UI ---
    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPhoneNumberChange(phone: String) {
        _uiState.update { it.copy(phoneNumber = phone) }
    }

    fun onDobChange(dob: String) {
        _uiState.update { it.copy(dob = dob) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { currentState ->
            currentState.copy(
                password = password,
                passwordMismatchError = password != currentState.confirmPassword && currentState.confirmPassword.isNotEmpty()
            )
        }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { currentState ->
            currentState.copy(
                confirmPassword = confirmPassword,
                passwordMismatchError = currentState.password != confirmPassword
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onSignUpClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, signUpError = null) }

            try {
                // Kiểm tra các trường đầu vào
                val email = _uiState.value.email.trim()
                val password = _uiState.value.password
                val fullName = _uiState.value.fullName.trim()
                val phoneNumber = _uiState.value.phoneNumber.trim()
                val dob = _uiState.value.dob

                if (email.isBlank() || password.isBlank() || fullName.isBlank() || phoneNumber.isBlank() || dob.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, signUpError = "All fields are required") }
                    return@launch
                }

                // Tạo tài khoản với Firebase Authentication
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("User ID not found")

                // Lưu thông tin người dùng vào Firestore
                val user = User(uid = uid, email = email, fullName = fullName, phoneNumber = phoneNumber, dob = dob)
                firestore.collection("users").document(uid).set(user).await()

                _uiState.update { it.copy(isLoading = false, signUpSuccess = true) }

            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Sign-up failed", e)
                _uiState.update { it.copy(isLoading = false, signUpError = e.message ?: "Sign-up failed") }
            }
        }
    }
}