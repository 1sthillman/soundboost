package com.soundboost.flash

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.soundboost.sync.FlashMode
import com.soundboost.sync.SyncMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ULTRA-OPTIMIZED SCREEN FLASH OVERLAY v2.0
 * 
 * - MAXIMUM brightness for full-screen flash
 * - Proper mode handling (TORCH_ONLY = no screen flash!)
 * - Instant full brightness (no fade-in)
 * - Color visibility optimized
 * - Guaranteed top-level rendering with Dialog
 * - EPILEPSY SAFETY: 333ms minimum interval between screen flashes
 */
@Composable
fun ScreenFlashOverlay(
    pendingFlash: SyncMessage.Flash?,
    onFlashConsumed: () -> Unit,
    onRequestTorchPulse: suspend (durationMs: Int, repeatCount: Int, intervalMs: Int) -> Unit = { _, _, _ -> },
    hasAcceptedWarning: Boolean
) {
    val alpha = remember { Animatable(0f) }
    val flashColor = remember(pendingFlash?.color) {
        runCatching { 
            Color(android.graphics.Color.parseColor(pendingFlash?.color ?: "#FFFFFF")) 
        }.getOrDefault(Color.White)
    }
    
    // EPILEPSY SAFETY: Track last screen flash time
    val lastScreenFlashTime = remember { mutableStateOf(0L) }
    
    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window

    LaunchedEffect(pendingFlash) {
        val flash = pendingFlash ?: return@LaunchedEffect
        
        // CRITICAL: Block flash if warning not accepted (epilepsy safety)
        if (!hasAcceptedWarning) {
            android.util.Log.w("ScreenFlashOverlay", "⚠️ Flash blocked - warning not accepted")
            onFlashConsumed()
            return@LaunchedEffect
        }
        
        // CRITICAL: Block screen flash if too soon after last one (epilepsy safety)
        val now = System.currentTimeMillis()
        val timeSinceLastFlash = now - lastScreenFlashTime.value
        if (timeSinceLastFlash < MIN_SCREEN_FLASH_INTERVAL_MS && lastScreenFlashTime.value > 0) {
            android.util.Log.w("ScreenFlashOverlay", "⚠️ Screen flash blocked - too fast (${timeSinceLastFlash}ms < ${MIN_SCREEN_FLASH_INTERVAL_MS}ms)")
            onFlashConsumed()
            return@LaunchedEffect
        }
        
        // CRITICAL: Set MAXIMUM screen brightness for flash visibility
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f
        window?.attributes = window?.attributes?.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL  // 1.0f = MAX
        }
        view.keepScreenOn = true

        coroutineScope {
            // Parallel execution for SCREEN_AND_TORCH mode
            if (flash.mode == FlashMode.TORCH_ONLY || flash.mode == FlashMode.SCREEN_AND_TORCH) {
                launch { 
                    onRequestTorchPulse(flash.durationMs, flash.repeatCount, flash.intervalMs) 
                }
            }

            // CRITICAL: Only show screen flash if mode includes SCREEN
            if (flash.mode == FlashMode.SCREEN_ONLY || flash.mode == FlashMode.SCREEN_AND_TORCH) {
                // Update last flash timestamp BEFORE starting animation
                lastScreenFlashTime.value = System.currentTimeMillis()
                
                repeat(flash.repeatCount) { index ->
                    // INSTANT full brightness - no fade-in!
                    alpha.snapTo(1f)
                    // Hold at FULL brightness
                    delay(flash.durationMs.toLong())
                    // Quick fade out
                    alpha.animateTo(
                        0f,
                        animationSpec = tween(durationMillis = FLASH_FADE_MS, easing = LinearEasing)
                    )
                    
                    // Interval between repeats
                    if (index < flash.repeatCount - 1 && flash.intervalMs > 0) {
                        delay(flash.intervalMs.toLong())
                    }
                }
            } else {
                // TORCH_ONLY mode - wait for duration without screen flash
                val totalDuration = (flash.durationMs * flash.repeatCount) + 
                                  (flash.intervalMs * (flash.repeatCount - 1))
                delay(totalDuration.toLong())
            }
        }
        
        // Restore original brightness
        window?.attributes = window?.attributes?.apply {
            screenBrightness = originalBrightness
        }
        view.keepScreenOn = false
        onFlashConsumed()
    }

    // CRITICAL: Render ONLY if mode includes SCREEN and alpha > 0
    if (alpha.value > 0f && 
        (pendingFlash?.mode == FlashMode.SCREEN_ONLY || pendingFlash?.mode == FlashMode.SCREEN_AND_TORCH)) {
        Dialog(
            onDismissRequest = { /* Cannot dismiss during flash */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false  // Fill entire screen including status bar
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(flashColor.copy(alpha = alpha.value))
                    .zIndex(Float.MAX_VALUE)
            )
        }
    }
}

private const val FLASH_FADE_MS = 50
private const val MIN_SCREEN_FLASH_INTERVAL_MS = 333L  // EPILEPSY SAFETY: minimum 333ms between screen flashes

