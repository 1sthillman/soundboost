package com.soundboost

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundboost.audio.AudioAnalysis
import com.soundboost.audio.BassFlashlightSync
import com.soundboost.data.SyncPreferences
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/**
 * FIXED: Party Mode ViewModel with BassFlashlightSync integration
 * - SHARES the SAME BassFlashlightSync instance from MainViewModel
 * - No resource conflicts - single camera access point
 * - Proper flash event handling for both host and client
 * - Manual flash trigger + bass-sync mode support
 */
class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SyncPreferences(application)
    private val discoveryManager = DiscoveryManager(application)
    
    // CRITICAL: Shared BassFlashlightSync instance (set by MainActivity)
    // This prevents camera resource conflicts
    private var bassFlashSync: BassFlashlightSync? = null

    private var syncServer: SyncServer? = null
    private val syncClient = SyncClient()
    
    // CRITICAL: Callback to trigger boost from MainViewModel when hosting
    private var onRequestBoostEnable: (() -> Unit)? = null
    
    // Auto-reconnect state for clients
    private val _reconnectAttempts = MutableStateFlow(0)
    private val _lastHostAddress = MutableStateFlow<String?>(null)
    private val _lastPort = MutableStateFlow<Int?>(null)
    private val _lastDeviceName = MutableStateFlow<String?>(null)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices.asStateFlow()
    
    private val _disconnectionReason = MutableStateFlow<String?>(null)
    val disconnectionReason: StateFlow<String?> = _disconnectionReason.asStateFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredRoom>> = _discoveredRooms.asStateFlow()

    /** ScreenFlashOverlay listening to this for screen flash */
    private val _pendingFlash = MutableStateFlow<SyncMessage.Flash?>(null)
    val pendingFlash: StateFlow<SyncMessage.Flash?> = _pendingFlash.asStateFlow()

    val hasAcceptedFlashWarning: StateFlow<Boolean> = preferences.hasAcceptedFlashWarning
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Bass-sync flash mode - STABLE StateFlow references
    private val _bassFlashEnabled = MutableStateFlow(false)
    val bassFlashEnabled: StateFlow<Boolean> = _bassFlashEnabled.asStateFlow()
    
    private val _bassFlashIntensity = MutableStateFlow(BassFlashlightSync.FlashIntensity.NORMAL)
    val bassFlashIntensity: StateFlow<BassFlashlightSync.FlashIntensity> = _bassFlashIntensity.asStateFlow()

    val isTorchSupported: Boolean get() = bassFlashSync?.hasFlashSupport() ?: false

    fun acceptFlashWarning() {
        viewModelScope.launch { preferences.setHasAcceptedFlashWarning(true) }
    }

    // Store audio analysis flow reference (still needed for logging)
    private var audioAnalysisFlow: SharedFlow<AudioAnalysis>? = null
    
    /**
     * CRITICAL: Set shared BassFlashlightSync instance from MainViewModel
     * This prevents camera resource conflicts by using the same instance
     * MUST be called during MainActivity initialization
     */
    fun setBassFlashSync(instance: BassFlashlightSync) {
        bassFlashSync = instance
        Log.d(TAG, "🔗 Shared BassFlashlightSync instance connected")
        
        // CRITICAL: Sync state from shared instance to our StateFlows
        viewModelScope.launch {
            instance.isEnabled.collect { enabled ->
                _bassFlashEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            instance.intensity.collect { intensity ->
                _bassFlashIntensity.value = intensity
            }
        }
    }
    
    /**
     * Set audio analysis flow from MainViewModel (for reference tracking)
     */
    fun setAudioAnalysisFlow(flow: SharedFlow<AudioAnalysis>) {
        audioAnalysisFlow = flow
        Log.d(TAG, "🎵 Audio analysis flow connected")
    }
    
    /**
     * CRITICAL: Set callback to request boost enable from MainViewModel
     * This is needed when host creates a party room - we need to trigger
     * the actual boost service through MainViewModel (not just preferences)
     */
    fun setBoostEnableCallback(callback: () -> Unit) {
        onRequestBoostEnable = callback
        Log.d(TAG, "🔗 Boost enable callback connected")
    }
    
    // Use SHARED BassFlashSync instance  
    fun toggleBassFlashSync(enabled: Boolean) {
        val sync = bassFlashSync
        if (sync == null) {
            Log.e(TAG, "❌ Cannot toggle bass flash - shared instance not set!")
            return
        }
        
        val flow = audioAnalysisFlow
        if (flow == null) {
            Log.e(TAG, "❌ Cannot toggle bass flash - audio analysis flow not set!")
            return
        }
        
        Log.d(TAG, "🔄 Bass flash toggle called: requested=$enabled, current=${sync.isEnabled.value}")
        
        // CRITICAL FIX: Bass-sync AÇILIRKEN boost'u otomatik başlat
        if (enabled && !sync.isEnabled.value) {
            Log.d(TAG, "🎚️ REQUESTING BOOST ENABLE via callback...")
            
            // ÖNCE boost'u aç (audio analiz başlasın)
            val callback = onRequestBoostEnable
            if (callback != null) {
                callback.invoke()
                Log.d(TAG, "✅ Boost enable callback invoked successfully")
            } else {
                Log.e(TAG, "❌ CRITICAL: Boost callback is NULL! Cannot start boost.")
            }
            
            // Biraz bekle ki audio flow başlasın
            viewModelScope.launch {
                delay(500)  // Audio analyzer başlaması için kısa bekleme
                sync.start(flow)
                _bassFlashEnabled.value = true  // Update our StateFlow
                Log.d(TAG, "✅ Bass flash sync STARTED (shared instance)")
            }
        } else if (!enabled) {
            sync.stop()
            _bassFlashEnabled.value = false  // Update our StateFlow
            Log.d(TAG, "⏹️ Bass flash sync STOPPED (shared instance)")
        } else {
            Log.d(TAG, "⚠️ Bass flash already in requested state, no action needed")
        }
    }

    fun setBassFlashIntensity(intensity: BassFlashlightSync.FlashIntensity) {
        bassFlashSync?.setIntensity(intensity)
        _bassFlashIntensity.value = intensity
    }

    // ---------- Host flow ----------

    fun startHosting(roomName: String) {
        val server = SyncServer(roomName = roomName)
        syncServer = server
        server.start()
        discoveryManager.advertise(roomName, SyncServer.DEFAULT_PORT)
        SyncForegroundService.start(getApplication(), roomName)

        _syncState.value = SyncState.Hosting(roomName)
        Log.d(TAG, "🎯 Hosting room: $roomName")
        
        // CRITICAL: Auto-start boost service when hosting!
        // Host shouldn't need to go back and manually start the service
        startBoostServiceIfNotRunning()

        viewModelScope.launch {
            server.connectedDevices.collect { devices ->
                _connectedDevices.value = devices
                SyncForegroundService.start(getApplication(), roomName, devices.size)
                Log.d(TAG, "👥 Connected devices: ${devices.size}")
            }
        }
        
        // CRITICAL: HOST moddayken bass analysis broadcast et
        startBassAnalysisBroadcast()
    }
    
    /**
     * HOST oda kurduğunda ses yükseltici servisi otomatik başlat.
     * Böylece host geri dönüp servisi manuel başlatmak zorunda kalmaz.
     * 
     * CRITICAL FIX: BoostForegroundService başlatmak yetmez!
     * Audio analyzer MainViewModel'de olduğu için boost'u oradan aktif etmeliyiz.
     */
    private fun startBoostServiceIfNotRunning() {
        viewModelScope.launch {
            Log.d(TAG, "🚀 AUTO-START: Triggering MainViewModel boost...")
            
            // CRITICAL: Trigger MainViewModel's toggleBoost() via callback
            // This properly starts the audio analyzer and all audio processing
            val callback = onRequestBoostEnable
            if (callback != null) {
                callback.invoke()
                Log.d(TAG, "✅ Boost enable request sent to MainViewModel")
            } else {
                Log.e(TAG, "❌ Cannot start boost - callback not set! Host must manually enable boost.")
            }
        }
    }
    
    /**
     * HOST modundayken audio analysis'i dinle ve bass beat geldiğinde
     * tüm client'lara broadcast et. Bu sayede OTOMATIK senkronize flaş olur.
     * 
     * CRITICAL FIX: Bass-sync broadcast'i bassFlashSync.isEnabled'dan BAĞIMSIZ!
     * Bass-sync mode sadece HOST'un local flaşını kontrol eder.
     * Broadcast her zaman çalışır, client'lar kendi local ayarlarına göre karar verir.
     */
    private fun startBassAnalysisBroadcast() {
        val server = syncServer
        val flow = audioAnalysisFlow
        
        if (server == null || flow == null) {
            Log.w(TAG, "⚠️ Cannot start bass broadcast - missing dependencies")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "🎵🎵🎵 BASS BROADCAST STARTED - Listening to audio flow...")
            Log.d(TAG, "📡 Will broadcast to clients on every bass beat (ZERO delay)")
            
            var lastBroadcastTime = 0L
            var sampleCount = 0
            val minBroadcastInterval = 0L  // ZERO DELAY: No throttling, instant transmission!
            
            flow.collect { analysis ->
                sampleCount++
                
                // DIAGNOSTIC: Log every 30 samples (~1 second)
                if (sampleCount % 30 == 0) {
                    Log.d(TAG, "📊 Audio samples: $sampleCount, clients: ${_connectedDevices.value.size}")
                    Log.d(TAG, "📊 Bass: ${analysis.bass}, Kick: ${analysis.hasKick}, Beat: ${analysis.isBeat}")
                }
                
                // Client bağlı mı kontrol et - bağlı değilse broadcast etmeye gerek yok
                if (_connectedDevices.value.isEmpty()) {
                    if (sampleCount % 30 == 0) {
                        Log.d(TAG, "⚠️ No clients connected - skipping broadcast")
                    }
                    return@collect
                }
                
                // ZERO-DELAY throttling: 0ms = instant transmission!
                val now = System.currentTimeMillis()
                if (now - lastBroadcastTime < minBroadcastInterval) return@collect
                
                // PRIORITY 1: KICK detection (instant, most reliable)
                val hasKick = analysis.hasKick
                
                // PRIORITY 2: BEAT detection (fallback for non-kick beats)
                val hasBeat = analysis.isBeat
                
                // Bass presence check - HYPER-SENSITIVE threshold!
                val hasBass = analysis.bass > 0.08f || analysis.subBass > 0.08f
                
                // Energy check - very low threshold for maximum sensitivity
                val hasEnergy = analysis.energy > 0.10f
                
                // BROADCAST CONDITION: (Kick OR Beat) AND (Bass OR Energy)
                val shouldBroadcast = (hasKick || hasBeat) && (hasBass || hasEnergy)
                
                if (shouldBroadcast) {
                    Log.d(TAG, "⚡⚡⚡ BASS BROADCAST! kick=$hasKick, beat=$hasBeat, bass=${"%.2f".format(analysis.bass)}, energy=${"%.2f".format(analysis.energy)}")
                    server.broadcastBassSync(
                        bass = analysis.bass,
                        subBass = analysis.subBass,
                        energy = analysis.energy,
                        hasKick = hasKick,
                        isBeat = hasBeat
                    )
                    lastBroadcastTime = now
                }
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
        Log.d(TAG, "⏹️ Stopped hosting")
    }

    /** 
     * MANUAL FLASH TRIGGER: Host button pressed
     * Broadcasts to all clients + triggers host's own screen
     */
    fun triggerFlash(pattern: FlashPattern) {
        val server = syncServer ?: run {
            Log.e(TAG, "❌ Cannot trigger flash - no server instance")
            return
        }
        
        Log.d(TAG, "🔥 TRIGGER FLASH - pattern: $pattern")
        
        viewModelScope.launch {
            val startAt = server.triggerFlash(pattern)
            Log.d(TAG, "📡 Broadcast sent, startAt: $startAt")
            
            // Host also sees the flash after same delay
            val delayMs = (startAt - System.currentTimeMillis()).coerceAtLeast(0)
            Log.d(TAG, "⏰ Host will flash after ${delayMs}ms")
            
            delay(delayMs)
            
            val flashMsg = SyncMessage.Flash(
                startAt = startAt,
                durationMs = pattern.durationMs,
                mode = pattern.mode,
                color = pattern.colorHex,
                repeatCount = pattern.repeatCount,
                intervalMs = pattern.intervalMs
            )
            
            Log.d(TAG, "💥 HOST SCREEN FLASH NOW!")
            _pendingFlash.value = flashMsg
        }
    }

    // ---------- Client flow ----------

    fun scanForRooms() {
        Log.d(TAG, "🔍 Scanning for rooms...")
        viewModelScope.launch {
            discoveryManager.discoverRooms().collect { room ->
                val current = _discoveredRooms.value
                if (current.none { it.hostAddress == room.hostAddress && it.port == room.port }) {
                    _discoveredRooms.value = current + room
                    Log.d(TAG, "📍 Found room: ${room.displayName} at ${room.hostAddress}")
                }
            }
        }
    }

    fun joinRoom(hostAddress: String, port: Int = SyncServer.DEFAULT_PORT, deviceName: String) {
        Log.d(TAG, "🔌 Joining room at $hostAddress:$port as $deviceName")
        
        // Store connection info for auto-reconnect
        _lastHostAddress.value = hostAddress
        _lastPort.value = port
        _lastDeviceName.value = deviceName
        
        _syncState.value = SyncState.Joining
        syncClient.connect(hostAddress, port, deviceName)
        
        // CRITICAL: Start foreground service for CLIENT too!
        // This keeps the client alive in background and prevents disconnection
        val context = getApplication<Application>()
        SyncForegroundService.start(context, "Connecting to room...", 0)
        Log.d(TAG, "✅ CLIENT: Foreground service started for stable connection")

        // Monitor connection state
        viewModelScope.launch {
            syncClient.connectionState.collect { state ->
                Log.d(TAG, "🔗 Connection state: $state")
                _syncState.value = when (state) {
                    is SyncConnectionState.Connected -> {
                        Log.d(TAG, "✅ CONNECTED to room: ${state.roomName}")
                        _disconnectionReason.value = null  // Clear previous reason
                        _reconnectAttempts.value = 0  // Reset reconnect counter
                        
                        // CRITICAL: Update foreground service notification with room name
                        val context = getApplication<Application>()
                        SyncForegroundService.start(context, state.roomName, 0)
                        Log.d(TAG, "✅ CLIENT: Updated notification with room name")
                        
                        SyncState.Connected(state.roomName)
                    }
                    is SyncConnectionState.Connecting -> SyncState.Joining
                    is SyncConnectionState.Error -> {
                        Log.e(TAG, "❌ Connection error: ${state.message}")
                        _disconnectionReason.value = state.message
                        
                        // Stop foreground service on error
                        SyncForegroundService.stop(getApplication())
                        
                        SyncState.Error(state.message)
                    }
                    SyncConnectionState.Disconnected -> {
                        // Collect disconnection diagnostic info
                        val reason = syncClient.getDisconnectionDiagnostic()
                        _disconnectionReason.value = reason
                        Log.w(TAG, "⚠️ DISCONNECTED: $reason")
                        
                        // AUTO-RECONNECT: Try to reconnect automatically
                        if (_reconnectAttempts.value < MAX_RECONNECT_ATTEMPTS) {
                            val attempt = _reconnectAttempts.value + 1
                            _reconnectAttempts.value = attempt
                            val delay = RECONNECT_DELAY_MS * attempt  // Exponential backoff
                            
                            Log.w(TAG, "🔄 Auto-reconnect attempt $attempt/$MAX_RECONNECT_ATTEMPTS in ${delay}ms...")
                            
                            viewModelScope.launch {
                                delay(delay)
                                val host = _lastHostAddress.value
                                val port = _lastPort.value ?: SyncServer.DEFAULT_PORT
                                val name = _lastDeviceName.value
                                
                                if (host != null && name != null) {
                                    Log.d(TAG, "🔄 Attempting reconnect to $host:$port")
                                    syncClient.connect(host, port, name)
                                } else {
                                    Log.e(TAG, "❌ Cannot reconnect: missing host/name info")
                                    // Stop foreground service if can't reconnect
                                    SyncForegroundService.stop(getApplication())
                                }
                            }
                            
                            SyncState.Joining  // Show "connecting" state during reconnect
                        } else {
                            Log.e(TAG, "❌ Max reconnect attempts reached, giving up")
                            // Stop foreground service after max attempts
                            SyncForegroundService.stop(getApplication())
                            SyncState.Idle
                        }
                    }
                    SyncConnectionState.Idle -> {
                        // Stop foreground service when idle
                        SyncForegroundService.stop(getApplication())
                        SyncState.Idle
                    }
                }
            }
        }

        // CRITICAL: Listen to flash events from server - ULTRA-STABLE with supervisor!
        viewModelScope.launch(SupervisorJob()) {
            try {
                Log.d(TAG, "👂 CLIENT: Listening to flash events (ULTRA-STABLE mode)...")
                syncClient.flashEvents.collect { flash ->
                    // CRITICAL: Process EACH flash in INDEPENDENT coroutine - no blocking!
                    // Even if one flash fails, others continue working
                    launch(SupervisorJob() + Dispatchers.Main) {
                        try {
                            Log.d(TAG, "⚡⚡⚡ CLIENT: Received flash event: startAt=${flash.startAt}, mode=${flash.mode}, color=${flash.color}")
                            
                            // Calculate delay with ClockSync
                            val delayMs = syncClient.localDelayFor(flash)
                            Log.d(TAG, "⏰ CLIENT: Will flash after ${delayMs}ms (clock-synced)")
                            
                            if (delayMs > 0) {
                                delay(delayMs)
                            }
                            
                            Log.d(TAG, "💥💥💥 CLIENT SCREEN FLASH NOW!")
                            _pendingFlash.value = flash
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing flash event (continuing...)", e)
                            // Continue with next flash event - don't crash!
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Flash event collection failed (will reconnect)", e)
                // Connection lost - will reconnect automatically
            }
        }
    }

    fun leaveRoom() {
        Log.d(TAG, "👋 Leaving room (manual)")
        _disconnectionReason.value = null  // Clear reason on manual leave
        
        // Clear reconnect state (manual leave = no auto-reconnect)
        _reconnectAttempts.value = 0
        _lastHostAddress.value = null
        _lastPort.value = null
        _lastDeviceName.value = null
        
        syncClient.disconnect()
        
        // CRITICAL: Stop foreground service when leaving
        SyncForegroundService.stop(getApplication())
        Log.d(TAG, "✅ CLIENT: Foreground service stopped, reconnect disabled")
        
        _syncState.value = SyncState.Idle
    }
    
    fun clearDisconnectionReason() {
        _disconnectionReason.value = null
    }

    // ---------- Common ----------

    fun onFlashConsumed() {
        _pendingFlash.value = null
        Log.d(TAG, "✨ Flash consumed, pendingFlash cleared")
    }

    /** Physical torch pulse - uses SHARED BassFlashSync manual pulse */
    suspend fun requestTorchPulse(durationMs: Int, repeatCount: Int, intervalMs: Int) {
        val sync = bassFlashSync
        if (sync == null) {
            Log.w(TAG, "⚠️ Cannot pulse - shared instance not set")
            return
        }
        
        if (!sync.hasFlashSupport()) {
            Log.w(TAG, "⚠️ Torch not supported")
            return
        }
        
        Log.d(TAG, "🔦 Manual torch pulse: ${durationMs}ms x$repeatCount (shared instance)")
        sync.manualPulse(durationMs, repeatCount, intervalMs)
    }

    override fun onCleared() {
        super.onCleared()
        syncServer?.stop()
        discoveryManager.stopAdvertising()
        syncClient.disconnect()
        // DON'T release bassFlashSync - it's shared with MainViewModel!
        // MainViewModel will release it when it's cleared
        Log.d(TAG, "🧹 ViewModel cleared (shared bassFlashSync NOT released)")
    }

    companion object {
        private const val TAG = "SyncViewModel"
        private const val MAX_RECONNECT_ATTEMPTS = 5  // Try 5 times before giving up
        private const val RECONNECT_DELAY_MS = 2000L  // Start with 2s, exponential backoff
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Hosting(val roomName: String) : SyncState()
    data object Joining : SyncState()
    data class Connected(val roomName: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
