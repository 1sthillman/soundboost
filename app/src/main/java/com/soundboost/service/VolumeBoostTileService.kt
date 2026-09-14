package com.soundboost.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.soundboost.MainActivity
import com.soundboost.data.BoostPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings Tile for Volume Boost
 * 
 * Features:
 * - Single tap: Toggle boost on/off
 * - Long press: Open main app
 * - Dynamic label showing current boost percentage
 * - Material 3 themed icon
 * 
 * Google Play Compliant:
 * - Standard Android Quick Settings API
 * - No data collection
 * - User-initiated actions only
 */
class VolumeBoostTileService : TileService() {
    
    companion object {
        private const val TAG = "VolumeBoostTile"
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var prefs: BoostPreferences
    
    override fun onCreate() {
        super.onCreate()
        prefs = BoostPreferences(applicationContext)
        Log.d(TAG, "📱 Tile service created")
    }
    
    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "👂 Tile listening started")
        updateTile()
    }
    
    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "🛑 Tile listening stopped")
    }
    
    override fun onClick() {
        super.onClick()
        Log.d(TAG, "👆 Tile clicked")
        
        scope.launch {
            try {
                val currentSettings = prefs.settings.first()
                val newState = !currentSettings.isBoostEnabled
                
                Log.d(TAG, "🔄 Toggling boost: ${currentSettings.isBoostEnabled} → $newState")
                
                // Toggle boost via service
                val intent = Intent(applicationContext, BoostForegroundService::class.java).apply {
                    action = if (newState) "START_BOOST" else "STOP_BOOST"
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                
                // Update tile immediately
                updateTile(forceState = newState)
                
                // Show feedback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    showDialog(null) // Collapse shade
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error toggling boost from tile", e)
            }
        }
    }
    
    /**
     * Update tile appearance based on current boost state
     */
    private fun updateTile(forceState: Boolean? = null) {
        scope.launch {
            try {
                val settings = prefs.settings.first()
                val isEnabled = forceState ?: settings.isBoostEnabled
                val boostPercent = settings.masterGainPercent
                
                qsTile?.apply {
                    // Set state (active/inactive)
                    state = if (isEnabled) {
                        Tile.STATE_ACTIVE
                    } else {
                        Tile.STATE_INACTIVE
                    }
                    
                    // Set label with current boost percentage
                    label = if (isEnabled) {
                        "Boost: $boostPercent%"
                    } else {
                        getString(com.soundboost.R.string.app_name)
                    }
                    
                    // Set subtitle (Android 10+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = if (isEnabled) {
                            "Active"
                        } else {
                            "Tap to enable"
                        }
                    }
                    
                    // Update tile
                    updateTile()
                    
                    Log.d(TAG, "✅ Tile updated: state=$state, label=$label")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating tile", e)
            }
        }
    }
    
    /**
     * Handle long press - open main app
     */
    override fun onTileAdded() {
        super.onTileAdded()
        Log.d(TAG, "➕ Tile added by user")
        updateTile()
    }
    
    override fun onTileRemoved() {
        super.onTileRemoved()
        Log.d(TAG, "➖ Tile removed by user")
    }
    
    /**
     * Open main app when tile is long-pressed
     */
    fun openApp() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivityAndCollapse(intent)
    }
}
