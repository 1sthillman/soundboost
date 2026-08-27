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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = BoostPreferences(appContext)
                val settings = prefs.settings.first()

                if (settings.autoStartOnBoot && settings.isBoostEnabled) {
                    val serviceIntent = Intent(appContext, BoostForegroundService::class.java).apply {
                        action = "START_BOOST"
                    }
                    ContextCompat.startForegroundService(appContext, serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
