package com.example.werun.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.werun.data.User

@Composable
fun WerunTopBar(title: String, onBackClicked: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClicked) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), content = actions, horizontalArrangement = Arrangement.End)
    }
}

@Composable
fun ProfileScreen(
    onBackClicked: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            WerunTopBar(
                title = "Profile",
                onBackClicked = onBackClicked,
                actions = {
                    if (uiState.user != null && !uiState.isEditing) {
                        IconButton(onClick = viewModel::startEditing) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    } else if (uiState.isEditing) {
                        IconButton(onClick = viewModel::saveProfile) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                        IconButton(onClick = viewModel::cancelEditing) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.errorMessage != null) {
                    Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                } else if (uiState.user == null) {
                    Text("User data not available", color = MaterialTheme.colorScheme.error)
                } else {
                    ProfileContent(uiState, viewModel)
                }
            }
        }
    )
}

@Composable
fun ProfileContent(uiState: ProfileUiState, viewModel: ProfileViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = uiState.user?.fullName ?: "Unknown User",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (!uiState.isEditing && uiState.user != null) {
            Button(onClick = { showDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
        }

        // Display Profile Details
        ProfileDetails(uiState.user)

        // Edit Dialog
        if (showDialog) {
            EditProfileDialog(uiState, viewModel) { showDialog = false }
        }
    }
}

@Composable
fun ProfileDetails(user: User?) {
    Column {
        Text("Email: ${user?.email ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
        Text("Phone: ${user?.phoneNumber ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
        Text("DOB: ${user?.dob ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
        Text("Address: ${user?.address ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
        Text("Gender: ${user?.gender ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
        Text("Public Profile: ${if (user?.public == true) "Yes" else "No"}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun EditProfileDialog(uiState: ProfileUiState, viewModel: ProfileViewModel, onDismiss: () -> Unit) {
    var fullName by remember { mutableStateOf(uiState.editedUser?.fullName ?: "") }
    var phoneNumber by remember { mutableStateOf(uiState.editedUser?.phoneNumber ?: "") }
    var dob by remember { mutableStateOf(uiState.editedUser?.dob ?: "") }
    var address by remember { mutableStateOf(uiState.editedUser?.address ?: "") }
    var gender by remember { mutableStateOf(uiState.editedUser?.gender ?: "") }
    var isPublic by remember { mutableStateOf(uiState.editedUser?.public ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Gender") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Public Profile", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateField("fullName", fullName)
                    viewModel.updateField("phoneNumber", phoneNumber)
                    viewModel.updateField("dob", dob)
                    viewModel.updateField("address", address)
                    viewModel.updateField("gender", gender)
                    viewModel.updateField("public", isPublic.toString())
                    viewModel.saveProfile()
                    onDismiss()
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                Text("Cancel")
            }
        }
    )
}