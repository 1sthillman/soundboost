package com.soundboost.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Premium Haptic Feedback System
 * Provides rich, nuanced tactile feedback for modern Android devices
 */
class HapticManager(private val context: Context, private val view: View?) {
    
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    /**
     * Light tap - for small UI interactions
     */
    fun lightTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }
    
    /**
     * Medium click - for button presses
     */
    fun click() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }
    
    /**
     * Heavy click - for important actions
     */
    fun heavyClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        }
    }
    
    /**
     * Double click - for toggle actions
     */
    fun doubleClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }
    
    /**
     * Success feedback - smooth and satisfying
     */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 50, 50)
            val amplitudes = intArrayOf(0, 80, 0, 120)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    /**
     * Error feedback - distinct and noticeable
     */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 80, 40, 80)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    /**
     * Drag feedback - continuous light taps during slider movement
     */
    fun drag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }
    
    /**
     * Volume change feedback - responsive to intensity
     */
    fun volumeChange(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (intensity * 100).toInt().coerceIn(1, 200)
            vibrator?.vibrate(VibrationEffect.createOneShot(15, amplitude))
        }
    }
    
    /**
     * Snap feedback - for snapping to preset values
     */
    fun snap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 25, 15, 40)
            val amplitudes = intArrayOf(0, 100, 0, 150)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    /**
     * Threshold feedback - when passing a limit
     */
    fun threshold() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 40)
            val amplitudes = intArrayOf(0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    /**
     * Audio reactive feedback - pulse with audio intensity
     */
    fun audioReactive(intensity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
            vibrator?.vibrate(VibrationEffect.createOneShot(50, amplitude))
        }
    }
}

/**
 * Composable to remember haptic manager
 */
@Composable
fun rememberHapticManager(): HapticManager {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        HapticManager(context, view)
    }
}
