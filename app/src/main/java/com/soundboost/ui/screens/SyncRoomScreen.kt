package com.soundboost.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.SyncState
import com.soundboost.SyncViewModel
import com.soundboost.sync.DiscoveredRoom
import com.soundboost.ui.theme.NeonOrange

/**
 * 🎨 ULTRA-MODERN PARTY MODE SCREEN v2.0
 * - Zero overflow, perfect spacing
 * - Smooth animations
 * - All 11 languages supported
 * - Premium glassmorphic design
 */
@Composable
fun SyncRoomScreen(
    viewModel: SyncViewModel,
    onRoomReady: () -> Unit
) {
    val syncState by viewModel.syncState.collectAsState()
    val discoveredRooms by viewModel.discoveredRooms.collectAsState()

    var deviceName by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(RoomMode.CHOOSE) }
    
    // Smooth pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Navigate when ready
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Hosting || syncState is SyncState.Connected) {
            onRoomReady()
        }
    }

    // Root container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Animated header icon
            val iconScale by animateFloatAsState(
                targetValue = if (mode == RoomMode.CHOOSE) 1f else 0.85f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "icon"
            )
            
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(iconScale)
                    .drawBehind {
                        // Outer glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NeonOrange.copy(alpha = glowAlpha * 0.25f),
                                    NeonOrange.copy(alpha = 0f)
                                )
                            ),
                            radius = size.minDimension * 1.2f
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = 0.15f),
                                NeonOrange.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(2.dp, NeonOrange.copy(alpha = glowAlpha * 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Title and subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.party_mode_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = stringResource(R.string.party_mode_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Main content area with cross fade animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(200))
                    },
                    label = "mode_transition"
                ) { targetMode ->
                    when (targetMode) {
                        RoomMode.CHOOSE -> ChooseModeSection(
                            onHostSelected = { mode = RoomMode.HOST },
                            onJoinSelected = {
                                mode = RoomMode.JOIN
                                viewModel.scanForRooms()
                            },
                            glowAlpha = glowAlpha
                        )

                        RoomMode.HOST -> HostSection(
                            roomName = roomName,
                            onRoomNameChange = { roomName = it },
                            onStartHosting = {
                                if (roomName.isNotBlank()) viewModel.startHosting(roomName.trim())
                            },
                            isStarting = syncState is SyncState.Hosting,
                            onBack = { mode = RoomMode.CHOOSE }
                        )

                        RoomMode.JOIN -> JoinSection(
                            deviceName = deviceName,
                            onDeviceNameChange = { deviceName = it },
                            discoveredRooms = discoveredRooms,
                            syncState = syncState,
                            onJoinRoom = { room ->
                                val name = deviceName.ifBlank { android.os.Build.MODEL }
                                viewModel.joinRoom(room.hostAddress, room.port, name)
                            },
                            onBack = { mode = RoomMode.CHOOSE }
                        )
                    }
                }
            }

            // Error message (if any)
            AnimatedVisibility(
                visible = syncState is SyncState.Error,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = (syncState as? SyncState.Error)?.message ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private enum class RoomMode { CHOOSE, HOST, JOIN }

@Composable
private fun ChooseModeSection(
    onHostSelected: () -> Unit,
    onJoinSelected: () -> Unit,
    glowAlpha: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // HOST CARD - Premium highlighted
        Surface(
            onClick = onHostSelected,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Background glow effect
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = glowAlpha * 0.1f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.6f,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                },
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = 0.12f),
                                NeonOrange.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NeonOrange.copy(alpha = 0.45f),
                                NeonOrange.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        NeonOrange.copy(alpha = 0.2f),
                                        NeonOrange.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Router,
                            contentDescription = null,
                            tint = NeonOrange,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.party_mode_host_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = NeonOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.party_mode_host_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // JOIN CARD - Clean minimal
        OutlinedCard(
            onClick = onJoinSelected,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
            ),
            border = BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.party_mode_join_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.party_mode_join_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HostSection(
    roomName: String,
    onRoomNameChange: (String) -> Unit,
    onStartHosting: () -> Unit,
    isStarting: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = onRoomNameChange,
                    label = { 
                        Text(
                            stringResource(R.string.party_mode_room_name),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange,
                        focusedLabelColor = NeonOrange
                    )
                )
                
                Button(
                    onClick = onStartHosting,
                    enabled = roomName.isNotBlank() && !isStarting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonOrange,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isStarting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        stringResource(R.string.party_mode_start_room),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        stringResource(R.string.common_back),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Success message
        AnimatedVisibility(
            visible = isStarting,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NeonOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, NeonOrange.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.party_mode_room_active),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.party_mode_boost_auto_started),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonOrange.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinSection(
    deviceName: String,
    onDeviceNameChange: (String) -> Unit,
    discoveredRooms: List<DiscoveredRoom>,
    syncState: SyncState,
    onJoinRoom: (DiscoveredRoom) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = onDeviceNameChange,
                    label = { 
                        Text(
                            stringResource(R.string.party_mode_device_name_optional),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            android.os.Build.MODEL,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonOrange,
                        focusedLabelColor = NeonOrange
                    )
                )

                // Room list or scanning indicator
                if (discoveredRooms.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = NeonOrange
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.party_mode_scanning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.party_mode_rooms_found, discoveredRooms.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = NeonOrange,
                            letterSpacing = 0.2.sp,
                            fontSize = 12.sp
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoveredRooms) { room ->
                                RoomCard(
                                    room = room,
                                    onJoin = { onJoinRoom(room) },
                                    joining = syncState is SyncState.Joining
                                )
                            }
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        stringResource(R.string.common_back),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomCard(
    room: DiscoveredRoom,
    onJoin: () -> Unit,
    joining: Boolean
) {
    Surface(
        onClick = onJoin,
        enabled = !joining,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = room.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${room.hostAddress}:${room.port}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onJoin,
                enabled = !joining,
                colors = ButtonDefaults.buttonColors(containerColor = NeonOrange),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    stringResource(R.string.party_mode_join),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
