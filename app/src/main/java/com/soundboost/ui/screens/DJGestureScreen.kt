package com.soundboost.ui.screens

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.soundboost.gesture.DJGesture
import com.soundboost.gesture.DJGestureController
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
    var currentGesture by remember { mutableStateOf<DJGesture>(DJGesture.None) }
    
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
            }
        )
    }
    
    val gestureState by gestureController.currentGesture.collectAsState()
    
    LaunchedEffect(gestureState) {
        currentGesture = gestureState
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
            .background(Color(0xFF0A0E14))
    ) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrameReady = { bitmap, timestamp ->
                    gestureController.processFrame(bitmap, timestamp)
                }
            )
            
            DJDeckOverlay(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(350.dp),
                volumeLevel = volumeLevel,
                bassLevel = bassLevel,
                currentGesture = currentGesture
            )
            
            GestureIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                gesture = currentGesture
            )
        } else {
            CameraPermissionRequest(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
        
        TopAppBar(
            title = { Text("DJ Gesture Control", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0A0E14).copy(alpha = 0.9f)
            )
        )
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
fun DJDeckOverlay(
    modifier: Modifier = Modifier,
    volumeLevel: Int,
    bassLevel: Int,
    currentGesture: DJGesture
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E14).copy(alpha = 0.7f),
                        Color(0xFF0A0E14).copy(alpha = 0.95f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DJFader(
                    label = "VOLUME",
                    value = volumeLevel,
                    color = Color(0xFFFFB74D),
                    isActive = currentGesture is DJGesture.VolumeFader
                )
                
                DJFader(
                    label = "BASS",
                    value = bassLevel,
                    color = Color(0xFF00E5FF),
                    isActive = currentGesture is DJGesture.BassCrossfader
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            WaveformVisualizer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                volumeLevel = volumeLevel,
                bassLevel = bassLevel,
                pulseAlpha = pulseAlpha
            )
        }
    }
}

@Composable
fun DJFader(
    label: String,
    value: Int,
    color: Color,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else Color.White.copy(alpha = 0.5f),
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(150.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp, 30.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (150.dp * (1f - value / 100f) - 15.dp))
                    .background(
                        color = if (isActive) color else Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(15.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$value%",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WaveformVisualizer(
    modifier: Modifier = Modifier,
    volumeLevel: Int,
    bassLevel: Int,
    pulseAlpha: Float
) {
    var phase by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            phase += 0.1f
            delay(16)
        }
    }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val path = Path()
        path.moveTo(0f, centerY)
        
        for (x in 0..width.toInt() step 10) {
            val normalizedX = x / width
            val amplitude = (volumeLevel / 100f) * height / 2
            val bassBoost = (bassLevel / 100f) * 0.5f + 0.5f
            
            val y = centerY + sin((normalizedX * 10 + phase) * bassBoost) * amplitude
            path.lineTo(x.toFloat(), y)
        }
        
        drawPath(
            path = path,
            color = Color(0xFFFFB74D).copy(alpha = pulseAlpha),
            style = Stroke(width = 3f)
        )
    }
}

@Composable
fun GestureIndicator(
    modifier: Modifier = Modifier,
    gesture: DJGesture
) {
    AnimatedVisibility(
        visible = gesture !is DJGesture.None,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2530).copy(alpha = 0.9f)
        ) {
            Text(
                text = when (gesture) {
                    is DJGesture.VolumeFader -> "Volume Control"
                    is DJGesture.BassCrossfader -> "Bass Control"
                    else -> ""
                },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
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
