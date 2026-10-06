package com.soundboost.sync

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DJ STATE MANAGER
 * 
 * Manages real-time audio manipulation state across all devices
 * HOST changes → Broadcast → All clients apply
 * 
 * CRITICAL: All changes are timestamped and synchronized
 */
class DJStateManager {
    
    private val _djState = MutableStateFlow(DJState())
    val djState: StateFlow<DJState> = _djState.asStateFlow()
    
    // Callbacks for audio engine
    var onEQChange: ((bass: Float, mid: Float, treble: Float) -> Unit)? = null
    var onVolumeChange: ((master: Float) -> Unit)? = null
    var onEffectChange: ((type: EffectType, enabled: Boolean, level: Float) -> Unit)? = null
    var onSeekChange: ((positionMs: Int, speed: Float) -> Unit)? = null
    
    /**
     * HOST: Update EQ settings
     */
    fun updateEQ(bass: Float, mid: Float, treble: Float) {
        val current = _djState.value
        _djState.value = current.copy(
            bass = bass.coerceIn(0f, 1f),
            mid = mid.coerceIn(0f, 1f),
            treble = treble.coerceIn(0f, 1f),
            timestamp = System.currentTimeMillis()
        )
        
        Log.d(TAG, "🎚️ EQ updated: bass=$bass, mid=$mid, treble=$treble")
        onEQChange?.invoke(bass, mid, treble)
    }
    
    /**
     * HOST: Update volume (master only)
     */
    fun updateVolume(
        master: Float? = null,
        vocalBalance: Float? = null,
        bass: Float? = null,
        vocal: Float? = null,
        instrumental: Float? = null
    ) {
        val current = _djState.value
        _djState.value = current.copy(
            masterVolume = master?.coerceIn(0f, 1f) ?: current.masterVolume,
            timestamp = System.currentTimeMillis()
        )
        
        Log.d(TAG, "🔊 Volume updated: master=$master")
        onVolumeChange?.invoke(_djState.value.masterVolume)
    }
    
    /**
     * HOST: Update effect
     */
    fun updateEffect(type: EffectType, enabled: Boolean, level: Float) {
        val current = _djState.value
        _djState.value = when (type) {
            EffectType.REVERB -> current.copy(
                reverbEnabled = enabled,
                reverbLevel = level.coerceIn(0f, 1f),
                timestamp = System.currentTimeMillis()
            )
            EffectType.ECHO -> current.copy(
                echoEnabled = enabled,
                echoLevel = level.coerceIn(0f, 1f),
                timestamp = System.currentTimeMillis()
            )
            else -> current
        }
        
        Log.d(TAG, "🎛️ Effect updated: $type enabled=$enabled, level=$level")
        onEffectChange?.invoke(type, enabled, level)
    }
    
    /**
     * HOST: Scrub/Scratch - Seek with playback speed
     */
    fun scratchSeek(positionMs: Int, speed: Float = 1.0f) {
        val current = _djState.value
        _djState.value = current.copy(
            playbackSpeed = speed.coerceIn(0.5f, 2.0f),
            timestamp = System.currentTimeMillis()
        )
        
        Log.d(TAG, "💿 Scratch: pos=${positionMs}ms, speed=$speed")
        onSeekChange?.invoke(positionMs, speed)
    }
    
    /**
     * CLIENT: Apply DJ state from broadcast
     */
    fun applyDJState(djState: DJState) {
        _djState.value = djState
        
        // Trigger callbacks to audio engine
        onEQChange?.invoke(djState.bass, djState.mid, djState.treble)
        onVolumeChange?.invoke(djState.masterVolume)
        
        if (djState.reverbEnabled) {
            onEffectChange?.invoke(EffectType.REVERB, true, djState.reverbLevel)
        }
        if (djState.echoEnabled) {
            onEffectChange?.invoke(EffectType.ECHO, true, djState.echoLevel)
        }
        
        Log.d(TAG, "✅ DJ state applied: bass=${djState.bass}, mid=${djState.mid}, treble=${djState.treble}")
    }
    
    /**
     * Get current state for broadcast
     */
    fun getCurrentState(): DJState = _djState.value
    
    companion object {
        private const val TAG = "DJStateManager"
    }
}
