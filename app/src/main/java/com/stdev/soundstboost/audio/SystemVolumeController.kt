package com.stdev.soundstboost.audio

import android.content.Context
import android.media.AudioManager

/**
 * Donanım ses seviyesini (medya/alarm/çağrı/zil) tek dokunuşla maksimuma
 * çıkaran yardımcı sınıf. Efekt tabanlı yükseltmeden bağımsız çalışır.
 */
class SystemVolumeController(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun maximizeAllStreams() {
        setStreamToMax(AudioManager.STREAM_MUSIC)
        setStreamToMax(AudioManager.STREAM_ALARM)
        setStreamToMax(AudioManager.STREAM_VOICE_CALL)
        setStreamToMax(AudioManager.STREAM_RING)
        setStreamToMax(AudioManager.STREAM_NOTIFICATION)
    }

    private fun setStreamToMax(streamType: Int) {
        try {
            val max = audioManager.getStreamMaxVolume(streamType)
            audioManager.setStreamVolume(streamType, max, 0)
        } catch (_: SecurityException) {
            // Bazı akışlar (örn. Rahatsız Etme modundayken RING) izin olmadan
            // değiştirilemeyebilir; bu durumda sessizce atlanır.
        }
    }

    fun currentMusicVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (max == 0) 0 else (current * 100) / max
    }
}
