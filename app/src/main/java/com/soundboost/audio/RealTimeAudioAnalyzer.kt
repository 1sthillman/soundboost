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
 * PROFESSIONAL REAL-TIME AUDIO ANALYZER v2.0
 * Studio-grade visualization with perceptual frequency weighting
 * 
 * NEW FEATURES:
 * - Mel-scale frequency distribution (matches human hearing)
 * - A-weighting curve (psychoacoustic correction)
 * - Transient detection (kick, snare, hihat separation)
 * - Spectral centroid (brightness/color)
 * - Improved dynamic range compression
 */
class RealTimeAudioAnalyzer {
    private var visualizer: Visualizer? = null
    
    // Multi-stage smoothing for ultra-smooth visuals
    private var smoothedBars: FloatArray? = null
    private var prevSmoothedBars: FloatArray? = null
    private var peakHold: FloatArray? = null
    private val smoothingFactor = 0.75f
    private val attackTime = 0.30f // Biraz daha hızlı - anında tepki
    private val releaseTime = 0.20f // Daha hızlı düşme - müzikal
    
    // Beat detection with adaptive threshold
    private val fluxHistory = ArrayDeque<Float>(90)
    private var prevMagnitudes: FloatArray? = null
    private var lastBeatTime = 0L
    private val minBeatInterval = 250L
    
    // Energy smoothing
    private var smoothedEnergy = 0f
    private var smoothedBass = 0f
    private var smoothedMid = 0f
    private var smoothedTreble = 0f
    
    // NEW: Transient tracking
    private var prevKickEnergy = 0f
    private var prevSnareEnergy = 0f
    private var prevHiHatEnergy = 0f
    
    // Calibration - automatic gain adjustment
    private val energyHistory = ArrayDeque<Float>(300)
    private var autoGainFactor = 1.0f
    
    // NEW: Peak normalization
    private val peakHistory = ArrayDeque<Float>(120) // 4 seconds
    
    fun startAnalysis(): Flow<AudioAnalysis> = flow {
        try {
            android.util.Log.d("AudioAnalyzer", "🎵 Starting STUDIO-GRADE analyzer with mel-scale + A-weighting...")
            
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                android.util.Log.d("AudioAnalyzer", "FFT size: $captureSize bins (Professional mode)")
                
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
                android.util.Log.d("AudioAnalyzer", "✅ Analyzer active - Mel-scale distribution enabled")
            }
            
            val fftBuffer = ByteArray(visualizer?.captureSize ?: 1024)
            var frameCount = 0
            
            while (coroutineContext.isActive) {
                val status = visualizer?.getFft(fftBuffer)
                
                if (status == Visualizer.SUCCESS) {
                    val analysis = analyzeAudioProfessional(fftBuffer)
                    emit(analysis)
                    
                    if (frameCount % 120 == 0) {
                        android.util.Log.d(
                            "AudioAnalyzer",
                            "Frame $frameCount | Energy: ${String.format("%.2f", analysis.energy)} " +
                            "Brightness: ${String.format("%.2f", analysis.spectralCentroid)} " +
                            "${if (analysis.isBeat) "🥁" else ""} " +
                            "${if (analysis.hasKick) "KICK" else ""} " +
                            "${if (analysis.hasSnare) "SNARE" else ""}"
                        )
                    }
                    frameCount++
                }
                
                kotlinx.coroutines.delay(16) // 60 FPS
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioAnalyzer", "Error: ${e.message}", e)
            emit(AudioAnalysis())
        } finally {
            stopAnalysis()
        }
    }.flowOn(Dispatchers.Default)
    
    /**
     * PROFESSIONAL AUDIO ANALYSIS with Mel-scale + A-weighting
     */
    private fun analyzeAudioProfessional(fftBytes: ByteArray): AudioAnalysis {
        val fftSize = fftBytes.size / 2
        val magnitudes = FloatArray(fftSize)
        
        // Convert FFT to magnitudes
        for (i in 0 until fftSize step 2) {
            val real = fftBytes[i].toFloat()
            val imag = fftBytes[i + 1].toFloat()
            val magnitude = sqrt(real * real + imag * imag)
            magnitudes[i / 2] = magnitude
        }
        
        // === AUTO-CALIBRATION with Peak Normalization ===
        val totalEnergy = magnitudes.average().toFloat()
        energyHistory.add(totalEnergy)
        if (energyHistory.size > 300) energyHistory.removeFirst()
        
        // Peak normalization for consistent visualization
        val currentPeak = magnitudes.maxOrNull() ?: 1f
        peakHistory.add(currentPeak)
        if (peakHistory.size > 120) peakHistory.removeFirst()
        val recentPeak = peakHistory.maxOrNull() ?: 1f
        
        if (energyHistory.size > 60) {
            val avgEnergy = energyHistory.average().toFloat()
            val targetEnergy = 0.25f
            if (avgEnergy > 0.01f) {
                autoGainFactor = (autoGainFactor * 0.99f + (targetEnergy / avgEnergy) * 0.01f)
                    .coerceIn(0.5f, 3.0f)
            }
        }
        
        // Apply auto-gain with peak normalization
        for (i in magnitudes.indices) {
            magnitudes[i] = (magnitudes[i] / recentPeak.coerceAtLeast(0.1f)) * autoGainFactor
        }
        
        // === SMOOTH FREQUENCY BANDS with A-weighting ===
        val subBass = analyzeBandPerceptual(magnitudes, 20, 60, fftSize)
        val bass = analyzeBandPerceptual(magnitudes, 60, 250, fftSize)
        val lowMid = analyzeBandPerceptual(magnitudes, 250, 500, fftSize)
        val mid = analyzeBandPerceptual(magnitudes, 500, 2000, fftSize)
        val highMid = analyzeBandPerceptual(magnitudes, 2000, 4000, fftSize)
        val presence = analyzeBandPerceptual(magnitudes, 4000, 6000, fftSize)
        val brilliance = analyzeBandPerceptual(magnitudes, 6000, 20000, fftSize)
        
        // Smooth energy bands
        smoothedBass = lerp(smoothedBass, (subBass + bass) / 2f, 0.25f)
        smoothedMid = lerp(smoothedMid, (lowMid + mid) / 2f, 0.25f)
        smoothedTreble = lerp(smoothedTreble, (presence + brilliance) / 2f, 0.25f)
        
        // === MEL-SCALE VISUALIZER BARS (Studio-grade!) ===
        val bars = createMelScaleBars(magnitudes, 48)
        
        // === TRANSIENT DETECTION (Kick/Snare/HiHat) ===
        val transients = detectTransients(magnitudes, fftSize)
        
        // === BEAT DETECTION ===
        val spectralFlux = calculateSpectralFlux(magnitudes)
        val isBeat = detectBeatProfessional(spectralFlux, smoothedBass, transients.hasKick)
        
        // === SPECTRAL CENTROID (Brightness/Color) ===
        val spectralCentroid = calculateSpectralCentroid(magnitudes)
        
        // === OVERALL ENERGY (smoothed) ===
        val rawEnergy = calculateEnergy(magnitudes)
        smoothedEnergy = lerp(smoothedEnergy, rawEnergy, 0.2f)
        
        // === WAVEFORM ===
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
            spectralCentroid = spectralCentroid, // NEW
            isBeat = isBeat,
            hasKick = transients.hasKick, // NEW
            hasSnare = transients.hasSnare, // NEW
            hasHiHat = transients.hasHiHat, // NEW
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * NEW: Mel-scale frequency conversion
     * Matches human hearing perception - more musical!
     */
    private fun hzToMel(hz: Float): Float {
        return 2595f * log10(1 + hz / 700f)
    }
    
    private fun melToHz(mel: Float): Float {
        return 700f * (10.0.pow((mel / 2595.0).toDouble()) - 1).toFloat()
    }
    
    /**
     * NEW: A-weighting curve (ISO 226:2003)
     * Compensates for human ear frequency response
     */
    private fun applyAWeighting(frequency: Float): Float {
        val f2 = frequency * frequency
        val f4 = f2 * f2
        
        val numerator = 12194f * 12194f * f4
        val denominator = (f2 + 20.6f * 20.6f) * 
                         sqrt((f2 + 107.7f * 107.7f).toDouble()).toFloat() *
                         sqrt((f2 + 737.9f * 737.9f).toDouble()).toFloat() *
                         (f2 + 12194f * 12194f)
        
        val weight = numerator / denominator.coerceAtLeast(0.0001f)
        return weight.coerceIn(0.1f, 2.0f) // Limit range
    }
    
    /**
     * Perceptual band analysis with A-weighting
     */
    private fun analyzeBandPerceptual(magnitudes: FloatArray, freqLow: Int, freqHigh: Int, fftSize: Int): Float {
        val sampleRate = 44100
        val binLow = (freqLow * fftSize / sampleRate).coerceIn(0, magnitudes.size - 1)
        val binHigh = (freqHigh * fftSize / sampleRate).coerceIn(0, magnitudes.size - 1)
        
        var weightedSum = 0.0
        var totalWeight = 0.0
        
        for (i in binLow..binHigh) {
            val frequency = (i * sampleRate / fftSize).toFloat()
            val aWeight = applyAWeighting(frequency)
            weightedSum += magnitudes[i] * aWeight
            totalWeight += aWeight
        }
        
        val avg = if (totalWeight > 0) weightedSum / totalWeight else 0.0
        
        // Perceptual scaling
        val db = if (avg > 0.001) 20 * log10(avg + 1) else -60.0
        val normalized = ((db + 30) / 45.0).coerceIn(0.0, 1.0)
        
        return normalized.toFloat()
    }
    
    /**
     * MEL-SCALE BAR DISTRIBUTION - Studio-grade!
     * Uses mel-scale for perceptually uniform frequency distribution
     */
    private fun createMelScaleBars(magnitudes: FloatArray, barCount: Int): FloatArray {
        val rawBars = FloatArray(barCount)
        
        // Mel-scale range: 20Hz - 8000Hz (music sweet spot)
        val melMin = hzToMel(20f)
        val melMax = hzToMel(8000f)
        val sampleRate = 44100
        
        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            
            // Linear interpolation in mel-space
            val mel = melMin + (melMax - melMin) * progress
            val hz = melToHz(mel)
            
            // Convert to FFT bin
            val binIndex = ((hz * magnitudes.size) / sampleRate).toInt()
                .coerceIn(0, magnitudes.size - 1)
            
            val magnitude = magnitudes[binIndex]
            
            // Enhanced scaling with A-weighting
            val aWeight = applyAWeighting(hz)
            val weightedMagnitude = magnitude * aWeight
            
            val db = if (weightedMagnitude > 0.001) {
                (20 * log10((weightedMagnitude + 1).toDouble())).toFloat()
            } else {
                -60f
            }
            
            // Normalize
            val normalized = ((db + 35f) / 50f).coerceIn(0f, 1f)
            
            rawBars[i] = normalized
        }
        
        // Initialize smoothing arrays
        if (smoothedBars == null) smoothedBars = FloatArray(barCount) { 0.05f }
        if (prevSmoothedBars == null) prevSmoothedBars = FloatArray(barCount) { 0.05f }
        if (peakHold == null) peakHold = FloatArray(barCount) { 0.05f }
        
        val result = FloatArray(barCount)
        
        for (i in 0 until barCount) {
            val raw = rawBars[i]
            val prev = smoothedBars!![i]
            
            // Attack/Release envelope
            val smoothed = if (raw > prev) {
                lerp(prev, raw, attackTime) // Fast attack
            } else {
                lerp(prev, raw, releaseTime) // Fast release
            }
            
            // Peak hold
            if (smoothed > peakHold!![i]) {
                peakHold!![i] = smoothed
            } else {
                peakHold!![i] *= 0.85f
            }
            
            smoothedBars!![i] = smoothed
            result[i] = maxOf(smoothed, 0.05f).coerceAtMost(0.95f)
        }
        
        // Frame interpolation for 120 FPS smoothness
        for (i in 0 until barCount) {
            result[i] = lerp(prevSmoothedBars!![i], result[i], 0.4f)
            prevSmoothedBars!![i] = result[i]
        }
        
        return result
    }
    
    /**
     * NEW: Transient detection - separate kick, snare, hihat
     */
    private fun detectTransients(magnitudes: FloatArray, fftSize: Int): TransientAnalysis {
        val kickEnergy = analyzeBandPerceptual(magnitudes, 60, 120, fftSize)
        val snareBodyEnergy = analyzeBandPerceptual(magnitudes, 150, 300, fftSize)
        val snareCrackEnergy = analyzeBandPerceptual(magnitudes, 5000, 8000, fftSize)
        val hiHatEnergy = analyzeBandPerceptual(magnitudes, 8000, 16000, fftSize)
        
        val kickFlux = kickEnergy - prevKickEnergy
        val snareFlux = (snareBodyEnergy + snareCrackEnergy) / 2 - prevSnareEnergy
        val hiHatFlux = hiHatEnergy - prevHiHatEnergy
        
        prevKickEnergy = kickEnergy
        prevSnareEnergy = (snareBodyEnergy + snareCrackEnergy) / 2
        prevHiHatEnergy = hiHatEnergy
        
        return TransientAnalysis(
            hasKick = kickFlux > 0.15f && kickEnergy > 0.4f,
            hasSnare = snareFlux > 0.12f && snareCrackEnergy > 0.3f,
            hasHiHat = hiHatFlux > 0.10f && hiHatEnergy > 0.4f
        )
    }
    
    /**
     * NEW: Spectral centroid - "brightness" of sound
     */
    private fun calculateSpectralCentroid(magnitudes: FloatArray): Float {
        var weightedSum = 0f
        var totalMagnitude = 0f
        
        for (i in magnitudes.indices) {
            weightedSum += i * magnitudes[i]
            totalMagnitude += magnitudes[i]
        }
        
        val centroid = if (totalMagnitude > 0) weightedSum / totalMagnitude else 0f
        return (centroid / magnitudes.size).coerceIn(0f, 1f)
    }
    
    private fun createWaveform(magnitudes: FloatArray, pointCount: Int): FloatArray {
        val waveform = FloatArray(pointCount)
        val step = maxOf(1, magnitudes.size / pointCount)
        
        for (i in 0 until pointCount) {
            val index = (i * step).coerceIn(0, magnitudes.size - 1)
            waveform[i] = magnitudes[index]
        }
        
        return waveform
    }
    
    private fun calculateSpectralFlux(magnitudes: FloatArray): Float {
        val prev = prevMagnitudes
        
        if (prev == null || prev.size != magnitudes.size) {
            prevMagnitudes = magnitudes.copyOf()
            return 0f
        }
        
        var flux = 0.0
        val maxBin = minOf(magnitudes.size / 3, magnitudes.size)
        
        for (i in 0 until maxBin) {
            val diff = magnitudes[i] - prev[i]
            if (diff > 0) {
                flux += diff
            }
        }
        
        prevMagnitudes = magnitudes.copyOf()
        return flux.toFloat()
    }
    
    private fun detectBeatProfessional(flux: Float, bassEnergy: Float, hasKick: Boolean): Boolean {
        fluxHistory.add(flux)
        if (fluxHistory.size > 90) fluxHistory.removeFirst()
        
        if (fluxHistory.size < 20) return false
        
        val avgFlux = fluxHistory.average().toFloat()
        val variance = fluxHistory.map { (it - avgFlux).pow(2) }.average()
        val stdDev = sqrt(variance).toFloat()
        val threshold = avgFlux + stdDev * 1.8f
        
        val now = System.currentTimeMillis()
        val timeSinceLastBeat = now - lastBeatTime
        
        // Enhanced beat detection with transient info
        val isBeat = (flux > threshold && 
                     flux > avgFlux * 2.5f && 
                     bassEnergy > 0.3f && 
                     timeSinceLastBeat > minBeatInterval) || hasKick
        
        if (isBeat) {
            lastBeatTime = now
        }
        
        return isBeat
    }
    
    private fun calculateEnergy(magnitudes: FloatArray): Float {
        val sum = magnitudes.take(magnitudes.size / 3).sum()
        val avg = sum / (magnitudes.size / 3)
        return (avg / 100f).coerceIn(0f, 1f)
    }
    
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
    
    fun stopAnalysis() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            smoothedBars = null
            prevSmoothedBars = null
            peakHold = null
            prevMagnitudes = null
            fluxHistory.clear()
            energyHistory.clear()
            peakHistory.clear()
        } catch (e: Exception) {
            android.util.Log.e("AudioAnalyzer", "Error stopping: ${e.message}")
        }
    }
}

/**
 * NEW: Transient analysis data
 */
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
    val spectralCentroid: Float = 0f, // NEW: Brightness (0-1)
    val isBeat: Boolean = false,
    val hasKick: Boolean = false, // NEW
    val hasSnare: Boolean = false, // NEW
    val hasHiHat: Boolean = false, // NEW
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
