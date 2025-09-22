package com.example.werun.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.werun.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawerContent(
    navController: NavController,
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = Color.White
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC4FF53)) // Vibrant lime green
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher), // Thay bằng ảnh đại diện nếu có
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.medium)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user?.displayName ?: "User Name",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
            Text(
                text = user?.email ?: "user@example.com",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Menu Items
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigate("profile") // Cần định nghĩa route trong NavGraph
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.History, contentDescription = "Run History") },
            label = { Text("Run History") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigate("run_history") // Sử dụng route khác để tránh xung đột
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Group, contentDescription = "Friends") },
            label = { Text("Friends") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigate("friends") // Cần định nghĩa route trong NavGraph
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Statistics") },
            label = { Text("Statistics") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigate("statistics") // Cần định nghĩa route trong NavGraph
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = false,
            onClick = {
                scope.launch { drawerState.close() }
                navController.navigate("settings") // Cần định nghĩa route trong NavGraph
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out") },
            label = { Text("Sign Out") },
            selected = false,
            onClick = {
                scope.launch {
                    drawerState.close()
                    FirebaseAuth.getInstance().signOut()
                    // Quay về HomeScreen hoặc kết thúc ứng dụng
                    navController.navigate("auth") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}