package com.soundboost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.SyncState
import com.soundboost.SyncViewModel
import com.soundboost.flash.ScreenFlashOverlay
import com.soundboost.sync.DeviceInfo
import com.soundboost.sync.FlashMode
import com.soundboost.sync.FlashPattern
import com.soundboost.ui.theme.NeonOrange

/**
 * STUNNING MODERN FLASH CONTROL SCREEN - v2.0
 * Professional party mode interface with premium design
 * - Gradient backgrounds with depth
 * - Smooth animations and transitions
 * - Clear visual hierarchy
 * - Premium spacing and typography
 * - Modern glassmorphism effects
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashControlScreen(
    viewModel: SyncViewModel,
    onBack: () -> Unit
) {
    val syncState by viewModel.syncState.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val pendingFlash by viewModel.pendingFlash.collectAsState()
    val bassFlashEnabled by viewModel.bassFlashEnabled.collectAsState()
    val bassFlashIntensity by viewModel.bassFlashIntensity.collectAsState()
    val disconnectionReason by viewModel.disconnectionReason.collectAsState()
    
    var flashDuration by remember { mutableFloatStateOf(250f) }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var repeatCount by remember { mutableIntStateOf(1) }
    var selectedMode by remember { mutableStateOf(FlashMode.SCREEN_ONLY) }
    
    val isHost = syncState is SyncState.Hosting
    val roomName = when (syncState) {
        is SyncState.Hosting -> (syncState as SyncState.Hosting).roomName
        is SyncState.Connected -> (syncState as SyncState.Connected).roomName
        else -> ""
    }
    
    // Animated glow effect - softer, more elegant
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Scaffold(
        topBar = {
            // Premium gradient top bar
            Surface(
                color = Color.Transparent,
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
            ) {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = if (isHost) {
                                    stringResource(R.string.party_mode_hosting)
                                } else {
                                    stringResource(R.string.party_mode_connected)
                                },
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                letterSpacing = 0.3.sp
                            )
                            if (roomName.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = roomName,
                                    fontSize = 13.sp,
                                    color = NeonOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isHost) viewModel.stopHosting() else viewModel.leaveRoom()
                                onBack()
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.common_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        // Premium device counter badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NeonOrange.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, NeonOrange.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = NeonOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = if (isHost) connectedDevices.size.toString() else "1",
                                    fontWeight = FontWeight.Black,
                                    color = NeonOrange,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Connected devices (host only) - Premium card
                if (isHost && connectedDevices.isNotEmpty()) {
                    ConnectedDevicesCard(devices = connectedDevices, glowAlpha = glowAlpha)
                }
                
                // Bass-sync flash toggle (HOST ONLY)
                if (isHost && viewModel.isTorchSupported) {
                    BassFlashSyncCard(
                        enabled = bassFlashEnabled,
                        intensity = bassFlashIntensity,
                        onToggle = { enabled ->
                            viewModel.toggleBassFlashSync(enabled)
                        },
                        onIntensityChanged = { viewModel.setBassFlashIntensity(it) },
                        glowAlpha = glowAlpha
                    )
                }
                
                // HOST: Full controls | CLIENT: Only status view
                if (isHost) {
                    // HOST controls everything - Premium UI
                    FlashModeCard(
                        selectedMode = selectedMode,
                        bassFlashAvailable = bassFlashEnabled && viewModel.isTorchSupported,
                        onModeSelected = { selectedMode = it }
                    )
                    
                    // Color selection - Premium color picker
                    ColorSelectionCard(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                        glowAlpha = glowAlpha
                    )
                    
                    // Duration slider - Refined controls
                    DurationSliderCard(
                        duration = flashDuration,
                        onDurationChanged = { flashDuration = it }
                    )
                    
                    // Repeat count - Refined controls
                    RepeatCountCard(
                        repeatCount = repeatCount,
                        onRepeatChanged = { repeatCount = it }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Trigger button - Stunning premium button
                    TriggerFlashButton(
                        enabled = connectedDevices.isNotEmpty(),
                        onTrigger = {
                            val pattern = FlashPattern(
                                durationMs = flashDuration.toInt(),
                                mode = selectedMode,
                                colorHex = "#${selectedColor.value.toString(16).substring(2, 8).uppercase()}",
                                repeatCount = repeatCount,
                                intervalMs = 100
                            )
                            viewModel.triggerFlash(pattern)
                        },
                        glowAlpha = glowAlpha
                    )
                } else {
                    // CLIENT: Premium status display
                    ClientStatusCard(
                        roomName = roomName,
                        bassFlashEnabled = bassFlashEnabled
                    )
                }
            }
            
            // Flash overlay - CRITICAL: Must be on top
            ScreenFlashOverlay(
                pendingFlash = pendingFlash,
                onFlashConsumed = { viewModel.onFlashConsumed() },
                onRequestTorchPulse = { duration, repeat, interval ->
                    viewModel.requestTorchPulse(duration, repeat, interval)
                }
            )
            
            // 🚨 DISCONNECTION DIAGNOSTIC DIALOG - User can see and copy the reason
            if (disconnectionReason != null && syncState is SyncState.Idle) {
                DisconnectionDiagnosticDialog(
                    reason = disconnectionReason!!,
                    onDismiss = { viewModel.clearDisconnectionReason() }
                )
            }
        }
    }
}

@Composable
private fun ClientStatusCard(
    roomName: String,
    bassFlashEnabled: Boolean
) {
    // Premium glassmorphism card for client status
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonOrange.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(2.dp, NeonOrange.copy(alpha = 0.25f)),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Animated connection icon with premium glow
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = 0.25f),
                                NeonOrange.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(3.dp, NeonOrange.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Room info with premium typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Connected to Room",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    letterSpacing = 0.5.sp,
                    color = NeonOrange
                )
                Text(
                    text = roomName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Premium divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                thickness = 1.dp
            )
            
            // Status indicators - Clean and modern
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusRow(
                    icon = Icons.Default.MusicNote,
                    label = "Bass-Sync",
                    value = if (bassFlashEnabled) "ACTIVE" else "Inactive",
                    isActive = bassFlashEnabled
                )
                StatusRow(
                    icon = Icons.Default.Speed,
                    label = "Latency",
                    value = "0ms (Instant)",
                    isActive = true
                )
                StatusRow(
                    icon = Icons.Default.Sync,
                    label = "Synchronization",
                    value = "Clock-Synced",
                    isActive = true
                )
            }
            
            // Info banner - Subtle and professional
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Host controls all settings. Your device will sync automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isActive) NeonOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isActive) NeonOrange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectedDevicesCard(devices: List<DeviceInfo>, glowAlpha: Float) {
    // Premium devices card with glassmorphism
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Soft ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonOrange.copy(alpha = glowAlpha * 0.1f),
                            Color.Transparent
                        )
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(size.width / 2, size.height / 2)
                )
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, NeonOrange.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    NeonOrange.copy(alpha = 0.2f),
                                    NeonOrange.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Devices,
                        null,
                        tint = NeonOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.party_mode_connected_devices),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 0.3.sp,
                    color = NeonOrange
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Device chips - Modern design
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(devices) { device ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = NeonOrange.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, NeonOrange.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(NeonOrange)
                            )
                            Text(
                                text = device.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                color = NeonOrange
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun BassFlashSyncCard(
    enabled: Boolean,
    intensity: com.soundboost.audio.BassFlashlightSync.FlashIntensity,
    onToggle: (Boolean) -> Unit,
    onIntensityChanged: (com.soundboost.audio.BassFlashlightSync.FlashIntensity) -> Unit,
    glowAlpha: Float
) {
    // Premium bass-sync card with dynamic glow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (enabled) {
                    // Animated pulsing glow when enabled
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.55f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
            },
        shape = RoundedCornerShape(28.dp),
        color = if (enabled) {
            NeonOrange.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = if (enabled) {
            BorderStroke(2.dp, NeonOrange.copy(alpha = 0.35f))
        } else {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        tonalElevation = if (enabled) 6.dp else 2.dp
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Premium icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            NeonOrange,
                                            NeonOrange.copy(alpha = 0.7f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                            .border(
                                2.5.dp,
                                if (enabled) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (enabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Bass-Sync Flash",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 0.3.sp,
                            color = if (enabled) NeonOrange else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (enabled) {
                                "ACTIVE - Syncing with bass"
                            } else {
                                "Auto flash with bass beats"
                            },
                            fontSize = 14.sp,
                            color = if (enabled) {
                                NeonOrange.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                
                // Premium switch
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonOrange,
                        checkedBorderColor = NeonOrange.copy(alpha = 0.5f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
            
            // Intensity controls - Only when enabled
            if (enabled) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    color = NeonOrange.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "SENSITIVITY: ${intensity.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = NeonOrange.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Premium intensity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(
                        com.soundboost.audio.BassFlashlightSync.FlashIntensity.LIGHT to "Light",
                        com.soundboost.audio.BassFlashlightSync.FlashIntensity.NORMAL to "Normal",
                        com.soundboost.audio.BassFlashlightSync.FlashIntensity.STRONG to "Strong"
                    ).forEach { (int, label) ->
                        val isSelected = intensity == int
                        Surface(
                            onClick = { onIntensityChanged(int) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                NeonOrange
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            },
                            border = if (isSelected) {
                                BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                            } else {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            },
                            tonalElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
private fun FlashModeCard(
    selectedMode: FlashMode,
    bassFlashAvailable: Boolean,
    onModeSelected: (FlashMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.party_mode_flash_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.3.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeButton(
                    label = stringResource(R.string.party_mode_screen_only),
                    icon = Icons.Default.PhoneAndroid,
                    selected = selectedMode == FlashMode.SCREEN_ONLY,
                    onClick = { onModeSelected(FlashMode.SCREEN_ONLY) },
                    modifier = Modifier.weight(1f)
                )
                
                if (bassFlashAvailable) {
                    ModeButton(
                        label = stringResource(R.string.party_mode_torch_only),
                        icon = Icons.Default.FlashlightOn,
                        selected = selectedMode == FlashMode.TORCH_ONLY,
                        onClick = { onModeSelected(FlashMode.TORCH_ONLY) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeButton(
                        label = stringResource(R.string.party_mode_both),
                        icon = Icons.Default.Flare,
                        selected = selectedMode == FlashMode.SCREEN_AND_TORCH,
                        onClick = { onModeSelected(FlashMode.SCREEN_AND_TORCH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) NeonOrange else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = if (selected) {
            BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
        } else {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        },
        tonalElevation = if (selected) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ColorSelectionCard(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    glowAlpha: Float
) {
    val colors = listOf(
        Color.White to "White",
        Color.Red to "Red",
        Color(0xFF00FF00) to "Green",
        Color.Blue to "Blue",
        Color.Yellow to "Yellow",
        Color.Magenta to "Magenta",
        Color.Cyan to "Cyan",
        NeonOrange to "Orange"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.party_mode_flash_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.3.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(colors) { (color, name) ->
                    val isSelected = selectedColor == color
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "color_scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .scale(scale)
                            .drawBehind {
                                if (isSelected) {
                                    // Glow effect for selected color
                                    drawCircle(
                                        color = color.copy(alpha = glowAlpha * 0.4f),
                                        radius = size.minDimension * 1.4f
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 2.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                            .clickable(onClick = { onColorSelected(color) }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color == Color.White || color == Color.Yellow) {
                                    Color.Black
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationSliderCard(
    duration: Float,
    onDurationChanged: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.party_mode_duration),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.3.sp
                )
                
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonOrange.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${duration.toInt()} ms",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = NeonOrange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Slider(
                value = duration,
                onValueChange = onDurationChanged,
                valueRange = 50f..2000f,
                steps = 39,
                colors = SliderDefaults.colors(
                    thumbColor = NeonOrange,
                    activeTrackColor = NeonOrange,
                    inactiveTrackColor = NeonOrange.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun RepeatCountCard(
    repeatCount: Int,
    onRepeatChanged: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.party_mode_repeat),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.3.sp
                )
                
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonOrange.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$repeatCount ${if (repeatCount == 1) "time" else "times"}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = NeonOrange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Slider(
                value = repeatCount.toFloat(),
                onValueChange = { onRepeatChanged(it.toInt()) },
                valueRange = 1f..10f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = NeonOrange,
                    activeTrackColor = NeonOrange,
                    inactiveTrackColor = NeonOrange.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
private fun TriggerFlashButton(
    enabled: Boolean,
    onTrigger: () -> Unit,
    glowAlpha: Float
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )
    
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Button(
        onClick = onTrigger,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .scale(if (enabled) scale * pulse else scale)
            .drawBehind {
                if (enabled) {
                    // Premium radial glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = glowAlpha * 0.5f),
                                NeonOrange.copy(alpha = glowAlpha * 0.2f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.85f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonOrange,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (enabled) 10.dp else 0.dp,
            pressedElevation = 14.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.FlashOn,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.party_mode_trigger_flash),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 🚨 DISCONNECTION DIAGNOSTIC DIALOG
 * Shows user why they were disconnected from the room
 * - Displays exception type and message
 * - Copy to clipboard functionality
 * - Professional error presentation
 */
@Composable
private fun DisconnectionDiagnosticDialog(
    reason: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.party_mode_disconnected_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.party_mode_disconnection_reason),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Reason box - monospace font for technical info
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(reason))
                            android.widget.Toast
                                .makeText(context, context.getString(R.string.party_mode_diagnostic_copied), android.widget.Toast.LENGTH_SHORT)
                                .show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.party_mode_copy_diagnostic),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = reason,
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Text(
                    text = stringResource(R.string.party_mode_disconnection_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonOrange
                )
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
