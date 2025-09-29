package com.example.werun.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.werun.services.MediaMusicManager

@Composable
fun MusicControlScreen() {
    val context = LocalContext.current
    val musicManager = remember { MediaMusicManager.getInstance() }

    val isPlaying by musicManager.isPlaying.collectAsState()
    val currentTrack by musicManager.currentTrack.collectAsState()
    val canControl by musicManager.canControl.collectAsState()

    // Khởi tạo khi component mount
    LaunchedEffect(Unit) {
        musicManager.initialize(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!canControl) {
            // Hiển thị hướng dẫn enable notification access
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cần cấp quyền Notification Access",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Để điều khiển nhạc, vui lòng bật Notification Access cho app",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Mở settings để enable notification access
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Mở Settings")
                    }
                }
            }
        } else {
            // Hiển thị thông tin bài hát
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentTrack != null) {
                        Text(
                            text = currentTrack!!.title,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = currentTrack!!.artist,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = currentTrack!!.album,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Không có nhạc đang phát",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Điều khiển phát nhạc
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(onClick = { musicManager.skipPrevious() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Play/Pause
                IconButton(
                    onClick = { musicManager.togglePlayPause() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(64.dp)
                    )
                }

                // Next
                IconButton(onClick = { musicManager.skipNext() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hiển thị app đang phát nhạc
            musicManager.getCurrentMusicApp()?.let { packageName ->
                Text(
                    text = "Source: $packageName",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// Sử dụng trong MainActivity
@Composable
fun MainScreen() {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ... Các UI khác của bạn

            // Thêm music control
            MusicControlScreen()
        }
    }
}