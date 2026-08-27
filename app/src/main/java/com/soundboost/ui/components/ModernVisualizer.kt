package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ModernVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier = Modifier
) {
    val barCount = 24
    val levels = audioLevels ?: FloatArray(barCount) { 0.1f }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 16.dp)
    ) {
        val barWidth = size.width / (barCount * 1.8f)
        val gap = barWidth * 0.8f
        val maxBarHeight = size.height * 0.9f
        
        for (i in 0 until barCount) {
            val level = if (isActive) levels.getOrNull(i) ?: 0.1f else 0.1f
            val barHeight = (maxBarHeight * level).coerceIn(size.height * 0.1f, maxBarHeight)
            val x = i * (barWidth + gap) + gap
            val y = size.height - barHeight
            
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(accent1, accent2),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
