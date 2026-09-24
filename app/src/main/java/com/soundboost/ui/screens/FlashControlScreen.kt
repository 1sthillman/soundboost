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
                ClientWaitingScreen(roomName, themeColors)
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DJ DECK
        DJDeckSection(viewModel, musicState, currentPlaylist, djState, themeColors)
        
        // PLAYLIST
        PlaylistCarousel(viewModel, currentPlaylist, musicState, themeColors)
        
        // MIXER
        DJMixerControls(viewModel, djState, musicState, themeColors)
        
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
    val stemSeparationState by viewModel.stemSeparationState.collectAsState()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = themeColors.surface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // AI STEM SEPARATION PROGRESS (if processing)
            if (stemSeparationState is com.soundboost.audio.StemSeparationState.Processing) {
                val state = stemSeparationState as com.soundboost.audio.StemSeparationState.Processing
                StemSeparationProgress(state, themeColors)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Track info
            val currentTrack = currentPlaylist?.currentTrack
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        currentTrack?.fileName ?: "No Track",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        when (musicState) {
                            is MusicShareState.Playing -> "Playing"
                            is MusicShareState.Paused -> "Paused"
                            else -> "Ready"
                        },
                        fontSize = 13.sp,
                        color = themeColors.onSurfaceVariant
                    )
                }
                if (musicState is MusicShareState.Playing || musicState is MusicShareState.Paused) {
                    Text(
                        "${(djState.playbackSpeed * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (djState.playbackSpeed != 1.0f) themeColors.accent1 else themeColors.onSurfaceVariant
                    )
                }
            }
            
            // Vinyl deck
            VinylDeck(viewModel, musicState, djState, themeColors)
            
            // Transport
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
    var lastDragX by remember { mutableFloatStateOf(0f) }
    var lastSpeedUpdate by remember { mutableLongStateOf(0L) }
    val haptic = LocalHapticFeedback.current
    
    LaunchedEffect(musicState, djState.playbackSpeed) {
        if (musicState is MusicShareState.Playing && !isDragging) {
            while (true) {
                kotlinx.coroutines.delay(16)
                rotation += djState.playbackSpeed * 2f
                if (rotation > 360f) rotation -= 360f
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true
                        lastDragX = 0f
                        lastSpeedUpdate = System.currentTimeMillis()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = { 
                        isDragging = false
                        // Reset to normal playback
                        if (djState.playbackSpeed != 1.0f) {
                            viewModel.djScratchSeek(0, 1.0f)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        
                        // Rotate vinyl
                        rotation += dragAmount.x * 0.5f
                        
                        // CRITICAL: THROTTLE speed updates - sadece 100ms'de bir güncelle
                        val now = System.currentTimeMillis()
                        if (now - lastSpeedUpdate < 100) {
                            return@detectDragGestures
                        }
                        lastSpeedUpdate = now
                        
                        // DJ SCRATCH EFFECT - SMOOTH, OPTIMIZED
                        // Daha yumuşak geçişler, daha az update
                        val dragVelocity = dragAmount.x
                        val speed = when {
                            // GERİYE - YAVAŞ scratch
                            dragVelocity < -8f -> 0.4f   // Orta yavaş
                            dragVelocity < -3f -> 0.7f   // Hafif yavaş
                            // İLERİYE - hızlandır
                            dragVelocity > 8f -> 1.4f    // Orta hızlı
                            dragVelocity > 3f -> 1.15f   // Hafif hızlı
                            else -> 1.0f                 // Normal
                        }
                        
                        // Sadece belirgin değişimde gönder
                        if (abs(speed - djState.playbackSpeed) > 0.15f) {
                            viewModel.djScratchSeek(0, speed)
                        }
                        
                        // Haptic feedback - daha az sık
                        if (abs(dragAmount.x - lastDragX) > 30f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastDragX = dragAmount.x
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Vinyl görselleştirme - KESİN SİYAH/DARK, MAVİ YOK!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.85f
            
            // Vinyl - Gradient siyah (MAVİ YOK!)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2a2a2a),  // Koyu gri
                        Color(0xFF0a0a0a)   // Çok koyu siyah
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            
            rotate(rotation, center) {
                // Grooves
                for (i in 1..12) {
                    val r = radius * (0.3f + i * 0.05f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                
                // Label - neutral gri
                drawCircle(
                    color = Color(0xFF3a3a3a),
                    radius = radius * 0.25f,
                    center = center
                )
                
                // Accent line - KIRMIZI (scratch indicator)
                drawLine(
                    color = if (isDragging) Color(0xFFFF5252) else Color(themeColors.accent1.value),
                    start = center,
                    end = Offset(center.x + radius * 0.2f, center.y),
                    strokeWidth = if (isDragging) 4.dp.toPx() else 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        Icon(
            if (musicState is MusicShareState.Playing) Icons.Default.PlayArrow else Icons.Default.Pause,
            null,
            tint = if (isDragging) Color(0xFFFF5252) else themeColors.accent1,
            modifier = Modifier.size(if (isDragging) 36.dp else 32.dp)
        )
    }
    
    // Scratch feedback - sadece gerçekten scratch yapıldığında göster
    if (isDragging && djState.playbackSpeed != 1.0f) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                if (djState.playbackSpeed < 1.0f) "◀ SCRATCH" else "SPEED ▶",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF5252),
                letterSpacing = 1.sp
            )
            Text(
                "${(djState.playbackSpeed * 100).toInt()}%",
                fontSize = 10.sp,
                color = Color(0xFFFF5252)
            )
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
                .weight(2f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColors.accent1
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = musicState !is MusicShareState.Idle && musicState !is MusicShareState.Downloading
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
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = themeColors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "PLAYLIST",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = { musicFileLauncher.launch("audio/*") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = themeColors.accent1)
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
                Text(
                    "Tap + to add tracks",
                    fontSize = 13.sp,
                    color = themeColors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
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
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = themeColors.surface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "DJ MIXER",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp
            )
            
            // VOKAL/MÜZİK CROSSFADER - KRİTİK! GERÇEKTEN ÇALIŞMALI!
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "MUSIC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (djState.vocalBalance < 0.3f) Color(0xFF00E676) else themeColors.onSurfaceVariant
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // DOUBLE TAP = RESET TO 0.5 (BALANCED)
                                    viewModel.updateDJVolume(vocalBalance = 0.5f)
                                }
                            )
                        }
                    ) {
                        Text(
                            "VOCAL/MUSIC CROSSFADER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            when {
                                djState.vocalBalance < 0.3f -> "MUSIC ONLY"
                                djState.vocalBalance > 0.7f -> "VOCAL ONLY"
                                abs(djState.vocalBalance - 0.5f) < 0.05f -> "✓ BALANCED"
                                else -> "BALANCED"
                            },
                            fontSize = 8.sp,
                            color = if (abs(djState.vocalBalance - 0.5f) < 0.05f) Color(0xFF00E676) else themeColors.accent2,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "VOCAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (djState.vocalBalance > 0.7f) Color(0xFF00E676) else themeColors.onSurfaceVariant
                    )
                }
                
                Text(
                    "Double tap label to reset",
                    fontSize = 8.sp,
                    color = themeColors.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                // CRITICAL: vocalBalance DOĞRU GÖNDERİLMELİ!
                Slider(
                    value = djState.vocalBalance,
                    onValueChange = { newBalance ->
                        // Haptic feedback her %10'da bir
                        if (abs(newBalance - djState.vocalBalance) > 0.1f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        // CRITICAL: vocalBalance parametresi GÖNDERİLMELİ!
                        viewModel.updateDJVolume(vocalBalance = newBalance)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = themeColors.accent2,
                        activeTrackColor = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00E676),  // Music (yeşil)
                                themeColors.accent2,
                                Color(0xFFFF5252)   // Vocal (kırmızı)
                            )
                        ).let { themeColors.accent2 },
                        inactiveTrackColor = themeColors.onSurface.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(48.dp)
                )
                
                // Progress göstergesi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .fillMaxWidth()
                            .background(themeColors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(djState.vocalBalance)
                                .background(
                                    when {
                                        djState.vocalBalance < 0.3f -> Color(0xFF00E676)
                                        djState.vocalBalance > 0.7f -> Color(0xFFFF5252)
                                        else -> themeColors.accent2
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            HorizontalDivider(color = themeColors.onSurface.copy(alpha = 0.1f))
            
            // EQ KNOBS
            Text("EQUALIZER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            
            HorizontalDivider(color = themeColors.onSurface.copy(alpha = 0.1f))
            
            // MASTER FADER
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("MASTER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${(djState.masterVolume * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColors.accent1)
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
                        activeTrackColor = themeColors.accent1
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
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
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
                    "Waiting for DJ...",
                    fontSize = 15.sp,
                    color = themeColors.onSurfaceVariant
                )
            }
        }
    }
}


/**
 * AI STEM SEPARATION PROGRESS INDICATOR
 * Shows real-time progress of vocal/music separation
 * MÜKEMMEL: GPU-accelerated, no freezing, optimized!
 */
@Composable
private fun StemSeparationProgress(
    state: com.soundboost.audio.StemSeparationState.Processing,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = themeColors.accent1.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress with percentage
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxSize(),
                    color = themeColors.accent1,
                    strokeWidth = 4.dp,
                    trackColor = themeColors.onSurface.copy(alpha = 0.1f)
                )
                Text(
                    "${(state.progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.accent1
                )
            }
            
            // Status text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = themeColors.accent1,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "AI Stem Separation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = themeColors.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    state.status,
                    fontSize = 12.sp,
                    color = themeColors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
