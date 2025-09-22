package com.example.werun.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.werun.ui.screens.history.RunningHistoryScreen
import com.example.werun.ui.screens.home.HomeScreen
import com.example.werun.ui.screens.run.MapScreen
import com.example.werun.ui.screens.run.RunScreen

@Composable
fun WeRunNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("run") {
            RunScreen(navController = navController)
        }
        composable("map") {
            MapScreen(navController = navController)
        }
        composable("run_history") {
            RunningHistoryScreen()
        }
        composable("profile") {
            // TODO: Tạo ProfileScreen
            Text("Profile Screen (To be implemented)")
        }
        composable("friends") {
            // TODO: Tạo FriendsScreen
            Text("Friends Screen (To be implemented)")
        }
        composable("statistics") {
            // TODO: Tạo StatisticsScreen
            Text("Statistics Screen (To be implemented)")
        }
        composable("settings") {
            // TODO: Tạo SettingsScreen
            Text("Settings Screen (To be implemented)")
        }
        composable("auth") {
            // TODO: Tạo AuthScreen
            Text("Auth Screen (To be implemented)")
        }
    }
}