package com.soundboost.ui.components

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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.soundboost.ui.theme.AppTheme
import kotlin.math.*

/**
 * Premium Audio Visualizer - Optimized for 60+ FPS
 * Modern, professional grade with performance optimization
 */
@Composable
fun EnhancedAudioVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    theme: AppTheme,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    // All themes use OptimizedNeonSpectrum
    OptimizedNeonSpectrum(isActive, audioLevels, accent1, accent2, modifier)
}

// NEON: Optimized Spectrum - Reduced glow layers
@Composable
private fun OptimizedNeonSpectrum(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 32 // Reduced from 40
        val totalWidth = size.width * 0.9f
        val barWidth = totalWidth / (barCount * 1.2f)
        val gap = barWidth * 0.2f
        val maxHeight = size.height * 0.42f
        val centerY = size.height / 2
        val startX = (size.width - totalWidth) / 2
        
        repeat(barCount) { index ->
            val level = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (index * audioLevels.size / barCount).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.08f, 1f)
            } else {
                0.08f
            }
            
            val barHeight = maxHeight * level
            val x = startX + index * (barWidth + gap)
            val progress = index.toFloat() / barCount
            val barColor = lerp(accent1, accent2, progress)
            
            // Single optimized glow layer
            if (isActive && level > 0.3f) {
                val glowAlpha = 0.2f * level * glowPulse
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            barColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(x + barWidth / 2, centerY),
                        radius = barWidth * 2
                    ),
                    topLeft = Offset(x - barWidth, centerY - barHeight / 2 - 4.dp.toPx()),
                    size = Size(barWidth * 3, barHeight + 8.dp.toPx()),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
            
            // Top bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(barColor, barColor.copy(alpha = 0.85f)),
                    startY = centerY - barHeight / 2,
                    endY = centerY
                ),
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight / 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            
            // Bottom bar (reflection)
            drawRoundRect(
                color = barColor.copy(alpha = 0.3f),
                topLeft = Offset(x, centerY),
                size = Size(barWidth, barHeight / 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            
            // Cap highlight
            if (isActive && level > 0.5f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = level * 0.6f),
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, 1.5.dp.toPx()),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
        }
    }
}

// OCEAN: Optimized Liquid Wave - Reduced layers and samples
@Composable
private fun OptimizedLiquidWave(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val phase by rememberInfiniteTransition(label = "phase").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
        label = "phase"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerY = size.height / 2
        val samples = 48 // Reduced from 80
        val maxAmplitude = size.height * 0.38f
        
        // Draw 2 wave layers instead of 4
        for (layer in 0..1) {
            val path = Path()
            var started = false
            
            val layerPhase = phase + layer * 90f
            val layerOpacity = 0.4f - layer * 0.15f
            val layerThickness = (4 - layer * 1.5f).dp.toPx()
            
            for (i in 0..samples) {
                val x = (i.toFloat() / samples) * size.width
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (i * audioLevels.size / samples).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0f, 1f)
                } else {
                    0.15f
                }
                
                // Simplified wave (2 frequencies instead of 3)
                val wave1 = sin((i * 8f + layerPhase) * PI / 180).toFloat()
                val wave2 = sin((i * 15f - layerPhase * 0.5f) * PI / 180).toFloat() * 0.5f
                val combinedWave = (wave1 + wave2) / 1.5f
                val amplitude = maxAmplitude * audioLevel * combinedWave * (1f - layer * 0.2f)
                val y = centerY + amplitude
                
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Wave stroke
            drawPath(
                path = path,
                color = if (layer == 0) {
                    lerp(accent1, accent2, 0.5f).copy(alpha = layerOpacity + 0.15f)
                } else {
                    accent1.copy(alpha = layerOpacity)
                },
                style = Stroke(width = layerThickness, cap = StrokeCap.Round)
            )
        }
        
        // Simplified sparkles (only 6 instead of 12)
        if (isActive && audioLevels != null && audioLevels.size >= 6) {
            repeat(6) { i ->
                val level = audioLevels[i * (audioLevels.size / 6)].coerceIn(0f, 1f)
                if (level > 0.65f) {
                    val x = (i.toFloat() / 6) * size.width
                    val wave = sin((i * 8f + phase) * PI / 180).toFloat()
                    val y = centerY + (wave * maxAmplitude * level)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = level * 0.7f),
                                accent2.copy(alpha = level * 0.3f),
                                Color.Transparent
                            ),
                            radius = 6.dp.toPx()
                        ),
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

// SUNSET: Optimized Radial - Reduced segments
@Composable
private fun OptimizedRadialPulse(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(25000, easing = LinearEasing)),
        label = "rotation"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = minOf(size.width, size.height) * 0.15f
        val maxRadius = minOf(size.width, size.height) * 0.48f
        val rings = 36 // Reduced from 52
        
        rotate(rotation * 0.2f, pivot = center) {
            repeat(rings) { index ->
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (index * audioLevels.size / rings).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0.15f, 1f)
                } else {
                    0.2f
                }
                
                val angle = (index * 360f / rings) * PI / 180
                val angleSpread = (360f / rings * 0.7f) * PI / 180
                val radiusScale = baseRadius + (maxRadius - baseRadius) * audioLevel
                val innerR = baseRadius * 0.9f
                val outerR = radiusScale
                
                val innerStart = Offset(
                    center.x + (innerR * cos(angle - angleSpread / 2)).toFloat(),
                    center.y + (innerR * sin(angle - angleSpread / 2)).toFloat()
                )
                val innerEnd = Offset(
                    center.x + (innerR * cos(angle + angleSpread / 2)).toFloat(),
                    center.y + (innerR * sin(angle + angleSpread / 2)).toFloat()
                )
                val outerStart = Offset(
                    center.x + (outerR * cos(angle - angleSpread / 2)).toFloat(),
                    center.y + (outerR * sin(angle - angleSpread / 2)).toFloat()
                )
                val outerEnd = Offset(
                    center.x + (outerR * cos(angle + angleSpread / 2)).toFloat(),
                    center.y + (outerR * sin(angle + angleSpread / 2)).toFloat()
                )
                
                val segmentColor = lerp(accent1, accent2, index.toFloat() / rings)
                val path = Path().apply {
                    moveTo(innerStart.x, innerStart.y)
                    lineTo(outerStart.x, outerStart.y)
                    lineTo(outerEnd.x, outerEnd.y)
                    lineTo(innerEnd.x, innerEnd.y)
                    close()
                }
                
                drawPath(
                    path = path,
                    color = segmentColor.copy(alpha = 0.5f + audioLevel * 0.4f)
                )
                
                // Simplified highlight (no glow)
                if (isActive && audioLevel > 0.75f) {
                    val edgeMid = Offset(
                        center.x + (outerR * cos(angle)).toFloat(),
                        center.y + (outerR * sin(angle)).toFloat()
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = (audioLevel - 0.75f) * 0.7f),
                        radius = 4.dp.toPx(),
                        center = edgeMid
                    )
                }
            }
        }
        
        // Simplified center orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                radius = baseRadius
            ),
            radius = baseRadius,
            center = center
        )
    }
}

// FOREST: Optimized Organic - Reduced complexity
@Composable
private fun OptimizedOrganicBlob(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val morph by rememberInfiniteTransition(label = "morph").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing)),
        label = "morph"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseSize = minOf(size.width, size.height) * 0.35f
        
        // Reduced ambient particles (12 instead of 24)
        if (isActive && audioLevels != null && audioLevels.size >= 12) {
            repeat(12) { i ->
                val level = audioLevels[i].coerceIn(0f, 1f)
                if (level > 0.4f) {
                    val angle = (i * 360f / 12 + morph * 0.3f) * PI / 180
                    val distance = baseSize * (1.2f + level * 0.5f)
                    val x = center.x + (distance * cos(angle)).toFloat()
                    val y = center.y + (distance * sin(angle)).toFloat()
                    
                    val particleSize = (4 + level * 8).dp.toPx()
                    val particleColor = lerp(accent1, accent2, i.toFloat() / 12)
                    
                    drawCircle(
                        color = particleColor.copy(alpha = level * 0.6f),
                        radius = particleSize,
                        center = Offset(x, y)
                    )
                }
            }
        }
        
        // Main blob (reduced points from 32 to 24)
        val blobPoints = 24
        val path = Path()
        
        for (i in 0..blobPoints) {
            val angle = (i * 360f / blobPoints) * PI / 180
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (i * audioLevels.size / blobPoints).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.3f, 1f)
            } else {
                0.4f
            }
            
            // Simplified morphing (2 frequencies instead of 3)
            val noise1 = sin((angle * 3 + morph) * PI / 180).toFloat() * 0.15f
            val noise2 = sin((angle * 5 - morph * 0.5f) * PI / 180).toFloat() * 0.1f
            
            val radius = baseSize * audioLevel * (1f + noise1 + noise2)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        
        // Blob fill (single gradient, no glow)
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.6f),
                    accent2.copy(alpha = 0.4f)
                ),
                center = center,
                radius = baseSize
            )
        )
        
        // Blob outline
        drawPath(
            path = path,
            color = accent1.copy(alpha = 0.3f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

// ROYAL: Optimized Matrix - Reduced grid size
@Composable
private fun OptimizedGeometricMatrix(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val gridRows = 6 // Reduced from 7
        val gridCols = 8 // Reduced from 9
        val cellWidth = size.width * 0.85f / gridCols
        val cellHeight = size.height * 0.85f / gridRows
        val startX = (size.width - (gridCols * cellWidth)) / 2
        val startY = (size.height - (gridRows * cellHeight)) / 2
        
        // Main grid
        repeat(gridRows) { row ->
            repeat(gridCols) { col ->
                val index = row * gridCols + col
                val cellCenter = Offset(
                    startX + col * cellWidth + cellWidth / 2,
                    startY + row * cellHeight + cellHeight / 2
                )
                
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (index * audioLevels.size / (gridRows * gridCols)).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0f, 1f)
                } else {
                    0.2f
                }
                
                val cellSize = minOf(cellWidth, cellHeight) * 0.7f * (0.5f + audioLevel * 0.5f)
                val colorProgress = index.toFloat() / (gridRows * gridCols)
                val cellColor = lerp(accent1, accent2, colorProgress)
                val alpha = 0.4f + audioLevel * 0.5f
                
                // Diamond shape
                val halfSize = cellSize / 2
                val diamondPath = Path().apply {
                    moveTo(cellCenter.x, cellCenter.y - halfSize)
                    lineTo(cellCenter.x + halfSize, cellCenter.y)
                    lineTo(cellCenter.x, cellCenter.y + halfSize)
                    lineTo(cellCenter.x - halfSize, cellCenter.y)
                    close()
                }
                
                // Diamond fill (simple color, no gradient)
                drawPath(
                    path = diamondPath,
                    color = cellColor.copy(alpha = alpha)
                )
                
                // High energy highlight only
                if (isActive && audioLevel > 0.75f) {
                    drawPath(
                        path = diamondPath,
                        color = Color.White.copy(alpha = (audioLevel - 0.75f) * 0.6f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
        
        // Simplified shimmer
        if (isActive) {
            val shimmerX = size.width * shimmer
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    startX = shimmerX - 60.dp.toPx(),
                    endX = shimmerX + 60.dp.toPx()
                ),
                blendMode = BlendMode.Screen
            )
        }
    }
}

// NEON: Premium Spectrum - Symmetric with glow & reflection
@Composable
private fun PremiumNeonSpectrum(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = 40
        val totalWidth = size.width * 0.9f
        val barWidth = totalWidth / (barCount * 1.2f)
        val gap = barWidth * 0.2f
        val maxHeight = size.height * 0.42f
        val centerY = size.height / 2
        val startX = (size.width - totalWidth) / 2
        
        repeat(barCount) { index ->
            val level = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (index * audioLevels.size / barCount).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.08f, 1f)
            } else {
                0.08f
            }
            
            val barHeight = maxHeight * level * (0.7f + (sin((index * 0.3f) * PI / 180).toFloat() * 0.3f))
            val x = startX + index * (barWidth + gap)
            val progress = index.toFloat() / barCount
            val barColor = lerp(accent1, accent2, progress)
            
            // Glow layers (bottom to top)
            if (isActive) {
                for (glowLayer in 3 downTo 1) {
                    val glowSize = glowLayer * 2.dp.toPx()
                    val glowAlpha = (0.15f / glowLayer) * level * glowPulse
                    
                    // Top glow
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                barColor.copy(alpha = glowAlpha),
                                Color.Transparent
                            ),
                            center = Offset(x + barWidth / 2, centerY - barHeight / 2),
                            radius = barWidth * 2
                        ),
                        topLeft = Offset(x - glowSize, centerY - barHeight / 2 - glowSize),
                        size = Size(barWidth + glowSize * 2, barHeight / 2 + glowSize * 2),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                    
                    // Bottom glow (reflection)
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                barColor.copy(alpha = glowAlpha * 0.7f),
                                Color.Transparent
                            ),
                            center = Offset(x + barWidth / 2, centerY + barHeight / 2),
                            radius = barWidth * 2
                        ),
                        topLeft = Offset(x - glowSize, centerY - glowSize),
                        size = Size(barWidth + glowSize * 2, barHeight / 2 + glowSize * 2),
                        cornerRadius = CornerRadius(barWidth / 2)
                    )
                }
            }
            
            // Top bar (sharp, clean)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barColor,
                        barColor.copy(alpha = 0.85f)
                    ),
                    startY = centerY - barHeight / 2,
                    endY = centerY
                ),
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight / 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            
            // Bottom bar (reflection with fade)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barColor.copy(alpha = 0.4f),
                        barColor.copy(alpha = 0.1f)
                    ),
                    startY = centerY,
                    endY = centerY + barHeight / 2
                ),
                topLeft = Offset(x, centerY),
                size = Size(barWidth, barHeight / 2),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            
            // Highlight cap
            if (isActive && level > 0.4f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = level * 0.8f),
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, 2.dp.toPx()),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
        }
    }
}

// OCEAN: Liquid Waveform - Smooth, flowing, organic
@Composable
private fun LiquidWaveform(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val phase1 by rememberInfiniteTransition(label = "phase1").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
        label = "phase1"
    )
    
    val phase2 by rememberInfiniteTransition(label = "phase2").animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)),
        label = "phase2"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerY = size.height / 2
        val samples = 80
        val maxAmplitude = size.height * 0.38f
        
        // Background fill gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent1.copy(alpha = 0.05f),
                    Color.Transparent
                )
            )
        )
        
        // Draw 4 flowing wave layers
        for (layer in 3 downTo 0) {
            val path = Path()
            val fillPath = Path()
            var started = false
            
            val layerPhase = phase1 + layer * 45f
            val layerOpacity = 0.35f - layer * 0.07f
            val layerThickness = (5 - layer).dp.toPx()
            
            for (i in 0..samples) {
                val x = (i.toFloat() / samples) * size.width
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (i * audioLevels.size / samples).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0f, 1f)
                } else {
                    0.15f
                }
                
                // Complex wave combining multiple frequencies
                val wave1 = sin((i * 8f + layerPhase) * PI / 180).toFloat()
                val wave2 = sin((i * 15f + phase2) * PI / 180).toFloat() * 0.5f
                val wave3 = sin((i * 25f - layerPhase * 0.5f) * PI / 180).toFloat() * 0.25f
                
                val combinedWave = (wave1 + wave2 + wave3) / 1.75f
                val amplitude = maxAmplitude * audioLevel * combinedWave * (1f - layer * 0.12f)
                val y = centerY + amplitude
                
                if (!started) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            
            // Fill under wave for front layer
            if (layer == 0 && isActive) {
                fillPath.lineTo(size.width, size.height)
                fillPath.lineTo(0f, size.height)
                fillPath.close()
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent1.copy(alpha = 0.2f),
                            accent2.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
            }
            
            // Wave stroke
            val strokeColor = if (layer == 0) {
                lerp(accent1, accent2, 0.5f).copy(alpha = layerOpacity + 0.15f)
            } else {
                accent1.copy(alpha = layerOpacity)
            }
            
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = layerThickness,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Glow on front wave
            if (layer == 0 && isActive) {
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent2.copy(alpha = 0.3f),
                            accent1.copy(alpha = 0.2f)
                        )
                    ),
                    style = Stroke(
                        width = layerThickness * 2,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    blendMode = BlendMode.Screen
                )
            }
        }
        
        // Sparkle particles on peaks
        if (isActive && audioLevels != null) {
            repeat(12) { i ->
                if (i < audioLevels.size) {
                    val level = audioLevels[i * (audioLevels.size / 12)].coerceIn(0f, 1f)
                    if (level > 0.6f) {
                        val x = (i.toFloat() / 12) * size.width
                        val wave = sin((i * 8f + phase1) * PI / 180).toFloat()
                        val y = centerY + (wave * maxAmplitude * level)
                        
                        val sparkleSize = (3 + level * 5).dp.toPx()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = level * 0.9f),
                                    accent2.copy(alpha = level * 0.5f),
                                    Color.Transparent
                                ),
                                radius = sparkleSize * 2
                            ),
                            radius = sparkleSize * 2,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

// SUNSET: Radial Pulse - Expanding circles, rhythmic
@Composable
private fun RadialPulse(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(20000, easing = LinearEasing)),
        label = "rotation"
    )
    
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = minOf(size.width, size.height) * 0.15f
        val maxRadius = minOf(size.width, size.height) * 0.48f
        val rings = 52
        
        // Background radial gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                radius = maxRadius * 1.5f
            ),
            radius = maxRadius * 1.5f,
            center = center
        )
        
        rotate(rotation * 0.3f, pivot = center) {
            repeat(rings) { index ->
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (index * audioLevels.size / rings).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0.15f, 1f)
                } else {
                    0.2f
                }
                
                val angle = (index * 360f / rings) * PI / 180
                val angleSpread = (360f / rings * 0.7f) * PI / 180
                
                val radiusScale = baseRadius + (maxRadius - baseRadius) * audioLevel * pulse
                val innerR = baseRadius * 0.9f
                val outerR = radiusScale
                
                // Calculate arc points
                val innerStart = Offset(
                    center.x + (innerR * cos(angle - angleSpread / 2)).toFloat(),
                    center.y + (innerR * sin(angle - angleSpread / 2)).toFloat()
                )
                val innerEnd = Offset(
                    center.x + (innerR * cos(angle + angleSpread / 2)).toFloat(),
                    center.y + (innerR * sin(angle + angleSpread / 2)).toFloat()
                )
                val outerStart = Offset(
                    center.x + (outerR * cos(angle - angleSpread / 2)).toFloat(),
                    center.y + (outerR * sin(angle - angleSpread / 2)).toFloat()
                )
                val outerEnd = Offset(
                    center.x + (outerR * cos(angle + angleSpread / 2)).toFloat(),
                    center.y + (outerR * sin(angle + angleSpread / 2)).toFloat()
                )
                
                val segmentColor = lerp(accent1, accent2, index.toFloat() / rings)
                val alpha = 0.6f + audioLevel * 0.4f
                
                // Glow behind segment
                if (isActive && audioLevel > 0.4f) {
                    val path = Path().apply {
                        moveTo(innerStart.x, innerStart.y)
                        lineTo(outerStart.x, outerStart.y)
                        lineTo(outerEnd.x, outerEnd.y)
                        lineTo(innerEnd.x, innerEnd.y)
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                segmentColor.copy(alpha = audioLevel * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(
                                center.x + ((innerR + outerR) / 2 * cos(angle)).toFloat(),
                                center.y + ((innerR + outerR) / 2 * sin(angle)).toFloat()
                            ),
                            radius = (outerR - innerR) * 1.5f
                        )
                    )
                }
                
                // Main segment
                val path = Path().apply {
                    moveTo(innerStart.x, innerStart.y)
                    lineTo(outerStart.x, outerStart.y)
                    lineTo(outerEnd.x, outerEnd.y)
                    lineTo(innerEnd.x, innerEnd.y)
                    close()
                }
                
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            segmentColor.copy(alpha = alpha),
                            segmentColor.copy(alpha = alpha * 0.7f)
                        ),
                        center = center,
                        radius = outerR
                    )
                )
                
                // Edge highlight on high peaks
                if (isActive && audioLevel > 0.7f) {
                    val edgeMid = Offset(
                        center.x + (outerR * cos(angle)).toFloat(),
                        center.y + (outerR * sin(angle)).toFloat()
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (audioLevel - 0.7f) * 0.9f),
                                Color.Transparent
                            ),
                            radius = 8.dp.toPx()
                        ),
                        radius = 8.dp.toPx(),
                        center = edgeMid
                    )
                }
            }
        }
        
        // Center orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.6f),
                    accent2.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                radius = baseRadius
            ),
            radius = baseRadius,
            center = center
        )
        
        // Center highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                radius = baseRadius * 0.4f
            ),
            radius = baseRadius * 0.4f,
            center = center
        )
    }
}

// FOREST: Organic Blob - Morphing, particle system
@Composable
private fun OrganicBlob(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val morph1 by rememberInfiniteTransition(label = "morph1").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(5000, easing = LinearEasing)),
        label = "morph1"
    )
    
    val morph2 by rememberInfiniteTransition(label = "morph2").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(7000, easing = LinearEasing)),
        label = "morph2"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseSize = minOf(size.width, size.height) * 0.35f
        
        // Background ambient particles
        if (isActive && audioLevels != null) {
            repeat(24) { i ->
                if (i < audioLevels.size) {
                    val level = audioLevels[i].coerceIn(0f, 1f)
                    if (level > 0.3f) {
                        val angle = (i * 360f / 24 + morph1 * 0.3f) * PI / 180
                        val distance = baseSize * (1.2f + level * 0.6f)
                        val x = center.x + (distance * cos(angle)).toFloat()
                        val y = center.y + (distance * sin(angle)).toFloat()
                        
                        val particleSize = (4 + level * 10).dp.toPx()
                        val particleColor = lerp(accent1, accent2, i.toFloat() / 24)
                        
                        // Particle glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    particleColor.copy(alpha = level * 0.5f),
                                    Color.Transparent
                                ),
                                radius = particleSize * 2
                            ),
                            radius = particleSize * 2,
                            center = Offset(x, y)
                        )
                        
                        // Particle core
                        drawCircle(
                            color = particleColor.copy(alpha = 0.9f),
                            radius = particleSize * 0.4f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        // Main morphing blob
        val blobPoints = 32
        val path = Path()
        
        for (i in 0..blobPoints) {
            val angle = (i * 360f / blobPoints) * PI / 180
            
            // Get audio level for this segment
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (i * audioLevels.size / blobPoints).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.3f, 1f)
            } else {
                0.4f
            }
            
            // Organic morphing calculation
            val noise1 = sin((angle * 3 + morph1) * PI / 180).toFloat() * 0.15f
            val noise2 = sin((angle * 5 - morph2) * PI / 180).toFloat() * 0.1f
            val noise3 = sin((angle * 7 + morph1 * 0.7f) * PI / 180).toFloat() * 0.08f
            
            val radius = baseSize * audioLevel * (1f + noise1 + noise2 + noise3)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        
        // Blob outer glow
        if (isActive) {
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent1.copy(alpha = 0.3f),
                        accent2.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseSize * 1.5f
                )
            )
        }
        
        // Blob fill
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.7f),
                    lerp(accent1, accent2, 0.5f).copy(alpha = 0.5f),
                    accent2.copy(alpha = 0.3f)
                ),
                center = center,
                radius = baseSize * 1.2f
            )
        )
        
        // Blob outline
        drawPath(
            path = path,
            color = accent1.copy(alpha = 0.4f),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Inner details
        val innerBlobSize = baseSize * 0.4f
        val innerPath = Path()
        
        for (i in 0..blobPoints) {
            val angle = (i * 360f / blobPoints - morph1 * 0.5f) * PI / 180
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (i * audioLevels.size / blobPoints).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.5f, 1f)
            } else {
                0.6f
            }
            
            val noise = sin((angle * 4 + morph2) * PI / 180).toFloat() * 0.2f
            val radius = innerBlobSize * audioLevel * (1f + noise)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            
            if (i == 0) {
                innerPath.moveTo(x, y)
            } else {
                innerPath.lineTo(x, y)
            }
        }
        innerPath.close()
        
        drawPath(
            path = innerPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    accent2.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = innerBlobSize
            )
        )
    }
}

// ROYAL: Geometric Matrix - Crystalline, precise, elegant
@Composable
private fun GeometricMatrix(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(30000, easing = LinearEasing)),
        label = "rotation"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val gridRows = 7
        val gridCols = 9
        val cellWidth = size.width * 0.85f / gridCols
        val cellHeight = size.height * 0.85f / gridRows
        val startX = (size.width - (gridCols * cellWidth)) / 2
        val startY = (size.height - (gridRows * cellHeight)) / 2
        
        // Background geometric pattern
        rotate(rotation * 0.1f, pivot = center) {
            for (ring in 3 downTo 1) {
                val ringRadius = minOf(size.width, size.height) * ring * 0.15f
                drawCircle(
                    color = accent1.copy(alpha = 0.02f * ring),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
        
        // Main grid
        repeat(gridRows) { row ->
            repeat(gridCols) { col ->
                val index = row * gridCols + col
                val cellCenter = Offset(
                    startX + col * cellWidth + cellWidth / 2,
                    startY + row * cellHeight + cellHeight / 2
                )
                
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (index * audioLevels.size / (gridRows * gridCols)).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0f, 1f)
                } else {
                    0.2f
                }
                
                val cellSize = minOf(cellWidth, cellHeight) * 0.7f * (0.5f + audioLevel * 0.5f)
                val colorProgress = index.toFloat() / (gridRows * gridCols)
                val cellColor = lerp(accent1, accent2, colorProgress)
                val alpha = 0.4f + audioLevel * 0.6f
                
                // Cell glow
                if (isActive && audioLevel > 0.4f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                cellColor.copy(alpha = audioLevel * 0.4f),
                                Color.Transparent
                            ),
                            radius = cellSize * 1.5f
                        ),
                        radius = cellSize * 1.5f,
                        center = cellCenter
                    )
                }
                
                // Diamond/rhombus shape
                val halfSize = cellSize / 2
                val diamondPath = Path().apply {
                    moveTo(cellCenter.x, cellCenter.y - halfSize) // Top
                    lineTo(cellCenter.x + halfSize, cellCenter.y) // Right
                    lineTo(cellCenter.x, cellCenter.y + halfSize) // Bottom
                    lineTo(cellCenter.x - halfSize, cellCenter.y) // Left
                    close()
                }
                
                // Diamond fill with gradient
                drawPath(
                    path = diamondPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            cellColor.copy(alpha = alpha),
                            cellColor.copy(alpha = alpha * 0.6f)
                        ),
                        start = Offset(cellCenter.x - halfSize, cellCenter.y - halfSize),
                        end = Offset(cellCenter.x + halfSize, cellCenter.y + halfSize)
                    )
                )
                
                // Diamond border
                drawPath(
                    path = diamondPath,
                    color = cellColor.copy(alpha = alpha + 0.2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                
                // High energy highlight
                if (isActive && audioLevel > 0.7f) {
                    drawPath(
                        path = diamondPath,
                        color = Color.White.copy(alpha = (audioLevel - 0.7f) * 0.8f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Center spark
                    drawCircle(
                        color = Color.White.copy(alpha = (audioLevel - 0.7f)),
                        radius = 2.dp.toPx(),
                        center = cellCenter
                    )
                }
            }
        }
        
        // Shimmer sweep effect
        if (isActive) {
            val shimmerX = size.width * shimmer
            val shimmerGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.15f),
                    accent1.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                startX = shimmerX - 100.dp.toPx(),
                endX = shimmerX + 100.dp.toPx()
            )
            
            drawRect(
                brush = shimmerGradient,
                blendMode = BlendMode.Screen
            )
        }
        
        // Corner accents
        val cornerSize = 20.dp.toPx()
        val cornerInset = 15.dp.toPx()
        val cornerColor = accent1.copy(alpha = 0.6f)
        
        // Top-left
        drawLine(
            color = cornerColor,
            start = Offset(cornerInset, cornerInset),
            end = Offset(cornerInset + cornerSize, cornerInset),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(cornerInset, cornerInset),
            end = Offset(cornerInset, cornerInset + cornerSize),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Top-right
        drawLine(
            color = cornerColor,
            start = Offset(size.width - cornerInset, cornerInset),
            end = Offset(size.width - cornerInset - cornerSize, cornerInset),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(size.width - cornerInset, cornerInset),
            end = Offset(size.width - cornerInset, cornerInset + cornerSize),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Bottom-left
        drawLine(
            color = cornerColor,
            start = Offset(cornerInset, size.height - cornerInset),
            end = Offset(cornerInset + cornerSize, size.height - cornerInset),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(cornerInset, size.height - cornerInset),
            end = Offset(cornerInset, size.height - cornerInset - cornerSize),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Bottom-right
        drawLine(
            color = cornerColor,
            start = Offset(size.width - cornerInset, size.height - cornerInset),
            end = Offset(size.width - cornerInset - cornerSize, size.height - cornerInset),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = cornerColor,
            start = Offset(size.width - cornerInset, size.height - cornerInset),
            end = Offset(size.width - cornerInset, size.height - cornerInset - cornerSize),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
