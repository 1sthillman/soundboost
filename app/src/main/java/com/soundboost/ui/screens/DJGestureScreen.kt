package com.soundboost.ui.screens

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.soundboost.gesture.DJGesture
import com.soundboost.gesture.DJGestureController
import com.soundboost.gesture.GestureMetrics
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DJGestureScreen(
    onBack: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onBassChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var volumeLevel by remember { mutableStateOf(50) }
    var bassLevel by remember { mutableStateOf(50) }
    var trebleLevel by remember { mutableStateOf(50) }
    var activePreset by remember { mutableStateOf(0) }
    var currentGesture by remember { mutableStateOf<DJGesture>(DJGesture.Idle) }
    var gestureMetrics by remember { mutableStateOf(GestureMetrics()) }
    
    val gestureController = remember {
        DJGestureController(
            context = context,
            onVolumeChange = { delta ->
                volumeLevel = (volumeLevel + delta).coerceIn(0, 100)
                onVolumeChange(volumeLevel)
            },
            onBassChange = { delta ->
                bassLevel = (bassLevel + delta).coerceIn(0, 100)
                onBassChange(bassLevel)
            },
            onTrebleChange = { delta ->
                trebleLevel = (trebleLevel + delta).coerceIn(0, 100)
            },
            onPresetChange = { fingerCount ->
                activePreset = fingerCount
            }
        )
    }
    
    val gestureState by gestureController.currentGesture.collectAsState()
    val metricsState by gestureController.gestureMetrics.collectAsState()
    
    LaunchedEffect(gestureState) {
        currentGesture = gestureState
    }
    
    LaunchedEffect(metricsState) {
        gestureMetrics = metricsState
    }
    
    LaunchedEffect(Unit) {
        gestureController.initialize()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            gestureController.release()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E14),
                        Color(0xFF151B24)
                    )
                )
            )
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview (Clean - No overlays)
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrameReady = { bitmap, timestamp ->
                    gestureController.processFrame(bitmap, timestamp)
                }
            )
            
            // Overlay gradient for better readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
            
            // Professional DJ Deck UI
            ModernDJDeckOverlay(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(400.dp),
                volumeLevel = volumeLevel,
                bassLevel = bassLevel,
                trebleLevel = trebleLevel,
                activePreset = activePreset,
                currentGesture = currentGesture,
                gestureMetrics = gestureMetrics
            )
            
            // Top Gesture Guide (Only when active)
            GestureGuideOverlay(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                currentGesture = currentGesture,
                gestureMetrics = gestureMetrics
            )
        } else {
            CameraPermissionRequest(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
        
        // Top Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            color = Color(0xFF0A0E14).copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DJ GESTURE CONTROL",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Professional Audio Control",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (currentGesture != DJGesture.Idle) Color(0xFF00E5FF).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        null,
                        tint = if (currentGesture != DJGesture.Idle) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrameReady: (android.graphics.Bitmap, Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            try {
                                val bitmap = imageProxy.toBitmap()
                                onFrameReady(bitmap, System.currentTimeMillis())
                            } finally {
                                imageProxy.close()
                            }
                        }
                    }
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DJGestureScreen", "Camera binding failed", e)
                }
            }, executor)
            
            previewView
        },
        modifier = modifier
            .blur(4.dp)
            .clip(RoundedCornerShape(24.dp))
    )
}

@Composable
fun ModernDJDeckOverlay(
    modifier: Modifier = Modifier,
    volumeLevel: Int,
    bassLevel: Int,
    trebleLevel: Int,
    activePreset: Int,
    currentGesture: DJGesture,
    gestureMetrics: GestureMetrics
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0E14).copy(alpha = 0.85f),
                            Color(0xFF0A0E14).copy(alpha = 0.98f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Preset Selector Pills
                if (activePreset > 0) {
                    PresetIndicator(
                        activePreset = activePreset,
                        pulseAlpha = pulseAlpha
                    )
                }
                
                // Three Professional Faders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfessionalFader(
                        label = "VOLUME",
                        value = volumeLevel,
                        color = Color(0xFFFFB74D),
                        icon = Icons.Default.VolumeUp,
                        isActive = currentGesture is DJGesture.ThumbsUp || 
                                   currentGesture is DJGesture.ThumbsDown || 
                                   currentGesture is DJGesture.SwipeVolume ||
                                   currentGesture is DJGesture.OpenHand ||
                                   currentGesture is DJGesture.Fist,
                        modifier = Modifier.weight(1f)
                    )
                    
                    ProfessionalFader(
                        label = "BASS",
                        value = bassLevel,
                        color = Color(0xFF00E5FF),
                        icon = Icons.Default.MusicNote,
                        isActive = currentGesture is DJGesture.TwoHandsBass,
                        modifier = Modifier.weight(1f)
                    )
                    
                    ProfessionalFader(
                        label = "TREBLE",
                        value = trebleLevel,
                        color = Color(0xFFFF5252),
                        icon = Icons.Default.GraphicEq,
                        isActive = false, // Treble removed for simplicity
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Advanced Waveform Visualizer
                AdvancedWaveformVisualizer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    volumeLevel = volumeLevel,
                    bassLevel = bassLevel,
                    trebleLevel = trebleLevel,
                    pulseAlpha = pulseAlpha,
                    gestureMetrics = gestureMetrics
                )
            }
        }
    }
}

@Composable
fun PresetIndicator(
    activePreset: Int,
    pulseAlpha: Float
) {
    val presetNames = listOf("FLAT", "BASS", "TREBLE", "VOCAL", "ROCK")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
            modifier = Modifier.animateContentSize()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    null,
                    tint = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "PRESET: ${presetNames.getOrElse(activePreset - 1) { "CUSTOM" }}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
fun ProfessionalFader(
    label: String,
    value: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "faderScale"
    )
    
    Column(
        modifier = modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isActive) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = if (isActive) color else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Label
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isActive) color else Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
        
        // Fader Track
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(25.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Fill indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value / 100f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(25.dp)
                    )
            )
            
            // Thumb
            val thumbOffset = (1f - value / 100f) * 160f
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = thumbOffset.dp)
                    .size(50.dp, 35.dp)
                    .background(
                        color = if (isActive) color else Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$value",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isActive) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun AdvancedWaveformVisualizer(
    modifier: Modifier = Modifier,
    volumeLevel: Int,
    bassLevel: Int,
    trebleLevel: Int,
    pulseAlpha: Float,
    gestureMetrics: GestureMetrics
) {
    var phase by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            phase += 0.12f
            delay(16)
        }
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Bass wave (thick, slow)
        val bassPath = Path()
        bassPath.moveTo(0f, centerY)
        for (x in 0..width.toInt() step 8) {
            val normalizedX = x / width
            val bassAmplitude = (bassLevel / 100f) * height / 3
            val y = centerY + sin((normalizedX * 3 + phase * 0.7) * 2.0 * Math.PI).toFloat() * bassAmplitude
            bassPath.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = bassPath,
            color = Color(0xFF00E5FF).copy(alpha = 0.4f * pulseAlpha),
            style = Stroke(width = 4f)
        )
        
        // Volume wave (medium)
        val volumePath = Path()
        volumePath.moveTo(0f, centerY)
        for (x in 0..width.toInt() step 6) {
            val normalizedX = x / width
            val volumeAmplitude = (volumeLevel / 100f) * height / 2.5f
            val y = centerY + sin((normalizedX * 8 + phase) * 2.0 * Math.PI).toFloat() * volumeAmplitude
            volumePath.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = volumePath,
            color = Color(0xFFFFB74D).copy(alpha = 0.6f * pulseAlpha),
            style = Stroke(width = 3f)
        )
        
        // Treble wave (thin, fast)
        val treblePath = Path()
        treblePath.moveTo(0f, centerY)
        for (x in 0..width.toInt() step 4) {
            val normalizedX = x / width
            val trebleAmplitude = (trebleLevel / 100f) * height / 4
            val y = centerY + sin((normalizedX * 15 + phase * 1.5) * 2.0 * Math.PI).toFloat() * trebleAmplitude
            treblePath.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = treblePath,
            color = Color(0xFFFF5252).copy(alpha = 0.5f * pulseAlpha),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun GestureGuideOverlay(
    modifier: Modifier = Modifier,
    currentGesture: DJGesture,
    gestureMetrics: GestureMetrics
) {
    AnimatedVisibility(
        visible = currentGesture != DJGesture.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E2530).copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gesture Name
                Text(
                    text = when (currentGesture) {
                        is DJGesture.ThumbsUp -> "👍 VOLUME UP"
                        is DJGesture.ThumbsDown -> "👎 VOLUME DOWN"
                        is DJGesture.PeaceSign -> "✌️ BASS PRESET"
                        is DJGesture.OkSign -> "👌 FLAT PRESET"
                        is DJGesture.RockSign -> "🤘 ROCK PRESET"
                        is DJGesture.OpenHand -> "🖐️ MAX VOLUME"
                        is DJGesture.Fist -> "✊ MUTE"
                        is DJGesture.TwoHandsBass -> "🙌 BASS CONTROL"
                        is DJGesture.SwipeVolume -> "👆 FINE VOLUME"
                        else -> "IDLE"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = when (currentGesture) {
                        is DJGesture.ThumbsUp, is DJGesture.ThumbsDown, 
                        is DJGesture.SwipeVolume, is DJGesture.OpenHand, is DJGesture.Fist -> Color(0xFFFFB74D)
                        is DJGesture.TwoHandsBass -> Color(0xFF00E5FF)
                        is DJGesture.PeaceSign, is DJGesture.OkSign, is DJGesture.RockSign -> Color(0xFF00E5FF)
                        else -> Color.White
                    },
                    letterSpacing = 2.sp
                )
                
                // Gesture Details
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (gestureMetrics.fingerCount > 0) {
                        MetricPill(
                            label = "Fingers",
                            value = gestureMetrics.fingerCount.toString(),
                            color = Color(0xFF00E5FF)
                        )
                    }
                    if (gestureMetrics.handOpenness > 0.1f) {
                        MetricPill(
                            label = "Open",
                            value = "${(gestureMetrics.handOpenness * 100).toInt()}%",
                            color = Color(0xFFFFB74D)
                        )
                    }
                    if (gestureMetrics.twoHandDistance > 0.1f) {
                        MetricPill(
                            label = "Distance",
                            value = "${(gestureMetrics.twoHandDistance * 100).toInt()}%",
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
                
                // Instruction
                Text(
                    text = when (currentGesture) {
                        is DJGesture.ThumbsUp -> "Keep thumb up to increase volume"
                        is DJGesture.ThumbsDown -> "Keep thumb down to decrease volume"
                        is DJGesture.PeaceSign -> "Hold peace sign for Bass preset"
                        is DJGesture.OkSign -> "Hold OK sign for Flat preset"
                        is DJGesture.RockSign -> "Hold rock sign for Rock preset"
                        is DJGesture.OpenHand -> "Open hand fully for max volume"
                        is DJGesture.Fist -> "Make fist to mute"
                        is DJGesture.TwoHandsBass -> "Move hands apart/together"
                        is DJGesture.SwipeVolume -> "Swipe up/down for fine control"
                        else -> ""
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.7f)
            )
            Text(
                value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "DJ Gesture Control needs camera access to detect your hand movements for controlling audio.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB74D)
                )
            ) {
                Text("Grant Camera Access", color = Color.Black)
            }
        }
    }
}

private fun ImageProxy.toBitmap(): android.graphics.Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
