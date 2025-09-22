package com.example.werun.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.werun.ui.screens.auth.AuthWelcomeScreen
import com.example.werun.ui.screens.auth.LoginScreen
import com.example.werun.ui.screens.auth.SignUpScreen
import com.example.werun.ui.screens.friends.FriendProfileScreen
import com.example.werun.ui.screens.friends.FriendsScreen
import com.example.werun.ui.screens.friends.SearchFriendsScreen
import com.example.werun.ui.screens.history.RunningHistoryScreen
import com.example.werun.ui.screens.home.HomeScreen
import com.example.werun.ui.screens.run.MapScreen
import com.example.werun.ui.screens.run.RunScreen
import com.example.werun.data.repository.UserRepository
import com.example.werun.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun WeRunNavGraph(
    navController: NavHostController,
    userRepository: UserRepository
) {
    // Check authentication state on startup
    LaunchedEffect(Unit) {
        if (userRepository.isAuthenticated()) {
            navController.navigate("home") {
                popUpTo("auth") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            AuthWelcomeScreen(
                onJoinUsClicked = { navController.navigate("register") },
                onLoginClicked = { navController.navigate("login") }
            )
        }
        composable("login") {
            LoginScreen(
                onBackClicked = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            SignUpScreen(
                onBackClicked = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
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
            ProfileScreen(onBackClicked = { navController.popBackStack() })
        }
        composable("friends") {
            FriendsScreen(navController = navController)
        }
        composable("statistics") {
            // TODO: Tạo StatisticsScreen
            Text("Statistics Screen (To be implemented)")
        }
        composable("settings") {
            // TODO: Tạo SettingsScreen
            Text("Settings Screen (To be implemented)")
        }
        composable("search_friends") {
            SearchFriendsScreen(navController = navController)
        }
        composable(
            "friend_profile/{friendId}",
            arguments = listOf(navArgument("friendId") { type = NavType.StringType })
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            FriendProfileScreen(
                friendId = friendId,
                navController = navController
            )
        }
    }

    // Handle logout navigation (listen for "logout" event)
    LaunchedEffect(Unit) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            backStackEntry.savedStateHandle.getLiveData<Boolean>("logout").observeForever { loggedOut ->
                if (loggedOut == true) {
                    userRepository.logout() // Sign out from Firebase/Auth
                    navController.navigate("auth") {
                        popUpTo("auth") { inclusive = true }
                    }
                    backStackEntry.savedStateHandle.remove<Boolean>("logout")
                }
            }
        }
    }
}