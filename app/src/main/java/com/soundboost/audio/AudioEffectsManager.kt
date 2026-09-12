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
    private val callEnhancer = CallAudioEnhancer()

    var isLoudnessSupported = false
        private set
    var isBassBoostSupported = false
        private set
    var isVirtualizerSupported = false
        private set
    var isEqualizerSupported = false
        private set
    var isCallEnhancementSupported = false
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
        
        // Initialize call enhancement
        isCallEnhancementSupported = callEnhancer.initialize()
        if (isCallEnhancementSupported) {
            Log.d(TAG, "✅ Call Enhancement is supported on this device")
        }
    }

    /** 
     * masterGainPercent: 60 = minimum, 100 = efekt yok (0dB), 500 = EXTREME maksimum.
     * maxGainDb: Maximum gain in dB (15-30dB), controls upper limit
     */
    fun setMasterGain(masterGainPercent: Int, maxGainDb: Int = 15) {
        val clamped = masterGainPercent.coerceIn(60, 500)
        val maxGainMb = maxGainDb.coerceIn(15, 30) * 100  // dB to milliBel
        val mB = (((clamped - 100) / 100f) * maxGainMb).toInt()
        val enabled = clamped > 100
        
        Log.d(TAG, "🔊 setMasterGain: percent=$masterGainPercent, maxGainDb=$maxGainDb, mB=$mB, enabled=$enabled")
        
        try {
            loudnessEnhancer?.let {
                it.setTargetGain(mB)
                it.enabled = enabled
                Log.d(TAG, "✅ LoudnessEnhancer applied: gain=${it.targetGain}mB, enabled=${it.enabled}")
            } ?: Log.w(TAG, "❌ LoudnessEnhancer is null!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ setMasterGain başarısız: ${e.message}", e)
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
     * YENİ: 10-Band Parametric Equalizer
     * bands: 10 element array for 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
     * Each value: -15dB to +15dB
     */
    fun set10BandEqualizer(bands: FloatArray) {
        require(bands.size == 10) { "Must provide exactly 10 band values" }
        val eq = equalizer ?: return
        
        try {
            val deviceBandCount = eq.numberOfBands.toInt()
            if (deviceBandCount <= 0) return
            
            val range = eq.bandLevelRange
            
            // Target frequencies (Hz)
            val targetFreqs = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
            
            // Map each device band to closest target frequency
            for (deviceBand in 0 until deviceBandCount) {
                val centerFreq = eq.getCenterFreq(deviceBand.toShort())  // in milliHertz
                val centerFreqHz = centerFreq / 1000
                
                // Find closest target frequency
                var closestIdx = 0
                var minDiff = kotlin.math.abs(centerFreqHz - targetFreqs[0])
                
                for (i in 1 until targetFreqs.size) {
                    val diff = kotlin.math.abs(centerFreqHz - targetFreqs[i])
                    if (diff < minDiff) {
                        minDiff = diff
                        closestIdx = i
                    }
                }
                
                // Apply gain from closest target band
                val targetDb = bands[closestIdx].coerceIn(-MAX_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB)
                val gainMb = (targetDb * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(deviceBand.toShort(), gainMb.toShort())
            }
            
            // Enable EQ if any band is non-zero
            eq.enabled = bands.any { it != 0f }
            
            Log.d(TAG, "✅ 10-Band EQ applied: ${bands.contentToString()}, enabled=${eq.enabled}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ set10BandEqualizer başarısız: ${e.message}", e)
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

    /**
     * Vocal/Music Balance - Smart EQ preset system
     * vocalBalance: 0.0 = music only, 0.5 = balanced, 1.0 = vocal only
     * 
     * Works by applying frequency-specific EQ based on human vocal range:
     * - Vocals: 300Hz - 3kHz (fundamental + harmonics)
     * - Music: <300Hz (bass) + >4kHz (treble/instruments)
     */
    fun setVocalMusicBalance(vocalBalance: Float) {
        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount <= 0) return
            
            val range = eq.bandLevelRange
            val clamped = vocalBalance.coerceIn(0f, 1f)
            
            // Convert 0.0-1.0 to -1.0 to +1.0 range (centered at 0.5)
            val vocalGain = (clamped - 0.5f) * 2f  // -1.0 to +1.0
            val musicGain = -vocalGain  // Inverse relationship
            
            for (band in 0 until bandCount) {
                val centerFreq = eq.getCenterFreq(band.toShort()) / 1000  // Hz to kHz
                
                // Frequency-specific gain calculation
                val targetDb = when {
                    // Bass range (50-250 Hz) - Music
                    centerFreq < 250 -> musicGain * 5f
                    
                    // Low-mid vocal fundamentals (250-800 Hz) - Vocal
                    centerFreq in 250..800 -> vocalGain * 6f
                    
                    // Mid vocal presence (800-3000 Hz) - Strong Vocal
                    centerFreq in 800..3000 -> vocalGain * 7f
                    
                    // High-mid clarity (3-5 kHz) - Slight Vocal
                    centerFreq in 3000..5000 -> vocalGain * 4f
                    
                    // Treble/Air (>5 kHz) - Music
                    else -> musicGain * 3f
                }.coerceIn(-MAX_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB)
                
                val gainMb = (targetDb * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(band.toShort(), gainMb.toShort())
            }
            
            // Enable EQ if not balanced (0.5)
            eq.enabled = kotlin.math.abs(clamped - 0.5f) > 0.05f
            
            Log.d(TAG, "Vocal/Music Balance applied: vocalBalance=$clamped, vocalGain=$vocalGain, musicGain=$musicGain")
        } catch (e: Exception) {
            Log.w(TAG, "setVocalMusicBalance başarısız: ${e.message}")
        }
    }

    /**
     * Enable/disable call audio enhancement
     * When enabled:
     * - Incoming call audio is boosted (via existing LoudnessEnhancer)
     * - Your microphone gets noise suppression + auto gain
     * - EQ is optimized for voice clarity
     */
    fun setCallEnhancement(enabled: Boolean) {
        if (!isCallEnhancementSupported) {
            Log.w(TAG, "Call enhancement not supported on this device")
            return
        }
        
        if (enabled) {
            callEnhancer.enable()
            
            // Apply voice-optimized EQ
            set10BandEqualizer(callEnhancer.getVoiceOptimizedEQ())
            Log.d(TAG, "📞 Call Enhancement ENABLED: Voice EQ applied")
        } else {
            callEnhancer.disable()
            
            // Reset EQ to flat
            set10BandEqualizer(FloatArray(10) { 0f })
            Log.d(TAG, "📞 Call Enhancement DISABLED: EQ reset")
        }
    }
    
    fun isCallEnhancementActive(): Boolean = callEnhancer.isActive

    fun release() {
        try { callEnhancer.release() } catch (_: Exception) {}
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
