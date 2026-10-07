package com.soundboost.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Bluetooth Call Volume Booster
 * 
 * Automatically boosts STREAM_VOICE_CALL volume to maximum when:
 * - A call is active (MODE_IN_CALL or MODE_IN_COMMUNICATION)
 * - Audio route is Bluetooth SCO (call audio path)
 * 
 * NO PERMISSIONS REQUIRED:
 * - Uses AudioManager.mode for call detection (no READ_PHONE_STATE)
 * - Uses AudioManager APIs for Bluetooth detection (no BLUETOOTH_CONNECT)
 * 
 * PRIVACY COMPLIANT:
 * - Only detects call state and Bluetooth routing
 * - Does NOT access call details, phone numbers, or contacts
 */
class CallVolumeBooster(
    private val context: Context
) {
    companion object {
        private const val TAG = "CallVolumeBooster"
        private const val PREFS_NAME = "call_volume_booster_prefs"
        private const val KEY_SAVED_LEVEL = "saved_voice_call_level"
        private const val KEY_BOOSTED_LEVEL = "boosted_level"
        
        // Target: maximum volume
        private const val TARGET_LEVEL_MULTIPLIER = 1.0f  // 100% of max
        
        // Polling interval for API < 31
        private const val POLLING_INTERVAL_MS = 1000L
        
        // SCO connection retry delays (in ms)
        private val SCO_RETRY_DELAYS = listOf(500L, 1000L, 2000L)
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private var isEnabled = false
    private var isCallActive = false
    private var savedLevel: Int? = null
    private var boostedLevel: Int? = null
    
    // For API < 31 polling
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    
    // For API >= 31 listener
    private var modeChangedListener: AudioManager.OnModeChangedListener? = null
    
    /**
     * Start monitoring if enabled
     * Must be called from main thread or will be posted to main thread
     */
    fun startMonitoring(enabled: Boolean) {
        runOnMainThread {
            Log.d(TAG, "startMonitoring: enabled=$enabled, SDK=${Build.VERSION.SDK_INT}, MANUFACTURER=${Build.MANUFACTURER}, MODEL=${Build.MODEL}")
            
            if (!enabled) {
                stopMonitoring()
                return@runOnMainThread
            }
            
            isEnabled = true
            
            // Restore any pending volume on startup
            restoreVolumeIfNeeded()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startModeListenerApi31()
            } else {
                startPollingApi24()
            }
        }
    }
    
    /**
     * Stop all monitoring and clean up
     * Can be called multiple times safely
     * Must be called from main thread or will be posted to main thread
     */
    fun stopMonitoring() {
        runOnMainThread {
            Log.d(TAG, "stopMonitoring")
            
            if (!isEnabled && modeChangedListener == null && pollingRunnable == null) {
                Log.d(TAG, "stopMonitoring: already stopped")
                return@runOnMainThread
            }
            
            isEnabled = false
            
            // Cancel all pending callbacks (polling and retries)
            handler.removeCallbacksAndMessages(null)
            
            // Restore volume before stopping
            if (isCallActive) {
                restoreVolume()
            }
            
            // Clean up listeners/handlers
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                stopModeListenerApi31()
            } else {
                stopPollingApi24()
            }
            
            isCallActive = false
        }
    }
    
    /**
     * API 31+: Use OnModeChangedListener
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startModeListenerApi31() {
        // Remove existing listener if any
        if (modeChangedListener != null) {
            stopModeListenerApi31()
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        
        modeChangedListener = AudioManager.OnModeChangedListener { mode ->
            Log.d(TAG, "onModeChanged: mode=$mode")
            handleModeChange(mode)
        }
        
        audioManager.addOnModeChangedListener(executor, modeChangedListener!!)
        
        // Check initial state
        handleModeChange(audioManager.mode)
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopModeListenerApi31() {
        modeChangedListener?.let {
            audioManager.removeOnModeChangedListener(it)
            modeChangedListener = null
        }
    }
    
    /**
     * API 24-30: Polling (when call is active OR Bluetooth audio connected)
     */
    private fun startPollingApi24() {
        // Remove existing polling if any
        if (pollingRunnable != null) {
            stopPollingApi24()
        }
        
        pollingRunnable = object : Runnable {
            override fun run() {
                // Poll if call is active OR Bluetooth audio is active
                // (to detect call end even if Bluetooth disconnects)
                if (isCallActive || isBluetoothAudioActive()) {
                    handleModeChange(audioManager.mode)
                }
                
                handler.postDelayed(this, POLLING_INTERVAL_MS)
            }
        }
        
        handler.post(pollingRunnable!!)
    }
    
    private fun stopPollingApi24() {
        pollingRunnable?.let {
            handler.removeCallbacks(it)
            pollingRunnable = null
        }
    }
    
    /**
     * Handle audio mode change
     */
    private fun handleModeChange(mode: Int) {
        val inCall = mode == AudioManager.MODE_IN_CALL || 
                     mode == AudioManager.MODE_IN_COMMUNICATION
        
        Log.d(TAG, "handleModeChange: mode=$mode, inCall=$inCall, wasCallActive=$isCallActive")
        
        if (inCall && !isCallActive) {
            // Call started
            onCallStarted()
        } else if (!inCall && isCallActive) {
            // Call ended
            onCallEnded()
        }
    }
    
    /**
     * Call started - check if Bluetooth and boost if needed
     */
    private fun onCallStarted() {
        Log.d(TAG, "📞 Call started")
        isCallActive = true
        
        // Check if Bluetooth SCO is active (may need retries)
        checkAndBoostWithRetries(0)
    }
    
    /**
     * Check Bluetooth and boost, with retries for SCO connection delay
     */
    private fun checkAndBoostWithRetries(attemptIndex: Int) {
        if (!isEnabled || !isCallActive) return
        
        val isBluetoothCall = isBluetoothScoActive()
        
        Log.d(TAG, "checkAndBoost: attempt=${attemptIndex + 1}, isBluetoothSco=$isBluetoothCall")
        
        if (isBluetoothCall) {
            boostCallVolume()
        } else if (attemptIndex < SCO_RETRY_DELAYS.size) {
            // Retry after delay
            val delay = SCO_RETRY_DELAYS[attemptIndex]
            Log.d(TAG, "SCO not active yet, retrying in ${delay}ms")
            
            handler.postDelayed({
                checkAndBoostWithRetries(attemptIndex + 1)
            }, delay)
        } else {
            Log.d(TAG, "SCO not active after all retries, not boosting (earpiece/speaker call)")
        }
    }
    
    /**
     * Call ended - restore volume
     */
    private fun onCallEnded() {
        Log.d(TAG, "📞 Call ended")
        isCallActive = false
        restoreVolume()
    }
    
    /**
     * Boost STREAM_VOICE_CALL to maximum
     */
    private fun boostCallVolume() {
        try {
            // Log device/route info
            val commDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.communicationDevice
            } else null
            val commDeviceType = commDevice?.type?.toString() ?: "null"
            val isScoOn = audioManager.isBluetoothScoOn
            val isA2dpOn = audioManager.isBluetoothA2dpOn
            val mode = audioManager.mode
            
            Log.d(TAG, "📞 boostCallVolume: commDevice=$commDeviceType, scoOn=$isScoOn, a2dpOn=$isA2dpOn, mode=$mode")
            
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            
            // Calculate target: max(current, TARGET)
            val targetVolume = maxOf(currentVolume, (maxVolume * TARGET_LEVEL_MULTIPLIER).toInt())
            
            Log.d(TAG, "📊 Volume levels: current=$currentVolume, max=$maxVolume, target=$targetVolume")
            
            // Only boost if target is higher than current
            if (targetVolume > currentVolume) {
                // Save current level
                savedLevel = currentVolume
                boostedLevel = targetVolume
                
                // Save to persistent storage
                prefs.edit()
                    .putInt(KEY_SAVED_LEVEL, currentVolume)
                    .putInt(KEY_BOOSTED_LEVEL, targetVolume)
                    .apply()
                
                // Apply boost
                audioManager.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    targetVolume,
                    0  // No UI flags
                )
                
                // Verify
                val actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                Log.d(TAG, "✅ Call volume boosted: $currentVolume -> $targetVolume (actual: $actualVolume)")
                
                if (actualVolume != targetVolume) {
                    Log.w(TAG, "⚠️ Actual volume ($actualVolume) differs from target ($targetVolume)")
                }
            } else {
                Log.d(TAG, "ℹ️ Current volume ($currentVolume) already at or above target ($targetVolume), not boosting")
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: Cannot modify VOICE_CALL volume", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to boost call volume", e)
        }
    }
    
    /**
     * Restore original volume
     */
    private fun restoreVolume() {
        try {
            // Log device/route info
            val commDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.communicationDevice
            } else null
            val commDeviceType = commDevice?.type?.toString() ?: "null"
            val isScoOn = audioManager.isBluetoothScoOn
            val isA2dpOn = audioManager.isBluetoothA2dpOn
            val mode = audioManager.mode
            
            Log.d(TAG, "🔄 restoreVolume: commDevice=$commDeviceType, scoOn=$isScoOn, a2dpOn=$isA2dpOn, mode=$mode")
            
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            val ourBoostedLevel = boostedLevel
            val originalLevel = savedLevel
            
            Log.d(TAG, "🔄 Restore check: current=$currentVolume, ourBoosted=$ourBoostedLevel, original=$originalLevel")
            
            // Only restore if:
            // 1. We have saved levels
            // 2. Current volume is still our boosted level (user didn't change it)
            if (originalLevel != null && ourBoostedLevel != null && currentVolume == ourBoostedLevel) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    originalLevel,
                    0  // No UI flags
                )
                
                val actualVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                Log.d(TAG, "✅ Volume restored: $ourBoostedLevel -> $originalLevel (actual: $actualVolume)")
            } else {
                Log.d(TAG, "ℹ️ Not restoring: user may have changed volume manually")
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: Cannot modify VOICE_CALL volume", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore volume", e)
        } finally {
            // Clear state
            savedLevel = null
            boostedLevel = null
            
            // Clear persistent storage
            prefs.edit()
                .remove(KEY_SAVED_LEVEL)
                .remove(KEY_BOOSTED_LEVEL)
                .apply()
        }
    }
    
    /**
     * Restore volume on startup if needed (e.g., after crash)
     */
    private fun restoreVolumeIfNeeded() {
        val savedLevelValue = prefs.getInt(KEY_SAVED_LEVEL, -1)
        val boostedLevelValue = prefs.getInt(KEY_BOOSTED_LEVEL, -1)
        
        if (savedLevelValue == -1 || boostedLevelValue == -1) {
            // No pending restore
            return
        }
        
        // Check if still in call
        val mode = audioManager.mode
        val inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        
        if (inCall) {
            // Still in call, don't restore yet
            Log.d(TAG, "🔄 Startup: still in call, not restoring")
            savedLevel = savedLevelValue
            boostedLevel = boostedLevelValue
            isCallActive = true
        } else {
            // Not in call anymore, restore if our level is still active
            try {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                
                if (currentVolume == boostedLevelValue) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_VOICE_CALL,
                        savedLevelValue,
                        0
                    )
                    Log.d(TAG, "✅ Startup: restored volume from $boostedLevelValue to $savedLevelValue")
                } else {
                    Log.d(TAG, "ℹ️ Startup: volume changed ($currentVolume != $boostedLevelValue), not restoring")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to restore on startup", e)
            }
            
            // Clear persistent storage
            prefs.edit()
                .remove(KEY_SAVED_LEVEL)
                .remove(KEY_BOOSTED_LEVEL)
                .apply()
        }
    }
    
    /**
     * Check if Bluetooth SCO is active (call audio routing)
     */
    private fun isBluetoothScoActive(): Boolean {
        val mode = audioManager.mode
        val isScoOn = audioManager.isBluetoothScoOn
        val isA2dpOn = audioManager.isBluetoothA2dpOn
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: Check communicationDevice
            val commDevice = audioManager.communicationDevice
            val commDeviceType = commDevice?.type?.toString() ?: "null"
            
            if (commDevice != null) {
                val isSco = commDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                           commDevice.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                Log.d(TAG, "isBluetoothScoActive: commDevice=$commDeviceType, scoOn=$isScoOn, a2dpOn=$isA2dpOn, mode=$mode -> $isSco")
                return isSco
            }
            
            Log.d(TAG, "isBluetoothScoActive: commDevice=null, scoOn=$isScoOn, a2dpOn=$isA2dpOn, mode=$mode")
        } else {
            Log.d(TAG, "isBluetoothScoActive: scoOn=$isScoOn, a2dpOn=$isA2dpOn, mode=$mode")
        }
        
        // Fallback: isBluetoothScoOn
        return isScoOn
    }
    
    /**
     * Check if any Bluetooth audio is active (for polling decision)
     */
    private fun isBluetoothAudioActive(): Boolean {
        return audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn
    }
    
    /**
     * Run code on main thread (post if needed)
     */
    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }
}
