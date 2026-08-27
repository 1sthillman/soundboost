package com.stdev.soundstboost.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.stdev.soundstboost.ui.theme.NeonCyan
import com.stdev.soundstboost.ui.theme.NeonPink
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tamamen dekoratif bir animasyondur; gerçek ses verisini OKUMAZ (bu yüzden
 * RECORD_AUDIO izni gerekmez ve istenmez). Boost aktifken hafif bir "canlı"
 * görünüm sağlamak için tasarlandı.
 */
@Composable
fun EqualizerVisualizer(isActive: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val barCount = 24
        val barWidth = size.width / (barCount * 1.6f)
        val gap = barWidth * 0.6f

        for (i in 0 until barCount) {
            val amplitude = if (isActive) abs(sin(phase.value + i * 0.4f)) else 0.08f
            val barHeight = size.height * (0.15f + amplitude * 0.85f)
            val x = i * (barWidth + gap)
            drawLine(
                brush = Brush.verticalGradient(listOf(NeonCyan, NeonPink)),
                start = Offset(x, size.height),
                end = Offset(x, size.height - barHeight),
                strokeWidth = barWidth
            )
        }
    }
}
