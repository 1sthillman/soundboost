package com.soundboost.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.*

/**
 * HIGH-PERFORMANCE REAL-TIME AUDIO ANALYZER v3.0
 * Optimized for smooth 60 FPS visualization on all devices
 * 
 * PERFORMANCE OPTIMIZATIONS:
 * - 30 FPS analysis (33ms intervals) - plenty for smooth visualization
 * - Simplified frequency bands (no A-weighting)
 * - Reduced history buffers
 * - Frame skipping for heavy calculations
 * - Direct bin sampling (no logarithmic operations)
 */
class RealTimeAudioAnalyzer {
    private var visualizer: Visualizer? = null
    
    // Smoothing for visuals
    private var smoothedBars: FloatArray? = null
    private var prevSmoothedBars: FloatArray? = null
    
    // Beat detection
    private val fluxHistory = ArrayDeque<Float>(60)
    private var prevMagnitudes: FloatArray? = null
    private var lastBeatTime = 0L
    private val minBeatInterval = 250L
    
    // Energy smoothing
    private var smoothedEnergy = 0f
    private var smoothedBass = 0f
    private var smoothedMid = 0f
    private var smoothedTreble = 0f
    
    // Transient tracking
    private var prevKickEnergy = 0f
    private var prevSnareEnergy = 0f
    private var prevHiHatEnergy = 0f
    private var transientFrameSkip = 0
    private var cachedTransients = TransientAnalysis(false, false, false)
    
    // Calibration
    private val energyHistory = ArrayDeque<Float>(150)
    private var autoGainFactor = 1.0f
    private val peakHistory = ArrayDeque<Float>(60)
    
    fun startAnalysis(): Flow<AudioAnalysis> = flow {
        try {
            android.util.Log.d("AudioAnalyzer", "🎵 Starting HIGH-PERFORMANCE analyzer (30 FPS)")
            
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                android.util.Log.d("AudioAnalyzer", "FFT size: $captureSize bins")
                
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, w: ByteArray?, s: Int) {}
                        override fun onFftDataCapture(v: Visualizer?, f: ByteArray?, s: Int) {}
                    },
                    Visualizer.getMaxCaptureRate(),
                    true,
                    true
                )
                enabled = true
                android.util.Log.d("AudioAnalyzer", "✅ Analyzer active - Performance mode")
            }
            
            val fftBuffer = ByteArray(visualizer?.captureSize ?: 1024)
            
            while (coroutineContext.isActive) {
                val status = visualizer?.getFft(fftBuffer)
                
                if (status == Visualizer.SUCCESS) {
                    val analysis = analyzeAudio(fftBuffer)
                    emit(analysis)
                }
                
                kotlinx.coroutines.delay(33) // 30 FPS
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioAnalyzer", "Error: ${e.message}", e)
            emit(AudioAnalysis())
        } finally {
            stopAnalysis()
        }
    }.flowOn(Dispatchers.Default)
    
    private fun analyzeAudio(fftBytes: ByteArray): AudioAnalysis {
        val fftSize = fftBytes.size / 2
        val magnitudes = FloatArray(fftSize)
        
        // Convert FFT to magnitudes
        var i = 0
        while (i < fftSize * 2) {
            val real = fftBytes[i].toFloat()
            val imag = fftBytes[i + 1].toFloat()
            magnitudes[i / 2] = sqrt(real * real + imag * imag)
            i += 2
        }
        
        // Auto-gain calibration
        val totalEnergy = magnitudes.average().toFloat()
        energyHistory.add(totalEnergy)
        if (energyHistory.size > 150) energyHistory.removeFirst()
        
        val currentPeak = magnitudes.maxOrNull() ?: 1f
        peakHistory.add(currentPeak)
        if (peakHistory.size > 60) peakHistory.removeFirst()
        val recentPeak = peakHistory.maxOrNull() ?: 1f
        
        if (energyHistory.size > 30) {
            val avgEnergy = energyHistory.average().toFloat()
            val targetEnergy = 0.25f
            if (avgEnergy > 0.01f) {
                autoGainFactor = (autoGainFactor * 0.95f + (targetEnergy / avgEnergy) * 0.05f)
                    .coerceIn(0.5f, 3.0f)
            }
        }
        
        // Apply gain
        val peakNorm = 1f / recentPeak.coerceAtLeast(0.1f)
        i = 0
        while (i < magnitudes.size) {
            magnitudes[i] = magnitudes[i] * peakNorm * autoGainFactor
            i++
        }
        
        // Frequency bands (simplified - no A-weighting)
        val subBass = analyzeBand(magnitudes, 20, 60, fftSize)
        val bass = analyzeBand(magnitudes, 60, 250, fftSize)
        val lowMid = analyzeBand(magnitudes, 250, 500, fftSize)
        val mid = analyzeBand(magnitudes, 500, 2000, fftSize)
        val highMid = analyzeBand(magnitudes, 2000, 4000, fftSize)
        val presence = analyzeBand(magnitudes, 4000, 6000, fftSize)
        val brilliance = analyzeBand(magnitudes, 6000, 20000, fftSize)
        
        // Smooth bands
        val targetBass = (subBass + bass) / 2f
        smoothedBass = smoothedBass + (targetBass - smoothedBass) * 0.25f
        val targetMid = (lowMid + mid) / 2f
        smoothedMid = smoothedMid + (targetMid - smoothedMid) * 0.25f
        val targetTreble = (presence + brilliance) / 2f
        smoothedTreble = smoothedTreble + (targetTreble - smoothedTreble) * 0.25f
        
        // Visualizer bars
        val bars = createBars(magnitudes, 48)
        
        // Transient detection (every 5th frame)
        val transients = detectTransients(magnitudes, fftSize)
        
        // Beat detection
        val spectralFlux = calculateFlux(magnitudes)
        val isBeat = detectBeat(spectralFlux, smoothedBass, transients.hasKick)
        
        // Brightness
        val spectralCentroid = calculateBrightness(magnitudes)
        
        // Overall energy
        val rawEnergy = calculateEnergy(magnitudes)
        smoothedEnergy = smoothedEnergy + (rawEnergy - smoothedEnergy) * 0.2f
        
        // Waveform
        val waveform = createWaveform(magnitudes, 128)
        
        return AudioAnalysis(
            bars = bars,
            waveform = waveform,
            subBass = smoothedBass * 0.8f,
            bass = smoothedBass,
            lowMid = lowMid * 0.85f,
            mid = smoothedMid,
            highMid = highMid * 0.85f,
            presence = presence * 0.8f,
            brilliance = smoothedTreble,
            energy = smoothedEnergy,
            spectralFlux = spectralFlux,
            spectralCentroid = spectralCentroid,
            isBeat = isBeat,
            hasKick = transients.hasKick,
            hasSnare = transients.hasSnare,
            hasHiHat = transients.hasHiHat,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun analyzeBand(magnitudes: FloatArray, freqLow: Int, freqHigh: Int, fftSize: Int): Float {
        val sampleRate = 44100
        val binLow = (freqLow * fftSize / sampleRate).coerceIn(0, magnitudes.size - 1)
        val binHigh = (freqHigh * fftSize / sampleRate).coerceIn(0, magnitudes.size - 1)
        
        // Sample every 3rd bin for speed
        var sum = 0f
        var count = 0
        var i = binLow
        while (i <= binHigh) {
            sum += magnitudes[i]
            count++
            i += 3
        }
        
        val avg = if (count > 0) sum / count else 0f
        return (avg * 4f).coerceIn(0f, 1f)
    }
    
    private fun createBars(magnitudes: FloatArray, barCount: Int): FloatArray {
        val rawBars = FloatArray(barCount)
        val sampleRate = 44100
        val maxFreq = 8000f
        
        // Linear distribution with slight bass emphasis
        for (i in 0 until barCount) {
            val progress = (i.toFloat() / barCount).pow(1.5f)
            val hz = 20f + (maxFreq - 20f) * progress
            val binIndex = ((hz * magnitudes.size) / sampleRate).toInt()
                .coerceIn(0, magnitudes.size - 1)
            
            rawBars[i] = (magnitudes[binIndex] * 3f).coerceIn(0f, 1f)
        }
        
        // Initialize smoothing
        if (smoothedBars == null) smoothedBars = FloatArray(barCount) { 0.05f }
        if (prevSmoothedBars == null) prevSmoothedBars = FloatArray(barCount) { 0.05f }
        
        val result = FloatArray(barCount)
        
        // Smooth
        var i = 0
        while (i < barCount) {
            val raw = rawBars[i]
            val prev = smoothedBars!![i]
            
            val smoothed = if (raw > prev) {
                prev + (raw - prev) * 0.4f
            } else {
                prev + (raw - prev) * 0.3f
            }
            
            smoothedBars!![i] = smoothed
            result[i] = maxOf(smoothed, 0.05f).coerceAtMost(0.95f)
            i++
        }
        
        return result
    }
    
    private fun detectTransients(magnitudes: FloatArray, fftSize: Int): TransientAnalysis {
        transientFrameSkip++
        if (transientFrameSkip % 5 != 0) {
            return cachedTransients
        }
        
        val sampleRate = 44100
        val binToFreq = sampleRate.toFloat() / fftSize
        
        val kickBin = (80 / binToFreq).toInt().coerceIn(0, magnitudes.size - 1)
        val snareBin = (6000 / binToFreq).toInt().coerceIn(0, magnitudes.size - 1)
        val hihatBin = (10000 / binToFreq).toInt().coerceIn(0, magnitudes.size - 1)
        
        val kickEnergy = magnitudes[kickBin]
        val snareEnergy = magnitudes[snareBin]
        val hihatEnergy = magnitudes[hihatBin]
        
        val kickFlux = kickEnergy - prevKickEnergy
        val snareFlux = snareEnergy - prevSnareEnergy
        val hiHatFlux = hihatEnergy - prevHiHatEnergy
        
        prevKickEnergy = kickEnergy
        prevSnareEnergy = snareEnergy
        prevHiHatEnergy = hihatEnergy
        
        cachedTransients = TransientAnalysis(
            hasKick = kickFlux > 0.2f,
            hasSnare = snareFlux > 0.15f,
            hasHiHat = hiHatFlux > 0.12f
        )
        
        return cachedTransients
    }
    
    private fun calculateBrightness(magnitudes: FloatArray): Float {
        val mid = magnitudes.size / 2
        var sum = 0f
        
        for (i in mid until magnitudes.size step 4) {
            sum += magnitudes[i]
        }
        
        return (sum / (magnitudes.size / 2 / 4)).coerceIn(0f, 1f)
    }
    
    private fun createWaveform(magnitudes: FloatArray, pointCount: Int): FloatArray {
        val waveform = FloatArray(pointCount)
        val step = maxOf(4, magnitudes.size / pointCount)
        
        for (i in 0 until pointCount) {
            val index = (i * step).coerceIn(0, magnitudes.size - 1)
            waveform[i] = magnitudes[index]
        }
        
        return waveform
    }
    
    private fun calculateFlux(magnitudes: FloatArray): Float {
        val prev = prevMagnitudes
        
        if (prev == null || prev.size != magnitudes.size) {
            prevMagnitudes = magnitudes.copyOf()
            return 0f
        }
        
        var flux = 0f
        val maxBin = magnitudes.size / 4
        
        var i = 0
        while (i < maxBin) {
            val diff = magnitudes[i] - prev[i]
            if (diff > 0) flux += diff
            i += 2
        }
        
        prevMagnitudes = magnitudes.copyOf()
        return flux
    }
    
    private fun detectBeat(flux: Float, bassEnergy: Float, hasKick: Boolean): Boolean {
        fluxHistory.add(flux)
        if (fluxHistory.size > 60) fluxHistory.removeFirst()
        
        if (fluxHistory.size < 15) return false
        
        val avgFlux = fluxHistory.average().toFloat()
        val threshold = avgFlux * 2.2f
        
        val now = System.currentTimeMillis()
        val timeSinceLastBeat = now - lastBeatTime
        
        val isBeat = (flux > threshold && 
                     bassEnergy > 0.25f && 
                     timeSinceLastBeat > minBeatInterval) || hasKick
        
        if (isBeat) {
            lastBeatTime = now
        }
        
        return isBeat
    }
    
    private fun calculateEnergy(magnitudes: FloatArray): Float {
        var sum = 0f
        val maxIdx = magnitudes.size / 3
        
        var i = 0
        while (i < maxIdx) {
            sum += magnitudes[i]
            i += 2
        }
        
        val avg = sum / (maxIdx / 2)
        return (avg / 100f).coerceIn(0f, 1f)
    }
    
    fun stopAnalysis() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            smoothedBars = null
            prevSmoothedBars = null
            prevMagnitudes = null
            fluxHistory.clear()
            energyHistory.clear()
            peakHistory.clear()
        } catch (e: Exception) {
            android.util.Log.e("AudioAnalyzer", "Error stopping: ${e.message}")
        }
    }
}

data class TransientAnalysis(
    val hasKick: Boolean,
    val hasSnare: Boolean,
    val hasHiHat: Boolean
)

data class AudioAnalysis(
    val bars: FloatArray = FloatArray(48) { 0.05f },
    val waveform: FloatArray = FloatArray(128) { 0f },
    val subBass: Float = 0f,
    val bass: Float = 0f,
    val lowMid: Float = 0f,
    val mid: Float = 0f,
    val highMid: Float = 0f,
    val presence: Float = 0f,
    val brilliance: Float = 0f,
    val energy: Float = 0f,
    val spectralFlux: Float = 0f,
    val spectralCentroid: Float = 0f,
    val isBeat: Boolean = false,
    val hasKick: Boolean = false,
    val hasSnare: Boolean = false,
    val hasHiHat: Boolean = false,
    val timestamp: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioAnalysis
        return timestamp == other.timestamp
    }

    override fun hashCode(): Int = timestamp.hashCode()
}
