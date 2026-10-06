package com.soundboost.sync

import kotlin.math.abs

/**
 * Cristian'ın algoritmasının basitleştirilmiş hali: host ile client arasındaki
 * saat farkını (offset) ve round-trip gecikmeyi (RTT) hesaplar.
 *
 * Tamamen saf fonksiyonlardan oluşur — Android bağımlılığı yok, kolay test edilir.
 * Gerçek zamanlı I/O (WebSocket ping/pong gönderimi) SyncClient içinde yapılır,
 * burada sadece matematik var.
 */
object ClockSync {

    /** Tek bir ping/pong turundan elde edilen ölçüm. */
    data class SampleResult(
        val offsetMillis: Long,
        val rttMillis: Long
    )

    /**
     * t0: client'ın ping gönderdiği an (client saati)
     * t1: host'un ping'i aldığı an (host saati)
     * t2: host'un pong gönderdiği an (host saati)
     * t3: client'ın pong'u aldığı an (client saati)
     *
     * offset = ((t1 - t0) + (t2 - t3)) / 2
     * Pozitif offset -> host saati client'tan ileride.
     * Client'ın "gerçek zamanı" bulmak için: localTime + offset
     */
    fun computeSample(t0: Long, t1: Long, t2: Long, t3: Long): SampleResult {
        val rtt = (t3 - t0) - (t2 - t1)
        val offset = ((t1 - t0) + (t2 - t3)) / 2
        return SampleResult(offsetMillis = offset, rttMillis = rtt.coerceAtLeast(0))
    }

    /**
     * Birden fazla örnek al, en düşük RTT'ye sahip olanı en güvenilir kabul et
     * (ağ gecikmesi asimetrik olabilir; düşük RTT = daha az jitter riski).
     * En az [minSamples] örnek biriktikten sonra çağır.
     */
    fun bestSample(samples: List<SampleResult>): SampleResult? =
        samples.minByOrNull { it.rttMillis }

    /**
     * Örnekler arasında aşırı sapma varsa (kötü Wi-Fi, ani jitter) medyan offset'i
     * kullanmak tek-örnek riskinden daha güvenli olabilir.
     */
    fun medianOffset(samples: List<SampleResult>): Long {
        if (samples.isEmpty()) return 0L
        val sorted = samples.map { it.offsetMillis }.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    /** İki offset ölçümü arasındaki fark kabul edilebilir mi? (ağ stabilitesi kontrolü) */
    fun isStable(previousOffset: Long, newOffset: Long, toleranceMillis: Long = 10L): Boolean =
        abs(previousOffset - newOffset) <= toleranceMillis

    /** Host'un yayınladığı mutlak tetikleme zamanına göre, bu cihazda ne zaman
     * (local monotonic delay olarak) tetiklenmesi gerektiğini hesaplar. 
     * CRITICAL: Minimum 0ms delay - anında tetikleme destekli! */
    fun localDelayUntil(hostStartAtEpochMillis: Long, localOffsetMillis: Long, nowLocalEpochMillis: Long): Long {
        val hostNowEquivalent = nowLocalEpochMillis + localOffsetMillis
        val delayMs = hostStartAtEpochMillis - hostNowEquivalent
        
        // CRITICAL: Allow NEGATIVE delays (missed timing - trigger immediately!)
        // This prevents "stuck" flash events when timing is slightly off
        return when {
            delayMs < -100 -> {
                // WAY too late - skip this event entirely
                android.util.Log.w("ClockSync", "⚠️ Event too late by ${-delayMs}ms - SKIPPING")
                -1  // Special value: skip event
            }
            delayMs < 0 -> {
                // Slightly late - trigger immediately
                android.util.Log.d("ClockSync", "⚡ Event ${-delayMs}ms late - TRIGGERING NOW")
                0  // Trigger immediately
            }
            else -> delayMs  // Normal: wait for the exact time
        }
    }
}
