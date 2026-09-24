package com.soundboost.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * Opus encoder for audio streaming
 * 
 * Uses Android's built-in Opus codec (available from API 21+)
 * Bitrate: 128kbps (music quality)
 * Frame: 10ms (ultra-low latency)
 * Latency: ~10-20ms encoding
 */
class AudioStreamEncoder {
    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_OPUS
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_COUNT = 2
        private const val BIT_RATE = 128000 // 128kbps
        private const val FRAME_SIZE_MS = 10
    }

    private var encoder: MediaCodec? = null
    private var encodeScope: CoroutineScope? = null
    private var encodeJob: Job? = null

    private val _encodedAudioFlow = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val encodedAudioFlow: SharedFlow<ByteArray> = _encodedAudioFlow

    private var isEncoding = false
    private var inputBufferQueue = mutableListOf<ByteArray>()

    /**
     * Initialize Opus encoder
     */
    fun start() {
        if (isEncoding) {
            android.util.Log.w("AudioStreamEncoder", "⚠️ Already encoding")
            return
        }

        android.util.Log.d("AudioStreamEncoder", "🎙️ Initializing Opus encoder (128kbps, 48kHz stereo)")

        try {
            // Create MediaFormat for Opus
            val format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
            }

            // Create encoder
            encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder?.start()

            isEncoding = true

            // Start encode loop
            encodeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            encodeJob = encodeScope?.launch {
                encodeLoop()
            }

            android.util.Log.d("AudioStreamEncoder", "✅ Opus encoder started")
        } catch (e: Exception) {
            android.util.Log.e("AudioStreamEncoder", "❌ Failed to start encoder: ${e.message}", e)
            stop()
        }
    }

    /**
     * Feed raw PCM audio to encoder
     */
    suspend fun encode(pcmData: ByteArray) {
        if (!isEncoding) {
            android.util.Log.w("AudioStreamEncoder", "⚠️ Encoder not running")
            return
        }

        synchronized(inputBufferQueue) {
            inputBufferQueue.add(pcmData)
        }
    }

    /**
     * Main encoding loop
     */
    private suspend fun encodeLoop() {
        android.util.Log.d("AudioStreamEncoder", "🔄 Encode loop started")
        var frameCount = 0L

        while (encodeScope?.isActive == true && isEncoding) {
            try {
                // Get input buffer
                val inputBufferId = encoder?.dequeueInputBuffer(10000) ?: -1
                if (inputBufferId >= 0) {
                    val pcmData = synchronized(inputBufferQueue) {
                        if (inputBufferQueue.isNotEmpty()) {
                            inputBufferQueue.removeAt(0)
                        } else null
                    }

                    if (pcmData != null) {
                        val inputBuffer = encoder?.getInputBuffer(inputBufferId)
                        inputBuffer?.clear()
                        inputBuffer?.put(pcmData)
                        encoder?.queueInputBuffer(
                            inputBufferId, 
                            0, 
                            pcmData.size, 
                            System.nanoTime() / 1000, 
                            0
                        )
                    }
                }

                // Get output buffer
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferId = encoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                
                when {
                    outputBufferId >= 0 -> {
                        val outputBuffer = encoder?.getOutputBuffer(outputBufferId)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val encodedData = ByteArray(bufferInfo.size)
                            outputBuffer.get(encodedData)
                            
                            // Emit encoded frame
                            _encodedAudioFlow.emit(encodedData)
                            frameCount++

                            if (frameCount % 100 == 0L) {
                                android.util.Log.d("AudioStreamEncoder", "📊 Encoded $frameCount frames (${encodedData.size}B/frame)")
                            }
                        }
                        encoder?.releaseOutputBuffer(outputBufferId, false)
                    }
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        android.util.Log.d("AudioStreamEncoder", "ℹ️ Output format changed: ${encoder?.outputFormat}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioStreamEncoder", "❌ Encode error: ${e.message}", e)
            }
        }

        android.util.Log.d("AudioStreamEncoder", "🛑 Encode loop ended ($frameCount frames)")
    }

    /**
     * Stop encoder
     */
    fun stop() {
        if (!isEncoding) return

        android.util.Log.d("AudioStreamEncoder", "🛑 Stopping encoder")
        
        isEncoding = false
        encodeJob?.cancel()
        encodeScope?.cancel()
        encodeScope = null

        try {
            encoder?.stop()
            encoder?.release()
        } catch (e: Exception) {
            android.util.Log.e("AudioStreamEncoder", "Error stopping encoder: ${e.message}")
        }
        encoder = null

        synchronized(inputBufferQueue) {
            inputBufferQueue.clear()
        }

        android.util.Log.d("AudioStreamEncoder", "✅ Encoder stopped")
    }

    fun isEncoding(): Boolean = isEncoding
}
