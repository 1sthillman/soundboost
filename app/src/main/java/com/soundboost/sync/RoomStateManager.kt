package com.soundboost.sync

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ROOM STATE MANAGER
 * 
 * Oda durumunu yönetir ve sync tutar:
 * - Mevcut playlist
 * - Hangi şarkı çalıyor
 * - Pozisyon nerede
 * - Playback durumu (playing/paused/idle)
 * 
 * KULLANIM:
 * - HOST: State'i günceller, broadcast eder
 * - CLIENT: State'i alır, sync olur
 */
class RoomStateManager {
    
    private val _roomState = MutableStateFlow<RoomState>(
        RoomState(
            playlist = Playlist(),
            playbackState = PlaybackState.IDLE,
            currentPosition = 0,
            lastUpdateTime = System.currentTimeMillis()
        )
    )
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Update room state (HOST only)
     * Includes DJ state for complete sync
     */
    fun updateState(
        playlist: Playlist? = null,
        playbackState: PlaybackState? = null,
        currentPosition: Int? = null,
        djState: DJState? = null
    ) {
        val current = _roomState.value
        _roomState.value = current.copy(
            playlist = playlist ?: current.playlist,
            playbackState = playbackState ?: current.playbackState,
            currentPosition = currentPosition ?: current.currentPosition,
            djState = djState ?: current.djState,
            lastUpdateTime = System.currentTimeMillis()
        )
        
        Log.d(TAG, "📊 Room state updated:")
        Log.d(TAG, "   - Playlist: ${_roomState.value.playlist.tracks.size} tracks")
        Log.d(TAG, "   - Current track: ${_roomState.value.playlist.currentTrackIndex}")
        Log.d(TAG, "   - State: ${_roomState.value.playbackState}")
        Log.d(TAG, "   - Position: ${_roomState.value.currentPosition}ms")
        Log.d(TAG, "   - Vocal balance: ${_roomState.value.djState.vocalBalance}")
    }
    
    /**
     * Load room state from message (CLIENT only)
     */
    fun loadState(roomState: RoomState) {
        _roomState.value = roomState
        
        Log.d(TAG, "📥 Room state loaded:")
        Log.d(TAG, "   - Playlist: ${roomState.playlist.tracks.size} tracks")
        Log.d(TAG, "   - Current track: ${roomState.playlist.currentTrackIndex}")
        Log.d(TAG, "   - State: ${roomState.playbackState}")
        Log.d(TAG, "   - Position: ${roomState.currentPosition}ms")
        Log.d(TAG, "   - Timestamp: ${roomState.lastUpdateTime}")
    }
    
    /**
     * Get current state for broadcast
     */
    fun getCurrentState(): RoomState = _roomState.value
    
    /**
     * Calculate current position based on timestamp
     * Kullanım: Late join - müzik çalıyorsa, elapsed time ekle
     */
    fun calculateCurrentPosition(): Int {
        val state = _roomState.value
        if (state.playbackState != PlaybackState.PLAYING) {
            return state.currentPosition
        }
        
        val elapsed = System.currentTimeMillis() - state.lastUpdateTime
        val calculated = state.currentPosition + elapsed.toInt()
        
        Log.d(TAG, "🕐 Position calculation:")
        Log.d(TAG, "   - Stored position: ${state.currentPosition}ms")
        Log.d(TAG, "   - Elapsed time: ${elapsed}ms")
        Log.d(TAG, "   - Calculated position: ${calculated}ms")
        
        return calculated
    }
    
    /**
     * Add track to playlist (HOST only)
     */
    fun addTrack(track: PlaylistTrack) {
        val current = _roomState.value
        val newPlaylist = current.playlist.withTrackAdded(track)
        updateState(playlist = newPlaylist)
        
        Log.d(TAG, "➕ Track added: ${track.fileName}")
        Log.d(TAG, "   - Total tracks: ${newPlaylist.tracks.size}")
    }
    
    /**
     * Remove track from playlist (HOST only)
     */
    fun removeTrack(trackId: String) {
        val current = _roomState.value
        val newPlaylist = current.playlist.withTrackRemoved(trackId)
        updateState(playlist = newPlaylist)
        
        Log.d(TAG, "➖ Track removed: $trackId")
        Log.d(TAG, "   - Total tracks: ${newPlaylist.tracks.size}")
    }
    
    /**
     * Next track (HOST only)
     */
    fun nextTrack(): PlaylistTrack? {
        val current = _roomState.value
        val newPlaylist = current.playlist.withNextTrack()
        updateState(playlist = newPlaylist, currentPosition = 0)
        
        val track = newPlaylist.currentTrack
        Log.d(TAG, "⏭️ Next track: ${track?.fileName ?: "none"}")
        return track
    }
    
    /**
     * Previous track (HOST only)
     */
    fun previousTrack(): PlaylistTrack? {
        val current = _roomState.value
        val newPlaylist = current.playlist.withPreviousTrack()
        updateState(playlist = newPlaylist, currentPosition = 0)
        
        val track = newPlaylist.currentTrack
        Log.d(TAG, "⏮️ Previous track: ${track?.fileName ?: "none"}")
        return track
    }
    
    /**
     * Select specific track (HOST only)
     */
    fun selectTrack(index: Int): PlaylistTrack? {
        val current = _roomState.value
        val newPlaylist = current.playlist.copy(currentTrackIndex = index)
        updateState(playlist = newPlaylist, currentPosition = 0)
        
        val track = newPlaylist.currentTrack
        Log.d(TAG, "🎵 Track selected: ${track?.fileName ?: "none"}")
        return track
    }
    
    /**
     * Check if need to download current track
     */
    fun needsDownload(downloadedSessions: Set<String>): Boolean {
        val currentTrack = _roomState.value.playlist.currentTrack
        return currentTrack != null && !downloadedSessions.contains(currentTrack.sessionId)
    }
    
    /**
     * Serialize state for network transmission
     */
    fun serializeState(): String {
        return json.encodeToString(_roomState.value)
    }
    
    /**
     * Deserialize state from network
     */
    fun deserializeState(stateJson: String): RoomState {
        return json.decodeFromString<RoomState>(stateJson)
    }
    
    companion object {
        private const val TAG = "RoomStateManager"
    }
}
