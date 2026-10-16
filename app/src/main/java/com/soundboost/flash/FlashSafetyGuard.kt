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
 * 2. Minimum interval between torch ON operations (prevent rapid flashing)
 *    - PARTY_MODE: 100ms (faster beats, controlled environment)
 *    - NORMAL: 333ms (maximum safety)
 * 3. Torch OFF is NEVER blocked
 */
object FlashSafetyGuard {
    private const val TAG = "FlashSafetyGuard"
    private const val MIN_FLASH_INTERVAL_NORMAL_MS = 333L
    private const val MIN_FLASH_INTERVAL_PARTY_MS = 100L  // Party Mode: faster beats allowed
    
    @Volatile
    private var lastTorchOnTimeMs = 0L
    
    @Volatile
    private var hasAcceptedWarningCache: Boolean? = null
    
    @Volatile
    private var partyModeEnabled = false
    
    /**
     * Enable Party Mode for faster flash rates (100ms vs 333ms).
     * User must still accept warning!
     */
    fun setPartyMode(enabled: Boolean) {
        partyModeEnabled = enabled
        Log.d(TAG, "🎉 Party Mode: ${if (enabled) "ENABLED (100ms limit)" else "DISABLED (333ms limit)"}")
    }
    
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
        val minInterval = if (partyModeEnabled) MIN_FLASH_INTERVAL_PARTY_MS else MIN_FLASH_INTERVAL_NORMAL_MS
        val now = System.currentTimeMillis()
        val elapsed = now - lastTorchOnTimeMs
        
        if (elapsed < minInterval) {
            Log.w(TAG, "⚠️ Torch blocked - too fast (${elapsed}ms < ${minInterval}ms) [Party: $partyModeEnabled]")
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
        partyModeEnabled = false
    }
}
