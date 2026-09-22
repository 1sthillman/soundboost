package com.soundboost.flash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.soundboost.sync.FlashMode
import com.soundboost.sync.SyncMessage

/**
 * Birincil flaş çıktısı: kamera izni gerektirmez, öngörülebilir gecikme.
 * NO fake data / real effects kuralına uygun — burada gerçekten ekranı beyaza boyuyoruz,
 * dekoratif bir animasyon değil.
 *
 * Kullanım: FlashControlScreen kendi tetikleme mantığını (host butonu veya SyncClient
 * flashEvents akışı) [pendingFlash] state'ine yazar, bu composable de görsel çıktıyı üretir.
 */
@Composable
fun ScreenFlashOverlay(
    pendingFlash: SyncMessage.Flash?,
    onFlashConsumed: () -> Unit,
    onRequestTorchPulse: suspend (durationMs: Int, repeatCount: Int, intervalMs: Int) -> Unit = { _, _, _ -> }
) {
    val alpha = remember { Animatable(0f) }
    val flashColor = remember(pendingFlash?.color) {
        runCatching { Color(android.graphics.Color.parseColor(pendingFlash?.color ?: "#FFFFFF")) }
            .getOrDefault(Color.White)
    }

    LaunchedEffect(pendingFlash) {
        val flash = pendingFlash ?: return@LaunchedEffect

        if (flash.mode == FlashMode.TORCH_ONLY || flash.mode == FlashMode.SCREEN_AND_TORCH) {
            onRequestTorchPulse(flash.durationMs, flash.repeatCount, flash.intervalMs)
        }

        if (flash.mode == FlashMode.SCREEN_ONLY || flash.mode == FlashMode.SCREEN_AND_TORCH) {
            repeat(flash.repeatCount) { index ->
                alpha.snapTo(0f)
                alpha.animateTo(1f, animationSpec = tween(durationMillis = FLASH_RAMP_MS, easing = LinearEasing))
                alpha.animateTo(
                    0f,
                    animationSpec = tween(durationMillis = flash.durationMs.coerceAtLeast(FLASH_RAMP_MS), easing = LinearEasing)
                )
                if (index != flash.repeatCount - 1 && flash.intervalMs > 0) {
                    kotlinx.coroutines.delay(flash.intervalMs.toLong())
                }
            }
        } else {
            // Sadece torch modundaysa ekranda görsel gösterge yok — süreyi bekleyip tüket.
            kotlinx.coroutines.delay((flash.durationMs * flash.repeatCount).toLong())
        }

        onFlashConsumed()
    }

    if (alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(flashColor.copy(alpha = alpha.value))
                .zIndex(FLASH_Z_INDEX)
        )
    }
}

private const val FLASH_RAMP_MS = 40
private const val FLASH_Z_INDEX = 999f
