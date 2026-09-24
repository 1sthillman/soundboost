package com.soundboost.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * CLIENT audio playback manager
 * 
 * Plays decoded PCM audio with ultra-low latency
 * Uses AudioTrack in low-latency mode
 */
class AudioPlaybackManager {
    companion object {
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var playbackScope: CoroutineScope? = null
    private var playbackJob: Job? = null

    private var isPlaying = false
    private val playbackQueue = mutableListOf<ByteArray>()
    private val queueLock = Any()

    /**
     * Initialize AudioTrack for playback
     */
    fun start() {
        if (isPlaying) {
            android.util.Log.w("AudioPlaybackManager", "⚠️ Already playing")
            return
        }

        android.util.Log.d("AudioPlaybackManager", "🔊 Initializing AudioTrack (48kHz stereo, low-latency)")

        try {
            val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
                .coerceAtLeast(4096)

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                android.util.Log.e("AudioPlaybackManager", "❌ Failed to initialize AudioTrack")
                return
            }

            audioTrack?.play()
            isPlaying = true

            // Start playback loop
            playbackScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            playbackJob = playbackScope?.launch {
                playbackLoop()
            }

            android.util.Log.d("AudioPlaybackManager", "✅ AudioTrack started (buffer: ${bufferSize}B)")
        } catch (e: Exception) {
            android.util.Log.e("AudioPlaybackManager", "❌ Failed to start playback: ${e.message}", e)
            stop()
        }
    }

    /**
     * Queue audio data for playback
     */
    fun queueAudio(pcmData: ByteArray) {
        if (!isPlaying) {
            android.util.Log.w("AudioPlaybackManager", "⚠️ Not playing")
            return
        }

        synchronized(queueLock) {
            playbackQueue.add(pcmData)
            
            // Prevent buffer overflow - keep max 100 frames (~1 second)
            if (playbackQueue.size > 100) {
                playbackQueue.removeAt(0)
                android.util.Log.w("AudioPlaybackManager", "⚠️ Buffer overflow, dropped oldest frame")
            }
        }
    }

    /**
     * Main playback loop
     */
    private suspend fun playbackLoop() {
        android.util.Log.d("AudioPlaybackManager", "🔄 Playback loop started")
        var frameCount = 0L

        while (playbackScope?.isActive == true && isPlaying) {
            try {
                val pcmData = synchronized(queueLock) {
                    if (playbackQueue.isNotEmpty()) {
                        playbackQueue.removeAt(0)
                    } else null
                }

                if (pcmData != null) {
                    val written = audioTrack?.write(pcmData, 0, pcmData.size) ?: 0
                    if (written > 0) {
                        frameCount++

                        if (frameCount % 100 == 0L) {
                            android.util.Log.d("AudioPlaybackManager", "📊 Played $frameCount frames (queue: ${playbackQueue.size})")
                        }
                    } else {
                        android.util.Log.w("AudioPlaybackManager", "⚠️ Write failed: $written")
                    }
                } else {
                    // No data - wait a bit
                    kotlinx.coroutines.delay(5)
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioPlaybackManager", "❌ Playback error: ${e.message}", e)
            }
        }

        android.util.Log.d("AudioPlaybackManager", "🛑 Playback loop ended ($frameCount frames)")
    }

    /**
     * Stop playback
     */
    fun stop() {
        if (!isPlaying) return

        android.util.Log.d("AudioPlaybackManager", "🛑 Stopping playback")
        
        isPlaying = false
        playbackJob?.cancel()
        playbackScope?.cancel()
        playbackScope = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            android.util.Log.e("AudioPlaybackManager", "Error stopping playback: ${e.message}")
        }
        audioTrack = null

        synchronized(queueLock) {
            playbackQueue.clear()
        }

        android.util.Log.d("AudioPlaybackManager", "✅ Playback stopped")
    }

    /**
     * Get playback stats
     */
    fun getQueueSize(): Int = synchronized(queueLock) { playbackQueue.size }
    fun isPlaying(): Boolean = isPlaying
}
