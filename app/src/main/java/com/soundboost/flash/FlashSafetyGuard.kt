package com.soundboost.flash

import android.util.Log
import com.soundboost.data.SyncPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * CRITICAL: Single point of control for ALL flash operations (epilepsy safety).
 * 
 * Rules:
 * 1. User MUST accept warning (hasAcceptedFlashWarning = true)
 * 2. Minimum 333ms between torch ON operations (prevent rapid flashing)
 * 3. Torch OFF is NEVER blocked
 */
object FlashSafetyGuard {
    private const val TAG = "FlashSafetyGuard"
    private const val MIN_FLASH_INTERVAL_MS = 333L
    
    @Volatile
    private var lastTorchOnTimeMs = 0L
    
    @Volatile
    private var hasAcceptedWarningCache: Boolean? = null
    
    /**
     * Check if torch ON operation is allowed.
     * NEVER call this for torch OFF operations.
     */
    @Synchronized
    fun canTurnOnTorch(preferences: SyncPreferences): Boolean {
        // Check 1: User must accept warning
        val accepted = hasAcceptedWarningCache ?: runBlocking {
            val value = preferences.hasAcceptedFlashWarning.first()
            hasAcceptedWarningCache = value
            value
        }
        
        if (!accepted) {
            Log.w(TAG, "⚠️ Torch blocked - warning not accepted")
            return false
        }
        
        // Check 2: Minimum interval between flashes (epilepsy prevention)
        val now = System.currentTimeMillis()
        val elapsed = now - lastTorchOnTimeMs
        
        if (elapsed < MIN_FLASH_INTERVAL_MS) {
            Log.w(TAG, "⚠️ Torch blocked - too fast (${elapsed}ms < ${MIN_FLASH_INTERVAL_MS}ms)")
            return false
        }
        
        // Update timestamp
        lastTorchOnTimeMs = now
        return true
    }
    
    /**
     * Call this when user accepts warning to update cache.
     */
    fun onWarningAccepted() {
        hasAcceptedWarningCache = true
        Log.d(TAG, "✅ Flash warning accepted, cache updated")
    }
    
    /**
     * For testing/debugging - reset state
     */
    fun resetForTesting() {
        lastTorchOnTimeMs = 0L
        hasAcceptedWarningCache = null
    }
}
