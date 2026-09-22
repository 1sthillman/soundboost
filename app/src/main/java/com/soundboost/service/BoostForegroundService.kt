package com.soundboost.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.soundboost.MainActivity
import com.soundboost.R
import com.soundboost.audio.AudioEffectsManager
import com.soundboost.audio.AudioOutputMonitor
import com.soundboost.audio.MultiStreamAudioManager
import com.soundboost.audio.SmartGainAdvisor
import com.soundboost.audio.SystemVolumeController
import com.soundboost.data.BoostPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

class BoostForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "SoundBoostChannel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "BoostService"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // YENİ: Çoklu akış desteği
    private var multiStreamManager: MultiStreamAudioManager? = null
    
    // YENİ: Ses çıkış cihazı izleme
    private var audioOutputMonitor: AudioOutputMonitor? = null
    
    // YENİ: Akıllı gain danışmanı
    private val smartGainAdvisor = SmartGainAdvisor()
    
    // ESKİ: Tek akış (yedek uyumluluk)
    private val audioEffects = AudioEffectsManager()
    
    // Kullanıcı tercihi: Multi-stream kullan veya eski sistemi kullan
    private var useMultiStream = true  // Varsayılan olarak yeni sistem aktif
    
    private lateinit var volumeController: SystemVolumeController
    private lateinit var prefs: BoostPreferences
    
    /**
     * Update all home screen widgets (Glance)
     */
    private fun updateWidgets() {
        try {
            // Glance widget'ları otomatik olarak StateFlow değişikliklerini dinler
            // Manuel update için GlanceAppWidgetManager kullanılabilir
            val updateIntent = Intent(this, com.soundboost.ui.widgets.RezonansGlanceWidgetReceiver::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            sendBroadcast(updateIntent)
            
            android.util.Log.d(TAG, "✅ Glance widget update broadcast sent")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to update Glance widgets: ${e.message}")
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d(TAG, "🚀 Service onCreate - Initializing audio systems...")
        
        createNotificationChannel()
        volumeController = SystemVolumeController(this)
        prefs = BoostPreferences(this)
        
        // Ses çıkış cihazı izleme başlat
        audioOutputMonitor = AudioOutputMonitor(this).apply {
            onDeviceChanged = { device, name ->
                android.util.Log.d(TAG, "🔊 Ses cihazı değişti: $device - $name")
                handleAudioDeviceChange(device, name)
            }
            startMonitoring()
            
            // İlk cihazı tespit et
            val (device, name) = detectCurrentDevice()
            android.util.Log.d(TAG, "📱 Başlangıç cihazı: $device - $name")
        }
        
        // Yeni multi-stream sistemi başlat
        try {
            multiStreamManager = MultiStreamAudioManager(this).apply {
                attachToAllStreams()
                logActiveStreams()
            }
            android.util.Log.d(TAG, "✅ Multi-stream sistem aktif: ${multiStreamManager?.getActiveEffectCount()} akış")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Multi-stream başlatılamadı, eski sisteme geçiliyor: ${e.message}")
            useMultiStream = false
            audioEffects.attach(0)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_BOOST" -> startBoost()
            "STOP_BOOST" -> {
                android.util.Log.d(TAG, "🛑 STOP_BOOST action received - stopping service completely")
                stopBoost()
                return START_NOT_STICKY  // Don't restart service after stop
            }
            "UPDATE_EFFECTS" -> updateEffects()
            "MAXIMIZE_VOLUME" -> maximizeVolume()
            "BASS_DOWN" -> adjustBass(-10)
            "BASS_UP" -> adjustBass(10)
            "TOGGLE_FLASH" -> toggleFlash()
            // Widget support (v1.4.0+)
            "SET_VOLUME" -> {
                val percent = intent.getStringExtra("percent")?.toIntOrNull() ?: 150
                setVolumeFromWidget(percent)
            }
            "INCREASE_VOLUME" -> adjustVolume(+20)
            "DECREASE_VOLUME" -> adjustVolume(-20)
        }
        return START_STICKY
    }

    private fun adjustBass(delta: Int) {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            val newBass = (settings.bassBoostPercent + delta).coerceIn(0, 100)
            
            prefs.setBassBoost(newBass)
            
            if (useMultiStream) {
                multiStreamManager?.setBassBoostForAllStreams(newBass)
            } else {
                audioEffects.setBassBoost(newBass)
            }
            
            val notification = createNotification(settings.masterGainPercent, newBass)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
    
    /**
     * Adjust master volume (for widgets)
     */
    private fun adjustVolume(delta: Int) {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            val newVolume = (settings.masterGainPercent + delta).coerceIn(60, 500)
            
            prefs.setMasterGain(newVolume)
            
            if (useMultiStream) {
                multiStreamManager?.setMasterGainForAllStreams(newVolume, settings.maxGainDb)
            } else {
                audioEffects.setMasterGain(newVolume, settings.maxGainDb)
            }
            
            val notification = createNotification(newVolume, settings.bassBoostPercent)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            // Update widgets
            updateWidgets()
            
            android.util.Log.d(TAG, "📊 Volume adjusted: $newVolume% (delta: $delta)")
        }
    }


    private fun toggleFlash() {
        serviceScope.launch {
            android.util.Log.d(TAG, "⚡ Toggle flash from notification")
            
            // Get current flash state from prefs and toggle it
            val settings = prefs.settings.firstOrNull() ?: return@launch
            
            // We need to store flash state in preferences
            // For now, send intent to MainActivity
            val intent = Intent(this@BoostForegroundService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("TOGGLE_FLASH", true)
            }
            startActivity(intent)
            
            android.util.Log.d(TAG, "✅ Flash toggle intent sent to MainActivity")
        }
    }

    private fun startBoost() {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            
            android.util.Log.d(TAG, "🔊 START_BOOST: masterGain=${settings.masterGainPercent}, maxGainDb=${settings.maxGainDb}")
            android.util.Log.d(TAG, "Multi-stream active: $useMultiStream")
            
            // YENİ: Akıllı gain kontrolü ve uyarı
            val currentDevice = audioOutputMonitor?.getCurrentDevice() ?: AudioOutputMonitor.OutputDevice.UNKNOWN
            val deviceName = audioOutputMonitor?.getCurrentDeviceName()
            val recommendation = smartGainAdvisor.getRecommendationForDevice(currentDevice, deviceName)
            
            // Gain seviyesini logla
            smartGainAdvisor.logGainChange(settings.masterGainPercent, currentDevice)
            
            // Uyarı kontrolü
            val warning = smartGainAdvisor.getWarningForGain(settings.masterGainPercent, recommendation)
            if (warning != null) {
                android.util.Log.w(TAG, "${warning.emoji} ${warning.title}")
                android.util.Log.w(TAG, "Mevcut: %${warning.currentGain}, Önerilen: %${warning.recommendedGain}")
                android.util.Log.w(TAG, warning.message.replace("\n", " | "))
            }
            
            if (useMultiStream) {
                multiStreamManager?.let { manager ->
                    manager.setMasterGainForAllStreams(settings.masterGainPercent, settings.maxGainDb)
                    manager.setBassBoostForAllStreams(settings.bassBoostPercent)
                    manager.setVirtualizerForAllStreams(settings.virtualizerPercent)
                    
                    // EQ Priority: 10-band > Vocal/Music Balance > 3-band
                    val bands10 = settings.get10BandEQ()
                    val is10BandActive = bands10.any { it != 0f }
                    
                    when {
                        is10BandActive -> {
                            manager.set10BandEqualizerForAllStreams(bands10)
                            android.util.Log.d(TAG, "✅ Applied 10-Band EQ to all streams")
                        }
                        kotlin.math.abs(settings.vocalMusicBalance - 0.5f) > 0.05f -> {
                            manager.setVocalMusicBalanceForAllStreams(settings.vocalMusicBalance)
                            android.util.Log.d(TAG, "✅ Applied Vocal/Music Balance to all streams")
                        }
                        else -> {
                            manager.setSimpleEqualizerForAllStreams(
                                settings.eqLowGain, 
                                settings.eqMidGain, 
                                settings.eqHighGain
                            )
                            android.util.Log.d(TAG, "✅ Applied 3-Band EQ to all streams")
                        }
                    }
                    
                    manager.logActiveStreams()
                }
            } else {
                // Eski sistem (fallback)
                android.util.Log.d(TAG, "Loudness supported: ${audioEffects.isLoudnessSupported}")
                audioEffects.setMasterGain(settings.masterGainPercent, settings.maxGainDb)
                audioEffects.setBassBoost(settings.bassBoostPercent)
                audioEffects.setVirtualizer(settings.virtualizerPercent)
                
                val bands10 = settings.get10BandEQ()
                val is10BandActive = bands10.any { it != 0f }
                
                when {
                    is10BandActive -> {
                        audioEffects.set10BandEqualizer(bands10)
                        android.util.Log.d(TAG, "✅ Applied 10-Band EQ")
                    }
                    kotlin.math.abs(settings.vocalMusicBalance - 0.5f) > 0.05f -> {
                        audioEffects.setVocalMusicBalance(settings.vocalMusicBalance)
                        android.util.Log.d(TAG, "✅ Applied Vocal/Music Balance")
                    }
                    else -> {
                        audioEffects.setEqualizer(settings.eqLowGain, settings.eqMidGain, settings.eqHighGain)
                        android.util.Log.d(TAG, "✅ Applied 3-Band EQ")
                    }
                }
            }
            
            val notification = createNotification(settings.masterGainPercent, settings.bassBoostPercent)
            startForeground(NOTIFICATION_ID, notification)
            
            // Update widgets
            updateWidgets()
        }
    }

    private fun stopBoost() {
        android.util.Log.d(TAG, "🛑 Stopping boost service COMPLETELY...")
        
        // Turn off boost in preferences so it doesn't restart
        serviceScope.launch {
            prefs.setBoostEnabled(false)
            android.util.Log.d(TAG, "✅ Boost disabled in preferences")
        }
        
        // Release all audio resources
        if (useMultiStream) {
            multiStreamManager?.release()
            multiStreamManager = null
            android.util.Log.d(TAG, "✅ Multi-stream manager released")
        } else {
            audioEffects.release()
            android.util.Log.d(TAG, "✅ Audio effects released")
        }
        
        // Stop monitoring
        audioOutputMonitor?.stopMonitoring()
        android.util.Log.d(TAG, "✅ Audio output monitor stopped")
        
        // Update widgets to show stopped state
        updateWidgets()
        
        // Stop foreground and kill service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        android.util.Log.d(TAG, "✅ Service stopped completely")
    }

    private fun updateEffects() {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            
            android.util.Log.d(TAG, "🔄 UPDATE_EFFECTS: masterGain=${settings.masterGainPercent}, maxGainDb=${settings.maxGainDb}")
            
            if (useMultiStream) {
                multiStreamManager?.let { manager ->
                    manager.setMasterGainForAllStreams(settings.masterGainPercent, settings.maxGainDb)
                    manager.setBassBoostForAllStreams(settings.bassBoostPercent)
                    manager.setVirtualizerForAllStreams(settings.virtualizerPercent)
                    
                    val bands10 = settings.get10BandEQ()
                    val is10BandActive = bands10.any { it != 0f }
                    
                    when {
                        is10BandActive -> {
                            manager.set10BandEqualizerForAllStreams(bands10)
                            android.util.Log.d(TAG, "✅ Updated 10-Band EQ to all streams")
                        }
                        kotlin.math.abs(settings.vocalMusicBalance - 0.5f) > 0.05f -> {
                            manager.setVocalMusicBalanceForAllStreams(settings.vocalMusicBalance)
                            android.util.Log.d(TAG, "✅ Updated Vocal/Music Balance to all streams")
                        }
                        else -> {
                            manager.setSimpleEqualizerForAllStreams(
                                settings.eqLowGain,
                                settings.eqMidGain,
                                settings.eqHighGain
                            )
                            android.util.Log.d(TAG, "✅ Updated 3-Band EQ to all streams")
                        }
                    }
                }
            } else {
                audioEffects.setMasterGain(settings.masterGainPercent, settings.maxGainDb)
                audioEffects.setBassBoost(settings.bassBoostPercent)
                audioEffects.setVirtualizer(settings.virtualizerPercent)
                
                val bands10 = settings.get10BandEQ()
                val is10BandActive = bands10.any { it != 0f }
                
                when {
                    is10BandActive -> {
                        audioEffects.set10BandEqualizer(bands10)
                        android.util.Log.d(TAG, "✅ Applied 10-Band EQ: ${bands10.contentToString()}")
                    }
                    kotlin.math.abs(settings.vocalMusicBalance - 0.5f) > 0.05f -> {
                        audioEffects.setVocalMusicBalance(settings.vocalMusicBalance)
                        android.util.Log.d(TAG, "✅ Applied Vocal/Music Balance: ${settings.vocalMusicBalance}")
                    }
                    else -> {
                        audioEffects.setEqualizer(settings.eqLowGain, settings.eqMidGain, settings.eqHighGain)
                        android.util.Log.d(TAG, "✅ Applied 3-Band EQ")
                    }
                }
            }
            
            // Apply call enhancement (microphone noise suppression + auto gain)
            if (useMultiStream) {
                multiStreamManager?.setCallEnhancementForAllStreams(settings.isCallEnhancementEnabled)
            } else {
                audioEffects.setCallEnhancement(settings.isCallEnhancementEnabled)
            }
            android.util.Log.d(TAG, "📞 Call Enhancement: ${settings.isCallEnhancementEnabled}")
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = createNotification(settings.masterGainPercent, settings.bassBoostPercent)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun maximizeVolume() {
        volumeController.maximizeAllStreams()
    }

    /**
     * Ses çıkış cihazı değiştiğinde otomatik ayarlama
     */
    private fun handleAudioDeviceChange(device: AudioOutputMonitor.OutputDevice, name: String?) {
        serviceScope.launch {
            val settings = prefs.settings.firstOrNull() ?: return@launch
            
            // Akıllı öneri al
            val recommendation = smartGainAdvisor.getRecommendationForDevice(device, name)
            
            android.util.Log.d(TAG, "🎚️ Cihaz değişti: ${recommendation.deviceName}")
            android.util.Log.d(TAG, "💡 Önerilen gain: ${recommendation.recommendedGain}% (mevcut: ${settings.masterGainPercent}%)")
            android.util.Log.d(TAG, "📊 Güvenli aralık: ${recommendation.minSafeGain}%-${recommendation.maxSafeGain}%")
            android.util.Log.d(TAG, "⚠️ Maksimum limit: ${recommendation.maxAbsoluteGain}%")
            
            // Uyarı kontrolü
            val warning = smartGainAdvisor.getWarningForGain(settings.masterGainPercent, recommendation)
            if (warning != null) {
                android.util.Log.w(TAG, "${warning.emoji} ${warning.title}: ${warning.message}")
            }
            
            // Gain log
            smartGainAdvisor.logGainChange(settings.masterGainPercent, device)
            
            // Bildirimde göster (NO EMOJIS)
            val deviceText = when (device) {
                AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> "Bluetooth: $name"
                AudioOutputMonitor.OutputDevice.BLUETOOTH_SPEAKER -> "Bluetooth Speaker: $name"
                AudioOutputMonitor.OutputDevice.WIRED_HEADSET -> "Wired Headset"
                AudioOutputMonitor.OutputDevice.WIRED_HEADPHONE -> "Headphone"
                AudioOutputMonitor.OutputDevice.USB_HEADSET -> "USB-C Headset"
                AudioOutputMonitor.OutputDevice.USB_DEVICE -> "USB DAC: $name"
                AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> "Phone Speaker"
                else -> "Audio Device"
            }
            
            // NO warning emoji
            val deviceTextClean = deviceText
            
            // Notification güncelle (cihaz bilgisi, NO EMOJIS)
            val notification = createNotificationWithDevice(settings.masterGainPercent, settings.bassBoostPercent, deviceTextClean)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(volumePercent: Int, bassPercent: Int): Notification {
        val deviceInfo = audioOutputMonitor?.let {
            val (device, name) = it.detectCurrentDevice()
            when (device) {
                AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> "BT: ${name ?: "Headset"}"
                AudioOutputMonitor.OutputDevice.WIRED_HEADSET -> "Wired"
                else -> null
            }
        }
        
        return createNotificationWithDevice(volumePercent, bassPercent, deviceInfo)
    }

    private fun createNotificationWithDevice(volumePercent: Int, bassPercent: Int, deviceText: String?): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop action
        val stopIntent = Intent(this, BoostForegroundService::class.java).apply {
            action = "STOP_BOOST"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Bass Down action
        val bassDownIntent = Intent(this, BoostForegroundService::class.java).apply {
            action = "BASS_DOWN"
        }
        val bassDownPendingIntent = PendingIntent.getService(
            this, 2, bassDownIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Bass Up action
        val bassUpIntent = Intent(this, BoostForegroundService::class.java).apply {
            action = "BASS_UP"
        }
        val bassUpPendingIntent = PendingIntent.getService(
            this, 3, bassUpIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Flash Toggle action
        val flashToggleIntent = Intent(this, BoostForegroundService::class.java).apply {
            action = "TOGGLE_FLASH"
        }
        val flashTogglePendingIntent = PendingIntent.getService(
            this, 6, flashToggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build modern content (NO EMOJIS - clean professional look)
        val volumeLabel = getString(R.string.notif_volume)
        val bassLabel = getString(R.string.notif_bass)
        
        // Clean content text - only Volume and Bass
        val contentText = "$volumeLabel $volumePercent% • $bassLabel $bassPercent%"
        
        // SubText: Device info (clean, no emojis)
        val subText = deviceText?.replace(Regex("[\\p{So}\\p{Cn}]"), "")?.trim() ?: getString(R.string.notif_active)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(
                android.graphics.BitmapFactory.decodeResource(
                    resources,
                    R.mipmap.ic_launcher
                )
            )
            .setContentTitle("Sound'ST Boost")
            .setContentText(contentText)
            .setSubText(subText)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2, 3)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .setColorized(true)
            .setColor(0xFFFFB74D.toInt()) // Modern orange color
            .addAction(
                R.drawable.ic_bass_down,
                getString(R.string.notif_bass_down),
                bassDownPendingIntent
            )
            .addAction(
                R.drawable.ic_bass_up,
                getString(R.string.notif_bass_up),
                bassUpPendingIntent
            )
            .addAction(
                R.drawable.ic_flash,
                "Flash",
                flashTogglePendingIntent
            )
            .addAction(
                R.drawable.ic_stop_notification,
                getString(R.string.notif_stop),
                stopPendingIntent
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
    
    /**
     * Set volume from widget (v1.4.0)
     * Enables boost if not already enabled and sets the volume
     */
    private fun setVolumeFromWidget(percent: Int) {
        serviceScope.launch {
            android.util.Log.d(TAG, "🎛️ Widget volume change: $percent%")
            
            // Save new volume to preferences
            prefs.setMasterGain(percent)
            
            // Start boost if not already enabled
            val settings = prefs.settings.firstOrNull()
            if (settings?.isBoostEnabled != true) {
                prefs.setBoostEnabled(true)
                startBoost()
            } else {
                // Just update the volume
                updateEffects()
            }
            
            // Update widgets to show new state
            updateWidgets()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d(TAG, "🔌 Service onDestroy - Releasing resources...")
        
        // Ses cihazı izlemeyi durdur
        audioOutputMonitor?.stopMonitoring()
        audioOutputMonitor = null
        
        if (useMultiStream) {
            multiStreamManager?.release()
            multiStreamManager = null
        } else {
            audioEffects.release()
        }
        
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
