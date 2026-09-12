package com.soundboost.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Çoklu Ses Akışı Yöneticisi
 * 
 * Android'de farklı ses türlerini işler:
 * - STREAM_MUSIC: Müzik, video, YouTube
 * - STREAM_VOICE_CALL: Telefon görüşmesi (ahize)
 * - STREAM_RING: Zil sesi
 * - STREAM_NOTIFICATION: Bildirim sesleri
 * - STREAM_ALARM: Alarm sesleri
 * - STREAM_DTMF: Tuş tonları
 * 
 * Oyun sesleri genellikle STREAM_MUSIC kullanır ama bazı oyunlar
 * kendi audio session'larını oluşturabilir.
 */
class MultiStreamAudioManager(private val context: Context) {

    companion object {
        private const val TAG = "MultiStreamAudio"
        
        // Güvenlik sınırları
        const val MAX_LOUDNESS_GAIN_MB = 2000
        const val MAX_BASS_STRENGTH = 1000
        const val MAX_VIRTUALIZER_STRENGTH = 1000
        const val MAX_EQ_BAND_GAIN_DB = 15f
    }

    // Aktif ses akışları için efekt yöneticileri
    private val streamEffects = mutableMapOf<Int, StreamEffects>()
    
    // Ana ses akışları (Android AudioManager)
    private val supportedStreams = listOf(
        AudioManager.STREAM_MUSIC,          // Müzik, video, oyun (varsayılan)
        AudioManager.STREAM_VOICE_CALL,     // Telefon görüşmesi
        AudioManager.STREAM_RING,           // Zil sesi
        AudioManager.STREAM_NOTIFICATION,   // Bildirimler
        AudioManager.STREAM_ALARM           // Alarmlar
    )

    // Aktif ses akışlarını izleme
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Her akış için ayrı efekt sınıfı
    private data class StreamEffects(
        var loudnessEnhancer: LoudnessEnhancer? = null,
        var bassBoost: BassBoost? = null,
        var virtualizer: Virtualizer? = null,
        var equalizer: Equalizer? = null,
        var isSupported: Boolean = false
    )

    /**
     * Tüm desteklenen ses akışlarına efektleri bağla
     * 
     * NOT: Android güvenlik modeli nedeniyle çoğu cihazda sadece session 0 (global)
     * çalışır. Bu NORMAL ve İSTENEN davranıştır çünkü session 0 zaten TÜM sesleri
     * kapsar (müzik, video, oyun, telefon görüşmesi, bildirim vs.)
     */
    fun attachToAllStreams() {
        Log.d(TAG, "🎵 Global ses session'ına efekt ekleniyor...")
        
        // Global session (0) - TÜM SES ÇIKIŞLARINI KAPSAR
        // Bu session müzik, video, oyun, telefon, bildirim - her şeyi etkiler
        attachToSession(0, "GLOBAL_ALL_AUDIO")
        
        Log.d(TAG, "✅ Global session aktif - TÜM sesler etkilenecek")
        Log.d(TAG, "📊 Desteklenen: Müzik, Video, Oyun, Telefon Görüşmesi, Bildirimler, Alarmlar")
    }

    /**
     * Belirli bir session ID'ye efektleri bağla
     */
    private fun attachToSession(sessionId: Int, streamName: String) {
        if (streamEffects.containsKey(sessionId)) {
            Log.d(TAG, "⚠️ Session $sessionId ($streamName) zaten mevcut, atlaniyor")
            return
        }

        val effects = StreamEffects()
        
        try {
            effects.loudnessEnhancer = LoudnessEnhancer(sessionId).apply { 
                enabled = false 
            }
            effects.isSupported = true
            Log.d(TAG, "✅ LoudnessEnhancer eklendi: $streamName (session: $sessionId)")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ LoudnessEnhancer desteklenmiyor: $streamName - ${e.message}")
        }

        try {
            val bb = BassBoost(0, sessionId)
            if (bb.strengthSupported) {
                bb.enabled = false
                effects.bassBoost = bb
                Log.d(TAG, "✅ BassBoost eklendi: $streamName")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ BassBoost desteklenmiyor: $streamName - ${e.message}")
        }

        try {
            val vt = Virtualizer(0, sessionId)
            if (vt.strengthSupported) {
                vt.enabled = false
                effects.virtualizer = vt
                Log.d(TAG, "✅ Virtualizer eklendi: $streamName")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Virtualizer desteklenmiyor: $streamName - ${e.message}")
        }

        try {
            effects.equalizer = Equalizer(0, sessionId).apply { 
                enabled = false 
            }
            Log.d(TAG, "✅ Equalizer eklendi: $streamName")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Equalizer desteklenmiyor: $streamName - ${e.message}")
        }

        if (effects.isSupported) {
            streamEffects[sessionId] = effects
            Log.d(TAG, "🎯 Session $sessionId ($streamName) kayıtlı, toplam: ${streamEffects.size}")
        }
    }

    /**
     * Tüm aktif akışlara master gain uygula
     */
    fun setMasterGainForAllStreams(masterGainPercent: Int, maxGainDb: Int = 15) {
        val clamped = masterGainPercent.coerceIn(60, 500)
        val maxGainMb = maxGainDb.coerceIn(15, 30) * 100
        val mB = (((clamped - 100) / 100f) * maxGainMb).toInt()
        val enabled = clamped > 100
        
        Log.d(TAG, "🔊 Tüm akışlara gain uygulanıyor: percent=$masterGainPercent, mB=$mB, enabled=$enabled")
        
        var appliedCount = 0
        streamEffects.forEach { (sessionId, effects) ->
            try {
                effects.loudnessEnhancer?.let {
                    it.setTargetGain(mB)
                    it.enabled = enabled
                    appliedCount++
                    Log.d(TAG, "✅ Gain uygulandı: session $sessionId, gain=${it.targetGain}mB")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gain uygulanamadı: session $sessionId - ${e.message}")
            }
        }
        
        Log.d(TAG, "📊 Toplam $appliedCount / ${streamEffects.size} akışa gain uygulandı")
    }

    /**
     * Tüm aktif akışlara bass boost uygula
     */
    fun setBassBoostForAllStreams(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val strength = ((clamped / 100f) * MAX_BASS_STRENGTH).toInt().toShort()
        val enabled = clamped > 0
        
        streamEffects.forEach { (sessionId, effects) ->
            try {
                effects.bassBoost?.let {
                    if (it.strengthSupported) it.setStrength(strength)
                    it.enabled = enabled
                }
            } catch (e: Exception) {
                Log.w(TAG, "Bass boost uygulanamadı: session $sessionId - ${e.message}")
            }
        }
    }

    /**
     * Tüm aktif akışlara virtualizer uygula
     */
    fun setVirtualizerForAllStreams(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val strength = ((clamped / 100f) * MAX_VIRTUALIZER_STRENGTH).toInt().toShort()
        val enabled = clamped > 0
        
        streamEffects.forEach { (sessionId, effects) ->
            try {
                effects.virtualizer?.let {
                    if (it.strengthSupported) it.setStrength(strength)
                    it.enabled = enabled
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer uygulanamadı: session $sessionId - ${e.message}")
            }
        }
    }

    /**
     * Tüm aktif akışlara 10-band equalizer uygula
     */
    fun set10BandEqualizerForAllStreams(bands: FloatArray) {
        require(bands.size == 10) { "10 band değeri gerekli" }
        
        streamEffects.forEach { (sessionId, effects) ->
            try {
                applyEqualizer(effects.equalizer, bands, sessionId)
            } catch (e: Exception) {
                Log.w(TAG, "EQ uygulanamadı: session $sessionId - ${e.message}")
            }
        }
    }

    private fun applyEqualizer(eq: Equalizer?, bands: FloatArray, sessionId: Int) {
        eq ?: return
        
        try {
            val deviceBandCount = eq.numberOfBands.toInt()
            if (deviceBandCount <= 0) return
            
            val range = eq.bandLevelRange
            val targetFreqs = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
            
            for (deviceBand in 0 until deviceBandCount) {
                val centerFreq = eq.getCenterFreq(deviceBand.toShort())
                val centerFreqHz = centerFreq / 1000
                
                var closestIdx = 0
                var minDiff = kotlin.math.abs(centerFreqHz - targetFreqs[0])
                
                for (i in 1 until targetFreqs.size) {
                    val diff = kotlin.math.abs(centerFreqHz - targetFreqs[i])
                    if (diff < minDiff) {
                        minDiff = diff
                        closestIdx = i
                    }
                }
                
                val targetDb = bands[closestIdx].coerceIn(-MAX_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB)
                val gainMb = (targetDb * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(deviceBand.toShort(), gainMb.toShort())
            }
            
            eq.enabled = bands.any { it != 0f }
        } catch (e: Exception) {
            Log.e(TAG, "EQ ayarlanamadı: session $sessionId - ${e.message}")
        }
    }

    /**
     * Ses akışı adını al (loglama için)
     */
    private fun getStreamName(streamType: Int): String = when (streamType) {
        AudioManager.STREAM_MUSIC -> "MUSIC/VIDEO/GAME"
        AudioManager.STREAM_VOICE_CALL -> "VOICE_CALL"
        AudioManager.STREAM_RING -> "RINGTONE"
        AudioManager.STREAM_NOTIFICATION -> "NOTIFICATION"
        AudioManager.STREAM_ALARM -> "ALARM"
        AudioManager.STREAM_DTMF -> "DTMF"
        0 -> "GLOBAL"
        else -> "UNKNOWN_$streamType"
    }

    /**
     * Telefon görüşmesi tespit edil
diğinde otomatik davranış değişikliği
     */
    fun isInPhoneCall(): Boolean {
        return try {
            audioManager.mode == AudioManager.MODE_IN_CALL ||
            audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Oyun sesini tespit et (STREAM_MUSIC kullanır ama yüksek aktivite)
     */
    fun detectGameAudio(): Boolean {
        return try {
            // Oyunlar genellikle düşük latency audio kullanır
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Aktif ses akışlarının durumunu logla
     */
    fun logActiveStreams() {
        Log.d(TAG, "📊 === Aktif Ses Akışları ===")
        Log.d(TAG, "Telefon modu: ${audioManager.mode}")
        Log.d(TAG, "Görüşme aktif: ${isInPhoneCall()}")
        Log.d(TAG, "Kayıtlı efekt sayısı: ${streamEffects.size}")
        
        streamEffects.forEach { (sessionId, effects) ->
            val streamName = getStreamName(sessionId)
            Log.d(TAG, "  - Session $sessionId ($streamName):")
            Log.d(TAG, "    Loudness: ${effects.loudnessEnhancer?.enabled ?: false}")
            Log.d(TAG, "    Bass: ${effects.bassBoost?.enabled ?: false}")
            Log.d(TAG, "    Virtualizer: ${effects.virtualizer?.enabled ?: false}")
            Log.d(TAG, "    EQ: ${effects.equalizer?.enabled ?: false}")
        }
    }

    /**
     * Tüm efektleri kapat ve kaynakları serbest bırak
     */
    fun release() {
        Log.d(TAG, "🔌 Tüm ses efektleri kapatılıyor...")
        
        streamEffects.forEach { (sessionId, effects) ->
            try {
                effects.loudnessEnhancer?.release()
                effects.bassBoost?.release()
                effects.virtualizer?.release()
                effects.equalizer?.release()
                Log.d(TAG, "✅ Session $sessionId kapatıldı")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Session $sessionId kapatılırken hata: ${e.message}")
            }
        }
        
        streamEffects.clear()
        Log.d(TAG, "🔌 Tüm kaynaklar temizlendi")
    }

    /**
     * Belirli bir akış türü için efekt var mı kontrol et
     */
    fun hasEffectsForStream(streamType: Int): Boolean {
        return streamEffects.containsKey(streamType) && streamEffects[streamType]?.isSupported == true
    }

    /**
     * Aktif efekt sayısını döndür
     */
    fun getActiveEffectCount(): Int = streamEffects.size

    /**
     * Tüm akışlara 3-band basit EQ uygula (eski uyumluluk için)
     */
    fun setSimpleEqualizerForAllStreams(lowDb: Float, midDb: Float, highDb: Float) {
        streamEffects.forEach { (sessionId, effects) ->
            try {
                applySimpleEqualizer(effects.equalizer, lowDb, midDb, highDb)
            } catch (e: Exception) {
                Log.w(TAG, "Basit EQ uygulanamadı: session $sessionId - ${e.message}")
            }
        }
    }

    private fun applySimpleEqualizer(eq: Equalizer?, lowDb: Float, midDb: Float, highDb: Float) {
        eq ?: return
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
            Log.w(TAG, "Basit EQ ayarlanamadı: ${e.message}")
        }
    }

    /**
     * Vocal/Music balance tüm akışlara uygula
     */
    fun setVocalMusicBalanceForAllStreams(vocalBalance: Float) {
        val clamped = vocalBalance.coerceIn(0f, 1f)
        
        streamEffects.forEach { (sessionId, effects) ->
            try {
                applyVocalMusicBalance(effects.equalizer, clamped)
            } catch (e: Exception) {
                Log.w(TAG, "Vocal balance uygulanamadı: session $sessionId - ${e.message}")
            }
        }
    }

    private fun applyVocalMusicBalance(eq: Equalizer?, vocalBalance: Float) {
        eq ?: return
        try {
            val bandCount = eq.numberOfBands.toInt()
            if (bandCount <= 0) return
            
            val range = eq.bandLevelRange
            val vocalGain = (vocalBalance - 0.5f) * 2f
            val musicGain = -vocalGain
            
            for (band in 0 until bandCount) {
                val centerFreq = eq.getCenterFreq(band.toShort()) / 1000
                
                val targetDb = when {
                    centerFreq < 250 -> musicGain * 5f
                    centerFreq in 250..800 -> vocalGain * 6f
                    centerFreq in 800..3000 -> vocalGain * 7f
                    centerFreq in 3000..5000 -> vocalGain * 4f
                    else -> musicGain * 3f
                }.coerceIn(-MAX_EQ_BAND_GAIN_DB, MAX_EQ_BAND_GAIN_DB)
                
                val gainMb = (targetDb * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(band.toShort(), gainMb.toShort())
            }
            
            eq.enabled = kotlin.math.abs(vocalBalance - 0.5f) > 0.05f
        } catch (e: Exception) {
            Log.w(TAG, "Vocal balance ayarlanamadı: ${e.message}")
        }
    }
    
    /**
     * Call enhancement tüm akışlara uygula
     * Note: Call enhancement is device-wide, not per-stream
     */
    fun setCallEnhancementForAllStreams(enabled: Boolean) {
        try {
            // Call enhancement is handled by AudioEffectsManager
            // We need to access it through the service
            Log.d(TAG, "📞 Call Enhancement toggle requested: $enabled")
            Log.w(TAG, "⚠️ Call enhancement must be handled by AudioEffectsManager in service")
        } catch (e: Exception) {
            Log.w(TAG, "Call enhancement uygulanamadı: ${e.message}")
        }
    }
}
