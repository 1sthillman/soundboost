package com.soundboost.sync

import kotlinx.serialization.Serializable

/**
 * DJ CONTROL SYSTEM - Real-time synchronized audio manipulation
 * 
 * HOST (DJ) değişiklik yapar → Tüm cihazlara sync edilir
 * Değişiklikler: EQ, Volume, Effects, Vocal/Music Balance
 */

@Serializable
data class DJState(
    // EQ Settings (synced across all devices)
    val bass: Float = 0.5f,          // 0.0 - 1.0
    val mid: Float = 0.5f,           // 0.0 - 1.0
    val treble: Float = 0.5f,        // 0.0 - 1.0
    
    // Volume Controls
    val masterVolume: Float = 1.0f,  // 0.0 - 1.0
    
    // VOCAL/MUSIC BALANCE - CRITICAL!
    // 0.0 = music only, 0.5 = balanced, 1.0 = vocal only
    // Uses frequency-based separation (300Hz-3kHz for vocals)
    val vocalBalance: Float = 0.5f,  // 0.0 - 1.0
    
    // Stem Volumes (for future multi-track stems)
    val bassVolume: Float = 1.0f,    // Stem volume
    val vocalVolume: Float = 1.0f,   // Stem volume
    val instrumentalVolume: Float = 1.0f, // Stem volume
    
    // Effects
    val reverbEnabled: Boolean = false,
    val reverbLevel: Float = 0.0f,
    val echoEnabled: Boolean = false,
    val echoLevel: Float = 0.0f,
    
    // Stem Separation State
    val stemSeparationEnabled: Boolean = false,
    val stemsAvailable: Boolean = false,
    
    // Playback Speed (for scratching/tempo)
    val playbackSpeed: Float = 1.0f, // 0.5 - 2.0
    
    // Last update timestamp
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DJ Control Messages - Sent from HOST to all clients
 */
@Serializable
sealed class DJCommand {
    @Serializable
    data class UpdateEQ(
        val bass: Float,
        val mid: Float,
        val treble: Float,
        val applyAt: Long  // Synchronized timestamp
    ) : DJCommand()
    
    @Serializable
    data class UpdateVolume(
        val masterVolume: Float? = null,
        val bassVolume: Float? = null,
        val vocalVolume: Float? = null,
        val instrumentalVolume: Float? = null,
        val applyAt: Long
    ) : DJCommand()
    
    @Serializable
    data class UpdateEffect(
        val effectType: EffectType,
        val enabled: Boolean,
        val level: Float,
        val applyAt: Long
    ) : DJCommand()
    
    @Serializable
    data class ScratchSeek(
        val positionMs: Int,
        val speed: Float,  // Playback speed during scratch
        val applyAt: Long
    ) : DJCommand()
    
    @Serializable
    data class EnableStems(
        val sessionId: String,
        val stemsReady: Boolean
    ) : DJCommand()
}

@Serializable
enum class EffectType {
    REVERB,
    ECHO,
    FLANGER,
    DELAY
}

/**
 * Stem Separation Data - Audio stems sent to clients
 */
@Serializable
data class AudioStem(
    val sessionId: String,
    val stemType: StemType,
    val chunkIndex: Int,
    val data: String,  // Base64 encoded audio data
    val totalChunks: Int
)

@Serializable
enum class StemType {
    BASS,
    VOCAL,
    INSTRUMENTAL,
    DRUMS,
    OTHER
}

/**
 * DJ Sync Messages - Added to SyncMessage
 */
@Serializable
data class DJStateSync(
    val djState: String,  // JSON-encoded DJState
    val applyAt: Long
)

@Serializable
data class StemData(
    val sessionId: String,
    val stems: Map<String, String>  // stemType -> base64 data
)
