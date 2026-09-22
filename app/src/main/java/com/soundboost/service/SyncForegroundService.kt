package com.soundboost.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.soundboost.R

/**
 * "Parti modu" açıkken çalışan ayrı foreground service.
 *
 * BoostForegroundService'e KARIŞTIRILMADI çünkü:
 *  - Ses boost servisi sürekli çalışabilir (kullanıcı boost'u açık bırakabilir)
 *  - Sync servisi sadece parti modu/flash-sync ekranı açıkken anlamlı
 *  - İkisi bağımsız start/stop edilebilmeli (biri diğerini durdurmamalı)
 *
 * MainViewModel bu servisin state'ini SyncViewModel üzerinden bağımsız tutar.
 * 
 * ✅ CRITICAL: WakeLock ile kilitli ekranda çalışır
 */
class SyncForegroundService : Service() {

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null

    inner class LocalBinder : android.os.Binder() {
        fun getService(): SyncForegroundService = this@SyncForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val roomName = intent?.getStringExtra(EXTRA_ROOM_NAME) ?: DEFAULT_ROOM_LABEL
        val deviceCount = intent?.getIntExtra(EXTRA_DEVICE_COUNT, 0) ?: 0

        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(roomName, deviceCount))
        return START_STICKY  // Sistem öldürürse otomatik yeniden başlasın
    }

    /**
     * ✅ CRITICAL: WakeLock ile kilitli ekranda çalışmasını sağlar
     * CPU uyanık kalır, network bağlantısı kesilmez
     */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SoundBoost:SyncPartyModeWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 dakika max (güvenlik için)
        }
    }

    /** Bağlı cihaz sayısı değiştikçe bildirimi günceller — sabit metin bırakmamak için. */
    fun updateDeviceCount(roomName: String, deviceCount: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(roomName, deviceCount))
    }

    private fun buildNotification(roomName: String, deviceCount: Int): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, Class.forName("com.soundboost.MainActivity"))
        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deviceText = when (deviceCount) {
            0 -> getString(R.string.party_mode_waiting)
            1 -> getString(R.string.party_mode_one_device)
            else -> getString(R.string.party_mode_n_devices, deviceCount)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.party_mode_active, roomName))
            .setContentText(deviceText)
            .setSmallIcon(android.R.drawable.ic_dialog_dialer)  // Will replace with proper icon
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.party_mode_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.party_mode_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    companion object {
        private const val CHANNEL_ID = "sync_party_mode_channel"
        private const val NOTIFICATION_ID = 4242
        private const val DEFAULT_ROOM_LABEL = "Room"

        const val EXTRA_ROOM_NAME = "extra_room_name"
        const val EXTRA_DEVICE_COUNT = "extra_device_count"

        fun start(context: Context, roomName: String, deviceCount: Int = 0) {
            val intent = Intent(context, SyncForegroundService::class.java).apply {
                putExtra(EXTRA_ROOM_NAME, roomName)
                putExtra(EXTRA_DEVICE_COUNT, deviceCount)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SyncForegroundService::class.java))
        }
    }
}
