package com.soundboost.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Captures audio from other apps using MediaProjection API (Android 10+)
 * 
 * Supported apps: YouTube, Amazon Music, Apple Music, Deezer, Games
 * NOT supported: Spotify, Chrome (DRM protected - ALLOW_CAPTURE_BY_NONE)
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaAudioCapture(private val mediaProjection: MediaProjection) {
    
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false
    
    private var onAudioDataCallback: ((FloatArray) -> Unit)? = null
    private var onErrorCallback: ((Exception) -> Unit)? = null
    
    fun startCapture(
        onAudioData: (FloatArray) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        if (isCapturing) {
            Log.w(TAG, "Already capturing")
            return
        }
        
        this.onAudioDataCallback = onAudioData
        this.onErrorCallback = onError
        
        try {
            initializeAudioRecord()
            audioRecord?.startRecording()
            isCapturing = true
            
            // Start capture thread
            captureThread = Thread(captureRunnable, "AudioCaptureThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            
            Log.d(TAG, "Audio capture started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture", e)
            onErrorCallback?.invoke(e)
            cleanup()
        }
    }
    
    private fun initializeAudioRecord() {
        val config = android.media.AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()
        
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val bufferSize = maxOf(minBufferSize, BUFFER_SIZE_BYTES)
        
        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
            .build()
        
        Log.d(TAG, "AudioRecord initialized: sampleRate=$SAMPLE_RATE, bufferSize=$bufferSize")
        
        // Verify state
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord not initialized properly")
        }
    }
    
    private val captureRunnable = Runnable {
        val buffer = ShortArray(CHUNK_SIZE_SAMPLES)
        val floatBuffer = FloatArray(CHUNK_SIZE_SAMPLES)
        
        var consecutiveZeroCount = 0
        val maxConsecutiveZeros = 50  // ~5 seconds of silence before warning
        
        while (isCapturing) {
            try {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                when {
                    read > 0 -> {
                        // Convert short to float (-1.0 to 1.0)
                        var hasNonZero = false
                        for (i in 0 until read) {
                            val sample = buffer[i] / 32768f
                            floatBuffer[i] = sample
                            if (sample != 0f) hasNonZero = true
                        }
                        
                        // Detect if app is DRM protected (all zeros)
                        if (!hasNonZero) {
                            consecutiveZeroCount++
                            if (consecutiveZeroCount == maxConsecutiveZeros) {
                                Log.w(TAG, "Receiving only zeros - app may be DRM protected (Spotify, Chrome)")
                            }
                        } else {
                            consecutiveZeroCount = 0
                        }
                        
                        // Send to callback
                        onAudioDataCallback?.invoke(floatBuffer.copyOf(read))
                    }
                    read == AudioRecord.ERROR_INVALID_OPERATION -> {
                        Log.e(TAG, "Invalid operation during read")
                        break
                    }
                    read == AudioRecord.ERROR_BAD_VALUE -> {
                        Log.e(TAG, "Bad value during read")
                        break
                    }
                    else -> {
                        Log.w(TAG, "Unexpected read result: $read")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during audio capture", e)
                onErrorCallback?.invoke(e)
                break
            }
        }
        
        Log.d(TAG, "Capture thread ended")
    }
    
    fun stopCapture() {
        if (!isCapturing) {
            return
        }
        
        Log.d(TAG, "Stopping audio capture...")
        isCapturing = false
        
        // Wait for thread to finish
        captureThread?.join(1000)
        
        cleanup()
        
        Log.d(TAG, "Audio capture stopped")
    }
    
    private fun cleanup() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord", e)
        }
        
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord", e)
        }
        
        audioRecord = null
        captureThread = null
        onAudioDataCallback = null
        onErrorCallback = null
    }
    
    fun isCapturing() = isCapturing
    
    companion object {
        private const val TAG = "MediaAudioCapture"
        
        const val SAMPLE_RATE = 44100
        const val CHUNK_SIZE_SAMPLES = 4096 * 2  // Stereo (93ms @ 44.1kHz)
        const val BUFFER_SIZE_BYTES = CHUNK_SIZE_SAMPLES * 2 * 4  // 4 chunks
    }
}
