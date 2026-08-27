package com.stdev.soundstboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.stdev.soundstboost.ui.theme.AppTheme
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun PremiumVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    theme: AppTheme,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val barCount = 32
    val levels = audioLevels ?: FloatArray(barCount) { 0.15f }
    
    val shimmerOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        when (theme) {
            AppTheme.NEON_DARK -> drawNeonStyle(levels, isActive, accent1, accent2, shimmerOffset)
            AppTheme.OCEAN_BLUE -> drawWaveStyle(levels, isActive, accent1, accent2, shimmerOffset)
            AppTheme.SUNSET_ORANGE -> drawFireStyle(levels, isActive, accent1, accent2, shimmerOffset)
            AppTheme.FOREST_GREEN -> drawPulseStyle(levels, isActive, accent1, accent2, shimmerOffset)
            AppTheme.ROYAL_PURPLE -> drawCrystalStyle(levels, isActive, accent1, accent2, shimmerOffset)
        }
    }
}

private fun DrawScope.drawNeonStyle(
    levels: FloatArray,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    shimmer: Float
) {
    val barCount = levels.size
    val barWidth = size.width / (barCount * 1.6f)
    val gap = barWidth * 0.6f
    val maxHeight = size.height * 0.95f
    
    for (i in 0 until barCount) {
        val level = if (isActive) levels.getOrNull(i) ?: 0.15f else 0.15f
        val barHeight = (maxHeight * level).coerceIn(size.height * 0.08f, maxHeight)
        val x = i * (barWidth + gap)
        val y = size.height - barHeight
        
        // Glow
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.6f),
                    accent2.copy(alpha = 0.3f)
                ),
                startY = y - 10f,
                endY = size.height
            ),
            topLeft = Offset(x - 4f, y - 10f),
            size = Size(barWidth + 8f, barHeight + 10f),
            cornerRadius = CornerRadius(barWidth, barWidth),
            blendMode = BlendMode.Plus
        )
        
        // Main bar
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
        
        // Shimmer
        if (isActive) {
            val shimmerY = y + (barHeight * shimmer)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(x, shimmerY - 2f),
                size = Size(barWidth, 4f),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
                blendMode = BlendMode.Plus
            )
        }
    }
}

private fun DrawScope.drawWaveStyle(
    levels: FloatArray,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    shimmer: Float
) {
    val barCount = levels.size
    val barWidth = size.width / (barCount * 1.3f)
    val gap = barWidth * 0.3f
    val centerY = size.height / 2
    
    for (i in 0 until barCount) {
        val level = if (isActive) levels.getOrNull(i) ?: 0.15f else 0.15f
        val barHeight = (size.height * 0.4f * level).coerceIn(size.height * 0.05f, size.height * 0.45f)
        val x = i * (barWidth + gap)
        
        // Mirror effect
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(accent2, accent1, accent2),
                startY = centerY - barHeight,
                endY = centerY + barHeight
            ),
            topLeft = Offset(x, centerY - barHeight),
            size = Size(barWidth, barHeight * 2),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
        
        // Reflection
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent1.copy(alpha = 0.2f)
                ),
                startY = centerY,
                endY = centerY + barHeight
            ),
            topLeft = Offset(x, centerY),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            blendMode = BlendMode.Plus
        )
    }
}

private fun DrawScope.drawFireStyle(
    levels: FloatArray,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    shimmer: Float
) {
    val barCount = levels.size
    val barWidth = size.width / (barCount * 1.4f)
    val gap = barWidth * 0.4f
    val maxHeight = size.height * 0.9f
    
    for (i in 0 until barCount) {
        val level = if (isActive) levels.getOrNull(i) ?: 0.15f else 0.15f
        val barHeight = (maxHeight * level).coerceIn(size.height * 0.1f, maxHeight)
        val x = i * (barWidth + gap)
        val y = size.height - barHeight
        
        // Flame effect with multiple layers
        for (layer in 0..2) {
            val layerHeight = barHeight * (1f - layer * 0.2f)
            val layerY = size.height - layerHeight
            val layerAlpha = 1f - layer * 0.3f
            
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = layerAlpha * 0.8f),
                        accent1.copy(alpha = layerAlpha),
                        accent2.copy(alpha = layerAlpha * 0.7f),
                        Color.Red.copy(alpha = layerAlpha * 0.3f)
                    ),
                    startY = layerY,
                    endY = size.height
                ),
                topLeft = Offset(x + layer * 2f, layerY),
                size = Size(barWidth - layer * 4f, layerHeight),
                cornerRadius = CornerRadius(barWidth, barWidth),
                blendMode = BlendMode.Plus
            )
        }
    }
}

private fun DrawScope.drawPulseStyle(
    levels: FloatArray,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    shimmer: Float
) {
    val barCount = levels.size
    val centerX = size.width / 2
    val centerY = size.height / 2
    val maxRadius = minOf(size.width, size.height) / 2 * 0.9f
    
    for (i in 0 until barCount) {
        val level = if (isActive) levels.getOrNull(i) ?: 0.15f else 0.15f
        val angle = (i.toFloat() / barCount) * 360f
        val radius = maxRadius * level
        
        val angleRad = Math.toRadians(angle.toDouble())
        val x = centerX + (radius * kotlin.math.cos(angleRad)).toFloat()
        val y = centerY + (radius * kotlin.math.sin(angleRad)).toFloat()
        
        // Pulse circles
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent1, accent2, Color.Transparent),
                center = Offset(x, y),
                radius = 15f
            ),
            radius = 15f * level,
            center = Offset(x, y),
            alpha = level,
            blendMode = BlendMode.Plus
        )
    }
    
    // Center glow
    if (isActive) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.5f),
                    accent2.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY),
                radius = maxRadius * 0.3f
            ),
            radius = maxRadius * 0.3f,
            center = Offset(centerX, centerY),
            blendMode = BlendMode.Plus
        )
    }
}

private fun DrawScope.drawCrystalStyle(
    levels: FloatArray,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    shimmer: Float
) {
    val barCount = levels.size
    val barWidth = size.width / (barCount * 1.8f)
    val gap = barWidth * 0.8f
    val maxHeight = size.height * 0.85f
    
    for (i in 0 until barCount) {
        val level = if (isActive) levels.getOrNull(i) ?: 0.15f else 0.15f
        val barHeight = (maxHeight * level).coerceIn(size.height * 0.12f, maxHeight)
        val x = i * (barWidth + gap)
        val y = size.height - barHeight
        
        // Crystal prism effect
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    accent1,
                    accent2,
                    accent1.copy(alpha = 0.7f)
                ),
                start = Offset(x, y),
                end = Offset(x + barWidth, size.height)
            ),
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
        
        // Facet highlights
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(x, y),
            size = Size(barWidth * 0.4f, barHeight),
            cornerRadius = CornerRadius(4f, 4f),
            blendMode = BlendMode.Plus
        )
        
        // Inner glow
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent1.copy(alpha = 0.6f)
                ),
                startY = y + barHeight * 0.7f,
                endY = size.height
            ),
            topLeft = Offset(x + 2f, y + barHeight * 0.7f),
            size = Size(barWidth - 4f, barHeight * 0.3f),
            cornerRadius = CornerRadius(4f, 4f),
            blendMode = BlendMode.Plus
        )
    }
}
