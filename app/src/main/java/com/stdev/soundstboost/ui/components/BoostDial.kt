package com.stdev.soundstboost.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.stdev.soundstboost.ui.theme.NeonCyan
import com.stdev.soundstboost.ui.theme.NeonPink
import com.stdev.soundstboost.ui.theme.PanelDark

@Composable
fun BoostDial(
    isActive: Boolean,
    progressPercent: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(400)
    )
    val labelColor = if (isActive) NeonPink else Color(0xFF6A6A80)

    Box(
        modifier = modifier
            .size(180.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            drawCircle(color = PanelDark, radius = size.minDimension / 2)
            val strokePx = 10f
            drawArc(
                brush = Brush.sweepGradient(listOf(NeonCyan, NeonPink, NeonCyan)),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                style = Stroke(width = strokePx),
                topLeft = Offset(strokePx / 2, strokePx / 2),
                size = Size(size.width - strokePx, size.height - strokePx)
            )
        }
        Text(
            text = if (isActive) "AÇIK" else "KAPALI",
            style = MaterialTheme.typography.titleLarge,
            color = labelColor
        )
    }
}
