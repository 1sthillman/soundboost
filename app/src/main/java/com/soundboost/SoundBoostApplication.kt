package com.soundboost

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.soundboost.data.LanguageManager

class SoundBoostApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // CRITICAL: Apply saved language on app start
        // This ensures system language detection works from the very beginning
        LanguageManager.applyLanguage(this)
        
        createNotificationChannel()
    }
    
    override fun attachBaseContext(base: Context) {
        // CRITICAL: Apply language to base context for proper system integration
        // This must be done before super.attachBaseContext()
        LanguageManager.applyLanguage(base)
        super.attachBaseContext(base)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sound Boost Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent notification when boost is active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sound_boost_status_channel"
    }
}
