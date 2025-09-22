package com.example.werun.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.werun.ui.components.NavigationDrawerContent
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(navController: NavController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                navController = navController,
                drawerState = drawerState
            )
        }
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                HomeTopBar(
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                RunControls(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onMenuClick: () -> Unit = {}) {
    TopAppBar(
        title = {
            Text(
                text = "WeRun",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.Black
                )
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(68.dp)) // Placeholder
        }
    )
}

@Composable
fun RunControls(navController: NavController) {
    val accentColor = Color(0xFFC4FF53) // Vibrant lime green

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF424242), shape = RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            borderColor = accentColor,
            onClick = { navController.navigate("settings") } // Điều hướng đến settings
        )

        StartRunButton(
            navController = navController,
            backgroundColor = accentColor
        )

        RoundIconButton(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Music",
            borderColor = accentColor,
            onClick = { /* TODO: Open music */ }
        )
    }
}

@Composable
fun RoundIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    borderColor: Color,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .border(BorderStroke(3.dp, borderColor), CircleShape)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun StartRunButton(
    navController: NavController,
    backgroundColor: Color,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = { navController.navigate("run") }, // Sử dụng route "run" thay vì AuthScreen.Run.route
        modifier = Modifier.size(90.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = "START",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenPreview() {
    HomeScreen(navController = rememberNavController())
}