package com.stdev.soundstboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stdev.soundstboost.R
import com.stdev.soundstboost.ui.theme.AppTheme
import kotlin.math.*

@Composable
fun ModernVolumeControl(
    volumePercent: Int,
    isActive: Boolean,
    theme: AppTheme,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (theme) {
        AppTheme.NEON_DARK -> NeonCircularDial(
            volumePercent, isActive, accent1, accent2, surfaceColor, 
            backgroundColor, onVolumeChange, onToggle, modifier
        )
        AppTheme.OCEAN_BLUE -> WaveVerticalSlider(
            volumePercent, isActive, accent1, accent2, surfaceColor,
            backgroundColor, onVolumeChange, onToggle, modifier
        )
        AppTheme.SUNSET_ORANGE -> HexagonalDial(
            volumePercent, isActive, accent1, accent2, surfaceColor,
            backgroundColor, onVolumeChange, onToggle, modifier
        )
        AppTheme.FOREST_GREEN -> OrganicBlobControl(
            volumePercent, isActive, accent1, accent2, surfaceColor,
            backgroundColor, onVolumeChange, onToggle, modifier
        )
        AppTheme.ROYAL_PURPLE -> CrystalPyramidControl(
            volumePercent, isActive, accent1, accent2, surfaceColor,
            backgroundColor, onVolumeChange, onToggle, modifier
        )
    }
}

// THEME 1: NEON - Futuristic Circular with Rings
@Composable
private fun NeonCircularDial(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { _, dragAmount ->
                    // Accumulate drag to avoid losing precision
                    accumulatedDrag += -dragAmount * 2.5f
                    val deltaInt = accumulatedDrag.toInt()
                    
                    if (deltaInt != 0) {
                        val newValue = (volumePercent + deltaInt).coerceIn(60, 200)
                        if (newValue != volumePercent) {
                            onVolumeChange(newValue)
                            accumulatedDrag -= deltaInt // Remove the used amount
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggle() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2 - 60.dp.toPx()
            
            // Rotating outer rings
            if (isActive) {
                for (i in 0..2) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                accent1.copy(alpha = 0.1f * (3 - i)),
                                accent2.copy(alpha = 0.15f * (3 - i)),
                                Color.Transparent,
                                Color.Transparent
                            ),
                            center = center
                        ),
                        radius = baseRadius + (i * 25.dp.toPx()),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            
            // Main circle with 3D effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.4f),
                        surfaceColor,
                        surfaceColor.copy(alpha = 0.9f)
                    ),
                    center = center
                ),
                radius = baseRadius,
                center = center
            )
            
            // Progress arc
            val progress = (animatedVolume - 60f) / 140f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(accent1, accent2, accent1.copy(alpha = 0.8f)),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                size = Size(baseRadius * 2, baseRadius * 2)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${volumePercent}%",
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) accent1 else Color.Gray,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = if (isActive) androidx.compose.ui.graphics.Shadow(
                        color = accent1.copy(alpha = 0.6f),
                        offset = Offset(0f, 8f),
                        blurRadius = 16f
                    ) else null
                )
            )
            Text(
                text = if (isActive) "● ${stringResource(R.string.status_active)}" else stringResource(R.string.status_double_tap),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (isActive) accent2 else Color.Gray
            )
        }
    }
}

// THEME 2: OCEAN - Vertical Wave Slider
@Composable
private fun WaveVerticalSlider(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    
    val waveOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        )
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Value label on top
        Text(
            text = "${volumePercent}%",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = if (isActive) accent1 else Color.Gray
        )
        
        Box(
            modifier = Modifier
                .width(120.dp)
                .weight(1f, fill = false)
                .heightIn(min = 200.dp, max = 350.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { accumulatedDrag = 0f },
                        onDragCancel = { accumulatedDrag = 0f }
                    ) { _, dragAmount ->
                        accumulatedDrag += -dragAmount * 2.5f
                        val deltaInt = accumulatedDrag.toInt()
                        
                        if (deltaInt != 0) {
                            val newValue = (volumePercent + deltaInt).coerceIn(60, 200)
                            if (newValue != volumePercent) {
                                onVolumeChange(newValue)
                                accumulatedDrag -= deltaInt
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onToggle() })
                },
            contentAlignment = Alignment.BottomCenter
        ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val progress = (animatedVolume - 60f) / 140f
            val fillHeight = height * progress
            
            // Background tube
            drawRoundRect(
                color = surfaceColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(70.dp.toPx())
            )
            
            // Wave fill with animated path
            val path = Path().apply {
                moveTo(0f, height)
                lineTo(0f, height - fillHeight)
                
                // Wave effect
                for (x in 0..width.toInt() step 4) {
                    val wave = sin((x + waveOffset) * PI / 180) * 8.dp.toPx()
                    lineTo(x.toFloat(), height - fillHeight + wave.toFloat())
                }
                
                lineTo(width, height)
                close()
            }
            
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(accent1, accent2, accent1.copy(alpha = 0.9f))
                )
            )
            
            // Bubbles effect when active
            if (isActive) {
                for (i in 0..5) {
                    val bubbleY = height - fillHeight + (i * 40.dp.toPx())
                    val bubbleX = width / 2 + sin((waveOffset + i * 60) * PI / 180).toFloat() * 20.dp.toPx()
                    if (bubbleY < height && bubbleY > 0) {
                        drawCircle(
                            color = accent1.copy(alpha = 0.3f),
                            radius = 6.dp.toPx(),
                            center = Offset(bubbleX, bubbleY)
                        )
                    }
                }
            }
        }
        }
        
        // Status text at bottom
        Text(
            text = if (isActive) "● ${stringResource(R.string.status_active)}" else stringResource(R.string.status_double_tap),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = if (isActive) accent2 else Color.Gray
        )
    }
}

// THEME 3: SUNSET - Hexagonal Dial  
@Composable
private fun HexagonalDial(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { _, dragAmount ->
                    accumulatedDrag += -dragAmount * 2.5f
                    val deltaInt = accumulatedDrag.toInt()
                    
                    if (deltaInt != 0) {
                        val newValue = (volumePercent + deltaInt).coerceIn(60, 200)
                        if (newValue != volumePercent) {
                            onVolumeChange(newValue)
                            accumulatedDrag -= deltaInt
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggle() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 40.dp.toPx()
            val progress = (animatedVolume - 60f) / 140f
            
            // Draw hexagon shape
            val hexPath = Path().apply {
                for (i in 0..6) {
                    val angle = (i * 60 - 90) * PI / 180
                    val r = if (i == 0) radius else radius
                    val x = center.x + (r * cos(angle)).toFloat()
                    val y = center.y + (r * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            
            // Background hexagon
            drawPath(
                path = hexPath,
                color = surfaceColor
            )
            
            // Progress hexagon segments
            val filledSegments = (progress * 6).toInt()
            for (i in 0 until filledSegments) {
                val segmentPath = Path().apply {
                    moveTo(center.x, center.y)
                    val angle1 = (i * 60 - 90) * PI / 180
                    val angle2 = ((i + 1) * 60 - 90) * PI / 180
                    val x1 = center.x + (radius * cos(angle1)).toFloat()
                    val y1 = center.y + (radius * sin(angle1)).toFloat()
                    val x2 = center.x + (radius * cos(angle2)).toFloat()
                    val y2 = center.y + (radius * sin(angle2)).toFloat()
                    lineTo(x1, y1)
                    lineTo(x2, y2)
                    close()
                }
                
                drawPath(
                    path = segmentPath,
                    brush = Brush.radialGradient(
                        colors = listOf(accent1, accent2),
                        center = center
                    )
                )
            }
            
            // Hexagon outline
            drawPath(
                path = hexPath,
                color = if (isActive) accent1 else Color.Gray,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${volumePercent}%",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) accent1 else Color.Gray
            )
            Text(
                text = if (isActive) "◆ ${stringResource(R.string.status_active)}" else stringResource(R.string.status_double_tap),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (isActive) accent2 else Color.Gray
            )
        }
    }
}

// THEME 4: FOREST - Organic Blob Shape
@Composable
private fun OrganicBlobControl(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)
    )
    
    val morphOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        )
    )
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { _, dragAmount ->
                    accumulatedDrag += -dragAmount * 2.5f
                    val deltaInt = accumulatedDrag.toInt()
                    
                    if (deltaInt != 0) {
                        val newValue = (volumePercent + deltaInt).coerceIn(60, 200)
                        if (newValue != volumePercent) {
                            onVolumeChange(newValue)
                            accumulatedDrag -= deltaInt
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggle() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2 - 50.dp.toPx()
            val progress = (animatedVolume - 60f) / 140f
            
            // Create organic blob shape
            val blobPath = Path().apply {
                for (angle in 0..360 step 5) {
                    val variation = sin((angle * 3 + morphOffset) * PI / 180) * 15.dp.toPx()
                    val r = baseRadius + variation.toFloat()
                    val rad = angle * PI / 180
                    val x = center.x + (r * cos(rad)).toFloat()
                    val y = center.y + (r * sin(rad)).toFloat()
                    if (angle == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            
            // Background blob
            drawPath(
                path = blobPath,
                color = surfaceColor
            )
            
            // Progress blob (scaled)
            val progressPath = Path().apply {
                for (angle in 0..360 step 5) {
                    val variation = sin((angle * 3 + morphOffset) * PI / 180) * 15.dp.toPx()
                    val r = (baseRadius + variation.toFloat()) * progress
                    val rad = angle * PI / 180
                    val x = center.x + (r * cos(rad)).toFloat()
                    val y = center.y + (r * sin(rad)).toFloat()
                    if (angle == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            
            drawPath(
                path = progressPath,
                brush = Brush.radialGradient(
                    colors = listOf(accent1, accent2, accent1.copy(alpha = 0.8f)),
                    center = center
                )
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${volumePercent}%",
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) Color.White else Color.Gray
            )
            Text(
                text = if (isActive) "🌿 ${stringResource(R.string.status_active)}" else stringResource(R.string.status_double_tap),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) accent1 else Color.Gray
            )
        }
    }
}

// THEME 5: ROYAL - Crystal Pyramid 3D
@Composable
private fun CrystalPyramidControl(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }
    
    val animatedVolume by animateFloatAsState(
        targetValue = volumePercent.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val shimmer by rememberInfiniteTransition().animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { _, dragAmount ->
                    accumulatedDrag += -dragAmount * 2.5f
                    val deltaInt = accumulatedDrag.toInt()
                    
                    if (deltaInt != 0) {
                        val newValue = (volumePercent + deltaInt).coerceIn(60, 200)
                        if (newValue != volumePercent) {
                            onVolumeChange(newValue)
                            accumulatedDrag -= deltaInt
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggle() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseWidth = size.width * 0.7f
            val height = size.height * 0.8f
            val progress = (animatedVolume - 60f) / 140f
            
            // Draw crystal/diamond shape layers
            for (layer in 0..4) {
                val layerProgress = ((progress * 5) - layer).coerceIn(0f, 1f)
                val layerHeight = height * layerProgress / 5
                val layerY = center.y + height / 2 - (layer * height / 5) - layerHeight
                
                val diamondPath = Path().apply {
                    // Top point
                    moveTo(center.x, layerY)
                    // Left
                    lineTo(center.x - baseWidth / 2, center.y)
                    // Bottom
                    lineTo(center.x, center.y + height / 2)
                    // Right
                    lineTo(center.x + baseWidth / 2, center.y)
                    close()
                }
                
                val layerColor = if (layerProgress > 0) {
                    val ratio = layer / 4f
                    androidx.compose.ui.graphics.lerp(accent1, accent2, ratio)
                } else Color.Transparent
                
                drawPath(
                    path = diamondPath,
                    color = layerColor.copy(alpha = layerProgress * 0.6f)
                )
                
                // Crystal edges
                drawPath(
                    path = diamondPath,
                    color = if (isActive) accent1 else Color.Gray,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${volumePercent}%",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) accent1 else Color.Gray,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = if (isActive) androidx.compose.ui.graphics.Shadow(
                        color = accent1.copy(alpha = 0.8f),
                        offset = Offset(0f, 6f),
                        blurRadius = 20f
                    ) else null
                )
            )
            Text(
                text = if (isActive) "◆ ${stringResource(R.string.status_active)}" else stringResource(R.string.status_double_tap),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = if (isActive) accent2 else Color.Gray
            )
        }
    }
}
