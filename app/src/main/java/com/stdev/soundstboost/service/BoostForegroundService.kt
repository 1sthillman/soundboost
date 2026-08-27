package com.stdev.soundstboost.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.stdev.soundstboost.MainActivity
import com.stdev.soundstboost.R
import com.stdev.soundstboost.audio.AudioEffectsManager
import com.stdev.soundstboost.audio.SystemVolumeController
import com.stdev.soundstboost.data.BoostPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

class BoostForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "SoundBoostChannel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioEffects = AudioEffectsManager()
    private lateinit var volumeController: SystemVolumeController
    private lateinit var prefs: BoostPreferences

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        volumeController = SystemVolumeController(this)
        prefs = BoostPreferences(this)
        audioEffects.attach(0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_BOOST" -> startBoost()
            "STOP_BOOST" -> stopBoost()
            "UPDATE_EFFECTS" -> updateEffects()
            "MAXIMIZE_VOLUME" -> maximizeVolume()
        }
        return START_STICKY
    }

    private fun startBoost() {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            
            audioEffects.setMasterGain(settings.masterGainPercent)
            audioEffects.setBassBoost(settings.bassBoostPercent)
            audioEffects.setVirtualizer(settings.virtualizerPercent)
            audioEffects.setEqualizer(settings.eqLowGain, settings.eqMidGain, settings.eqHighGain)
            
            val notification = createNotification(settings.masterGainPercent, settings.bassBoostPercent)
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopBoost() {
        audioEffects.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateEffects() {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            
            audioEffects.setMasterGain(settings.masterGainPercent)
            audioEffects.setBassBoost(settings.bassBoostPercent)
            audioEffects.setVirtualizer(settings.virtualizerPercent)
            audioEffects.setEqualizer(settings.eqLowGain, settings.eqMidGain, settings.eqHighGain)
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = createNotification(settings.masterGainPercent, settings.bassBoostPercent)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun maximizeVolume() {
        volumeController.maximizeAllStreams()
    }

    private fun createNotification(volumePercent: Int, bassPercent: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BoostForegroundService::class.java).apply {
            action = "STOP_BOOST"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text, volumePercent, bassPercent))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Stop",
                stopPendingIntent
            )
            .setStyle(
                NotificationCompat.DecoratedCustomViewStyle()
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sound Boost Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when sound boost is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEffects.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
