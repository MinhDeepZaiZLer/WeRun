package com.example.werun.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.werun.data.User
import com.example.werun.data.model.RunData
import com.example.werun.ui.screens.run.formatDuration

// Sample data class for friend's stats
data class FriendStats(
    val totalRuns: Int = 0,
    val totalDistance: Double = 0.0,
    val totalTime: Long = 0L,
    val averageSpeed: Double = 0.0,
    val bestTime: Long = 0L,
    val longestRun: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileScreen(
    friendId: String,
    navController: NavController,
    viewModel: FriendProfileViewModel = hiltViewModel()
) {
    val friend by viewModel.friend.collectAsState()
    val friendStats by viewModel.friendStats.collectAsState()
    val recentRuns by viewModel.recentRuns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showRemoveFriendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(friendId) {
        viewModel.loadFriendProfile(friendId)
        viewModel.loadFriendStats(friendId)
        viewModel.loadRecentRuns(friendId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(friend?.fullName ?: "") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showRemoveFriendDialog = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        if (isLoading) {
            ProfileSkeleton()
        } else {
            friend?.let { user ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ProfileHeader(
                            user = user,
                            onMessageClick = {
                                // Navigate to chat
                            },
                            onChallengeClick = {
                                // Send running challenge
                            }
                        )
                    }

                    item {
                        StatsSection(stats = friendStats)
                    }

                    item {
                        Text(
                            "Hoạt động gần đây",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    if (recentRuns.isEmpty()) {
                        item {
                            EmptyRunsState()
                        }
                    } else {
                        items(recentRuns) { run ->
                            RunHistoryItem(run = run)
                        }
                    }
                }
            }
        }
    }

    // Remove Friend Dialog
    if (showRemoveFriendDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFriendDialog = false },
            title = { Text("Xóa bạn bè") },
            text = { Text("Bạn có chắc chắn muốn xóa ${friend?.fullName} khỏi danh sách bạn bè?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeFriend(friendId)
                        showRemoveFriendDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Xóa", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveFriendDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun ProfileHeader(
    user: User,
    onMessageClick: () -> Unit,
    onChallengeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = lightGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(primaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Chưa có hoạt động",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Bạn này chưa có hoạt động chạy bộ nào",
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun ProfileSkeleton() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile header skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(24.dp)
                            .background(
                                Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .background(
                                Color.Gray.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            // Stats skeleton
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(20.dp)
                            .background(
                                Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    repeat(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            repeat(3) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                Color.Gray.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(16.dp)
                                            .background(
                                                Color.Gray.copy(alpha = 0.3f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(12.dp)
                                            .background(
                                                Color.Gray.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                        if (it == 0) Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Helper functions
    fun formatDate(date: java.util.Date?): String {
        if (date == null) return ""

        val now = System.currentTimeMillis()
        val diff = now - date.time
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            days == 0L -> "Hôm nay"
            days == 1L -> "Hôm qua"
            days < 7 -> "$days ngày trước"
            days < 30 -> "${days / 7} tuần trước"
            else -> "${days / 30} tháng trước"
        }
    }

    fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0:00"

        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        val remainingSecs = seconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSecs)
        } else {
            String.format("%d:%02d", remainingMinutes, remainingSecs)
        }
    }



    // Name
    Text(
        user.fullName,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = textPrimary
    )

    // Email
    Text(
        user.email,
        fontSize = 14.sp,
        color = textSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Action Buttons
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onMessageClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryGreen
            )
        ) {
            Icon(
                Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nhắn tin")
        }

        OutlinedButton(
            onClick = onChallengeClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = primaryGreen
            )
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thách đấu")
        }
    }
}


@Composable
fun StatsSection(stats: FriendStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Thống kê",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = Icons.Default.DirectionsRun,
                    value = "${stats.totalRuns}",
                    label = "Lượt chạy"
                )
                StatItem(
                    icon = Icons.Default.Straighten,
                    value = String.format("%.1f km", stats.totalDistance / 1000),
                    label = "Tổng quãng đường"
                )
                StatItem(
                    icon = Icons.Default.Speed,
                    value = String.format("%.1f km/h", stats.averageSpeed),
                    label = "Tốc độ TB"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = Icons.Default.Timer,
                    value = formatDuration(stats.bestTime),
                    label = "Thời gian tốt nhất"
                )
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = String.format("%.1f km", stats.longestRun / 1000),
                    label = "Quãng đường xa nhất"
                )
                StatItem(
                    icon = Icons.Default.Schedule,
                    value = formatDuration(stats.totalTime),
                    label = "Tổng thời gian"
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = primaryGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RunHistoryItem(run: RunData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Run icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Run details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    String.format("%.2f km", run.distance / 1000),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Text(
                    "${formatDuration(run.duration)} • ${String.format("%.1f km/h", run.averageSpeed)}",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            // Date
            Text(
                formatDate(run.startTime),
                fontSize = 12.sp,
                color = textSecondary
            )
        }
    }
}
fun formatDate(date: java.util.Date?): String {
    if (date == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - date.time
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days == 0L -> "Hôm nay"
        days == 1L -> "Hôm qua"
        days < 7 -> "$days ngày trước"
        days < 30 -> "${days / 7} tuần trước"
        else -> "${days / 30} tháng trước"
    }
}

@Composable
fun EmptyRunsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.DirectionsRun,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = textSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chưa có hoạt động chạy bộ",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Người bạn này chưa ghi lại hoạt động chạy bộ nào.",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header skeleton
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(24.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(16.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        // Stats skeleton
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        repeat(3) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            Color.Gray.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(16.dp)
                                        .background(
                                            Color.Gray.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(12.dp)
                                        .background(
                                            Color.Gray.copy(alpha = 0.2f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                    if (it == 0) Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}