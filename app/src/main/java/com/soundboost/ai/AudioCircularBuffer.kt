package com.soundboost.ai

import kotlin.math.min

/**
 * Thread-safe circular buffer for audio data
 * Used to decouple capture, processing, and playback threads
 */
class AudioCircularBuffer(private val capacity: Int) {
    
    private val buffer = FloatArray(capacity)
    private var writePos = 0
    private var readPos = 0
    private var availableData = 0
    private val lock = Any()
    
    /**
     * Write audio data to buffer
     * Returns number of samples actually written
     */
    @Synchronized
    fun write(data: FloatArray): Int {
        synchronized(lock) {
            val toWrite = min(data.size, capacity - availableData)
            
            if (toWrite == 0) {
                // Buffer full - this is a drop
                return 0
            }
            
            for (i in 0 until toWrite) {
                buffer[writePos] = data[i]
                writePos = (writePos + 1) % capacity
            }
            
            availableData += toWrite
            
            return toWrite
        }
    }
    
    /**
     * Read audio data from buffer
     * Returns number of samples actually read
     */
    @Synchronized
    fun read(output: FloatArray): Int {
        synchronized(lock) {
            val toRead = min(output.size, availableData)
            
            if (toRead == 0) {
                // Buffer empty
                return 0
            }
            
            for (i in 0 until toRead) {
                output[i] = buffer[readPos]
                readPos = (readPos + 1) % capacity
            }
            
            availableData -= toRead
            
            return toRead
        }
    }
    
    /**
     * Peek at next samples without consuming them
     */
    @Synchronized
    fun peek(output: FloatArray, offset: Int = 0): Int {
        synchronized(lock) {
            val toRead = min(output.size, availableData - offset)
            
            if (toRead <= 0) {
                return 0
            }
            
            var peekPos = (readPos + offset) % capacity
            
            for (i in 0 until toRead) {
                output[i] = buffer[peekPos]
                peekPos = (peekPos + 1) % capacity
            }
            
            return toRead
        }
    }
    
    /**
     * Skip samples without reading them
     */
    @Synchronized
    fun skip(count: Int): Int {
        synchronized(lock) {
            val toSkip = min(count, availableData)
            readPos = (readPos + toSkip) % capacity
            availableData -= toSkip
            return toSkip
        }
    }
    
    /**
     * Clear all data from buffer
     */
    @Synchronized
    fun clear() {
        synchronized(lock) {
            writePos = 0
            readPos = 0
            availableData = 0
        }
    }
    
    /**
     * Get number of available samples
     */
    fun available() = availableData
    
    /**
     * Get buffer fill percentage (0-100)
     */
    fun fillPercentage() = (availableData * 100) / capacity
    
    /**
     * Check if buffer is full
     */
    fun isFull() = availableData >= capacity
    
    /**
     * Check if buffer is empty
     */
    fun isEmpty() = availableData == 0
    
    /**
     * Get total capacity
     */
    fun getCapacity() = capacity
}
