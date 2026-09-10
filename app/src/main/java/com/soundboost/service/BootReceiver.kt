package com.soundboost.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.soundboost.data.BoostPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Telefon yeniden başladığında, kullanıcı "otomatik başlat" ayarını açtıysa
 * ve boost daha önce etkinse, son ayarlarla foreground servisi tekrar başlatır.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Tüm boot action'larını yakala
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )
        
        if (intent.action !in validActions) return
        
        android.util.Log.d("BootReceiver", "📱 Device boot detected: ${intent.action}")

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = BoostPreferences(appContext)
                val settings = prefs.settings.first()

                android.util.Log.d("BootReceiver", "⚙️ Settings: autoStart=${settings.autoStartOnBoot}, boost=${settings.isBoostEnabled}")

                if (settings.autoStartOnBoot && settings.isBoostEnabled) {
                    android.util.Log.d("BootReceiver", "🚀 Starting BoostForegroundService...")
                    val serviceIntent = Intent(appContext, BoostForegroundService::class.java).apply {
                        action = "START_BOOST"
                    }
                    try {
                        ContextCompat.startForegroundService(appContext, serviceIntent)
                        android.util.Log.d("BootReceiver", "✅ Service started successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("BootReceiver", "❌ Failed to start service: ${e.message}")
                    }
                } else {
                    android.util.Log.d("BootReceiver", "⏭️ Auto-start disabled or boost was off")
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "❌ Error in boot receiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
