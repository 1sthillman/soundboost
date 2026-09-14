package com.soundboost.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Battery Optimization Helper
 * 
 * Helps users disable battery optimization for the app to prevent
 * Android from killing the boost service in background.
 * 
 * GOOGLE PLAY COMPLIANT:
 * - Does NOT auto-request exemption (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
 * - Only navigates user to settings (user must manually whitelist)
 * - Provides OEM-specific deep links for better UX
 * 
 * Reference: https://dontkillmyapp.com/
 */
object BatteryOptimizationHelper {
    
    private const val TAG = "BatteryOptHelper"
    
    /**
     * Check if app is already whitelisted from battery optimization
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // No battery optimization on Android < 6.0
        }
    }
    
    /**
     * Open battery optimization settings
     * GOOGLE PLAY COMPLIANT: Navigates to settings list, user must manually select app
     */
    fun openBatterySettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        Log.d(TAG, "Opening battery settings for manufacturer: $manufacturer")
        
        // Try manufacturer-specific settings first
        val manufacturerIntent = when {
            manufacturer.contains("xiaomi") -> getXiaomiIntent(context)
            manufacturer.contains("samsung") -> getSamsungIntent(context)
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> getHuaweiIntent(context)
            manufacturer.contains("oppo") -> getOppoIntent(context)
            manufacturer.contains("oneplus") -> getOnePlusIntent(context)
            manufacturer.contains("vivo") -> getVivoIntent(context)
            manufacturer.contains("asus") -> getAsusIntent(context)
            manufacturer.contains("nokia") -> getNokiaIntent(context)
            else -> null
        }
        
        // Try manufacturer intent first, fallback to generic
        if (manufacturerIntent != null && tryStartActivity(context, manufacturerIntent)) {
            Log.d(TAG, "✅ Opened manufacturer-specific settings")
            return
        }
        
        // Fallback: Generic Android battery optimization settings
        val genericIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (tryStartActivity(context, genericIntent)) {
            Log.d(TAG, "✅ Opened generic battery settings")
            return
        }
        
        // Last resort: App info page
        val appInfoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        tryStartActivity(context, appInfoIntent)
        Log.d(TAG, "⚠️ Opened app info as fallback")
    }
    
    /**
     * Get device manufacturer name for display
     */
    fun getManufacturerName(): String {
        return Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Get manufacturer-specific instruction
     */
    fun getManufacturerTip(context: Context): String? {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> context.getString(com.soundboost.R.string.battery_dialog_tip_xiaomi)
            "samsung" -> context.getString(com.soundboost.R.string.battery_dialog_tip_samsung)
            "oneplus" -> context.getString(com.soundboost.R.string.battery_dialog_tip_oneplus)
            "huawei", "honor" -> context.getString(com.soundboost.R.string.battery_dialog_tip_huawei)
            else -> null
        }
    }
    
    // --- OEM-Specific Intents ---
    
    private fun getXiaomiIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager))
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getSamsungIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getHuaweiIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getOppoIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getOnePlusIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getVivoIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getAsusIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.powersaver.PowerSaverSettings"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getNokiaIntent(context: Context): Intent? {
        return try {
            Intent().apply {
                component = ComponentName(
                    "com.evenwell.powersaving.g3",
                    "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Try to start activity, return true if successful
     */
    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start activity: ${e.message}")
            false
        }
    }
}
