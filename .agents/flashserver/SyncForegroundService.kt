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
import androidx.core.app.NotificationCompat

/**
 * "Parti modu" açıkken çalışan ayrı foreground service.
 *
 * BoostForegroundService'e KARIŞTIRILMADI çünkü:
 *  - Ses boost servisi sürekli çalışabilir (kullanıcı boost'u açık bırakabilir)
 *  - Sync servisi sadece parti modu/flash-sync ekranı açıkken anlamlı
 *  - İkisi bağımsız start/stop edilebilmeli (biri diğerini durdurmamalı)
 *
 * MainViewModel bu servisin state'ini SyncViewModel üzerinden bağımsız tutar.
 */
class SyncForegroundService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : android.os.Binder() {
        fun getService(): SyncForegroundService = this@SyncForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val roomName = intent?.getStringExtra(EXTRA_ROOM_NAME) ?: DEFAULT_ROOM_LABEL
        val deviceCount = intent?.getIntExtra(EXTRA_DEVICE_COUNT, 0) ?: 0

        ensureNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(roomName, deviceCount))
        return START_STICKY
    }

    /** Bağlı cihaz sayısı değiştikçe bildirimi günceller — sabit metin bırakmamak için. */
    fun updateDeviceCount(roomName: String, deviceCount: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(roomName, deviceCount))
    }

    private fun buildNotification(roomName: String, deviceCount: Int): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deviceText = if (deviceCount == 1) "1 cihaz bagli" else "$deviceCount cihaz bagli"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Parti Modu Aktif - $roomName")
            .setContentText(deviceText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Parti Modu",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Flash-Sync parti modu aktifken gosterilen bildirim"
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        private const val CHANNEL_ID = "sync_party_mode_channel"
        private const val NOTIFICATION_ID = 4242
        private const val DEFAULT_ROOM_LABEL = "Oda"

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
