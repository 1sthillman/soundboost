package com.soundboost.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Sistem sesini capture eder - MİKROFON İZNİ GEREKTIRMEZ!
 * Android'in Visualizer API'sini kullanır
 */
class SystemAudioCapture {
    private var visualizer: Visualizer? = null
    
    fun startCapture(barCount: Int = 24): Flow<FloatArray> = flow {
        try {
            android.util.Log.d("AudioCapture", "Starting visualizer capture...")
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                android.util.Log.d("AudioCapture", "Capture size: $captureSize")
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Not used
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            // FFT data processed in flow
                        }
                    },
                    Visualizer.getMaxCaptureRate(),  // Maximum rate for real-time response
                    true,
                    true
                )
                enabled = true
                android.util.Log.d("AudioCapture", "Visualizer enabled successfully")
            }
            
            val buffer = ByteArray(visualizer?.captureSize ?: 1024)
            var frameCount = 0
            
            while (coroutineContext.isActive) {
                val status = visualizer?.getFft(buffer)
                
                if (status == Visualizer.SUCCESS) {
                    val levels = processFftData(buffer, barCount)
                    emit(levels)
                    
                    // Log every 30 frames (~1 second)
                    if (frameCount % 30 == 0) {
                        val avgLevel = levels.average()
                        android.util.Log.d("AudioCapture", "Frame $frameCount - Avg level: $avgLevel, Sample: ${levels.take(5).joinToString()}")
                    }
                    frameCount++
                } else {
                    android.util.Log.e("AudioCapture", "getFft failed with status: $status")
                }
                
                kotlinx.coroutines.delay(30) // ~33 FPS for smooth visualization
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioCapture", "Error in capture: ${e.message}", e)
            emit(FloatArray(barCount) { 0.15f })
        } finally {
            stopCapture()
        }
    }.flowOn(Dispatchers.Default)
    
    private fun processFftData(fft: ByteArray, barCount: Int): FloatArray {
        val levels = FloatArray(barCount)
        val binSize = (fft.size / 2) / barCount
        
        for (i in 0 until barCount) {
            var sum = 0.0
            val start = i * binSize
            val end = minOf(start + binSize, fft.size / 2)
            
            for (j in start until end step 2) {
                val real = fft[j].toInt()
                val imaginary = fft[j + 1].toInt()
                val magnitude = sqrt((real * real + imaginary * imaginary).toDouble())
                sum += magnitude
            }
            
            val avg = sum / binSize
            val db = if (avg > 0) 20 * log10(avg) else -96.0
            val normalized = ((db + 60) / 60).coerceIn(0.0, 1.0)
            
            levels[i] = (normalized * 0.85 + 0.15).toFloat()
        }
        
        return levels
    }
    
    fun stopCapture() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
