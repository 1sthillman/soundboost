package com.soundboost.audio

import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Opus decoder for audio streaming
 * 
 * Decodes Opus-encoded audio packets back to PCM
 * Latency: ~10-20ms decoding
 */
class AudioStreamDecoder {
    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_OPUS
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL_COUNT = 2
    }

    private var decoder: MediaCodec? = null
    private var decodeScope: CoroutineScope? = null
    private var decodeJob: Job? = null

    private val _decodedAudioFlow = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val decodedAudioFlow: SharedFlow<ByteArray> = _decodedAudioFlow

    private var isDecoding = false
    private var inputBufferQueue = mutableListOf<ByteArray>()

    /**
     * Initialize Opus decoder
     */
    fun start() {
        if (isDecoding) {
            android.util.Log.w("AudioStreamDecoder", "⚠️ Already decoding")
            return
        }

        android.util.Log.d("AudioStreamDecoder", "🎧 Initializing Opus decoder (48kHz stereo)")

        try {
            // Create MediaFormat for Opus
            val format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, CHANNEL_COUNT)

            // Create decoder
            decoder = MediaCodec.createDecoderByType(MIME_TYPE)
            decoder?.configure(format, null, null, 0)
            decoder?.start()

            isDecoding = true

            // Start decode loop
            decodeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            decodeJob = decodeScope?.launch {
                decodeLoop()
            }

            android.util.Log.d("AudioStreamDecoder", "✅ Opus decoder started")
        } catch (e: Exception) {
            android.util.Log.e("AudioStreamDecoder", "❌ Failed to start decoder: ${e.message}", e)
            stop()
        }
    }

    /**
     * Feed encoded Opus data to decoder
     */
    suspend fun decode(opusData: ByteArray) {
        if (!isDecoding) {
            android.util.Log.w("AudioStreamDecoder", "⚠️ Decoder not running")
            return
        }

        synchronized(inputBufferQueue) {
            inputBufferQueue.add(opusData)
        }
    }

    /**
     * Main decoding loop
     */
    private suspend fun decodeLoop() {
        android.util.Log.d("AudioStreamDecoder", "🔄 Decode loop started")
        var frameCount = 0L

        while (decodeScope?.isActive == true && isDecoding) {
            try {
                // Get input buffer
                val inputBufferId = decoder?.dequeueInputBuffer(10000) ?: -1
                if (inputBufferId >= 0) {
                    val opusData = synchronized(inputBufferQueue) {
                        if (inputBufferQueue.isNotEmpty()) {
                            inputBufferQueue.removeAt(0)
                        } else null
                    }

                    if (opusData != null) {
                        val inputBuffer = decoder?.getInputBuffer(inputBufferId)
                        inputBuffer?.clear()
                        inputBuffer?.put(opusData)
                        decoder?.queueInputBuffer(
                            inputBufferId, 
                            0, 
                            opusData.size, 
                            System.nanoTime() / 1000, 
                            0
                        )
                    }
                }

                // Get output buffer
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferId = decoder?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                
                when {
                    outputBufferId >= 0 -> {
                        val outputBuffer = decoder?.getOutputBuffer(outputBufferId)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val decodedData = ByteArray(bufferInfo.size)
                            outputBuffer.get(decodedData)
                            
                            // Emit decoded PCM frame
                            _decodedAudioFlow.emit(decodedData)
                            frameCount++

                            if (frameCount % 100 == 0L) {
                                android.util.Log.d("AudioStreamDecoder", "📊 Decoded $frameCount frames (${decodedData.size}B/frame)")
                            }
                        }
                        decoder?.releaseOutputBuffer(outputBufferId, false)
                    }
                    outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        android.util.Log.d("AudioStreamDecoder", "ℹ️ Output format changed: ${decoder?.outputFormat}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioStreamDecoder", "❌ Decode error: ${e.message}", e)
            }
        }

        android.util.Log.d("AudioStreamDecoder", "🛑 Decode loop ended ($frameCount frames)")
    }

    /**
     * Stop decoder
     */
    fun stop() {
        if (!isDecoding) return

        android.util.Log.d("AudioStreamDecoder", "🛑 Stopping decoder")
        
        isDecoding = false
        decodeJob?.cancel()
        decodeScope?.cancel()
        decodeScope = null

        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) {
            android.util.Log.e("AudioStreamDecoder", "Error stopping decoder: ${e.message}")
        }
        decoder = null

        synchronized(inputBufferQueue) {
            inputBufferQueue.clear()
        }

        android.util.Log.d("AudioStreamDecoder", "✅ Decoder stopped")
    }

    fun isDecoding(): Boolean = isDecoding
}
