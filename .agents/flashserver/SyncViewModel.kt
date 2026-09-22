package com.soundboost

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundboost.data.SyncPreferences
import com.soundboost.flash.TorchController
import com.soundboost.service.SyncForegroundService
import com.soundboost.sync.DeviceInfo
import com.soundboost.sync.DiscoveredRoom
import com.soundboost.sync.DiscoveryManager
import com.soundboost.sync.FlashMode
import com.soundboost.sync.FlashPattern
import com.soundboost.sync.SyncClient
import com.soundboost.sync.SyncConnectionState
import com.soundboost.sync.SyncMessage
import com.soundboost.sync.SyncServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Flash-Sync icin bagimsiz ViewModel. MainViewModel'i sismirmemek icin ayri tutuluyor —
 * ses boost state'i (AudioEffectsManager) ile parti modu state'i (SyncServer/SyncClient)
 * birbirinden habersiz calisir. SyncRoomScreen ve FlashControlScreen sadece bunu kullanir.
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SyncPreferences(application)
    private val discoveryManager = DiscoveryManager(application)
    private val torchController = TorchController(application)

    private var syncServer: SyncServer? = null
    private val syncClient = SyncClient()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices.asStateFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    /** ScreenFlashOverlay'in dinlediği tek olay kaynağı — host ve client için ortak. */
    private val _pendingFlash = MutableStateFlow<SyncMessage.Flash?>(null)
    val pendingFlash: StateFlow<SyncMessage.Flash?> = _pendingFlash.asStateFlow()

    val hasAcceptedFlashWarning: StateFlow<Boolean> = preferences.hasAcceptedFlashWarning
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val torchEnabled: StateFlow<Boolean> = preferences.torchEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isTorchSupported: Boolean get() = torchController.isSupported()

    fun acceptFlashWarning() {
        viewModelScope.launch { preferences.setHasAcceptedFlashWarning(true) }
    }

    fun setTorchEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setTorchEnabled(enabled) }
    }

    // ---------- Host akışı ----------

    fun startHosting(roomName: String) {
        val server = SyncServer(roomName = roomName)
        syncServer = server
        server.start()
        discoveryManager.advertise(roomName, SyncServer.DEFAULT_PORT)
        SyncForegroundService.start(getApplication(), roomName)

        _syncState.value = SyncState.Hosting(roomName)

        viewModelScope.launch {
            server.connectedDevices.collect { devices ->
                _connectedDevices.value = devices
                SyncForegroundService.start(getApplication(), roomName, devices.size)
            }
        }
    }

    fun stopHosting() {
        syncServer?.stop()
        syncServer = null
        discoveryManager.stopAdvertising()
        SyncForegroundService.stop(getApplication())
        _connectedDevices.value = emptyList()
        _syncState.value = SyncState.Idle
    }

    /** Host butona bastığında: tüm cihazlara aynı anda flaşı yayınla + kendi ekranını tetikle. */
    fun triggerFlash(pattern: FlashPattern) {
        val server = syncServer ?: return
        viewModelScope.launch {
            server.triggerFlash(pattern)
        }
        // Host kendi ekranını da görmeli — yerel gecikme sıfır kabul edilir.
        _pendingFlash.value = SyncMessage.Flash(
            startAt = System.currentTimeMillis(),
            durationMs = pattern.durationMs,
            mode = pattern.mode,
            color = pattern.colorHex,
            repeatCount = pattern.repeatCount,
            intervalMs = pattern.intervalMs
        )
    }

    // ---------- Client akışı ----------

    fun scanForRooms() {
        viewModelScope.launch {
            discoveryManager.discoverRooms().collect { room ->
                val current = _discoveredRooms.value
                if (current.none { it.hostAddress == room.hostAddress && it.port == room.port }) {
                    _discoveredRooms.value = current + room
                }
            }
        }
    }

    fun joinRoom(hostAddress: String, port: Int = SyncServer.DEFAULT_PORT, deviceName: String) {
        _syncState.value = SyncState.Joining
        syncClient.connect(hostAddress, port, deviceName)

        viewModelScope.launch {
            syncClient.connectionState.collect { state ->
                _syncState.value = when (state) {
                    is SyncConnectionState.Connected -> SyncState.Connected(state.roomName)
                    is SyncConnectionState.Connecting -> SyncState.Joining
                    is SyncConnectionState.Error -> SyncState.Error(state.message)
                    SyncConnectionState.Disconnected, SyncConnectionState.Idle -> SyncState.Idle
                }
            }
        }

        viewModelScope.launch {
            syncClient.flashEvents.collect { flash ->
                // ClockSync ile hesaplanan gecikme kadar bekleyip TAM O ANDA tetikle.
                val delayMs = syncClient.localDelayFor(flash)
                kotlinx.coroutines.delay(delayMs)
                _pendingFlash.value = flash
            }
        }
    }

    fun leaveRoom() {
        syncClient.disconnect()
        _syncState.value = SyncState.Idle
    }

    // ---------- Ortak ----------

    fun onFlashConsumed() {
        _pendingFlash.value = null
    }

    suspend fun requestTorchPulse(durationMs: Int, repeatCount: Int, intervalMs: Int) {
        if (!torchEnabled.value || !torchController.isSupported()) return
        torchController.strobe(durationMs, repeatCount, intervalMs)
    }

    override fun onCleared() {
        super.onCleared()
        syncServer?.stop()
        discoveryManager.stopAdvertising()
        syncClient.disconnect()
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Hosting(val roomName: String) : SyncState()
    data object Joining : SyncState()
    data class Connected(val roomName: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
