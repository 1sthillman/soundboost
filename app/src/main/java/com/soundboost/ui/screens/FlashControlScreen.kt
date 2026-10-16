package com.soundboost.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.soundboost.R
import com.soundboost.SyncState
import com.soundboost.SyncViewModel
import com.soundboost.data.BoostSettings
import com.soundboost.flash.ScreenFlashOverlay
import com.soundboost.sync.MusicAction
import com.soundboost.sync.MusicShareState
import com.soundboost.ui.theme.getThemeColors
import kotlin.math.abs

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
    val disconnectionReason by viewModel.disconnectionReason.collectAsState()
    val hasAcceptedFlashWarning by viewModel.hasAcceptedFlashWarning.collectAsState()
    
    var showFlashWarning by remember { mutableStateOf(false) }
    
    // CRITICAL: Show warning on first entry if not accepted
    LaunchedEffect(hasAcceptedFlashWarning) {
        if (!hasAcceptedFlashWarning) {
            showFlashWarning = true
        }
    }
    
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    val isHost = syncState is SyncState.Hosting
    val roomName = when (syncState) {
        is SyncState.Hosting -> (syncState as SyncState.Hosting).roomName
        is SyncState.Connected -> (syncState as SyncState.Connected).roomName
        else -> ""
    }

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
                HostDJConsole(viewModel, themeColors)
            } else {
                ClientWaitingScreen(roomName, viewModel, themeColors)
            }
            
            ScreenFlashOverlay(
                pendingFlash = pendingFlash,
                onFlashConsumed = { viewModel.onFlashConsumed() },
                onRequestTorchPulse = { d, r, i ->
                    viewModel.requestTorchPulse(d, r, i)
                },
                hasAcceptedWarning = hasAcceptedFlashWarning
            )
            
            // CRITICAL: Flash warning dialog (epilepsy safety)
            if (showFlashWarning) {
                AlertDialog(
                    onDismissRequest = { 
                        if (hasAcceptedFlashWarning) {
                            showFlashWarning = false
                        }
                    },
                    icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800)) },
                    title = { 
                        Text(
                            stringResource(R.string.flash_warning_title),
                            color = themeColors.onSurface,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    text = { 
                        Text(
                            stringResource(R.string.flash_warning_message),
                            color = themeColors.onSurfaceVariant
                        ) 
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                viewModel.acceptFlashWarning()
                                showFlashWarning = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text(stringResource(R.string.flash_warning_accept))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                onBack()
                            }
                        ) {
                            Text(stringResource(R.string.flash_warning_cancel))
                        }
                    }
                )
            }
            
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
private fun HostDJConsole(
    viewModel: SyncViewModel,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val musicState by viewModel.musicState.collectAsState()
    val currentPlaylist by viewModel.currentPlaylist.collectAsState()
    val djState by viewModel.djState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0a0a0a),
                        Color(0xFF151515),
                        Color(0xFF0a0a0a)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // DJ DECK
        DJDeckSection(viewModel, musicState, currentPlaylist, djState, themeColors)
        
        // PLAYLIST
        PlaylistCarousel(viewModel, currentPlaylist, musicState, themeColors)
        
        // MIXER
        DJMixerControls(viewModel, djState, musicState, themeColors)
        
        // DJ EFFECTS PANEL - Modern professional design
        DJEffectsPanel(viewModel, themeColors)
        
        // BASS FLASH
        if (viewModel.isTorchSupported) {
            BassFlashSection(viewModel, themeColors)
        }
    }
}

@Composable
private fun DJDeckSection(
    viewModel: SyncViewModel,
    musicState: MusicShareState,
    currentPlaylist: com.soundboost.sync.Playlist?,
    djState: com.soundboost.sync.DJState,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1a1a1a)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF202020),
                            Color(0xFF1a1a1a)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val currentTrack = currentPlaylist?.currentTrack
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentTrack?.fileName ?: "NO TRACK",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        when (musicState) {
                            is MusicShareState.Playing -> "PLAYING"
                            is MusicShareState.Paused -> "PAUSED"
                            else -> "READY"
                        },
                        fontSize = 11.sp,
                        color = when (musicState) {
                            is MusicShareState.Playing -> Color(0xFF00E676)
                            is MusicShareState.Paused -> Color(0xFFFFB74D)
                            else -> Color(0xFF888888)
                        },
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                if (musicState is MusicShareState.Playing || musicState is MusicShareState.Paused) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (djState.playbackSpeed != 1.0f) 
                            themeColors.accent1.copy(alpha = 0.25f)
                        else 
                            Color(0xFF2a2a2a)
                    ) {
                        Text(
                            "${(djState.playbackSpeed * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (djState.playbackSpeed != 1.0f) themeColors.accent1 else Color(0xFF999999),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            VinylDeck(viewModel, musicState, djState, themeColors)
            TransportControls(viewModel, musicState, currentPlaylist, themeColors)
        }
    }
}

@Composable
private fun VinylDeck(
    viewModel: SyncViewModel,
    musicState: MusicShareState,
    djState: com.soundboost.sync.DJState,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var scratchStartPos by remember { mutableFloatStateOf(0f) }
    var currentScratchPos by remember { mutableFloatStateOf(0f) }
    var lastHapticPos by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(musicState, djState.playbackSpeed) {
        if (musicState is MusicShareState.Playing && !isDragging) {
            while (true) {
                kotlinx.coroutines.delay(16)
                rotation += djState.playbackSpeed * 2f
                if (rotation > 360f) rotation -= 360f
                else if (rotation < -360f) rotation += 360f
            }
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "SCRATCH VINYL",
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            letterSpacing = 1.5.sp
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            scratchStartPos = offset.x
                            currentScratchPos = offset.x
                            lastHapticPos = offset.x
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = { 
                            isDragging = false
                            if (djState.playbackSpeed != 1.0f) {
                                viewModel.djScratchSeek(0, 1.0f)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentScratchPos += dragAmount.x
                            
                            val scratchDelta = currentScratchPos - scratchStartPos
                            rotation += dragAmount.x * 0.8f
                            
                            val speed = when {
                                dragAmount.x < -20f -> -2.0f
                                dragAmount.x < -14f -> -1.6f
                                dragAmount.x < -8f -> -1.2f
                                dragAmount.x < -3f -> -0.6f
                                dragAmount.x > 20f -> 2.0f
                                dragAmount.x > 14f -> 1.8f
                                dragAmount.x > 8f -> 1.4f
                                dragAmount.x > 3f -> 1.1f
                                else -> 0.05f
                            }
                            
                            scope.launch {
                                viewModel.djScratchSeek(0, speed)
                            }
                            
                            if (abs(currentScratchPos - lastHapticPos) > 12f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastHapticPos = currentScratchPos
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f * 0.88f
                
                if (isDragging) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF3D00).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.2f
                        ),
                        radius = radius * 1.2f,
                        center = center
                    )
                }
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1a1a1a),
                            Color(0xFF0a0a0a)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                
                rotate(rotation, center) {
                    for (i in 1..12) {
                        val r = radius * (0.3f + i * 0.05f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2a2a2a),
                                Color(0xFF1a1a1a)
                            )
                        ),
                        radius = radius * 0.26f,
                        center = center
                    )
                    
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = if (isDragging) {
                                listOf(Color(0xFFFF3D00), Color(0xFFFF6E40))
                            } else {
                                listOf(themeColors.accent1.copy(alpha = 0.8f), themeColors.accent1)
                            }
                        ),
                        start = center,
                        end = Offset(center.x + radius * 0.2f, center.y),
                        strokeWidth = if (isDragging) 6.dp.toPx() else 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            
            Icon(
                if (musicState is MusicShareState.Playing) Icons.Default.PlayArrow else Icons.Default.Pause,
                null,
                tint = if (isDragging) Color(0xFFFF3D00) else Color(0xFFCCCCCC),
                modifier = Modifier.size(if (isDragging) 42.dp else 38.dp)
            )
        }
        
        if (isDragging) {
            val isReverse = djState.playbackSpeed < 0
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isReverse) Color(0xFFFF3D00) else Color(0xFF00E676)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isReverse) "◀◀" else "▶▶",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        "${if (isReverse) "" else "+"}${(djState.playbackSpeed * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportControls(
    viewModel: SyncViewModel,
    musicState: MusicShareState,
    currentPlaylist: com.soundboost.sync.Playlist?,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    // CRITICAL: Check if all clients have downloaded music
    val allClientsReady by viewModel.allClientsReady.collectAsState()
    val clientDownloadStatus by viewModel.clientDownloadStatus.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = { viewModel.previousTrack() },
            enabled = currentPlaylist?.hasPrevious == true,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(
                    if (currentPlaylist?.hasPrevious == true) themeColors.onSurface.copy(alpha = 0.1f) 
                    else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(Icons.Default.SkipPrevious, null)
        }
        
        // MAIN PLAY/PAUSE BUTTON
        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    when (musicState) {
                        is MusicShareState.Playing -> viewModel.controlSharedMusic(MusicAction.PAUSE)
                        is MusicShareState.Paused -> viewModel.controlSharedMusic(MusicAction.RESUME)
                        is MusicShareState.Ready, is MusicShareState.Prepared -> viewModel.startSharedMusic()
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.accent1
                ),
                shape = RoundedCornerShape(16.dp),
                // CRITICAL: Enable only when:
                // 1. Music is ready/prepared/playing/paused (not idle/downloading)
                // 2. ALL clients have finished downloading (allClientsReady)
                enabled = when (musicState) {
                    is MusicShareState.Playing, is MusicShareState.Paused -> true
                    is MusicShareState.Ready, is MusicShareState.Prepared -> {
                        // For START: require all clients ready
                        allClientsReady
                    }
                    else -> false
                }
            ) {
                Icon(
                    when (musicState) {
                        is MusicShareState.Playing -> Icons.Default.Pause
                        else -> Icons.Default.PlayArrow
                    },
                    null,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // CLIENT READY STATUS INDICATOR
            if (connectedDevices.isNotEmpty() && musicState is MusicShareState.Ready) {
                val readyCount = clientDownloadStatus.count { it.value >= 1.0f }
                val totalClients = connectedDevices.size
                
                Text(
                    if (allClientsReady) "✅ All Ready" else "⏳ $readyCount/$totalClients Ready",
                    fontSize = 10.sp,
                    color = if (allClientsReady) Color(0xFF00E676) else themeColors.onSurfaceVariant,
                    fontWeight = if (allClientsReady) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        IconButton(
            onClick = { viewModel.nextTrack() },
            enabled = currentPlaylist?.hasNext == true,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(
                    if (currentPlaylist?.hasNext == true) themeColors.onSurface.copy(alpha = 0.1f) 
                    else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
        ) {
            Icon(Icons.Default.SkipNext, null)
        }
        
        if (musicState is MusicShareState.Playing || musicState is MusicShareState.Paused) {
            IconButton(
                onClick = { viewModel.controlSharedMusic(MusicAction.STOP) },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFFF5252), CircleShape)
            ) {
                Icon(Icons.Default.Stop, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun PlaylistCarousel(
    viewModel: SyncViewModel,
    currentPlaylist: com.soundboost.sync.Playlist?,
    musicState: MusicShareState,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val musicFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addTrackToPlaylist(it) }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1a1a1a)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF222222),
                            Color(0xFF1a1a1a)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PLAYLIST",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Surface(
                    onClick = { musicFileLauncher.launch("audio/*") },
                    shape = CircleShape,
                    color = themeColors.accent1
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(8.dp)
                    )
                }
            }
            
            if (currentPlaylist != null && currentPlaylist.tracks.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(currentPlaylist.tracks) { index, track ->
                        val isCurrent = index == currentPlaylist.currentTrackIndex
                        TrackCard(
                            track = track,
                            isPlaying = isCurrent && musicState is MusicShareState.Playing,
                            isCurrent = isCurrent,
                            onClick = { if (!isCurrent) viewModel.selectTrack(index) },
                            onRemove = { viewModel.removeTrackFromPlaylist(track.id) },
                            themeColors = themeColors
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF2a2a2a)
                ) {
                    Text(
                        "📀 Tap + to add music tracks",
                        fontSize = 13.sp,
                        color = Color(0xFFBBBBBB),
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: com.soundboost.sync.PlaylistTrack,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(130.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) 
            themeColors.accent1.copy(alpha = 0.15f)
        else 
            themeColors.onSurface.copy(alpha = 0.05f),
        border = if (isCurrent) 
            androidx.compose.foundation.BorderStroke(2.dp, themeColors.accent1) 
        else null
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCurrent) themeColors.accent1.copy(alpha = 0.3f)
                            else themeColors.onSurface.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                        null,
                        tint = if (isCurrent) themeColors.accent1 else themeColors.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                Text(
                    track.fileName,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) themeColors.accent1 else themeColors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    null,
                    tint = themeColors.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DJMixerControls(
    viewModel: SyncViewModel,
    djState: com.soundboost.sync.DJState,
    musicState: MusicShareState,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    if (musicState !is MusicShareState.Playing && 
        musicState !is MusicShareState.Paused && 
        musicState !is MusicShareState.Ready) return
    
    val haptic = LocalHapticFeedback.current
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1a1a1a)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF222222),
                            Color(0xFF1a1a1a)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "DJ MIXER",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp,
                color = Color.White
            )
            
            // EQ KNOBS
            Text(
                "EQUALIZER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBBBBBB)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EQKnob("BASS", djState.bass, Color(0xFFFF5252), haptic) {
                    viewModel.updateDJEQ(it, djState.mid, djState.treble)
                }
                EQKnob("MID", djState.mid, Color(0xFFFFEB3B), haptic) {
                    viewModel.updateDJEQ(djState.bass, it, djState.treble)
                }
                EQKnob("HIGH", djState.treble, Color(0xFF00E676), haptic) {
                    viewModel.updateDJEQ(djState.bass, djState.mid, it)
                }
            }
            
            HorizontalDivider(color = Color(0xFF2a2a2a))
            
            // MASTER FADER
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "MASTER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBBBBBB)
                    )
                    Text(
                        "${(djState.masterVolume * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.accent1
                    )
                }
                Slider(
                    value = djState.masterVolume,
                    onValueChange = { newVol ->
                        if (abs(newVol - djState.masterVolume) > 0.05f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        viewModel.updateDJVolume(master = newVol)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = themeColors.accent1,
                        activeTrackColor = themeColors.accent1,
                        inactiveTrackColor = Color(0xFF2a2a2a)
                    ),
                    modifier = Modifier.height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun RowScope.EQKnob(
    label: String,
    value: Float,
    color: Color,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var rotation by remember { mutableFloatStateOf((value - 0.5f) * 270f) }
        
        Box(
            modifier = Modifier
                .size(65.dp)
                .pointerInput(Unit) {
                    // DOUBLE TAP = RESET TO 0.5
                    detectTapGestures(
                        onDoubleTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onValueChange(0.5f)
                            rotation = 0f
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val oldRotation = rotation
                            rotation = (rotation - dragAmount.y * 0.5f).coerceIn(-135f, 135f)
                            
                            // Haptic feedback her 20 derece rotasyonda
                            if (abs(rotation - oldRotation) > 20f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            
                            val newValue = ((rotation / 270f) + 0.5f).coerceIn(0f, 1f)
                            onValueChange(newValue)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f * 0.8f
                
                // Background - DARK, MAVİ YOK!
                drawCircle(
                    color = Color(0xFF1a1a1a),
                    radius = radius,
                    center = center
                )
                
                // Outer ring
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Value indicator
                rotate(rotation, center) {
                    drawLine(
                        color = color,
                        start = center,
                        end = Offset(center.x, center.y - radius * 0.7f),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                // Center dot - yeşil olsun varsayılana yakınsa
                drawCircle(
                    color = if (abs(value - 0.5f) < 0.05f) Color(0xFF00E676) else color,
                    radius = 5.dp.toPx(),
                    center = center
                )
            }
        }
        
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (abs(value - 0.5f) < 0.05f) "✓ 50" else "${(value * 100).toInt()}",
            fontSize = 9.sp,
            color = if (abs(value - 0.5f) < 0.05f) Color(0xFF00E676) else color,
            fontWeight = FontWeight.Bold
        )
        if (abs(value - 0.5f) > 0.05f) {
            Text(
                "2x tap",
                fontSize = 7.sp,
                color = color.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun BassFlashSection(
    viewModel: SyncViewModel,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val bassFlashEnabled by viewModel.bassFlashEnabled.collectAsState()
    val bassFlashIntensity by viewModel.bassFlashIntensity.collectAsState()
    
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Bass Flash Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        if (bassFlashEnabled) "Active" else "Inactive",
                        fontSize = 12.sp,
                        color = themeColors.onSurfaceVariant
                    )
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
                    listOf(
                        com.soundboost.audio.BassFlashlightSync.FlashIntensity.LIGHT to "Light",
                        com.soundboost.audio.BassFlashlightSync.FlashIntensity.NORMAL to "Normal",
                        com.soundboost.audio.BassFlashlightSync.FlashIntensity.STRONG to "Strong"
                    ).forEach { (intensity, label) ->
                        val selected = bassFlashIntensity == intensity
                        Surface(
                            onClick = { viewModel.setBassFlashIntensity(intensity) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) 
                                themeColors.accent1.copy(alpha = 0.2f)
                            else 
                                themeColors.onSurface.copy(alpha = 0.05f),
                            border = if (selected) 
                                androidx.compose.foundation.BorderStroke(2.dp, themeColors.accent1) 
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
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

@Composable
private fun ClientWaitingScreen(
    roomName: String,
    viewModel: SyncViewModel,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val bassFlashEnabled by viewModel.bassFlashEnabled.collectAsState()
    val musicState by viewModel.musicState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val djState by viewModel.djState.collectAsState()
    
    // CRITICAL: AUTO-ENABLE bass flash when CLIENT joins!
    LaunchedEffect(Unit) {
        if (!bassFlashEnabled && viewModel.isTorchSupported) {
            kotlinx.coroutines.delay(1000)
            android.util.Log.d("FlashControl", "CLIENT: Auto-enabling bass flash")
            viewModel.toggleBassFlashSync(true)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER - Room name with gradient
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = themeColors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                themeColors.accent1.copy(alpha = 0.15f),
                                themeColors.surface
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = null,
                        tint = themeColors.accent1,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        roomName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        stringResource(R.string.party_mode_client_subtitle),
                        fontSize = 14.sp,
                        color = themeColors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // MUSIC STATUS CARD - Download/Playing state
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = themeColors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status icon with animation
                    val animatedSize by animateDpAsState(
                        targetValue = if (musicState is MusicShareState.Playing) 48.dp else 40.dp,
                        label = "icon_size"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(animatedSize)
                            .background(
                                when (musicState) {
                                    is MusicShareState.Playing -> Color(0xFF00E676).copy(alpha = 0.2f)
                                    is MusicShareState.Ready -> Color(0xFF00BCD4).copy(alpha = 0.2f)
                                    is MusicShareState.Downloading -> themeColors.accent1.copy(alpha = 0.2f)
                                    else -> themeColors.onSurface.copy(alpha = 0.1f)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (musicState) {
                                is MusicShareState.Playing -> Icons.Default.PlayArrow
                                is MusicShareState.Paused -> Icons.Default.Pause
                                is MusicShareState.Downloading -> Icons.Default.Download
                                is MusicShareState.Ready -> Icons.Default.CheckCircle
                                else -> Icons.Default.MusicNote
                            },
                            contentDescription = null,
                            tint = when (musicState) {
                                is MusicShareState.Playing -> Color(0xFF00E676)
                                is MusicShareState.Ready -> Color(0xFF00BCD4)
                                is MusicShareState.Downloading -> themeColors.accent1
                                else -> themeColors.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (musicState) {
                                is MusicShareState.Downloading -> stringResource(R.string.party_mode_downloading)
                                is MusicShareState.Ready -> stringResource(R.string.party_mode_ready)
                                is MusicShareState.Playing -> stringResource(R.string.party_mode_playing)
                                is MusicShareState.Paused -> stringResource(R.string.party_mode_paused)
                                else -> stringResource(R.string.party_mode_waiting)
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface
                        )
                        Text(
                            when (musicState) {
                                is MusicShareState.Downloading -> {
                                    val progress = (downloadProgress * 100).toInt()
                                    "$progress% ${stringResource(R.string.party_mode_complete)}"
                                }
                                is MusicShareState.Ready -> stringResource(R.string.party_mode_waiting_dj_start)
                                is MusicShareState.Playing -> stringResource(R.string.party_mode_synced)
                                is MusicShareState.Paused -> stringResource(R.string.party_mode_paused_by_dj)
                                else -> stringResource(R.string.party_mode_connecting)
                            },
                            fontSize = 13.sp,
                            color = themeColors.onSurfaceVariant
                        )
                    }
                }
                
                // Download progress bar
                if (musicState is MusicShareState.Downloading) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = themeColors.accent1,
                        trackColor = themeColors.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
        
        // DJ MIXER PREVIEW - Show what DJ is doing
        if (musicState is MusicShareState.Playing || musicState is MusicShareState.Paused) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = themeColors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.party_mode_dj_controls),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface,
                        letterSpacing = 0.5.sp
                    )
                    
                    // EQ visualization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EQBar(stringResource(R.string.eq_bass), djState.bass, Color(0xFFFF5252), themeColors)
                        EQBar(stringResource(R.string.eq_mid), djState.mid, Color(0xFFFFEB3B), themeColors)
                        EQBar(stringResource(R.string.eq_treble), djState.treble, Color(0xFF00E676), themeColors)
                    }
                    
                    // Master volume
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.dj_master_volume),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.onSurface
                        )
                        Text(
                            "${(djState.masterVolume * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.accent1
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = { djState.masterVolume },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = themeColors.accent1,
                        trackColor = themeColors.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
        
        // FLASH SYNC CONTROL
        if (viewModel.isTorchSupported) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = themeColors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (bassFlashEnabled) Color(0xFF00E676).copy(alpha = 0.2f)
                                        else themeColors.onSurface.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (bassFlashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                    contentDescription = null,
                                    tint = if (bassFlashEnabled) Color(0xFF00E676) else themeColors.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Column {
                                Text(
                                    stringResource(R.string.party_mode_flash_sync),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColors.onSurface
                                )
                                Text(
                                    if (bassFlashEnabled) 
                                        stringResource(R.string.party_mode_flash_active)
                                    else 
                                        stringResource(R.string.party_mode_flash_inactive),
                                    fontSize = 12.sp,
                                    color = themeColors.onSurfaceVariant
                                )
                            }
                        }
                        
                        Switch(
                            checked = bassFlashEnabled,
                            onCheckedChange = { viewModel.toggleBassFlashSync(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00E676),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = themeColors.onSurface.copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    if (bassFlashEnabled) {
                        HorizontalDivider(color = themeColors.onSurface.copy(alpha = 0.1f))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF00E676).copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                stringResource(R.string.party_mode_flash_info),
                                fontSize = 11.sp,
                                color = themeColors.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.EQBar(
    label: String,
    value: Float,
    color: Color,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(120.dp)
                .background(
                    themeColors.onSurface.copy(alpha = 0.1f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.6f),
                                color
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
            )
        }
        
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.onSurface
        )
        Text(
            "${(value * 100).toInt()}%",
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
private fun DJEffectsPanel(
    viewModel: SyncViewModel,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val haptic = LocalHapticFeedback.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0a0a0a)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF151515),
                            Color(0xFF0a0a0a)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FX BANK",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = themeColors.accent1.copy(alpha = 0.2f)
                ) {
                    Text(
                        "20ms SYNC",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.accent1,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            
            HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
            
            EffectCategorySection(
                title = "DROPS",
                effects = com.soundboost.sync.DJEffects.getByCategory(com.soundboost.sync.EffectCategory.DROP),
                onEffectTrigger = { effectId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerDJEffect(effectId)
                },
                themeColors = themeColors
            )
            
            EffectCategorySection(
                title = "RISERS",
                effects = com.soundboost.sync.DJEffects.getByCategory(com.soundboost.sync.EffectCategory.RISER),
                onEffectTrigger = { effectId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerDJEffect(effectId)
                },
                themeColors = themeColors
            )
            
            EffectCategorySection(
                title = "IMPACTS",
                effects = com.soundboost.sync.DJEffects.getByCategory(com.soundboost.sync.EffectCategory.IMPACT),
                onEffectTrigger = { effectId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerDJEffect(effectId)
                },
                themeColors = themeColors
            )
            
            val vocalsAndMemes = com.soundboost.sync.DJEffects.getByCategory(com.soundboost.sync.EffectCategory.VOCAL) +
                    com.soundboost.sync.DJEffects.getByCategory(com.soundboost.sync.EffectCategory.MEME) +
                    com.soundboost.sync.DJEffects.getByCategory(com.soundboost.sync.EffectCategory.WEAPON)
            
            EffectCategorySection(
                title = "SAMPLES",
                effects = vocalsAndMemes,
                onEffectTrigger = { effectId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.triggerDJEffect(effectId)
                },
                themeColors = themeColors
            )
        }
    }
}

@Composable
private fun EffectCategorySection(
    title: String,
    effects: List<com.soundboost.sync.DJEffect>,
    onEffectTrigger: (String) -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    if (effects.isEmpty()) return
    
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF666666),
            letterSpacing = 1.2.sp
        )
        val rows = effects.chunked(3)
        rows.forEach { rowEffects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowEffects.forEach { effect ->
                    EffectButton(
                        effect = effect,
                        onClick = { onEffectTrigger(effect.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowEffects.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EffectButton(
    effect: com.soundboost.sync.DJEffect,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )
    
    val buttonColor = try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(effect.color))
    } catch (e: Exception) {
        Color(0xFFFF5252)
    }
    
    Surface(
        onClick = {
            isPressed = true
            onClick()
            scope.launch {
                kotlinx.coroutines.delay(120)
                isPressed = false
            }
        },
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed) {
                            listOf(
                                buttonColor.copy(alpha = 0.4f),
                                buttonColor.copy(alpha = 0.25f)
                            )
                        } else {
                            listOf(
                                buttonColor.copy(alpha = 0.2f),
                                buttonColor.copy(alpha = 0.08f)
                            )
                        }
                    )
                )
                .border(
                    width = if (isPressed) 2.dp else 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = if (isPressed) 0.8f else 0.5f),
                            buttonColor.copy(alpha = if (isPressed) 0.5f else 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    buttonColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            Text(
                effect.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isPressed) Color.White else buttonColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}
