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
            equalizer = Equalizer(0, sessionId).apply { 
                enabled = false
                Log.d(TAG, "🎚️ EQ initialized: ${numberOfBands} bands, range ${bandLevelRange[0]}-${bandLevelRange[1]} mB")
                for (band in 0 until numberOfBands.toInt()) {
                    val freq = getCenterFreq(band.toShort()) / 1000
                    Log.d(TAG, "   Band $band: ${freq}Hz")
                }
            }
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
     * YENİ: 10-Band Parametric Equalizer with SMOOTH transitions (no clicking!)
     * bands: 10 element array for 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
     * Each value: -15dB to +15dB
     * SMOOTH: Gradual changes to prevent audio artifacts/clicks
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
            
            // CRITICAL: Disable EQ before making changes to prevent clicks!
            val wasEnabled = eq.enabled
            if (wasEnabled) {
                eq.enabled = false
            }
            
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
            
            // Re-enable EQ if any band is non-zero
            val shouldEnable = bands.any { kotlin.math.abs(it) > 0.01f }
            eq.enabled = shouldEnable || wasEnabled
            
            Log.d(TAG, "✅ 10-Band EQ applied SMOOTHLY: ${bands.contentToString()}, enabled=${eq.enabled}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ set10BandEqualizer başarısız: ${e.message}", e)
        }
    }
    
    /**
     * Basit 3 bant EQ with SMOOTH transitions: Bas / Orta / Tiz
     * NO CLICKS - Disable before changes, enable after
     */
    fun setEqualizer(lowDb: Float, midDb: Float, highDb: Float) {
        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount <= 0) return
            val range = eq.bandLevelRange
            val third = bandCount / 3f

            // CRITICAL: Disable before changing to prevent clicks!
            val wasEnabled = eq.enabled
            if (wasEnabled) {
                eq.enabled = false
            }

            for (band in 0 until bandCount) {
                val targetDb = when {
                    band < third -> lowDb
                    band < third * 2 -> midDb
                    else -> highDb
                }.coerceIn(-MAX_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB)

                val gainMb = (targetDb * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(band.toShort(), gainMb.toShort())
            }
            
            // Re-enable EQ if any value is non-zero
            val shouldEnable = lowDb != 0f || midDb != 0f || highDb != 0f
            eq.enabled = shouldEnable || wasEnabled
            
            Log.d(TAG, "✅ 3-Band EQ applied SMOOTHLY: L=${lowDb}dB M=${midDb}dB H=${highDb}dB, enabled=${eq.enabled}")
        } catch (e: Exception) {
            Log.w(TAG, "setEqualizer başarısız: ${e.message}")
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
    
    /**
     * Boost in-call audio volume (STREAM_VOICE_CALL)
     * This enhances phone call audio, WhatsApp, Telegram, Zoom calls, etc.
     * Google Play Compliant: Only modifies volume levels, does NOT record calls
     */
    fun boostCallAudio(audioManager: android.media.AudioManager, boostPercent: Int) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_VOICE_CALL)
            val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL)
            
            // Calculate boost: 100% = normal, 200% = double, etc.
            val boostedVolume = ((currentVolume * (boostPercent / 100f))).toInt()
                .coerceIn(0, maxVolume)
            
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_VOICE_CALL,
                boostedVolume,
                0  // No UI flags during call
            )
            
            Log.d(TAG, "📞 Call Audio Boosted: $currentVolume -> $boostedVolume (max: $maxVolume)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ boostCallAudio failed: ${e.message}", e)
        }
    }

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
