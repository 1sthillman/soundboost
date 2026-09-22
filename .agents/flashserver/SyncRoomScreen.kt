package com.soundboost.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PartyMode
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundboost.SyncState
import com.soundboost.SyncViewModel
import com.soundboost.sync.DiscoveredRoom
import com.soundboost.ui.components.CyberCard
import com.soundboost.ui.theme.SoundBoostColors

/**
 * "Parti Modu" giris ekrani: host olarak oda kur ya da mevcut bir odaya katil.
 * HomeScreen'e dokunmaz, ayri bir navigasyon hedefi olarak eklenir.
 *
 * Tek accent kurali: bu ekranin ana rengi CyberBlue (senkron/baglanti temasi),
 * FlashControlScreen'de ise NeonOrange kullanilir (aksiyon/tetikleme temasi).
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

    LaunchedEffect(syncState) {
        if (syncState is SyncState.Hosting || syncState is SyncState.Connected) {
            onRoomReady()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.PartyMode,
            contentDescription = null,
            tint = SoundBoostColors.CyberBlue,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )

        Text(
            text = "Parti Modu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Ayni Wi-Fi'deki tum telefonlari isik senkronuna bagla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (mode) {
            RoomMode.CHOOSE -> ChooseModeSection(
                onHostSelected = { mode = RoomMode.HOST },
                onJoinSelected = {
                    mode = RoomMode.JOIN
                    viewModel.scanForRooms()
                }
            )

            RoomMode.HOST -> HostSection(
                roomName = roomName,
                onRoomNameChange = { roomName = it },
                onStartHosting = {
                    if (roomName.isNotBlank()) viewModel.startHosting(roomName.trim())
                },
                isStarting = syncState is SyncState.Hosting
            )

            RoomMode.JOIN -> JoinSection(
                deviceName = deviceName,
                onDeviceNameChange = { deviceName = it },
                discoveredRooms = discoveredRooms,
                syncState = syncState,
                onJoinRoom = { room ->
                    val name = deviceName.ifBlank { android.os.Build.MODEL }
                    viewModel.joinRoom(room.hostAddress, room.port, name)
                }
            )
        }

        if (syncState is SyncState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (syncState as SyncState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private enum class RoomMode { CHOOSE, HOST, JOIN }

@Composable
private fun ChooseModeSection(
    onHostSelected: () -> Unit,
    onJoinSelected: () -> Unit
) {
    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Oda kur",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sen host olursun, diger telefonlar sana baglanir ve flas komutlarini sen tetiklersin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onHostSelected, modifier = Modifier.fillMaxWidth()) {
                Text("Host Ol")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Odaya katil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Ayni agdaki bir host'un odasina baglan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onJoinSelected, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Odalari Tara")
            }
        }
    }
}

@Composable
private fun HostSection(
    roomName: String,
    onRoomNameChange: (String) -> Unit,
    onStartHosting: () -> Unit,
    isStarting: Boolean
) {
    OutlinedTextField(
        value = roomName,
        onValueChange = onRoomNameChange,
        label = { Text("Oda adi") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onStartHosting,
        enabled = roomName.isNotBlank() && !isStarting,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isStarting) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
        } else {
            Text("Odayi Baslat")
        }
    }

    AnimatedVisibility(visible = isStarting) {
        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.QrCode, contentDescription = null, tint = SoundBoostColors.CyberBlue)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "Diger cihazlar QR kod ile de baglanabilir",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun JoinSection(
    deviceName: String,
    onDeviceNameChange: (String) -> Unit,
    discoveredRooms: List<DiscoveredRoom>,
    syncState: SyncState,
    onJoinRoom: (DiscoveredRoom) -> Unit
) {
    OutlinedTextField(
        value = deviceName,
        onValueChange = onDeviceNameChange,
        label = { Text("Cihaz adin (opsiyonel)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))

    if (discoveredRooms.isEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.padding(6.dp))
            Text("Odalar araniyor...", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(discoveredRooms) { room ->
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(room.displayName, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { onJoinRoom(room) },
                            enabled = syncState !is SyncState.Joining
                        ) {
                            Text("Katil")
                        }
                    }
                }
            }
        }
    }
}
