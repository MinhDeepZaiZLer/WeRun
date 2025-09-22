package com.example.werun.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.werun.presentation.auth.SignUpViewModel
import com.example.werun.ui.components.AuthTextField
import com.example.werun.ui.components.SocialLoginSection
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBackClicked: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    // Listen for events from the ViewModel
    LaunchedEffect(key1 = Unit) {
        viewModel.uiState.collectLatest { state ->
            if (state.signUpSuccess) {
                onSignUpSuccess()
            }
            if (state.signUpError != null) {
                Toast.makeText(context, state.signUpError, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = { AuthTopBar(title = "Create an account", onBackClicked = onBackClicked) },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- FORM FIELDS ---
                AuthTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = "Full Name",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Full Name") }
                )
                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Field
                AuthTextField(
                    value = uiState.phoneNumber,
                    onValueChange = viewModel::onPhoneNumberChange,
                    label = "Phone Number",
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Date of Birth Field - Clickable to open dialog
                Box {
                    AuthTextField(
                        value = uiState.dob,
                        onValueChange = { }, // Not directly editable
                        label = "Date of Birth",
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Date of Birth") },
                        readOnly = true
                    )
                    // This Box makes the entire text field area clickable
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0f) // Invisible
                            .clickable { showDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    // ... (rest of password field is the same)
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(16.dp))

                AuthTextField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirm Password",
                    // ... (rest of confirm password field is the same)
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                    trailingIcon = {
                        IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle confirm password visibility"
                            )
                        }
                    },
                    visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = uiState.passwordMismatchError,
                    supportingText = {
                        if (uiState.passwordMismatchError) {
                            Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ... (Button and Social Login remain the same)
                val isButtonEnabled = uiState.fullName.isNotBlank() &&
                        uiState.email.isNotBlank() &&
                        uiState.password.isNotBlank() &&
                        !uiState.passwordMismatchError

                Button(
                    onClick = viewModel::onSignUpClicked,
                    enabled = isButtonEnabled && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFADFF2F),
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Text("Join Us", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
                SocialLoginSection()
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        // Convert selected date to a displayable format
        val selectedDate = datePickerState.selectedDateMillis?.let {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.format(Date(it))
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        selectedDate?.let {
                            viewModel.onDobChange(it)
                        }
                        showDatePicker = false
                    },
                    // Enable button only when a date is selected
                    enabled = selectedDate != null
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


@Composable
private fun AuthTopBar(title: String, onBackClicked: () -> Unit) {
    // ... (This component remains unchanged)
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier.border(2.dp, Color(0xFFADFF2F), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFADFF2F))
        }
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun SignUpScreenPreview() {
    SignUpScreen(onBackClicked = {}, onSignUpSuccess = {})
}