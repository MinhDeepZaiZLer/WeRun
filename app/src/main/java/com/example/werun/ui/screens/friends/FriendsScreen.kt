package com.example.werun.ui.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.werun.data.User
import com.example.werun.data.Friendship
import com.example.werun.data.FriendRequest
import kotlinx.coroutines.launch

// Colors
val primaryGreen = Color(0xFF4CAF50)
val lightGreen = Color(0xFFE8F5E9)
val textPrimary = Color(0xFF212121)
val textSecondary = Color(0xFF757575)
val cardBackground = Color(0xFFFAFAFA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navController: NavController,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Bạn bè", "Lời mời", "Tìm bạn")
    val friends by viewModel.friends.collectAsState()
    val friendRequests by viewModel.friendRequests.collectAsState()
    val suggestedFriends by viewModel.suggestedFriends.collectAsState()
    val nearbyRunners by viewModel.nearbyRunners.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadFriends()
        viewModel.loadFriendRequests()
        viewModel.loadSuggestedFriends()
        // Load current user's last run location
        // TODO: Fetch from Firestore or LocationService
        viewModel.loadNearbyRunners(currentLat = 10.7769, currentLng = 106.7009) // Ho Chi Minh City
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    "Bạn bè",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            actions = {
                IconButton(onClick = { navController.navigate("search_friends") }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { showBottomSheet = true }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = primaryGreen
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> FriendsListContent(
                friends = friends,
                isLoading = isLoading,
                onFriendClick = { friend ->
                    navController.navigate("friend_profile/${friend.uid}")
                },
                onMessageClick = { friend ->
                    // Navigate to chat
                }
            )
            1 -> FriendRequestsContent(
                requests = friendRequests,
                isLoading = isLoading,
                onAcceptRequest = { request ->
                    viewModel.acceptFriendRequest(request.id)
                },
                onRejectRequest = { request ->
                    viewModel.rejectFriendRequest(request.id)
                }
            )
            2 -> SuggestedFriendsContent(
                suggestedFriends = suggestedFriends,
                isLoading = isLoading,
                onSendRequest = { user ->
                    viewModel.sendFriendRequest(user.uid)
                }
            )
        }
    }

    // Bottom Sheet for Nearby Runners
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Người chạy gần bạn",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (isLoading) {
                    repeat(5) {
                        SearchResultSkeleton()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (nearbyRunners.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.LocationOn,
                        title = "Không tìm thấy",
                        description = "Không có người chạy nào gần vị trí của bạn."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(nearbyRunners) { user ->
                            SuggestedFriendCard(
                                user = user,
                                onSendRequest = {
                                    viewModel.sendFriendRequest(user.uid)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch { bottomSheetState.hide() }
                        showBottomSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                ) {
                    Text("Đóng", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun FriendsListContent(
    friends: List<Friendship>,
    isLoading: Boolean,
    onFriendClick: (User) -> Unit,
    onMessageClick: (User) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (friends.isEmpty() && !isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Group,
                    title = "Chưa có bạn bè",
                    description = "Hãy thêm bạn bè để cùng chạy và thi đấu nhé!"
                )
            }
        }

        items(friends) { friendship ->
            FriendCard(
                friendship = friendship,
                onFriendClick = onFriendClick,
                onMessageClick = onMessageClick
            )
        }

        if (isLoading) {
            items(5) {
                FriendCardSkeleton()
            }
        }
    }
}

@Composable
fun FriendRequestsContent(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    onAcceptRequest: (FriendRequest) -> Unit,
    onRejectRequest: (FriendRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (requests.isEmpty() && !isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.PersonAdd,
                    title = "Không có lời mời",
                    description = "Bạn chưa có lời mời kết bạn nào"
                )
            }
        }

        items(requests) { request ->
            FriendRequestCard(
                request = request,
                onAccept = { onAcceptRequest(request) },
                onReject = { onRejectRequest(request) }
            )
        }
    }
}

@Composable
fun SuggestedFriendsContent(
    suggestedFriends: List<User>,
    isLoading: Boolean,
    onSendRequest: (User) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Gợi ý kết bạn",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (suggestedFriends.isEmpty() && !isLoading) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.PersonSearch,
                    title = "Không có gợi ý",
                    description = "Hiện tại chưa có gợi ý kết bạn nào"
                )
            }
        }

        items(suggestedFriends.chunked(2)) { friendsRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                friendsRow.forEach { user ->
                    SuggestedFriendCard(
                        user = user,
                        onSendRequest = { onSendRequest(user) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendCard(
    friendship: Friendship,
    onFriendClick: (User) -> Unit,
    onMessageClick: (User) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFriendClick(friendship.user2) },
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(primaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    friendship.user2.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friendship.user2.fullName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Text(
                    "Last active: ${formatDate(friendship.lastActivity)}",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            // Actions
            Row {
                IconButton(
                    onClick = { onMessageClick(friendship.user2) }
                ) {
                    Icon(
                        Icons.Default.Message,
                        contentDescription = "Message",
                        tint = primaryGreen
                    )
                }

                IconButton(
                    onClick = { /* More options */ }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun FriendRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(primaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        request.fromUser.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // User info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.fromUser.fullName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        "Muốn kết bạn với bạn",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryGreen
                    )
                ) {
                    Text("Chấp nhận", color = Color.White)
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textSecondary
                    )
                ) {
                    Text("Từ chối")
                }
            }
        }
    }
}

@Composable
fun SuggestedFriendCard(
    user: User,
    onSendRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(primaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.fullName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                user.fullName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Add friend button
            Button(
                onClick = onSendRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryGreen
                ),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Kết bạn",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = lightGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = primaryGreen.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                description,
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FriendCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.2f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}



private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days == 0L -> "Hôm nay"
        days == 1L -> "Hôm qua"
        days < 7 -> "$days ngày trước"
        days < 30 -> "${days / 7} tuần trước"
        else -> "${days / 30} tháng trước"
    }
}