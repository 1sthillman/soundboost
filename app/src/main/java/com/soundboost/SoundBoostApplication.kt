package com.soundboost

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.soundboost.cache.ThemePreloader
import com.soundboost.data.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundBoostApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // CRITICAL: Apply saved language on app start
        // This ensures system language detection works from the very beginning
        LanguageManager.applyLanguage(this)
        
        createNotificationChannel()
        
        // Preload all themes in background to prevent lag when switching
        applicationScope.launch {
            ThemePreloader.preloadAllThemes(this@SoundBoostApplication)
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear theme cache if system is running low on memory
        ThemePreloader.clearCache()
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
