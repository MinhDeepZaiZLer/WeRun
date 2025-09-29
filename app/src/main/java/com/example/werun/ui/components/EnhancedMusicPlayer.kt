package com.example.werun.ui.components

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.werun.services.CurrentTrack
import com.example.werun.services.MediaMusicManager

@Composable
fun EnhancedMusicPlayer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFD0FD3E),
    textColor: Color = Color.Black,
    onMusicControlClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val musicManager = remember { MediaMusicManager.getInstance() }
    val isPlaying by musicManager.isPlaying.collectAsState()
    val currentTrack by musicManager.currentTrack.collectAsState()
    val canControl by musicManager.canControl.collectAsState()

    var showMusicDialog by remember { mutableStateOf(false) }

    // Launcher để mở cài đặt Notification Listener
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Kiểm tra lại quyền sau khi quay lại từ Settings
        musicManager.initialize(context)
    }

    // Khởi tạo MediaMusicManager
    LaunchedEffect(Unit) {
        musicManager.initialize(context)
        if (!musicManager.isNotificationListenerEnabled(context)) {
            showMusicDialog = true // Hiển thị dialog yêu cầu quyền
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("MediaMusicPlayer", "EnhancedMusicPlayer disposed")
            musicManager.disconnect()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable {
                Log.d("MediaMusicPlayer", "Card nhạc được nhấn")
                onMusicControlClick()
                showMusicDialog = true
            }
    ) {
        when {
            !musicManager.isNotificationListenerEnabled(context) -> {
                // Hiển thị thông báo yêu cầu quyền
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Yêu cầu quyền",
                        modifier = Modifier.size(24.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yêu cầu quyền truy cập thông báo",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = textColor
                    )
                }
            }
            !canControl -> {
                // Hiển thị trạng thái không có media session
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Nhạc",
                        modifier = Modifier.size(24.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Không có ứng dụng nhạc đang chạy",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = textColor
                    )
                }
            }
            currentTrack != null -> {
                // Hiển thị thông tin bài hát
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { musicManager.skipPrevious() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Bài trước",
                            modifier = Modifier.size(28.dp),
                            tint = textColor
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentTrack!!.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentTrack!!.artist,
                            fontSize = 12.sp,
                            color = textColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            Log.d("MediaMusicPlayer", "Nhấn Play/Pause, isPlaying=$isPlaying")
                            musicManager.togglePlayPause()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                            modifier = Modifier.size(32.dp),
                            tint = textColor
                        )
                    }

                    IconButton(
                        onClick = { musicManager.skipNext() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Bài tiếp theo",
                            modifier = Modifier.size(28.dp),
                            tint = textColor
                        )
                    }
                }
            }
            else -> {
                // Hiển thị trạng thái không có bài hát
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Nhạc",
                        modifier = Modifier.size(24.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Không có nhạc đang phát",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = textColor
                    )
                }
            }
        }
    }

    if (showMusicDialog) {
        MusicControlDialog(
            musicManager = musicManager,
            onRequestPermission = {
                permissionLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onDismiss = { showMusicDialog = false }
        )
    }
}

@Composable
fun MusicControlDialog(
    musicManager: MediaMusicManager,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentTrack by musicManager.currentTrack.collectAsState()
    val isPlaying by musicManager.isPlaying.collectAsState()
    val canControl by musicManager.canControl.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Điều khiển Nhạc", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (!musicManager.isNotificationListenerEnabled(LocalContext.current)) {
                    // Yêu cầu quyền Notification Listener
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Yêu cầu quyền",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Yêu cầu quyền truy cập thông báo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Vui lòng bật quyền truy cập thông báo để điều khiển nhạc.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRequestPermission) {
                            Text("Mở cài đặt quyền")
                        }
                    }
                } else if (!canControl) {
                    // Không có media session
                    Text(
                        text = "Không có ứng dụng nhạc đang chạy",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                } else if (currentTrack != null) {
                    // Hiển thị thông tin bài hát
                    Column {
                        Text(
                            text = currentTrack!!.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = currentTrack!!.artist,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                Log.d("MediaMusicPlayer", "Nhấn Skip Previous trong dialog")
                                musicManager.skipPrevious()
                            }) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "Bài trước",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(onClick = {
                                Log.d("MediaMusicPlayer", "Nhấn Play/Pause trong dialog, isPlaying=$isPlaying")
                                musicManager.togglePlayPause()
                            }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            IconButton(onClick = {
                                Log.d("MediaMusicPlayer", "Nhấn Skip Next trong dialog")
                                musicManager.skipNext()
                            }) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "Bài tiếp theo",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Không có bài hát
                    Text(
                        text = "Chưa có bài hát nào được tải",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )
}