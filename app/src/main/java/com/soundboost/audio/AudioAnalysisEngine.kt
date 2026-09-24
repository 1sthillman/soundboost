package com.soundboost.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * AUDIO ANALYSIS ENGINE
 * 
 * Comprehensive audio analysis for DJ features:
 * - BPM Detection (tempo)
 * - Beat Grid Detection
 * - Waveform Generation
 * - Frequency Analysis
 * - Key Detection (future)
 * 
 * CRITICAL: Runs in background, optimized for mobile
 */
class AudioAnalysisEngine(private val context: Context) {
    
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState
    
    /**
     * Analyze audio file - BPM, beats, waveform
     * OPTIMIZED: Fast analysis, low memory
     */
    suspend fun analyzeAudioFile(uri: Uri): AudioAnalysisResult? = withContext(Dispatchers.IO) {
        try {
            _analysisState.value = AnalysisState.Analyzing(0f)
            Log.d(TAG, "🎵 Starting audio analysis...")
            
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            
            // Find audio track
            val trackIndex = findAudioTrack(extractor)
            if (trackIndex < 0) {
                Log.e(TAG, "❌ No audio track found")
                _analysisState.value = AnalysisState.Error("No audio track")
                return@withContext null
            }
            
            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val duration = format.getLong(MediaFormat.KEY_DURATION)
            
            Log.d(TAG, "📊 Audio info: ${sampleRate}Hz, ${channelCount}ch, ${duration/1000000}s")
            
            // Decode audio samples
            _analysisState.value = AnalysisState.Analyzing(0.2f)
            val samples = decodeAudioSamples(extractor, format, duration)
            
            if (samples == null) {
                Log.e(TAG, "❌ Failed to decode audio")
                _analysisState.value = AnalysisState.Error("Decode failed")
                return@withContext null
            }
            
            Log.d(TAG, "✅ Decoded ${samples.size} samples")
            
            // BPM detection
            _analysisState.value = AnalysisState.Analyzing(0.5f)
            val bpm = detectBPM(samples, sampleRate)
            Log.d(TAG, "🥁 BPM detected: $bpm")
            
            // Beat detection
            _analysisState.value = AnalysisState.Analyzing(0.7f)
            val beats = detectBeats(samples, sampleRate, bpm)
            Log.d(TAG, "🎯 Detected ${beats.size} beats")
            
            // Waveform generation (downsampled for visualization)
            _analysisState.value = AnalysisState.Analyzing(0.9f)
            val waveform = generateWaveform(samples, 500) // 500 points
            Log.d(TAG, "📈 Waveform generated: ${waveform.size} points")
            
            extractor.release()
            
            val result = AudioAnalysisResult(
                bpm = bpm,
                beats = beats,
                waveform = waveform,
                sampleRate = sampleRate,
                duration = duration,
                channelCount = channelCount
            )
            
            _analysisState.value = AnalysisState.Complete(result)
            Log.d(TAG, "✅ Audio analysis complete!")
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Audio analysis failed", e)
            _analysisState.value = AnalysisState.Error(e.message ?: "Unknown error")
            null
        }
    }
    
    /**
     * BPM Detection using autocorrelation
     * Range: 60-180 BPM (most music)
     */
    private fun detectBPM(samples: FloatArray, sampleRate: Int): Float {
        // Downsample for faster processing
        val downsampleFactor = 4
        val downsampled = samples.filterIndexed { index, _ -> index % downsampleFactor == 0 }
        
        // Energy envelope
        val windowSize = (sampleRate / downsampleFactor) / 10 // 100ms windows
        val energy = calculateEnergyEnvelope(downsampled.toFloatArray(), windowSize)
        
        // Autocorrelation
        val minLag = (sampleRate / downsampleFactor) / 3 // 180 BPM max
        val maxLag = sampleRate / downsampleFactor // 60 BPM min
        
        var maxCorrelation = 0f
        var bestLag = minLag
        
        for (lag in minLag..maxLag step 10) {
            val correlation = autocorrelation(energy, lag)
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }
        
        // Convert lag to BPM
        val bpm = (60f * sampleRate / downsampleFactor) / bestLag
        return bpm.coerceIn(60f, 180f)
    }
    
    /**
     * Beat detection using onset detection
     */
    private fun detectBeats(samples: FloatArray, sampleRate: Int, bpm: Float): List<Long> {
        val beats = mutableListOf<Long>()
        
        // Expected beat interval
        val beatInterval = (60f / bpm * sampleRate).toInt()
        
        // Energy envelope
        val windowSize = sampleRate / 100 // 10ms windows
        val energy = calculateEnergyEnvelope(samples, windowSize)
        
        // Find peaks in energy
        val threshold = energy.average() * 1.5f
        var lastBeat = 0
        
        for (i in energy.indices) {
            if (energy[i] > threshold && i - lastBeat > beatInterval / 2) {
                // Convert to milliseconds
                val timeMs = (i.toLong() * windowSize * 1000) / sampleRate
                beats.add(timeMs)
                lastBeat = i
            }
        }
        
        return beats
    }
    
    /**
     * Generate waveform for visualization
     * Downsample to targetPoints for efficiency
     */
    private fun generateWaveform(samples: FloatArray, targetPoints: Int): FloatArray {
        val samplesPerPoint = samples.size / targetPoints
        val waveform = FloatArray(targetPoints)
        
        for (i in 0 until targetPoints) {
            val start = i * samplesPerPoint
            val end = min((i + 1) * samplesPerPoint, samples.size)
            
            // RMS value for this segment
            var sum = 0f
            for (j in start until end) {
                sum += samples[j] * samples[j]
            }
            waveform[i] = sqrt(sum / (end - start))
        }
        
        return waveform
    }
    
    /**
     * Calculate energy envelope
     */
    private fun calculateEnergyEnvelope(samples: FloatArray, windowSize: Int): FloatArray {
        val numWindows = samples.size / windowSize
        val energy = FloatArray(numWindows)
        
        for (i in 0 until numWindows) {
            val start = i * windowSize
            val end = min((i + 1) * windowSize, samples.size)
            
            var sum = 0f
            for (j in start until end) {
                sum += samples[j] * samples[j]
            }
            energy[i] = sqrt(sum / windowSize)
        }
        
        return energy
    }
    
    /**
     * Autocorrelation for periodicity detection
     */
    private fun autocorrelation(data: FloatArray, lag: Int): Float {
        if (lag >= data.size) return 0f
        
        var sum = 0f
        val count = data.size - lag
        
        for (i in 0 until count) {
            sum += data[i] * data[i + lag]
        }
        
        return sum / count
    }
    
    /**
     * Decode audio to PCM samples
     */
    private fun decodeAudioSamples(
        extractor: MediaExtractor,
        format: MediaFormat,
        duration: Long
    ): FloatArray? {
        try {
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            
            val samples = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                sawInputEOS = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        // Convert to float samples
                        val shortBuffer = outputBuffer.asShortBuffer()
                        while (shortBuffer.hasRemaining()) {
                            samples.add(shortBuffer.get() / 32768f) // Normalize to -1..1
                        }
                    }
                    
                    codec.releaseOutputBuffer(outputIndex, false)
                    
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }
            
            codec.stop()
            codec.release()
            
            return samples.toFloatArray()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Decode error", e)
            return null
        }
    }
    
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
    
    companion object {
        private const val TAG = "AudioAnalysisEngine"
    }
}

/**
 * Analysis state
 */
sealed class AnalysisState {
    object Idle : AnalysisState()
    data class Analyzing(val progress: Float) : AnalysisState()
    data class Complete(val result: AudioAnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

/**
 * Analysis result
 */
data class AudioAnalysisResult(
    val bpm: Float,
    val beats: List<Long>, // Beat timestamps in milliseconds
    val waveform: FloatArray, // Downsampled waveform for visualization
    val sampleRate: Int,
    val duration: Long, // Microseconds
    val channelCount: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioAnalysisResult
        if (bpm != other.bpm) return false
        if (beats != other.beats) return false
        if (!waveform.contentEquals(other.waveform)) return false
        return true
    }
    
    override fun hashCode(): Int {
        var result = bpm.hashCode()
        result = 31 * result + beats.hashCode()
        result = 31 * result + waveform.contentHashCode()
        return result
    }
}
