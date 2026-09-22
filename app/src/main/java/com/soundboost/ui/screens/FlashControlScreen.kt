package com.soundboost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.SyncState
import com.soundboost.SyncViewModel
import com.soundboost.data.BoostSettings
import com.soundboost.flash.ScreenFlashOverlay
import com.soundboost.sync.FlashMode
import com.soundboost.sync.FlashPattern
import com.soundboost.ui.theme.getThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashControlScreen(
    viewModel: SyncViewModel,
    state: BoostSettings,
    onBack: () -> Unit
) {
    val syncState by viewModel.syncState.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val pendingFlash by viewModel.pendingFlash.collectAsState()
    val bassFlashEnabled by viewModel.bassFlashEnabled.collectAsState()
    val bassFlashIntensity by viewModel.bassFlashIntensity.collectAsState()
    val disconnectionReason by viewModel.disconnectionReason.collectAsState()
    
    // CRITICAL: Use theme system for dark/light mode
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
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
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = roomName.ifEmpty { "Party Mode" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = themeColors.onSurface
                        )
                        if (connectedDevices.isNotEmpty() || !isHost) {
                            Surface(
                                shape = CircleShape,
                                color = themeColors.accent1.copy(alpha = 0.15f),
                                modifier = Modifier.shadow(4.dp, CircleShape)
                            ) {
                                Text(
                                    if (isHost) "${connectedDevices.size}" else "1",
                                    fontWeight = FontWeight.Black,
                                    color = themeColors.accent1,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isHost) viewModel.stopHosting() else viewModel.leaveRoom()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = themeColors.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isHost) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Bass Sync - MODERN CARD
                        if (viewModel.isTorchSupported) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                color = themeColors.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (bassFlashEnabled) 
                                                    themeColors.accent1.copy(alpha = 0.2f) 
                                                else 
                                                    themeColors.onSurface.copy(alpha = 0.1f),
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        if (bassFlashEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                                        null,
                                                        tint = if (bassFlashEnabled) themeColors.accent1 else themeColors.onSurfaceVariant,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    stringResource(R.string.party_mode_bass_flash_sync),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = themeColors.onSurface
                                                )
                                                Text(
                                                    if (bassFlashEnabled) 
                                                        stringResource(R.string.party_mode_bass_sync_active)
                                                    else 
                                                        stringResource(R.string.party_mode_bass_sync_inactive),
                                                    fontSize = 13.sp,
                                                    color = themeColors.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = bassFlashEnabled,
                                            onCheckedChange = { viewModel.toggleBassFlashSync(it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = themeColors.accent1
                                            )
                                        )
                                    }
                                    
                                    if (bassFlashEnabled) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val intensities = listOf(
                                                com.soundboost.audio.BassFlashlightSync.FlashIntensity.LIGHT to stringResource(R.string.party_mode_intensity_light),
                                                com.soundboost.audio.BassFlashlightSync.FlashIntensity.NORMAL to stringResource(R.string.party_mode_intensity_normal),
                                                com.soundboost.audio.BassFlashlightSync.FlashIntensity.STRONG to stringResource(R.string.party_mode_intensity_strong)
                                            )
                                            intensities.forEach { (intensity, label) ->
                                                val selected = bassFlashIntensity == intensity
                                                Surface(
                                                    onClick = { viewModel.setBassFlashIntensity(intensity) },
                                                    modifier = Modifier.weight(1f).height(42.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (selected) 
                                                        themeColors.accent1.copy(alpha = 0.2f)
                                                    else 
                                                        themeColors.onSurface.copy(alpha = 0.05f)
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.border(
                                                            1.dp,
                                                            if (selected) themeColors.accent1 else Color.Transparent,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                    ) {
                                                        Text(
                                                            label,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (selected) themeColors.accent1 else themeColors.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Flash Mode - COMPACT CARDS
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = themeColors.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.party_mode_flash_mode),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = themeColors.onSurface
                                )
                                
                                ModeOption(
                                    label = stringResource(R.string.party_mode_screen_only),
                                    icon = Icons.Default.PhoneAndroid,
                                    selected = selectedMode == FlashMode.SCREEN_ONLY,
                                    onClick = { selectedMode = FlashMode.SCREEN_ONLY },
                                    themeColors = themeColors
                                )
                                
                                if (viewModel.isTorchSupported) {
                                    ModeOption(
                                        label = stringResource(R.string.party_mode_torch_only),
                                        icon = Icons.Default.FlashlightOn,
                                        selected = selectedMode == FlashMode.TORCH_ONLY,
                                        onClick = { selectedMode = FlashMode.TORCH_ONLY },
                                        themeColors = themeColors
                                    )
                                    ModeOption(
                                        label = stringResource(R.string.party_mode_both),
                                        icon = Icons.Default.Flare,
                                        selected = selectedMode == FlashMode.SCREEN_AND_TORCH,
                                        onClick = { selectedMode = FlashMode.SCREEN_AND_TORCH },
                                        themeColors = themeColors
                                    )
                                }
                            }
                        }
                        
                        // Color Picker - MOBILE RESPONSIVE GRID
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = themeColors.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    stringResource(R.string.party_mode_flash_color),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = themeColors.onSurface
                                )
                                
                                // 2 ROWS x 4 COLORS - Perfect mobile layout
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Row 1
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf(
                                            Color.White, Color(0xFFFF5252), 
                                            Color(0xFF00E676), Color(0xFF2979FF)
                                        ).forEach { color ->
                                            ColorCircle(color, selectedColor, themeColors) { selectedColor = it }
                                        }
                                    }
                                    // Row 2
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf(
                                            Color(0xFFFFEB3B), Color(0xFFE040FB),
                                            Color(0xFF00E5FF), Color(0xFFFF9800)
                                        ).forEach { color ->
                                            ColorCircle(color, selectedColor, themeColors) { selectedColor = it }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Duration & Repeat - MOBILE OPTIMIZED
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = themeColors.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Duration
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.party_mode_duration),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = themeColors.onSurface
                                        )
                                        Text(
                                            "${flashDuration.toInt()}ms",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.accent1
                                        )
                                    }
                                    Slider(
                                        value = flashDuration,
                                        onValueChange = { flashDuration = it },
                                        valueRange = 50f..2000f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = themeColors.accent1,
                                            activeTrackColor = themeColors.accent1,
                                            inactiveTrackColor = themeColors.accent1.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                                
                                // Repeat
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.party_mode_repeat),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = themeColors.onSurface
                                        )
                                        Text(
                                            "$repeatCount",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.accent2
                                        )
                                    }
                                    Slider(
                                        value = repeatCount.toFloat(),
                                        onValueChange = { repeatCount = it.toInt() },
                                        valueRange = 1f..10f,
                                        steps = 9,
                                        colors = SliderDefaults.colors(
                                            thumbColor = themeColors.accent2,
                                            activeTrackColor = themeColors.accent2,
                                            inactiveTrackColor = themeColors.accent2.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    
                    // TRIGGER BUTTON - PREMIUM WITH GRADIENT SHADOW
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        themeColors.background
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Button(
                            onClick = {
                                val pattern = FlashPattern(
                                    durationMs = flashDuration.toInt(),
                                    mode = selectedMode,
                                    colorHex = "#${selectedColor.value.toString(16).substring(2, 8).uppercase()}",
                                    repeatCount = repeatCount,
                                    intervalMs = 100
                                )
                                viewModel.triggerFlash(pattern)
                            },
                            enabled = connectedDevices.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .shadow(if (connectedDevices.isNotEmpty()) 16.dp else 0.dp, RoundedCornerShape(18.dp))
                                .scale(if (connectedDevices.isNotEmpty()) pulse else 1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.accent1,
                                disabledContainerColor = themeColors.onSurface.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.party_mode_trigger_flash).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            } else {
                // CLIENT - ELEGANT WAITING
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = themeColors.surface,
                        modifier = Modifier.shadow(12.dp, RoundedCornerShape(28.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                null,
                                tint = themeColors.accent1,
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                roomName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent1
                            )
                            Text(
                                "Waiting for host",
                                fontSize = 15.sp,
                                color = themeColors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            ScreenFlashOverlay(
                pendingFlash = pendingFlash,
                onFlashConsumed = { viewModel.onFlashConsumed() },
                onRequestTorchPulse = { d, r, i ->
                    viewModel.requestTorchPulse(d, r, i)
                }
            )
            
            if (disconnectionReason != null && syncState is SyncState.Idle) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearDisconnectionReason() },
                    icon = { Icon(Icons.Default.Warning, null, tint = themeColors.accent1) },
                    title = { 
                        Text(
                            stringResource(R.string.party_mode_disconnected_title),
                            color = themeColors.onSurface
                        ) 
                    },
                    text = { Text(disconnectionReason!!, color = themeColors.onSurfaceVariant) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearDisconnectionReason() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.accent1
                            )
                        ) {
                            Text(stringResource(R.string.common_ok))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    selectedColor: Color,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    onSelect: (Color) -> Unit
) {
    val isSelected = selectedColor == color
    Surface(
        onClick = { onSelect(color) },
        modifier = Modifier
            .size(if (isSelected) 60.dp else 56.dp)
            .shadow(if (isSelected) 8.dp else 2.dp, CircleShape),
        shape = CircleShape,
        color = color
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = themeColors.accent1,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) 
            themeColors.accent1.copy(alpha = 0.15f)
        else 
            themeColors.onSurface.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .border(
                    1.dp,
                    if (selected) themeColors.accent1 else Color.Transparent,
                    RoundedCornerShape(14.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (selected) themeColors.accent1 else themeColors.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) themeColors.accent1 else themeColors.onSurface
            )
        }
    }
}

// Color luminance helper
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
