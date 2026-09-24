package com.soundboost.audio

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.PriorityQueue

/**
 * Jitter buffer for CLIENT audio playback
 * 
 * Handles network jitter and packet reordering
 * Buffer: 50-100ms (adaptive)
 * Late packet handling: Interpolate or drop
 */
class JitterBuffer(
    private val minBufferMs: Int = 50,
    private val maxBufferMs: Int = 200
) {
    companion object {
        private const val SAMPLE_RATE = 48000
        private const val FRAME_SIZE_MS = 10
        private const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_SIZE_MS) / 1000
    }

    data class AudioPacket(
        val timestamp: Long,        // Host timestamp (clock-synced)
        val sequence: Long,         // Packet sequence number
        val data: ByteArray          // Decoded PCM data
    ) : Comparable<AudioPacket> {
        override fun compareTo(other: AudioPacket): Int {
            return timestamp.compareTo(other.timestamp)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AudioPacket) return false
            return sequence == other.sequence
        }

        override fun hashCode(): Int {
            return sequence.hashCode()
        }
    }

    private val buffer = PriorityQueue<AudioPacket>()
    private val bufferMutex = Mutex()

    private var nextExpectedSequence = 0L
    private var packetsReceived = 0L
    private var packetsDropped = 0L
    private var packetsLate = 0L

    // Adaptive buffer tuning
    private var currentBufferMs = minBufferMs
    private var jitterHistory = mutableListOf<Long>()

    /**
     * Add packet to buffer
     */
    suspend fun addPacket(packet: AudioPacket) {
        bufferMutex.withLock {
            packetsReceived++

            // Check if packet is late
            if (packet.sequence < nextExpectedSequence) {
                packetsLate++
                android.util.Log.w("JitterBuffer", "⚠️ Late packet: seq=${packet.sequence}, expected=$nextExpectedSequence")
                return
            }

            // Add to buffer
            buffer.offer(packet)

            // Adaptive buffer tuning
            updateJitterStats(packet)

            if (packetsReceived % 100 == 0L) {
                android.util.Log.d("JitterBuffer", "📊 Buffer: ${buffer.size} packets, ${currentBufferMs}ms latency (dropped: $packetsDropped, late: $packetsLate)")
            }
        }
    }

    /**
     * Get next packet if available and buffer is full enough
     */
    suspend fun getNextPacket(currentTimeMs: Long): AudioPacket? {
        return bufferMutex.withLock {
            if (buffer.isEmpty()) {
                return null
            }

            // Wait until buffer reaches minimum size (cold start)
            if (buffer.size < (currentBufferMs / FRAME_SIZE_MS)) {
                return null
            }

            // Get oldest packet
            val packet = buffer.poll()
            if (packet != null) {
                nextExpectedSequence = packet.sequence + 1
            }
            packet
        }
    }

    /**
     * Update jitter statistics and adapt buffer size
     */
    private fun updateJitterStats(packet: AudioPacket) {
        val currentTime = System.currentTimeMillis()
        val latency = currentTime - packet.timestamp
        
        jitterHistory.add(latency)
        if (jitterHistory.size > 100) {
            jitterHistory.removeAt(0)
        }

        // Calculate jitter (variance in latency)
        if (jitterHistory.size >= 10) {
            val avgLatency = jitterHistory.average()
            val variance = jitterHistory.map { (it - avgLatency) * (it - avgLatency) }.average()
            val jitter = kotlin.math.sqrt(variance).toLong()

            // Adapt buffer size based on jitter
            currentBufferMs = when {
                jitter < 10 -> minBufferMs                          // Low jitter: minimal buffer
                jitter < 30 -> minBufferMs + 25                     // Medium jitter: add 25ms
                jitter < 50 -> minBufferMs + 50                     // High jitter: add 50ms
                else -> maxBufferMs                                  // Very high jitter: max buffer
            }

            if (packetsReceived % 100 == 0L) {
                android.util.Log.d("JitterBuffer", "📈 Jitter: ${jitter}ms, buffer adapted to ${currentBufferMs}ms")
            }
        }
    }

    /**
     * Clear buffer
     */
    suspend fun clear() {
        bufferMutex.withLock {
            buffer.clear()
            nextExpectedSequence = 0
            packetsReceived = 0
            packetsDropped = 0
            packetsLate = 0
            jitterHistory.clear()
            currentBufferMs = minBufferMs
        }
        android.util.Log.d("JitterBuffer", "🗑️ Buffer cleared")
    }

    /**
     * Get buffer stats
     */
    data class BufferStats(
        val bufferSize: Int,
        val bufferMs: Int,
        val packetsReceived: Long,
        val packetsDropped: Long,
        val packetsLate: Long,
        val lossRate: Float
    )

    suspend fun getStats(): BufferStats {
        return bufferMutex.withLock {
            BufferStats(
                bufferSize = buffer.size,
                bufferMs = currentBufferMs,
                packetsReceived = packetsReceived,
                packetsDropped = packetsDropped,
                packetsLate = packetsLate,
                lossRate = if (packetsReceived > 0) {
                    (packetsDropped + packetsLate).toFloat() / packetsReceived.toFloat()
                } else 0f
            )
        }
    }
}
