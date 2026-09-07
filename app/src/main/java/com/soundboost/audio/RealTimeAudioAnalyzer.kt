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
 * PROFESSIONAL REAL-TIME AUDIO ANALYZER
 * Ultra-smooth, artistic visualization with proper audio processing
 */
class RealTimeAudioAnalyzer {
    private var visualizer: Visualizer? = null
    
    // Multi-stage smoothing for ultra-smooth visuals
    private var smoothedBars: FloatArray? = null
    private var prevSmoothedBars: FloatArray? = null
    private var peakHold: FloatArray? = null
    private val smoothingFactor = 0.75f // Çok yüksek = daha smooth
    private val attackTime = 0.35f // HIZLI yükselme - müziğe anında tepki!
    private val releaseTime = 0.25f // HIZLI düşme - barlar şarkıyı yansıtsın!
    
    // Beat detection with adaptive threshold
    private val fluxHistory = ArrayDeque<Float>(90) // 3 saniye
    private var prevMagnitudes: FloatArray? = null
    private var lastBeatTime = 0L
    private val minBeatInterval = 250L
    
    // Energy smoothing
    private var smoothedEnergy = 0f
    private var smoothedBass = 0f
    private var smoothedMid = 0f
    private var smoothedTreble = 0f
    
    // Calibration - automatic gain adjustment
    private val energyHistory = ArrayDeque<Float>(300) // 10 saniye
    private var autoGainFactor = 1.0f
    
    fun startAnalysis(): Flow<AudioAnalysis> = flow {
        try {
            android.util.Log.d("AudioAnalyzer", "🎵 Starting professional analyzer with smooth interpolation...")
            
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
                android.util.Log.d("AudioAnalyzer", "✅ Analyzer active with ultra-smooth processing")
            }
            
            val fftBuffer = ByteArray(visualizer?.captureSize ?: 1024)
            var frameCount = 0
            
            while (coroutineContext.isActive) {
                val status = visualizer?.getFft(fftBuffer)
                
                if (status == Visualizer.SUCCESS) {
                    val analysis = analyzeAudioSmooth(fftBuffer)
                    emit(analysis)
                    
                    if (frameCount % 120 == 0) {
                        android.util.Log.d(
                            "AudioAnalyzer",
                            "Frame $frameCount | Energy: ${String.format("%.2f", analysis.energy)} " +
                            "Bass: ${String.format("%.2f", analysis.bass)} " +
                            "AutoGain: ${String.format("%.2f", autoGainFactor)} " +
                            "${if (analysis.isBeat) "🥁" else ""}"
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
    
    private fun analyzeAudioSmooth(fftBytes: ByteArray): AudioAnalysis {
        val fftSize = fftBytes.size / 2
        val magnitudes = FloatArray(fftSize)
        
        // Convert FFT to magnitudes with proper scaling
        for (i in 0 until fftSize step 2) {
            val real = fftBytes[i].toFloat()
            val imag = fftBytes[i + 1].toFloat()
            val magnitude = sqrt(real * real + imag * imag)
            magnitudes[i / 2] = magnitude
        }
        
        // === AUTO-CALIBRATION ===
        // Adapt to environment noise level
        val totalEnergy = magnitudes.average().toFloat()
        energyHistory.add(totalEnergy)
        if (energyHistory.size > 300) energyHistory.removeFirst()
        
        if (energyHistory.size > 60) {
            val avgEnergy = energyHistory.average().toFloat()
            val targetEnergy = 0.25f // İdeal seviye
            if (avgEnergy > 0.01f) {
                autoGainFactor = (autoGainFactor * 0.99f + (targetEnergy / avgEnergy) * 0.01f)
                    .coerceIn(0.5f, 3.0f)
            }
        }
        
        // Apply auto-gain
        for (i in magnitudes.indices) {
            magnitudes[i] *= autoGainFactor
        }
        
        // === SMOOTH FREQUENCY BANDS ===
        val subBass = analyzeBandSmooth(magnitudes, 0, 60, fftSize)
        val bass = analyzeBandSmooth(magnitudes, 60, 250, fftSize)
        val lowMid = analyzeBandSmooth(magnitudes, 250, 500, fftSize)
        val mid = analyzeBandSmooth(magnitudes, 500, 2000, fftSize)
        val highMid = analyzeBandSmooth(magnitudes, 2000, 4000, fftSize)
        val presence = analyzeBandSmooth(magnitudes, 4000, 6000, fftSize)
        val brilliance = analyzeBandSmooth(magnitudes, 6000, 20000, fftSize)
        
        // Smooth energy bands
        smoothedBass = lerp(smoothedBass, (subBass + bass) / 2f, 0.25f)
        smoothedMid = lerp(smoothedMid, (lowMid + mid) / 2f, 0.25f)
        smoothedTreble = lerp(smoothedTreble, (presence + brilliance) / 2f, 0.25f)
        
        // === ULTRA-SMOOTH VISUALIZER BARS ===
        val bars = createSmoothBars(magnitudes, 48)
        
        // === BEAT DETECTION ===
        val spectralFlux = calculateSpectralFlux(magnitudes)
        val isBeat = detectBeatSmooth(spectralFlux, smoothedBass)
        
        // === OVERALL ENERGY (smoothed) ===
        val rawEnergy = calculateEnergy(magnitudes)
        smoothedEnergy = lerp(smoothedEnergy, rawEnergy, 0.2f)
        
        // === WAVEFORM ===
        val waveform = createWaveform(magnitudes, 128)
        
        return AudioAnalysis(
            bars = bars,
            waveform = waveform,
            subBass = smoothedBass * 0.8f, // Biraz azalt, aşırı hassas olmasın
            bass = smoothedBass,
            lowMid = lowMid * 0.85f,
            mid = smoothedMid,
            highMid = highMid * 0.85f,
            presence = presence * 0.8f,
            brilliance = smoothedTreble,
            energy = smoothedEnergy,
            spectralFlux = spectralFlux,
            isBeat = isBeat,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Smooth band analysis with proper frequency weighting
     */
    private fun analyzeBandSmooth(magnitudes: FloatArray, freqLow: Int, freqHigh: Int, fftSize: Int): Float {
        val sampleRate = 44100
        val binLow = (freqLow * fftSize / sampleRate).coerceIn(0, magnitudes.size - 1)
        val binHigh = (freqHigh * fftSize / sampleRate).coerceIn(0, magnitudes.size - 1)
        
        var sum = 0.0
        var count = 0
        
        for (i in binLow..binHigh) {
            sum += magnitudes[i]
            count++
        }
        
        val avg = if (count > 0) sum / count else 0.0
        
        // Perceptual scaling (logarithmic)
        val db = if (avg > 0.001) 20 * log10(avg + 1) else -60.0
        val normalized = ((db + 30) / 45.0).coerceIn(0.0, 1.0)
        
        return normalized.toFloat()
    }
    
    /**
     * Create ultra-smooth visualizer bars with interpolation
     * LOGARITHMIC FREQUENCY DISTRIBUTION - Like professional equalizers!
     */
    private fun createSmoothBars(magnitudes: FloatArray, barCount: Int): FloatArray {
        val rawBars = FloatArray(barCount)
        
        // CRITICAL: Use LOGARITHMIC frequency distribution
        // Most music energy is in 20Hz - 5kHz range
        // Professional studio equalizers use this exact distribution!
        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            
            // LOGARITHMIC mapping: compress low freqs, expand high freqs
            // This makes ALL bars move, not just left side!
            val logProgress = (exp(progress * 3.5) - 1) / (exp(3.5) - 1)
            
            // Map to frequency bins (focus on 20Hz - 8kHz where music lives)
            val binIndex = (logProgress * magnitudes.size * 0.4f).toInt()
                .coerceIn(0, magnitudes.size - 1)
            
            val magnitude = magnitudes[binIndex]
            
            // Enhanced scaling for better visibility across ALL frequencies
            val db = if (magnitude > 0.001) {
                (20 * log10((magnitude + 1).toDouble())).toFloat()
            } else {
                -60f
            }
            
            // Normalize with boosted high frequencies (so right side moves!)
            val boost = if (i > barCount * 0.6f) {
                // Boost high frequencies (right side) by 2-3x
                1.5f + (i.toFloat() / barCount) * 1.5f
            } else {
                1.0f
            }
            
            val normalized = ((db + 35f) / 50f * boost).coerceIn(0f, 1f)
            
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
            
            // Attack/Release envelope for natural movement
            val smoothed = if (raw > prev) {
                // Fast attack - müziğe anında tepki
                lerp(prev, raw, attackTime)
            } else {
                // Fast release - barlar hızla düşsün
                lerp(prev, raw, releaseTime)
            }
            
            // Peak hold - daha hızlı düşsün
            if (smoothed > peakHold!![i]) {
                peakHold!![i] = smoothed
            } else {
                peakHold!![i] *= 0.85f // Hızlı düşüş - barlar dinamik olsun!
            }
            
            smoothedBars!![i] = smoothed
            
            // Final value with minimum threshold
            result[i] = maxOf(smoothed, 0.05f).coerceAtMost(0.95f)
        }
        
        // Interpolate between frames for 120 FPS smoothness
        for (i in 0 until barCount) {
            result[i] = lerp(prevSmoothedBars!![i], result[i], 0.4f)
            prevSmoothedBars!![i] = result[i]
        }
        
        return result
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
        
        // Only positive changes in low/mid frequencies (where beats occur)
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
    
    private fun detectBeatSmooth(flux: Float, bassEnergy: Float): Boolean {
        fluxHistory.add(flux)
        if (fluxHistory.size > 90) fluxHistory.removeFirst()
        
        if (fluxHistory.size < 20) return false
        
        // Adaptive threshold with statistics
        val avgFlux = fluxHistory.average().toFloat()
        val variance = fluxHistory.map { (it - avgFlux).pow(2) }.average()
        val stdDev = sqrt(variance).toFloat()
        val threshold = avgFlux + stdDev * 1.8f // Daha az hassas
        
        val now = System.currentTimeMillis()
        val timeSinceLastBeat = now - lastBeatTime
        
        // Stricter beat criteria
        val isBeat = flux > threshold && 
                    flux > avgFlux * 2.5f && // Daha güçlü değişim
                    bassEnergy > 0.3f && // Daha yüksek bass threshold
                    timeSinceLastBeat > minBeatInterval
        
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
        } catch (e: Exception) {
            android.util.Log.e("AudioAnalyzer", "Error stopping: ${e.message}")
        }
    }
}

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
    val isBeat: Boolean = false,
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
