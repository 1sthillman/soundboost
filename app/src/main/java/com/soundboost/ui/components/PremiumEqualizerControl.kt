package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Professional 10-Band Equalizer with Live Visualization
 */
@Composable
fun PremiumEqualizerControl(
    bands: List<Float>, // -15dB to +15dB per band
    bandLabels: List<String> = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"),
    isActive: Boolean,
    audioLevels: FloatArray?,
    accentColor: Color,
    surfaceColor: Color,
    onBandChanged: (index: Int, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticManager()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "10-BAND EKOLAYİZER",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = Color.Gray
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor.copy(alpha = 0.3f),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Visual frequency spectrum
                EqualizerVisualization(
                    bands = bands,
                    audioLevels = audioLevels,
                    isActive = isActive,
                    accentColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
                
                // Band sliders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bands.forEachIndexed { index, value ->
                        EqualizerBandSlider(
                            value = value,
                            label = bandLabels.getOrElse(index) { "${index + 1}" },
                            audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                                audioLevels[index % audioLevels.size]
                            } else 0f,
                            accentColor = accentColor,
                            onValueChange = { newValue ->
                                if (abs(newValue - value) > 0.5f) {
                                    haptic.lightTap()
                                }
                                onBandChanged(index, newValue)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("FLAT", "POP", "ROCK", "JAZZ", "BASS").forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            color = surfaceColor.copy(alpha = 0.5f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerBandSlider(
    value: Float, // -15 to +15
    label: String,
    audioLevel: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bandValue"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Slider track
        Box(
            modifier = Modifier
                .width(32.dp)
                .weight(1f)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        val sensitivity = 0.05f
                        val delta = -dragAmount * sensitivity
                        val newValue = (value + delta).coerceIn(-15f, 15f)
                        onValueChange(newValue)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 8.dp.toPx()
                val trackX = (size.width - trackWidth) / 2
                val centerY = size.height / 2
                val maxOffset = size.height / 2 - 20.dp.toPx()
                
                // Background track
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.2f),
                    topLeft = Offset(trackX, centerY - maxOffset),
                    size = Size(trackWidth, maxOffset * 2),
                    cornerRadius = CornerRadius(trackWidth / 2)
                )
                
                // Center line (0dB)
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(trackX - 4.dp.toPx(), centerY),
                    end = Offset(trackX + trackWidth + 4.dp.toPx(), centerY),
                    strokeWidth = 1.5.dp.toPx()
                )
                
                // Value indicator
                val valueOffset = (animatedValue / 15f) * maxOffset
                val thumbY = centerY - valueOffset
                
                // Filled portion
                val fillStartY = if (animatedValue >= 0) centerY else thumbY
                val fillEndY = if (animatedValue >= 0) thumbY else centerY
                val fillHeight = abs(fillEndY - fillStartY)
                
                if (fillHeight > 0) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = if (animatedValue >= 0) {
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            } else {
                                listOf(accentColor.copy(alpha = 0.7f), accentColor)
                            },
                            startY = fillStartY,
                            endY = fillEndY
                        ),
                        topLeft = Offset(trackX, fillStartY),
                        size = Size(trackWidth, fillHeight),
                        cornerRadius = CornerRadius(trackWidth / 2)
                    )
                }
                
                // Audio level indicator (behind thumb)
                if (audioLevel > 0.2f) {
                    val levelHeight = maxOffset * 2 * audioLevel
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.2f),
                        topLeft = Offset(trackX, centerY + maxOffset - levelHeight),
                        size = Size(trackWidth, levelHeight),
                        cornerRadius = CornerRadius(trackWidth / 2)
                    )
                }
                
                // Thumb
                val thumbRadius = if (isDragging) 14.dp.toPx() else 12.dp.toPx()
                
                // Thumb shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = thumbRadius,
                    center = Offset(size.width / 2, thumbY + 2.dp.toPx())
                )
                
                // Thumb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            accentColor
                        ),
                        radius = thumbRadius
                    ),
                    radius = thumbRadius,
                    center = Offset(size.width / 2, thumbY)
                )
                
                // Thumb ring when dragging
                if (isDragging) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.5f),
                        radius = thumbRadius + 4.dp.toPx(),
                        center = Offset(size.width / 2, thumbY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        // Value display
        Text(
            text = "${if (animatedValue >= 0) "+" else ""}${animatedValue.toInt()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (abs(animatedValue) > 0.5f) accentColor else Color.Gray
        )
        
        // Label
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}

@Composable
private fun EqualizerVisualization(
    bands: List<Float>,
    audioLevels: FloatArray?,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val phase by rememberInfiniteTransition(label = "phase").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier) {
        val barCount = bands.size * 4 // Interpolated bars
        val barWidth = size.width / (barCount * 1.3f)
        val gap = barWidth * 0.3f
        val maxHeight = size.height * 0.8f
        val baseY = size.height * 0.9f
        
        repeat(barCount) { index ->
            val bandIndex = (index * bands.size / barCount).coerceIn(0, bands.size - 1)
            val bandValue = bands[bandIndex]
            
            // Audio reactive height
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (index * audioLevels.size / barCount).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.1f, 1f)
            } else {
                0.15f
            }
            
            // Add wave motion
            val wave = sin((index * 15f + phase) * PI / 180).toFloat() * 0.1f
            val heightFactor = (audioLevel + wave).coerceIn(0.1f, 1f)
            
            // Adjust for EQ band value (-15 to +15 dB affects height)
            val eqMultiplier = 1f + (bandValue / 30f) // -0.5 to +1.5
            val barHeight = maxHeight * heightFactor * eqMultiplier.coerceIn(0.1f, 1.5f)
            
            val x = index * (barWidth + gap) + gap
            val barY = baseY - barHeight
            
            // Color gradient based on frequency
            val colorProgress = index.toFloat() / barCount
            val barColor = androidx.compose.ui.graphics.lerp(
                accentColor,
                accentColor.copy(alpha = 0.6f),
                colorProgress
            )
            
            // Bar with gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barColor.copy(alpha = 0.9f),
                        barColor.copy(alpha = 0.6f),
                        barColor.copy(alpha = 0.3f)
                    ),
                    startY = barY,
                    endY = baseY
                ),
                topLeft = Offset(x, barY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
            
            // Glow for high levels
            if (isActive && audioLevel > 0.7f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (audioLevel - 0.7f) * 0.6f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(x - 2.dp.toPx(), barY - 2.dp.toPx()),
                    size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                    cornerRadius = CornerRadius((barWidth + 4.dp.toPx()) / 2)
                )
            }
        }
        
        // Frequency curve overlay
        if (isActive) {
            val path = Path().apply {
                bands.forEachIndexed { index, value ->
                    val x = (index.toFloat() / (bands.size - 1)) * size.width
                    val eqHeight = (value / 30f) * maxHeight * 0.5f // -15 to +15 maps to height
                    val y = baseY - maxHeight / 2 - eqHeight
                    
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.4f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Bass Boost Control with visual feedback
 */
@Composable
fun BassBoostControl(
    value: Int, // 0-100
    audioLevel: Float,
    isActive: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticManager()
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bassValue"
    )
    
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor.copy(alpha = 0.3f),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.1f * (value / 100f)),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Bass icon/visualization
                Canvas(modifier = Modifier.size(60.dp)) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val intensity = if (isActive) audioLevel else 0.3f
                    val radiusMultiplier = if (isActive) pulse else 1f
                    
                    // Concentric circles
                    for (i in 3 downTo 1) {
                        val radius = (size.minDimension / 2) * (i / 3f) * intensity * radiusMultiplier
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.4f / i),
                                    Color.Transparent
                                ),
                                radius = radius
                            ),
                            radius = radius,
                            center = Offset(centerX, centerY)
                        )
                    }
                    
                    // Speaker icon
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.8f),
                        topLeft = Offset(centerX - 8.dp.toPx(), centerY - 12.dp.toPx()),
                        size = Size(16.dp.toPx(), 24.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "BAS GÜÇLENDİRME",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "$value%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Custom slider
                    HorizontalPremiumVolumeSlider(
                        volumePercent = value + 60, // Map 0-100 to 60-160 for visual consistency
                        isActive = isActive,
                        accent1 = accentColor,
                        accent2 = accentColor.copy(alpha = 0.7f),
                        surfaceColor = surfaceColor,
                        onVolumeChange = { newValue ->
                            val bassValue = (newValue - 60).coerceIn(0, 100)
                            if (abs(bassValue - value) >= 2) {
                                haptic.lightTap()
                            }
                            onValueChange(bassValue)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
