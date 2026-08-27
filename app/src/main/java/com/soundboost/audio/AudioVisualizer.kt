package com.soundboost.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class AudioVisualizer {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    fun getAudioLevels(barCount: Int = 24): Flow<FloatArray> = flow {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                emit(FloatArray(barCount) { 0.1f })
                return@flow
            }
            
            audioRecord?.startRecording()
            val buffer = ShortArray(bufferSize)
            
            while (coroutineContext.isActive) {
                val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                
                if (read > 0) {
                    val levels = processAudioData(buffer, read, barCount)
                    emit(levels)
                }
            }
        } catch (e: SecurityException) {
            emit(FloatArray(barCount) { 0.1f })
        } catch (e: Exception) {
            emit(FloatArray(barCount) { 0.1f })
        } finally {
            stopRecording()
        }
    }.flowOn(Dispatchers.IO)
    
    private fun processAudioData(buffer: ShortArray, read: Int, barCount: Int): FloatArray {
        val barSize = read / barCount
        val levels = FloatArray(barCount)
        
        for (i in 0 until barCount) {
            var sum = 0.0
            val start = i * barSize
            val end = minOf(start + barSize, read)
            
            for (j in start until end) {
                val sample = buffer[j].toDouble() / Short.MAX_VALUE
                sum += sample * sample
            }
            
            val rms = sqrt(sum / barSize)
            val db = if (rms > 0) 20 * log10(rms) else -96.0
            val normalized = ((db + 96) / 96).coerceIn(0.0, 1.0)
            
            levels[i] = (normalized * 0.8 + 0.2).toFloat()
        }
        
        return levels
    }
    
    fun stopRecording() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
