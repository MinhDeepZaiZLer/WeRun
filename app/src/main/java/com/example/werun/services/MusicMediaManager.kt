package com.example.werun.services

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CurrentTrack(
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long = 0L
)

class MediaMusicManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: MediaMusicManager? = null

        fun getInstance(): MediaMusicManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaMusicManager().also { INSTANCE = it }
            }
        }
    }

    private var mediaController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null

    // State flows for UI
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<CurrentTrack?>(null)
    val currentTrack: StateFlow<CurrentTrack?> = _currentTrack.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _canControl = MutableStateFlow(false)
    val canControl: StateFlow<Boolean> = _canControl.asStateFlow()

    // Callback để lắng nghe thay đổi
    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state?.let {
                _isPlaying.value = it.state == PlaybackState.STATE_PLAYING
                _playbackPosition.value = it.position
                Log.d("MediaMusicManager", "Playback state: ${it.state}")
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata?.let {
                val track = CurrentTrack(
                    title = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                    artist = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                    album = it.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown",
                    duration = it.getLong(MediaMetadata.METADATA_KEY_DURATION)
                )
                _currentTrack.value = track
                Log.d("MediaMusicManager", "Now playing: ${track.title} - ${track.artist}")
            }
        }
    }

    /**
     * Kiểm tra xem app có quyền Notification Listener không
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        if (flat.isNullOrEmpty()) {
            return false
        }
        val names = flat.split(":")
        return names.any {
            val componentName = ComponentName.unflattenFromString(it)
            componentName != null && componentName.packageName == packageName
        }
    }

    /**
     * Khởi tạo và kết nối với media session đang active
     */
    fun initialize(context: Context) {
        // Kiểm tra quyền notification listener
        if (!isNotificationListenerEnabled(context)) {
            Log.w("MediaMusicManager", "Notification Listener not enabled")
            _canControl.value = false
            return
        }

        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

        try {
            // Lấy danh sách active sessions
            val sessions = mediaSessionManager?.getActiveSessions(
                ComponentName(context, NotificationListener::class.java)
            )

            if (sessions.isNullOrEmpty()) {
                Log.w("MediaMusicManager", "No active media sessions found")
                Log.w("MediaMusicManager", "Please play music from any music app")
                _canControl.value = false
                return
            }

            // Lấy session đầu tiên (thường là app đang phát nhạc)
            mediaController = sessions.firstOrNull()

            mediaController?.let { controller ->
                controller.registerCallback(callback)
                _canControl.value = true

                // Cập nhật trạng thái hiện tại
                updateCurrentState(controller)

                Log.d("MediaMusicManager", "Connected to: ${controller.packageName}")
            }

        } catch (e: SecurityException) {
            Log.e("MediaMusicManager", "Permission denied. Need NOTIFICATION_LISTENER permission", e)
            _canControl.value = false
        }
    }

    private fun updateCurrentState(controller: MediaController) {
        // Cập nhật metadata
        controller.metadata?.let { metadata ->
            val track = CurrentTrack(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown",
                duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            )
            _currentTrack.value = track
        }

        // Cập nhật playback state
        controller.playbackState?.let { state ->
            _isPlaying.value = state.state == PlaybackState.STATE_PLAYING
            _playbackPosition.value = state.position
        }
    }

    /**
     * Phát/tiếp tục nhạc
     */
    fun play() {
        mediaController?.transportControls?.play()
    }

    /**
     * Tạm dừng nhạc
     */
    fun pause() {
        mediaController?.transportControls?.pause()
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    /**
     * Bài tiếp theo
     */
    fun skipNext() {
        mediaController?.transportControls?.skipToNext()
    }

    /**
     * Bài trước
     */
    fun skipPrevious() {
        mediaController?.transportControls?.skipToPrevious()
    }

    /**
     * Tua đến vị trí cụ thể (ms)
     */
    fun seekTo(positionMs: Long) {
        mediaController?.transportControls?.seekTo(positionMs)
    }

    /**
     * Ngắt kết nối
     */
    fun disconnect() {
        mediaController?.unregisterCallback(callback)
        mediaController = null
        _canControl.value = false
        _isPlaying.value = false
        _currentTrack.value = null
    }

    /**
     * Kiểm tra xem có đang phát nhạc không
     */
    fun isCurrentlyPlaying(): Boolean {
        return _isPlaying.value
    }

    /**
     * Lấy tên package của app đang phát nhạc
     */
    fun getCurrentMusicApp(): String? {
        return mediaController?.packageName
    }
}