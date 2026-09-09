package com.soundboost.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundboost.ai.AIVocalSeparationState
import com.soundboost.ai.AppCompatibility
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.ModernSlider
import com.soundboost.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIVocalSeparationScreen(
    state: BoostSettings,
    aiState: AIVocalSeparationState = AIVocalSeparationState(),  // TODO: Wire to ViewModel
    onStartCapture: () -> Unit = {},
    onStopCapture: () -> Unit = {},
    onVocalVolumeChange: (Float) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI Vocal Separation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "BETA - Experimental Feature",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Beta Warning
            BetaWarningCard(themeColors)
            
            // Status Card
            StatusCard(
                aiState = aiState,
                themeColors = themeColors
            )
            
            // Start/Stop Button
            Button(
                onClick = {
                    if (aiState.isCapturing) {
                        onStopCapture()
                    } else {
                        onStartCapture()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = aiState.canStart || aiState.isCapturing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aiState.isCapturing) {
                        Color.Red.copy(alpha = 0.8f)
                    } else {
                        themeColors.accent1
                    }
                )
            ) {
                Text(
                    if (aiState.isCapturing) "STOP SEPARATION" else "START SEPARATION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
            
            // Volume Controls (only visible when capturing)
            AnimatedVisibility(visible = aiState.isCapturing) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    VolumeControlCard(
                        title = "Vocal Volume",
                        value = aiState.vocalVolume,
                        onValueChange = onVocalVolumeChange,
                        themeColors = themeColors
                    )
                    
                    VolumeControlCard(
                        title = "Music Volume",
                        value = aiState.musicVolume,
                        onValueChange = onMusicVolumeChange,
                        themeColors = themeColors
                    )
                }
            }
            
            // Performance Stats (when capturing)
            AnimatedVisibility(visible = aiState.isCapturing) {
                PerformanceCard(
                    aiState = aiState,
                    themeColors = themeColors
                )
            }
            
            // Compatibility Info
            CompatibilityCard(themeColors)
            
            // How It Works
            HowItWorksCard(themeColors)
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BetaWarningCard(themeColors: ThemeColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800)
            )
            Column {
                Text(
                    "Experimental Feature",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
                Text(
                    "May not work with all apps. Requires Android 10+.",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    aiState: AIVocalSeparationState,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (aiState.isCapturing) Color.Green.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                ) {
                    Text(
                        if (aiState.isCapturing) "ACTIVE" else "INACTIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (aiState.isCapturing) Color.Green else Color.Gray
                    )
                }
            }
            
            if (aiState.detectedApp != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Detected: ${aiState.detectedApp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.onSurface.copy(alpha = 0.8f)
                )
                
                aiState.isAppSupported?.let { supported ->
                    Text(
                        if (supported) "✓ Supported" else "✗ Not Supported (DRM)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (supported) Color.Green else Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (aiState.isCapturing && aiState.totalLatencyMs > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Latency: ${aiState.totalLatencyMs}ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        aiState.totalLatencyMs < 300 -> Color.Green
                        aiState.totalLatencyMs < 500 -> Color(0xFFFF9800)
                        else -> Color.Red
                    },
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (aiState.hasError) {
                Spacer(Modifier.height(12.dp))
                Text(
                    aiState.captureError ?: aiState.modelLoadError ?: "Unknown error",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun VolumeControlCard(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
                Text(
                    "${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent1
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            ModernSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..2f,
                valueLabel = "${(value * 100).toInt()}%",
                accentColor = themeColors.accent1,
                surfaceColor = themeColors.surface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun PerformanceCard(
    aiState: AIVocalSeparationState,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Performance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )
            
            Spacer(Modifier.height(12.dp))
            
            PerformanceRow("Buffer", "${aiState.bufferFillPercent}%", themeColors)
            PerformanceRow("Dropped", "${aiState.droppedFrames} frames", themeColors)
            PerformanceRow("CPU", "${aiState.cpuUsagePercent}%", themeColors)
        }
    }
}

@Composable
private fun PerformanceRow(label: String, value: String, themeColors: ThemeColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = themeColors.onSurface.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = themeColors.onSurface
        )
    }
}

@Composable
private fun CompatibilityCard(themeColors: ThemeColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "App Compatibility",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                "✓ Supported",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Green
            )
            Text(
                AppCompatibility.SUPPORTED_APPS.joinToString(", ") { it.appName },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "✗ Not Supported (DRM)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )
            Text(
                AppCompatibility.UNSUPPORTED_APPS.joinToString(", ") { it.appName },
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun HowItWorksCard(themeColors: ThemeColors) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "How It Works",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = themeColors.onSurface
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                "1. Captures audio from playing apps\n" +
                "2. AI separates vocals from music\n" +
                "3. Adjustable volume mix\n" +
                "4. Real-time processing\n\n" +
                "Note: Latency 200-400ms is normal",
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurface.copy(alpha = 0.7f),
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.5f
            )
        }
    }
}
