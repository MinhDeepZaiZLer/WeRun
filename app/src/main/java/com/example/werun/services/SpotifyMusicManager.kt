package com.example.werun.services

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.PlayerState
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpotifyTrack(
    val name: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val imageUri: String? = null
)

class SpotifyMusicManager private constructor() {

    companion object {
        private const val CLIENT_ID = "29bbf28b5be94a6092d1056de2c2a9c6"
        private const val REDIRECT_URI = "werun://callback"
        const val AUTH_REQUEST_CODE = 1337

        @Volatile
        private var INSTANCE: SpotifyMusicManager? = null

        fun getInstance(): SpotifyMusicManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpotifyMusicManager().also { INSTANCE = it }
            }
        }
    }

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var accessToken: String? = null

    // State flows for UI
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<SpotifyTrack?>(null)
    val currentTrack: StateFlow<SpotifyTrack?> = _currentTrack.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _isPaused = MutableStateFlow(true)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /**
     * Bước 1: Authenticate người dùng với Spotify
     * Gọi hàm này từ Activity
     */
    fun authenticate(context: Context) {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )

        builder.setScopes(
            arrayOf(
                "user-read-playback-state",
                "user-modify-playback-state",
                "user-read-currently-playing",
                "streaming",
                "app-remote-control"
            )
        )

        val request = builder.build()
        AuthorizationClient.openLoginActivity(context as Activity?, AUTH_REQUEST_CODE, request)
    }

    /**
     * Bước 2: Xử lý kết quả authentication
     * Gọi từ onActivityResult trong Activity
     */
    fun handleAuthResponse(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    accessToken = response.accessToken
                    _isAuthenticated.value = true
                    Log.d("SpotifyMusicManager", "Authentication successful")
                }
                AuthorizationResponse.Type.ERROR -> {
                    _isAuthenticated.value = false
                    Log.e("SpotifyMusicManager", "Auth error: ${response.error}")
                }
                else -> {
                    _isAuthenticated.value = false
                    Log.d("SpotifyMusicManager", "Auth cancelled or failed")
                }
            }
        }
    }

    /**
     * Bước 3: Connect tới Spotify App Remote sau khi đã authenticate
     */
    fun connect(context: Context, onConnectionResult: (Boolean) -> Unit = {}) {
        if (!_isAuthenticated.value) {
            Log.e("SpotifyMusicManager", "User not authenticated. Call authenticate() first.")
            onConnectionResult(false)
            return
        }

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                _isConnected.value = true
                subscribeToPlayerState()
                Log.d("SpotifyMusicManager", "Connected to Spotify App Remote")
                onConnectionResult(true)
            }

            override fun onFailure(throwable: Throwable) {
                _isConnected.value = false
                Log.e("SpotifyMusicManager", "Failed to connect to Spotify", throwable)
                onConnectionResult(false)
            }
        })
    }

    fun disconnect() {
        playerStateSubscription?.cancel()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        spotifyAppRemote = null
        _isConnected.value = false
        _isPlaying.value = false
        _currentTrack.value = null
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote?.let { remote ->
            playerStateSubscription = remote.playerApi.subscribeToPlayerState().setEventCallback { playerState ->
                updatePlayerState(playerState)
            }
        }
    }

    private fun updatePlayerState(playerState: PlayerState) {
        _isPlaying.value = !playerState.isPaused
        _isPaused.value = playerState.isPaused
        _playbackPosition.value = playerState.playbackPosition

        playerState.track?.let { track ->
            _currentTrack.value = SpotifyTrack(
                name = track.name,
                artist = track.artist.name,
                album = track.album.name,
                duration = track.duration,
                uri = track.uri,
                imageUri = track.imageUri?.raw
            )
        }
    }

    // Playback control functions
    fun play() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun skipNext() {
        spotifyAppRemote?.playerApi?.skipNext()
    }

    fun skipPrevious() {
        spotifyAppRemote?.playerApi?.skipPrevious()
    }

    fun playTrack(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
    }

    fun playPlaylist(playlistUri: String) {
        spotifyAppRemote?.playerApi?.play(playlistUri)
    }

    fun togglePlayPause() {
        if (_isPaused.value) {
            play()
        } else {
            pause()
        }
    }

    fun seekTo(positionMs: Long) {
        spotifyAppRemote?.playerApi?.seekTo(positionMs)
    }

    fun getCurrentPlayerState(callback: (PlayerState?) -> Unit) {
        spotifyAppRemote?.playerApi?.playerState?.setResultCallback { playerState ->
            callback(playerState)
        }
    }

    fun getRunningPlaylists(): List<String> {
        return listOf(
            "spotify:playlist:37i9dQZF1DX76Wlfdnj7AP", // Beast Mode
            "spotify:playlist:37i9dQZF1DWSJHnPb1f0X3", // Cardio
            "spotify:playlist:37i9dQZF1DX32NsLKyzScr", // Power Workout
            "spotify:playlist:37i9dQZF1DX70RN3TfWWJh", // Energy Booster
        )
    }
}