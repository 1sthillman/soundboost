package com.soundboost.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * HOST'un çaldığı müziği yakalayan manager - AudioPlaybackCapture API
 * 
 * MÜKEMMEL ÇÖZÜM: AudioPlaybackCapture (Android 10+) ile GERÇEK müziği yakalar
 * MediaProjection permission gerekli - kullanıcı izin vermeli
 * Mikrofon KULLANMIYOR - sistem ses çıkışını doğrudan yakalar!
 * 
 * Format: PCM 16-bit, 48kHz stereo
 * Buffer: 480 samples (10ms @ 48kHz) - Ultra low latency!
 */
@RequiresApi(Build.VERSION_CODES.Q)
class AudioCaptureManager(
    private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_SIZE_MS = 10
        private const val BUFFER_SIZE_SAMPLES = (SAMPLE_RATE * FRAME_SIZE_MS) / 1000 // 480 samples
        private const val BUFFER_SIZE_BYTES = BUFFER_SIZE_SAMPLES * 2 * 2 // 16-bit stereo = 4 bytes per sample
    }

    private var audioRecord: AudioRecord? = null
    private var mediaProjection: MediaProjection? = null
    private var captureScope: CoroutineScope? = null
    private var captureJob: Job? = null

    private val _audioCaptureFlow = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64, // Buffer 64 frames (640ms)
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val audioCaptureFlow: SharedFlow<ByteArray> = _audioCaptureFlow

    private var isCapturing = false

    /**
     * Start capturing audio using AudioPlaybackCapture
     * 
     * CRITICAL: MediaProjection ZORUNLU! User must grant permission first
     * Captures REAL audio playback (music/video) - NOT microphone!
     */
    @SuppressLint("MissingPermission")
    fun startCapture(projection: MediaProjection?) {
        if (isCapturing) {
            android.util.Log.w("AudioCaptureManager", "⚠️ Already capturing")
            return
        }

        if (projection == null) {
            android.util.Log.e("AudioCaptureManager", "❌ MediaProjection is NULL! Cannot capture without permission.")
            return
        }

        android.util.Log.d("AudioCaptureManager", "🎵 Starting AudioPlaybackCapture (48kHz stereo, 10ms frames)")
        
        mediaProjection = projection
        
        // Calculate buffer size
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
            .coerceAtLeast(BUFFER_SIZE_BYTES * 4) // 4x for safety
        
        try {
            // Build AudioPlaybackCaptureConfiguration
            val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA) // Music/Video
                .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME) // Games
                .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN) // Unknown sources
                .build()

            // Create AudioFormat
            val audioFormat = AudioFormat.Builder()
                .setEncoding(ENCODING)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build()

            // Create AudioRecord with AudioPlaybackCaptureConfiguration
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config) // MÜKEMMEL! Real audio capture
                .build()

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                android.util.Log.e("AudioCaptureManager", "❌ Failed to initialize AudioRecord")
                return
            }

            audioRecord?.startRecording()
            isCapturing = true

            // Start capture loop in background
            captureScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            captureJob = captureScope?.launch {
                captureLoop()
            }

            android.util.Log.d("AudioCaptureManager", "✅ AudioPlaybackCapture started (buffer: ${bufferSize}B)")
        } catch (e: Exception) {
            android.util.Log.e("AudioCaptureManager", "❌ Failed to start capture: ${e.message}", e)
            stopCapture()
        }
    }

    /**
     * Main capture loop - reads audio data and emits to flow
     */
    private suspend fun captureLoop() {
        val buffer = ByteArray(BUFFER_SIZE_BYTES)
        var frameCount = 0L

        android.util.Log.d("AudioCaptureManager", "🔄 Capture loop started")

        while (captureScope?.isActive == true && isCapturing) {
            try {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1

                if (bytesRead > 0) {
                    // Emit audio frame to encoder
                    _audioCaptureFlow.emit(buffer.copyOf(bytesRead))
                    frameCount++

                    if (frameCount % 100 == 0L) { // Log every 1 second
                        android.util.Log.d("AudioCaptureManager", "📊 Captured $frameCount frames (${bytesRead}B/frame)")
                    }
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                    android.util.Log.e("AudioCaptureManager", "❌ Invalid operation")
                    break
                } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    android.util.Log.e("AudioCaptureManager", "❌ Bad value")
                    break
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioCaptureManager", "❌ Capture error: ${e.message}", e)
                delay(100) // Back off on error
            }
        }

        android.util.Log.d("AudioCaptureManager", "🛑 Capture loop ended ($frameCount frames captured)")
    }

    /**
     * Stop capturing audio
     */
    fun stopCapture() {
        if (!isCapturing) {
            android.util.Log.w("AudioCaptureManager", "⚠️ Not capturing")
            return
        }

        android.util.Log.d("AudioCaptureManager", "🛑 Stopping audio capture")
        
        isCapturing = false
        captureJob?.cancel()
        captureScope?.cancel()
        captureScope = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            android.util.Log.e("AudioCaptureManager", "Error stopping audio record: ${e.message}")
        }
        audioRecord = null

        mediaProjection?.stop()
        mediaProjection = null

        android.util.Log.d("AudioCaptureManager", "✅ Audio capture stopped")
    }

    /**
     * Check if capturing is active
     */
    fun isCapturing(): Boolean = isCapturing

    /**
     * Get capture stats
     */
    data class CaptureStats(
        val sampleRate: Int = SAMPLE_RATE,
        val channels: Int = 2,
        val frameSizeMs: Int = FRAME_SIZE_MS,
        val bufferSizeBytes: Int = BUFFER_SIZE_BYTES
    )

    fun getStats(): CaptureStats = CaptureStats()
}
