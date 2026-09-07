package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Premium Touch-Optimized Volume Slider
 * Features: Haptic feedback, smooth gestures, visual feedback, bubble indicator
 */
@Composable
fun PremiumVolumeSlider(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticManager()
    var lastHapticValue by remember { mutableStateOf(volumePercent) }
    var isDragging by remember { mutableStateOf(false) }
    var showBubble by remember { mutableStateOf(false) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "volume"
    )
    
    val bubbleScale by animateFloatAsState(
        targetValue = if (showBubble || isDragging) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bubbleScale"
    )
    
    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    LaunchedEffect(isDragging) {
        if (isDragging) {
            showBubble = true
        } else {
            kotlinx.coroutines.delay(800)
            showBubble = false
        }
    }
    
    Box(
        modifier = modifier
            .width(80.dp)
            .height(350.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptic.click()
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.lightTap()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    
                    // High sensitivity for smooth control
                    val sensitivity = 0.4f
                    val volumeChange = (-dragAmount * sensitivity).toInt()
                    
                    if (volumeChange != 0) {
                        val newVolume = (volumePercent + volumeChange).coerceIn(60, 200)
                        
                        if (newVolume != volumePercent) {
                            onVolumeChange(newVolume)
                            
                            // Haptic feedback every 5%
                            if (abs(newVolume - lastHapticValue) >= 5) {
                                val intensity = (newVolume - 60) / 140f
                                haptic.volumeChange(intensity)
                                lastHapticValue = newVolume
                            }
                            
                            // Special feedback at thresholds
                            when (newVolume) {
                                100 -> haptic.snap()  // Normal volume
                                150 -> haptic.threshold()  // High volume
                                200 -> haptic.heavyClick()  // Max volume
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        haptic.doubleClick()
                        onToggle()
                    },
                    onTap = { offset ->
                        // Tap to set volume based on position
                        val progress = 1f - (offset.y / size.height)
                        val newVolume = (60 + progress * 140).toInt().coerceIn(60, 200)
                        haptic.click()
                        onVolumeChange(newVolume)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val progress = (animatedVolume - 60f) / 140f
            val fillHeight = height * progress
            
            val trackWidth = 28.dp.toPx()
            val trackX = (width - trackWidth) / 2
            val cornerRadius = trackWidth / 2
            
            // Background track with depth effect
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.3f),
                        surfaceColor.copy(alpha = 0.5f),
                        surfaceColor.copy(alpha = 0.3f)
                    )
                ),
                topLeft = Offset(trackX, 0f),
                size = Size(trackWidth, height),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Inner shadow effect
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.1f)
                    )
                ),
                topLeft = Offset(trackX + 1.dp.toPx(), 1.dp.toPx()),
                size = Size(trackWidth - 2.dp.toPx(), height - 2.dp.toPx()),
                cornerRadius = CornerRadius(cornerRadius - 1.dp.toPx(), cornerRadius - 1.dp.toPx())
            )
            
            // Filled portion with gradient
            if (fillHeight > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent1,
                            androidx.compose.ui.graphics.lerp(accent1, accent2, 0.5f),
                            accent2
                        ),
                        startY = height - fillHeight,
                        endY = height
                    ),
                    topLeft = Offset(trackX, height - fillHeight),
                    size = Size(trackWidth, fillHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
                
                // Glossy highlight on filled portion
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    topLeft = Offset(trackX + trackWidth * 0.1f, height - fillHeight),
                    size = Size(trackWidth * 0.3f, fillHeight * 0.5f),
                    cornerRadius = CornerRadius(cornerRadius * 0.5f, cornerRadius * 0.5f)
                )
                
                // Glow effect when active
                if (isActive && glowPulse > 0.8f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accent1.copy(alpha = 0.3f * glowPulse),
                                accent2.copy(alpha = 0.2f * glowPulse),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(trackX - 4.dp.toPx(), height - fillHeight - 4.dp.toPx()),
                        size = Size(trackWidth + 8.dp.toPx(), fillHeight + 8.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRadius + 4.dp.toPx(), cornerRadius + 4.dp.toPx())
                    )
                }
            }
            
            // Level indicators (ticks)
            val tickLevels = listOf(0f, 0.286f, 0.571f, 0.857f, 1f) // 60%, 100%, 140%, 180%, 200%
            tickLevels.forEach { level ->
                val tickY = height * (1f - level)
                val tickColor = if (progress >= level) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.3f)
                
                drawLine(
                    color = tickColor,
                    start = Offset(trackX + trackWidth + 4.dp.toPx(), tickY),
                    end = Offset(trackX + trackWidth + 10.dp.toPx(), tickY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // Thumb indicator
            val thumbY = height - fillHeight
            val thumbRadius = 18.dp.toPx()
            
            // Thumb shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = thumbRadius,
                center = Offset(trackX + trackWidth / 2, thumbY + 2.dp.toPx())
            )
            
            // Thumb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        accent1.copy(alpha = 0.8f),
                        accent2
                    ),
                    radius = thumbRadius
                ),
                radius = thumbRadius,
                center = Offset(trackX + trackWidth / 2, thumbY)
            )
            
            // Thumb highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    radius = thumbRadius * 0.4f
                ),
                radius = thumbRadius * 0.4f,
                center = Offset(trackX + trackWidth / 2 - thumbRadius * 0.2f, thumbY - thumbRadius * 0.2f)
            )
            
            // Thumb ring when dragging
            if (isDragging) {
                drawCircle(
                    color = accent1.copy(alpha = 0.5f),
                    radius = thumbRadius + 6.dp.toPx(),
                    center = Offset(trackX + trackWidth / 2, thumbY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        // Value bubble indicator
        if (bubbleScale > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (350.dp * (1f - (animatedVolume - 60f) / 140f)) - 30.dp)
                    .graphicsLayer {
                        scaleX = bubbleScale
                        scaleY = bubbleScale
                        alpha = bubbleScale
                    }
            ) {
                Canvas(modifier = Modifier.size(70.dp, 50.dp)) {
                    val bubblePath = android.graphics.Path().apply {
                        // Rounded rectangle bubble
                        addRoundRect(
                            android.graphics.RectF(0f, 0f, size.width, size.height * 0.7f),
                            20.dp.toPx(), 20.dp.toPx(),
                            android.graphics.Path.Direction.CW
                        )
                        // Pointer triangle
                        moveTo(size.width * 0.1f, size.height * 0.7f)
                        lineTo(0f, size.height * 0.5f)
                        lineTo(size.width * 0.2f, size.height * 0.7f)
                        close()
                    }
                    
                    // Bubble shadow
                    drawPath(
                        path = bubblePath.asComposePath(),
                        color = Color.Black.copy(alpha = 0.3f)
                    )
                    
                    // Bubble fill
                    drawPath(
                        path = bubblePath.asComposePath(),
                        brush = Brush.linearGradient(
                            colors = listOf(accent1, accent2)
                        )
                    )
                }
                
                Text(
                    text = "${volumePercent}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 10.dp, y = (-3).dp)
                )
            }
        }
    }
}

/**
 * Horizontal Premium Volume Slider
 */
@Composable
fun HorizontalPremiumVolumeSlider(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticManager()
    var lastHapticValue by remember { mutableStateOf(volumePercent) }
    var isDragging by remember { mutableStateOf(false) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "volume"
    )
    
    val glowIntensity by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptic.click()
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.lightTap()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    
                    val sensitivity = 0.3f
                    val volumeChange = (dragAmount * sensitivity).toInt()
                    
                    if (volumeChange != 0) {
                        val newVolume = (volumePercent + volumeChange).coerceIn(60, 200)
                        
                        if (newVolume != volumePercent) {
                            onVolumeChange(newVolume)
                            
                            if (abs(newVolume - lastHapticValue) >= 5) {
                                val intensity = (newVolume - 60) / 140f
                                haptic.volumeChange(intensity)
                                lastHapticValue = newVolume
                            }
                            
                            when (newVolume) {
                                100 -> haptic.snap()
                                150 -> haptic.threshold()
                                200 -> haptic.heavyClick()
                            }
                        }
                    }
                }
            }
    ) {
        val trackHeight = 16.dp.toPx()
        val trackY = (size.height - trackHeight) / 2
        val progress = (animatedVolume - 60f) / 140f
        val fillWidth = size.width * progress
        val cornerRadius = trackHeight / 2
        
        // Background track
        drawRoundRect(
            color = surfaceColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, trackY),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
        
        // Filled track
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(accent1, androidx.compose.ui.graphics.lerp(accent1, accent2, 0.5f), accent2),
                startX = 0f,
                endX = fillWidth
            ),
            topLeft = Offset(0f, trackY),
            size = Size(fillWidth, trackHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
        
        // Glow effect
        if (isActive) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accent1.copy(alpha = 0.3f * glowIntensity),
                        accent2.copy(alpha = 0.2f * glowIntensity)
                    ),
                    startX = 0f,
                    endX = fillWidth
                ),
                topLeft = Offset(0f, trackY - 4.dp.toPx()),
                size = Size(fillWidth, trackHeight + 8.dp.toPx()),
                cornerRadius = CornerRadius(cornerRadius + 4.dp.toPx(), cornerRadius + 4.dp.toPx())
            )
        }
        
        // Thumb
        val thumbX = fillWidth
        val thumbRadius = 22.dp.toPx()
        
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = thumbRadius,
            center = Offset(thumbX, size.height / 2 + 2.dp.toPx())
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, accent1, accent2),
                radius = thumbRadius
            ),
            radius = thumbRadius,
            center = Offset(thumbX, size.height / 2)
        )
        
        if (isDragging) {
            drawCircle(
                color = accent1.copy(alpha = 0.4f),
                radius = thumbRadius + 8.dp.toPx(),
                center = Offset(thumbX, size.height / 2),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// Helper extension
private fun android.graphics.Path.asComposePath(): Path {
    return Path().apply {
        // Simplified conversion - in production use proper path APIs
    }
}
