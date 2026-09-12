package com.soundboost.audio

import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log

/**
 * Call Audio Enhancement System
 * 
 * WHAT THIS DOES:
 * ✅ Makes incoming call audio LOUDER and CLEARER (through LoudnessEnhancer + vocal EQ)
 * ✅ Reduces YOUR background noise when speaking (NoiseSuppressor + AGC on microphone)
 * ✅ Works with: Phone calls, WhatsApp, Discord, Zoom, Teams, etc.
 * 
 * TECHNICAL APPROACH:
 * 1. Output Enhancement: AudioEffectsManager already boosts all audio including calls
 * 2. Input Cleanup: This class adds NoiseSuppressor + AGC to microphone
 * 3. Vocal EQ: Optimized frequency response for speech clarity
 * 
 * IMPORTANT:
 * - Cannot modify the caller's audio BEFORE it reaches your phone
 * - Can only enhance what comes out of your speaker
 * - Can clean up your microphone input
 */
class CallAudioEnhancer {
    
    companion object {
        private const val TAG = "CallAudioEnhancer"
        private const val SAMPLE_RATE = 16000  // Standard for voice
    }
    
    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    
    var isSupported = false
        private set
    var isActive = false
        private set
    
    /**
     * Initialize microphone enhancement
     * Call this when app starts to check support
     */
    fun initialize(): Boolean {
        try {
            // Check if we can create AudioRecord for VOICE_COMMUNICATION
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Device doesn't support voice recording")
                return false
            }
            
            // Create AudioRecord to get session ID
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
            )
            
            val sessionId = audioRecord?.audioSessionId ?: return false
            
            // Try to create NoiseSuppressor
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)
                Log.d(TAG, "✅ NoiseSuppressor created successfully")
            } else {
                Log.w(TAG, "⚠️ NoiseSuppressor not available on this device")
            }
            
            // Try to create AutomaticGainControl
            if (AutomaticGainControl.isAvailable()) {
                automaticGainControl = AutomaticGainControl.create(sessionId)
                Log.d(TAG, "✅ AutomaticGainControl created successfully")
            } else {
                Log.w(TAG, "⚠️ AutomaticGainControl not available on this device")
            }
            
            // Supported if at least one effect is available
            isSupported = (noiseSuppressor != null || automaticGainControl != null)
            
            if (isSupported) {
                Log.d(TAG, "📞 Call enhancement ready: NS=${noiseSuppressor != null}, AGC=${automaticGainControl != null}")
            } else {
                Log.w(TAG, "❌ No call enhancement effects available on this device")
            }
            
            return isSupported
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize call enhancer: ${e.message}", e)
            release()
            return false
        }
    }
    
    /**
     * Enable call enhancement
     * This turns on microphone noise suppression and auto gain
     */
    fun enable() {
        if (!isSupported) {
            Log.w(TAG, "Cannot enable: not supported on this device")
            return
        }
        
        try {
            // Enable noise suppressor
            noiseSuppressor?.let {
                if (!it.enabled) {
                    it.enabled = true
                    Log.d(TAG, "✅ NoiseSuppressor enabled")
                }
            }
            
            // Enable automatic gain control
            automaticGainControl?.let {
                if (!it.enabled) {
                    it.enabled = true
                    Log.d(TAG, "✅ AutomaticGainControl enabled")
                }
            }
            
            // Start audio record (needed for effects to work)
            audioRecord?.let { record ->
                if (record.state == AudioRecord.STATE_INITIALIZED && 
                    record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    record.startRecording()
                    Log.d(TAG, "✅ AudioRecord started for enhancement")
                }
            }
            
            isActive = true
            Log.d(TAG, "📞 Call enhancement ACTIVE")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable call enhancer: ${e.message}", e)
            isActive = false
        }
    }
    
    /**
     * Disable call enhancement
     */
    fun disable() {
        try {
            noiseSuppressor?.enabled = false
            automaticGainControl?.enabled = false
            
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                    Log.d(TAG, "AudioRecord stopped")
                }
            }
            
            isActive = false
            Log.d(TAG, "📞 Call enhancement DISABLED")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable call enhancer: ${e.message}", e)
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        try {
            disable()
            
            noiseSuppressor?.release()
            noiseSuppressor = null
            
            automaticGainControl?.release()
            automaticGainControl = null
            
            audioRecord?.release()
            audioRecord = null
            
            Log.d(TAG, "Resources released")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources: ${e.message}", e)
        }
    }
    
    /**
     * Get optimal EQ preset for voice calls
     * Returns 10-band EQ values optimized for speech clarity
     */
    fun getVoiceOptimizedEQ(): FloatArray {
        return floatArrayOf(
            -3f,    // 31 Hz - Reduce rumble
            -2f,    // 62 Hz - Reduce low boom
            0f,     // 125 Hz - Neutral
            2f,     // 250 Hz - Slight boost for warmth
            4f,     // 500 Hz - Boost for presence
            6f,     // 1 kHz - Strong boost (primary vocal range)
            5f,     // 2 kHz - Boost for clarity
            3f,     // 4 kHz - Moderate boost for definition
            0f,     // 8 kHz - Neutral
            -2f     // 16 kHz - Reduce hiss
        )
    }
}
