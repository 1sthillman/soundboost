package com.soundboost.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Android'in resmi `android.media.audiofx` API'lerini global ses çıkışına
 * (audio session 0) bağlayarak ses efektlerini uygular.
 *
 * Her efekt bağımsız try/catch içinde kurulur ve kontrol edilir; bir cihazda
 * desteklenmeyen efekt sessizce devre dışı kalır, uygulama asla çökmez.
 * "Tüm telefonlarda birebir aynı güçte çalışır" garantisi yoktur çünkü ses
 * donanımı (DAC, amplifikatör) ve OEM ses HAL kısıtlamaları cihazdan cihaza
 * değişir — bu, Play Store'daki tüm benzer uygulamalar için geçerli bir
 * gerçektir.
 */
class AudioEffectsManager {

    companion object {
        private const val TAG = "AudioEffectsManager"

        // Güvenlik sınırı: 20dB (milliBel cinsinden 2000) üzerinde ciddi ses
        // bozulması ve hoparlör hasarı riski başlar; bu yüzden bilinçli bir
        // üst sınır konuldu.
        const val MAX_LOUDNESS_GAIN_MB = 2000
        const val MAX_BASS_STRENGTH = 1000
        const val MAX_VIRTUALIZER_STRENGTH = 1000
        const val MAX_EQ_BAND_GAIN_DB = 15f
    }

    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var equalizer: Equalizer? = null

    var isLoudnessSupported = false
        private set
    var isBassBoostSupported = false
        private set
    var isVirtualizerSupported = false
        private set
    var isEqualizerSupported = false
        private set

    /**
     * Efektleri belirtilen ses oturumuna bağlar (0 = global/tüm cihaz çıkışı).
     */
    fun attach(sessionId: Int = 0) {
        release()

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = false }
            isLoudnessSupported = true
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer bu cihazda desteklenmiyor: ${e.message}")
            isLoudnessSupported = false
        }

        try {
            val bb = BassBoost(0, sessionId)
            isBassBoostSupported = bb.strengthSupported
            bb.enabled = false
            bassBoost = bb
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost bu cihazda desteklenmiyor: ${e.message}")
            isBassBoostSupported = false
        }

        try {
            val vt = Virtualizer(0, sessionId)
            isVirtualizerSupported = vt.strengthSupported
            vt.enabled = false
            virtualizer = vt
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer bu cihazda desteklenmiyor: ${e.message}")
            isVirtualizerSupported = false
        }

        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = false }
            isEqualizerSupported = true
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer bu cihazda desteklenmiyor: ${e.message}")
            isEqualizerSupported = false
        }
    }

    /** masterGainPercent: 60 = minimum, 100 = efekt yok (0dB), 200 = maksimum (+20dB). */
    fun setMasterGain(masterGainPercent: Int) {
        val clamped = masterGainPercent.coerceIn(60, 200)
        val mB = (((clamped - 100) / 100f) * MAX_LOUDNESS_GAIN_MB).toInt()
        try {
            loudnessEnhancer?.let {
                it.setTargetGain(mB)
                it.enabled = clamped > 100
            }
        } catch (e: Exception) {
            Log.w(TAG, "setMasterGain başarısız: ${e.message}")
        }
    }

    /** percent: 0..100 kullanıcı arayüzü değeri, platformun 0..1000 aralığına ölçeklenir. */
    fun setBassBoost(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val strength = ((clamped / 100f) * MAX_BASS_STRENGTH).toInt().toShort()
        try {
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(strength)
                it.enabled = clamped > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "setBassBoost başarısız: ${e.message}")
        }
    }

    fun setVirtualizer(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val strength = ((clamped / 100f) * MAX_VIRTUALIZER_STRENGTH).toInt().toShort()
        try {
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(strength)
                it.enabled = clamped > 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "setVirtualizer başarısız: ${e.message}")
        }
    }

    /**
     * Basit 3 bant: Bas / Orta / Tiz. Cihazın gerçek bant sayısı kaç olursa
     * olsun, bantlar 3 gruba bölünüp ilgili kazanç uygulanır.
     */
    fun setEqualizer(lowDb: Float, midDb: Float, highDb: Float) {
        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount <= 0) return
            val range = eq.bandLevelRange
            val third = bandCount / 3f

            for (band in 0 until bandCount) {
                val targetDb = when {
                    band < third -> lowDb
                    band < third * 2 -> midDb
                    else -> highDb
                }.coerceIn(-MAX_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB)

                val gainMb = (targetDb * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(band.toShort(), gainMb.toShort())
            }
            eq.enabled = lowDb != 0f || midDb != 0f || highDb != 0f
        } catch (e: Exception) {
            Log.w(TAG, "setEqualizer başarısız: ${e.message}")
        }
    }

    fun release() {
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { equalizer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
        bassBoost = null
        virtualizer = null
        equalizer = null
    }
}
