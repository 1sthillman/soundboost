package com.soundboost.sync

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * MUSIC PLAYLIST SYSTEM
 * 
 * Kritik Özellikler:
 * - Birden fazla şarkı eklenebilir
 * - Şarkılar arasında geçiş yapılabilir
 * - Queue sistemi (sıradaki şarkı otomatik başlar)
 * - Tüm cihazlar playlist'i sync tutar
 */

@Serializable
data class PlaylistTrack(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val durationMs: Long = 0,
    val artist: String? = null,
    val title: String? = null
)

@Serializable
data class Playlist(
    val playlistId: String = UUID.randomUUID().toString(),
    val tracks: List<PlaylistTrack> = emptyList(),
    val currentTrackIndex: Int = 0,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val currentTrack: PlaylistTrack? 
        get() = tracks.getOrNull(currentTrackIndex)
    
    val hasNext: Boolean
        get() = when (repeatMode) {
            RepeatMode.OFF -> currentTrackIndex < tracks.size - 1
            RepeatMode.ALL, RepeatMode.ONE -> true
        }
    
    val hasPrevious: Boolean
        get() = currentTrackIndex > 0 || repeatMode == RepeatMode.ALL
    
    fun withNextTrack(): Playlist {
        val nextIndex = when (repeatMode) {
            RepeatMode.OFF -> (currentTrackIndex + 1).coerceAtMost(tracks.size - 1)
            RepeatMode.ALL -> (currentTrackIndex + 1) % tracks.size
            RepeatMode.ONE -> currentTrackIndex
        }
        return copy(currentTrackIndex = nextIndex)
    }
    
    fun withPreviousTrack(): Playlist {
        val prevIndex = when {
            currentTrackIndex > 0 -> currentTrackIndex - 1
            repeatMode == RepeatMode.ALL -> tracks.size - 1
            else -> 0
        }
        return copy(currentTrackIndex = prevIndex)
    }
    
    fun withTrackAdded(track: PlaylistTrack): Playlist {
        return copy(tracks = tracks + track)
    }
    
    fun withTrackRemoved(trackId: String): Playlist {
        val newTracks = tracks.filterNot { it.id == trackId }
        val newIndex = currentTrackIndex.coerceAtMost(newTracks.size - 1).coerceAtLeast(0)
        return copy(tracks = newTracks, currentTrackIndex = newIndex)
    }
}

@Serializable
enum class RepeatMode {
    OFF,    // Playlist sonunda dur
    ALL,    // Playlist'i tekrar et
    ONE     // Aynı şarkıyı tekrar et
}

/**
 * ROOM STATE - Oda durumu (şu an ne çalıyor, pozisyon, DJ state, vb.)
 * Yeni katılan client'lar bu state'i alır ve senkronize olur
 */
@Serializable
data class RoomState(
    val playlist: Playlist,
    val playbackState: PlaybackState,
    val currentPosition: Int = 0,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    // CRITICAL: DJ state for late join sync
    val djState: DJState = DJState()
)

@Serializable
enum class PlaybackState {
    IDLE,       // Hiçbir şey çalmıyor
    PLAYING,    // Şarkı çalıyor
    PAUSED      // Durakladı
}
