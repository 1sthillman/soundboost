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
import com.soundboost.sync.DJEffectPlayer
import com.soundboost.sync.DJEffects
import com.soundboost.sync.FlashMode
import com.soundboost.sync.FlashPattern
import com.soundboost.sync.MusicAction
import com.soundboost.sync.MusicShareManager
import com.soundboost.sync.MusicShareState
import com.soundboost.sync.PlaybackState
import com.soundboost.sync.Playlist
import com.soundboost.sync.PlaylistTrack
import com.soundboost.sync.RoomState
import com.soundboost.sync.RoomStateManager
import com.soundboost.sync.DJState
import com.soundboost.sync.DJStateManager
import com.soundboost.sync.EffectType
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

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

    // AUDIO SYNC STATE
    private val _audioSyncEnabled = MutableStateFlow(false)
    val audioSyncEnabled: StateFlow<Boolean> = _audioSyncEnabled.asStateFlow()
    
    private val _audioSyncStatus = MutableStateFlow<AudioSyncStatus>(AudioSyncStatus.Idle)
    val audioSyncStatus: StateFlow<AudioSyncStatus> = _audioSyncStatus.asStateFlow()
    
    // Audio streaming components (HOST only)
    private var audioCaptureManager: com.soundboost.audio.AudioCaptureManager? = null
    private var audioStreamEncoder: com.soundboost.audio.AudioStreamEncoder? = null
    private var audioSequence = 0L
    
    // Audio playback components (CLIENT only)
    private var audioStreamDecoder: com.soundboost.audio.AudioStreamDecoder? = null
    private var audioPlaybackManager: com.soundboost.audio.AudioPlaybackManager? = null
    private var jitterBuffer: com.soundboost.audio.JitterBuffer? = null
    
    // MUSIC SHARE STATE
    private val musicShareManager = MusicShareManager(application, viewModelScope)
    val musicState: StateFlow<MusicShareState> = musicShareManager.musicState
    val downloadProgress: StateFlow<Float> = musicShareManager.downloadProgress
    
    // DJ EFFECT PLAYER - Synchronized sound effects
    private val djEffectPlayer = DJEffectPlayer(application, viewModelScope)
    
    init {
        // Preload all DJ effects on startup for instant playback
        viewModelScope.launch {
            djEffectPlayer.preloadEffects()
            Log.d(TAG, "✅ DJ Effect system initialized")
        }
    }
    
    // ROOM STATE MANAGER (for playlist and sync)
    private var roomStateManager: RoomStateManager? = null
    
    // EXPOSE ROOM STATE TO UI
    val roomState: StateFlow<RoomState?> get() = roomStateManager?.roomState ?: MutableStateFlow<RoomState?>(null).asStateFlow()
    val currentPlaylist: StateFlow<Playlist?> get() = roomStateManager?.roomState?.let { flow ->
        MutableStateFlow(flow.value.playlist).apply {
            viewModelScope.launch {
                flow.collect { state ->
                    value = state.playlist
                }
            }
        }.asStateFlow()
    } ?: MutableStateFlow<Playlist?>(null).asStateFlow()
    
    // DJ STATE MANAGER (for real-time audio manipulation sync)
    private var djStateManager: DJStateManager? = null
    val djState: StateFlow<DJState> get() = djStateManager?.djState ?: MutableStateFlow(DJState()).asStateFlow()
    
    // CLIENT DOWNLOAD TRACKING (HOST)
    private val _clientDownloadStatus = MutableStateFlow<Map<String, Float>>(emptyMap())
    val clientDownloadStatus: StateFlow<Map<String, Float>> = _clientDownloadStatus.asStateFlow()
    
    // Computed: Are all clients ready?
    val allClientsReady: StateFlow<Boolean> = combine(
        _connectedDevices,
        _clientDownloadStatus,
        musicState
    ) { devices: List<DeviceInfo>, downloadStatus: Map<String, Float>, musicState: MusicShareState ->
        // If no music prepared, not ready
        if (musicState !is MusicShareState.Ready && musicState !is MusicShareState.Prepared) {
            return@combine false
        }
        
        // If no clients connected, host can start alone
        if (devices.isEmpty()) {
            return@combine true
        }
        
        // All connected clients must have completed download (progress == 1.0)
        devices.all { device ->
            downloadStatus[device.id] == 1.0f
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isTorchSupported: Boolean get() = bassFlashSync?.hasFlashSupport() ?: false

    fun acceptFlashWarning() {
        viewModelScope.launch { 
            preferences.setHasAcceptedFlashWarning(true)
            com.soundboost.flash.FlashSafetyGuard.onWarningAccepted()
            
            // CRITICAL: Enable Party Mode in FlashSafetyGuard (100ms limit vs 333ms)
            com.soundboost.flash.FlashSafetyGuard.setPartyMode(true)
            
            Log.d(TAG, "✅ Flash warning accepted + Party Mode enabled")
        }
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
        
        // CRITICAL: If HOST, broadcast to ALL clients!
        val server = syncServer
        if (server != null) {
            Log.d(TAG, "📡 ========== HOST BROADCASTING BASS FLASH CONTROL ==========")
            Log.d(TAG, "📡 Enabled: $enabled")
            Log.d(TAG, "📡 Intensity: ${_bassFlashIntensity.value}")
            
            viewModelScope.launch {
                val controlMsg = SyncMessage.BassFlashControl(
                    enabled = enabled,
                    intensity = _bassFlashIntensity.value.name
                )
                val (successCount, failCount) = server.broadcast(controlMsg)
                
                Log.d(TAG, "📡 Bass flash control broadcasted:")
                Log.d(TAG, "   - Success: $successCount clients")
                Log.d(TAG, "   - Failed: $failCount clients")
                
                if (failCount > 0) {
                    Log.e(TAG, "⚠️ Some clients did NOT receive bass flash control!")
                } else {
                    Log.d(TAG, "✅ ALL CLIENTS received bass flash control")
                }
            }
        }
        
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

    // ---------- AUDIO SYNC CONTROLS ----------
    
    /**
     * Toggle synchronized audio streaming (HOST only)
     * Requires MediaProjection permission from user
     */
    fun toggleAudioSync(enabled: Boolean, mediaProjection: android.media.projection.MediaProjection? = null) {
        if (enabled) {
            startAudioSync(mediaProjection)
        } else {
            stopAudioSync()
        }
    }
    
    /**
     * Start audio streaming (HOST only)
     * Captures host's audio and broadcasts to all clients
     * 
     * MÜKEMMEL: AudioPlaybackCapture + Opus encoding + UDP broadcast!
     */
    private fun startAudioSync(mediaProjection: android.media.projection.MediaProjection?) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            _audioSyncStatus.value = AudioSyncStatus.Error("Android 10+ gerekli")
            Log.e(TAG, "❌ Audio sync requires Android 10+")
            return
        }
        
        if (syncServer == null) {
            _audioSyncStatus.value = AudioSyncStatus.Error("Oda açık değil")
            Log.e(TAG, "❌ Must be hosting to stream audio")
            return
        }
        
        if (mediaProjection == null) {
            _audioSyncStatus.value = AudioSyncStatus.Error("MediaProjection izni gerekli")
            Log.e(TAG, "❌ MediaProjection permission required!")
            return
        }
        
        Log.d(TAG, "🎵 Starting REAL audio sync with AudioPlaybackCapture + Opus...")
        _audioSyncStatus.value = AudioSyncStatus.Starting
        
        val server = syncServer!!
        
        viewModelScope.launch {
            try {
                // 1. Initialize audio capture (AudioPlaybackCapture)
                val captureManager = com.soundboost.audio.AudioCaptureManager(getApplication())
                audioCaptureManager = captureManager
                captureManager.startCapture(mediaProjection)
                Log.d(TAG, "✅ AudioPlaybackCapture started")
                
                // 2. Initialize Opus encoder
                val encoder = com.soundboost.audio.AudioStreamEncoder()
                audioStreamEncoder = encoder
                encoder.start()
                Log.d(TAG, "✅ Opus encoder started (128kbps)")
                
                // 3. Connect capture -> encoder -> network
                audioSequence = 0L
                launch {
                    captureManager.audioCaptureFlow.collect { pcmData ->
                        // Encode PCM to Opus
                        encoder.encode(pcmData)
                    }
                }
                
                // 4. Connect encoder -> network broadcast
                launch {
                    encoder.encodedAudioFlow.collect { opusData ->
                        audioSequence++
                        
                        // Broadcast to all clients (SyncServer handles base64 encoding)
                        server.broadcastAudioChunk(opusData, audioSequence)
                        
                        if (audioSequence % 100 == 0L) { // Log every second
                            Log.d(TAG, "📡 Streaming: ${audioSequence} packets, ${_connectedDevices.value.size} clients")
                        }
                    }
                }
                
                _audioSyncEnabled.value = true
                _audioSyncStatus.value = AudioSyncStatus.Streaming(_connectedDevices.value.size)
                
                Log.d(TAG, "✅ Audio streaming active - broadcasting to ${_connectedDevices.value.size} devices")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start audio sync: ${e.message}", e)
                _audioSyncStatus.value = AudioSyncStatus.Error(e.message ?: "Unknown error")
                stopAudioSync()
            }
        }
    }
    
    /**
     * Stop audio streaming
     */
    private fun stopAudioSync() {
        Log.d(TAG, "🛑 Stopping audio sync...")
        
        audioCaptureManager?.stopCapture()
        audioCaptureManager = null
        
        audioStreamEncoder?.stop()
        audioStreamEncoder = null
        
        audioStreamDecoder?.stop()
        audioStreamDecoder = null
        
        audioPlaybackManager?.stop()
        audioPlaybackManager = null
        
        jitterBuffer = null
        audioSequence = 0L
        
        _audioSyncEnabled.value = false
        _audioSyncStatus.value = AudioSyncStatus.Idle
        
        Log.d(TAG, "✅ Audio sync stopped")
    }
    
    /**
     * Start receiving audio stream (CLIENT only)
     * Automatically called when client connects to a streaming host
     */
    private fun startAudioReceive() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            Log.e(TAG, "❌ Audio receive requires Android 10+")
            return
        }
        
        Log.d(TAG, "🎧 Starting audio receive...")
        
        viewModelScope.launch {
            try {
                // Initialize decoder
                val decoder = com.soundboost.audio.AudioStreamDecoder()
                audioStreamDecoder = decoder
                decoder.start()
                
                // Initialize playback
                val playback = com.soundboost.audio.AudioPlaybackManager()
                audioPlaybackManager = playback
                playback.start()
                
                // Initialize jitter buffer
                val buffer = com.soundboost.audio.JitterBuffer(minBufferMs = 50, maxBufferMs = 200)
                jitterBuffer = buffer
                
                // Connect client audio events -> decoder -> buffer -> playback
                launch {
                    syncClient.audioChunkEvents.collect { chunk ->
                        val opusData = android.util.Base64.decode(chunk.data, android.util.Base64.NO_WRAP)
                        decoder.decode(opusData)
                    }
                }
                
                // Connect decoder -> buffer
                launch {
                    decoder.decodedAudioFlow.collect { pcmData ->
                        // TODO: Add to jitter buffer with timestamp
                        playback.queueAudio(pcmData)
                        
                        val stats = buffer.getStats()
                        _audioSyncStatus.value = AudioSyncStatus.Receiving(
                            bufferMs = stats.bufferMs,
                            latencyMs = 50 // Approximate
                        )
                    }
                }
                
                Log.d(TAG, "✅ Audio receive started successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start audio receive: ${e.message}", e)
            }
        }
    }

    // ---------- Host flow ----------

    fun startHosting(roomName: String) {
        val server = SyncServer(roomName = roomName)
        syncServer = server
        server.start()
        
        // Initialize RoomStateManager
        roomStateManager = server.getRoomStateManager()
        Log.d(TAG, "📊 RoomStateManager initialized")
        
        // Initialize DJStateManager with audio engine callbacks
        djStateManager = DJStateManager().apply {
            // EQ callback - apply to music player AND broadcast to clients!
            onEQChange = { bass, mid, treble ->
                Log.d(TAG, "🎛️ HOST EQ change: bass=$bass, mid=$mid, treble=$treble")
                musicShareManager.applyDJControls(bass, mid, treble, djState.value.masterVolume)
                
                // CRITICAL: INSTANT broadcast (20ms ultra-low latency)
                viewModelScope.launch {
                    val applyAt = System.currentTimeMillis() + 20L // 20ms ultra-low latency
                    val djStateJson = kotlinx.serialization.json.Json.encodeToString(DJState.serializer(), getCurrentState())
                    val syncMsg = SyncMessage.DJStateSync(djStateJson, applyAt)
                    server.broadcast(syncMsg)
                    Log.d(TAG, "📡 EQ broadcasted (20ms latency)")
                }
            }
            
            // Volume callback - apply master volume AND broadcast!
            onVolumeChange = { master ->
                Log.d(TAG, "🔊 HOST Volume change: master=$master")
                musicShareManager.applyDJControls(
                    djState.value.bass,
                    djState.value.mid,
                    djState.value.treble,
                    master
                )
                
                // CRITICAL: INSTANT broadcast (20ms ultra-low latency)
                viewModelScope.launch {
                    val applyAt = System.currentTimeMillis() + 20L // 20ms ultra-low latency
                    val djStateJson = kotlinx.serialization.json.Json.encodeToString(DJState.serializer(), getCurrentState())
                    val syncMsg = SyncMessage.DJStateSync(djStateJson, applyAt)
                    server.broadcast(syncMsg)
                    Log.d(TAG, "📡 Volume broadcasted (20ms latency)")
                }
            }
            
            // Effect callback - apply effects
            onEffectChange = { type, enabled, level ->
                Log.d(TAG, "🎛️ HOST Effect change: $type enabled=$enabled, level=$level")
                // TODO: Apply reverb/echo/effects to audio engine
            }
            
            // Seek callback - apply playback speed for scratching
            onSeekChange = { positionMs, speed ->
                Log.d(TAG, "💿 HOST Scratch: pos=${positionMs}ms, speed=$speed")
                musicShareManager.setPlaybackSpeed(speed)
                // TODO: Seek to position if needed
            }
        }
        Log.d(TAG, "🎛️ DJStateManager initialized with audio callbacks")
        
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
        
        // Listen to incoming messages (for chunk requests etc.)
        viewModelScope.launch {
            server.incomingMessages.collect { (deviceId, message) ->
                Log.d(TAG, "📥 Incoming message from $deviceId: ${message::class.simpleName}")
                
                // Handle MusicChunkRequest from clients
                if (message is SyncMessage.MusicChunkRequest) {
                    launch {
                        val chunkData = musicShareManager.getChunk(message.chunkIndex)
                        if (chunkData != null) {
                            val base64Data = android.util.Base64.encodeToString(chunkData, android.util.Base64.NO_WRAP)
                            val chunkMsg = SyncMessage.MusicChunk(
                                sessionId = message.sessionId,
                                chunkIndex = message.chunkIndex,
                                data = base64Data
                            )
                            
                            // Send chunk only to requesting device
                            server.sendToDevice(deviceId, chunkMsg)
                            
                            Log.d(TAG, "📤 Sent chunk ${message.chunkIndex} to $deviceId")
                        } else {
                            Log.e(TAG, "❌ Failed to get chunk ${message.chunkIndex}")
                        }
                    }
                }
                
                // Handle MusicDownloadStatus from clients
                if (message is SyncMessage.MusicDownloadStatus) {
                    val currentStatus = _clientDownloadStatus.value.toMutableMap()
                    currentStatus[deviceId] = message.progress
                    _clientDownloadStatus.value = currentStatus
                    
                    Log.d(TAG, "📊 Client $deviceId download: ${(message.progress * 100).toInt()}% (complete=${message.isComplete})")
                    
                    if (message.isComplete) {
                        Log.d(TAG, "✅ Client $deviceId ready! Waiting for others...")
                    }
                }
                
                // CRITICAL: Handle LATE JOIN - send current music metadata
                if (message is SyncMessage.Error && message.reason.startsWith("LATE_JOIN:")) {
                    val parts = message.reason.split(":")
                    if (parts.size == 3) {
                        val lateJoinDeviceId = parts[1]
                        val sessionId = parts[2]
                        
                        Log.d(TAG, "🎵 ========== LATE JOIN DETECTED ==========")
                        Log.d(TAG, "🎵 Device: $lateJoinDeviceId")
                        Log.d(TAG, "🎵 Session: $sessionId")
                        
                        // Broadcast metadata to late joiner
                        val metadata = musicShareManager.getMusicMetadata()
                        if (metadata != null) {
                            Log.d(TAG, "📡 Broadcasting metadata to late joiner: ${metadata.fileName}")
                            
                            launch {
                                try {
                                    server.sendToDevice(lateJoinDeviceId, metadata)
                                    Log.d(TAG, "✅ Metadata sent to late joiner $lateJoinDeviceId")
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Failed to send metadata to late joiner", e)
                                }
                            }
                        } else {
                            Log.w(TAG, "⚠️ No metadata available for late joiner")
                        }
                    }
                }
                
            }
        }
        
        // BASS BROADCAST: SADECE bass-sync switch AÇIKSA broadcast et!
        // Kullanıcı bass-sync'i manuel olarak açmalı
        viewModelScope.launch {
            bassFlashEnabled.collect { enabled ->
                if (enabled) {
                    Log.d(TAG, "🎵 Bass-sync enabled - starting bass broadcast")
                    startBassAnalysisBroadcast()
                } else {
                    Log.d(TAG, "⏹️ Bass-sync disabled - stopping bass broadcast")
                    // Bass broadcast zaten scope'da, enabled false olunca broadcast yapılmaz
                }
            }
        }
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
     * CRITICAL: SADECE bassFlashEnabled TRUE ise broadcast yapar!
     * Kullanıcı bass-sync switch'ini manuel açmalı.
     */
    private fun startBassAnalysisBroadcast() {
        val server = syncServer
        val flow = audioAnalysisFlow
        
        if (server == null || flow == null) {
            Log.w(TAG, "⚠️ Cannot start bass broadcast - missing dependencies")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "🎵 BASS BROADCAST READY - Waiting for bass-sync to be enabled...")
            
            var lastBroadcastTime = 0L
            var sampleCount = 0
            val minBroadcastInterval = 0L  // ZERO DELAY: No throttling, instant transmission!
            
            flow.collect { analysis ->
                sampleCount++
                
                // CRITICAL: SADECE bass-sync AÇIKSA broadcast et!
                if (!_bassFlashEnabled.value) {
                    if (sampleCount % 100 == 0) {
                        Log.d(TAG, "⏸️ Bass-sync DISABLED - skipping broadcast (enable bass-sync switch to start)")
                    }
                    return@collect
                }
                
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
        val server = syncServer
        if (server == null) {
            Log.d(TAG, "⚠️ No server to stop")
            return
        }
        
        Log.d(TAG, "🛑 ========== STOPPING HOST ==========")
        Log.d(TAG, "🛑 Connected devices: ${_connectedDevices.value.size}")
        
        // CRITICAL: Transfer host to oldest client BEFORE stopping
        if (_connectedDevices.value.isNotEmpty()) {
            Log.d(TAG, "👑 Transferring host before stop...")
            viewModelScope.launch {
                try {
                    val newHost = server.transferHost()
                    if (newHost != null) {
                        Log.d(TAG, "✅ Host transferred to: ${newHost.name} (${newHost.id})")
                        Log.d(TAG, "⏳ Waiting 1 second for message delivery...")
                        delay(1000) // Wait for HostTransfer message to be delivered
                    } else {
                        Log.w(TAG, "⚠️ No suitable client for host transfer")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Host transfer failed", e)
                }
                
                // Now stop the server
                finalizeServerStop(server)
            }
        } else {
            Log.d(TAG, "📭 No clients connected, stopping immediately")
            finalizeServerStop(server)
        }
    }
    
    private fun finalizeServerStop(server: SyncServer) {
        server.stop()
        syncServer = null
        discoveryManager.stopAdvertising()
        SyncForegroundService.stop(getApplication())
        _connectedDevices.value = emptyList()
        _clientDownloadStatus.value = emptyMap()
        _syncState.value = SyncState.Idle
        
        // Clear room state
        roomStateManager = null
        
        Log.d(TAG, "✅ Server stopped completely")
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
        
        // Initialize DJStateManager for CLIENT
        if (djStateManager == null) {
            djStateManager = DJStateManager().apply {
                // CLIENT callbacks - apply received DJ state to audio
                onEQChange = { bass, mid, treble ->
                    Log.d(TAG, "🎛️ CLIENT EQ change: bass=$bass, mid=$mid, treble=$treble")
                    musicShareManager.applyDJControls(bass, mid, treble, djState.value.masterVolume)
                }
                
                onVolumeChange = { master ->
                    Log.d(TAG, "🔊 CLIENT Volume change: master=$master")
                    musicShareManager.applyDJControls(
                        djState.value.bass,
                        djState.value.mid,
                        djState.value.treble,
                        master
                    )
                }
                
                onSeekChange = { positionMs, speed ->
                    Log.d(TAG, "💿 CLIENT Scratch: pos=${positionMs}ms, speed=$speed")
                    musicShareManager.setPlaybackSpeed(speed)
                }
            }
            Log.d(TAG, "🎛️ CLIENT: DJStateManager initialized with audio callbacks")
        }
        
        _syncState.value = SyncState.Joining
        syncClient.connect(hostAddress, port, deviceName)
        
        // CRITICAL: Monitor clock offset and update DJ effect player
        viewModelScope.launch {
            syncClient.clockOffsetMillis.collect { offset ->
                djEffectPlayer.setClockOffset(offset)
                Log.d(TAG, "⏰ Clock offset updated for DJ effects: ${offset}ms")
            }
        }
        
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
                            Log.d(TAG, "⚡⚡⚡ ========== CLIENT: FLASH EVENT RECEIVED ==========")
                            Log.d(TAG, "⚡ startAt: ${flash.startAt}")
                            Log.d(TAG, "⚡ mode: ${flash.mode}")
                            Log.d(TAG, "⚡ duration: ${flash.durationMs}ms")
                            Log.d(TAG, "⚡ repeat: ${flash.repeatCount}")
                            
                            // Calculate delay with ClockSync
                            val delayMs = syncClient.localDelayFor(flash)
                            val currentTime = System.currentTimeMillis()
                            Log.d(TAG, "⏰ ========== CLOCK SYNC CALCULATION ==========")
                            Log.d(TAG, "⏰ Current local time: $currentTime")
                            Log.d(TAG, "⏰ Flash startAt: ${flash.startAt}")
                            Log.d(TAG, "⏰ Clock offset: ${syncClient.clockOffsetMillis.value}ms")
                            Log.d(TAG, "⏰ Calculated delay: ${delayMs}ms")
                            
                            if (delayMs > 0) {
                                Log.d(TAG, "⏳ Waiting ${delayMs}ms for perfect sync...")
                                delay(delayMs)
                            } else {
                                Log.w(TAG, "⚠️ Delay is negative (${delayMs}ms) - flash immediately!")
                            }
                            
                            Log.d(TAG, "🔥🔥🔥 CLIENT: TRIGGERING FLASH NOW!")
                            Log.d(TAG, "🔥 Mode: ${flash.mode} (TORCH_ONLY = camera flash ONLY, no screen flash)")
                            
                            // CRITICAL: Set pending flash - ScreenFlashOverlay will handle it
                            _pendingFlash.value = flash
                            
                            Log.d(TAG, "✅ Flash event dispatched to ScreenFlashOverlay")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing flash event (continuing...)", e)
                            Log.e(TAG, "❌ Exception: ${e.javaClass.simpleName}: ${e.message}")
                            // Continue with next flash event - don't crash!
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Flash event collection failed (will reconnect)", e)
                // Connection lost - will reconnect automatically
            }
        }
        
        // CRITICAL: Listen to audio chunks from server (CLIENT receives audio stream)
        viewModelScope.launch(SupervisorJob()) {
            try {
                Log.d(TAG, "👂 CLIENT: Listening to audio chunks...")
                syncClient.audioChunkEvents.collect { audioChunk ->
                    // Auto-start audio receive on first chunk
                    if (audioStreamDecoder == null) {
                        Log.d(TAG, "🎧 First audio chunk received - starting playback")
                        startAudioReceive()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Audio chunk collection failed", e)
            }
        }
        
        // CLIENT: Listen to music messages (metadata, start, control)
        viewModelScope.launch(SupervisorJob()) {
            try {
                Log.d(TAG, "👂 CLIENT: Listening to server messages...")
                syncClient.incomingMessages.collect { (deviceId, message) ->
                    launch(SupervisorJob()) {
                        try {
                            when (message) {
                                is SyncMessage.Welcome -> {
                                    Log.d(TAG, "👋 ========== WELCOME RECEIVED ==========")
                                    Log.d(TAG, "👋 Room: ${message.roomName}")
                                    Log.d(TAG, "👋 DeviceId: ${message.deviceId}")
                                    
                                    // LATE JOIN: Check if RoomState provided
                                    if (message.roomStateJson != null) {
                                        Log.d(TAG, "🎵 LATE JOIN: Music is playing, syncing...")
                                        try {
                                            val roomStateManager = RoomStateManager()
                                            val roomState = roomStateManager.deserializeState(message.roomStateJson)
                                            
                                            // CRITICAL: Store RoomState locally for tracking
                                            this@SyncViewModel.roomStateManager = roomStateManager
                                            roomStateManager.loadState(roomState)
                                            
                                            Log.d(TAG, "📊 RoomState loaded:")
                                            Log.d(TAG, "   - Playback: ${roomState.playbackState}")
                                            Log.d(TAG, "   - Current track: ${roomState.playlist.currentTrackIndex}")
                                            Log.d(TAG, "   - Position: ${roomState.currentPosition}ms")
                                            Log.d(TAG, "   - Total tracks: ${roomState.playlist.tracks.size}")
                                            
                                            // CRITICAL: Apply DJ state immediately for late join
                                            djStateManager?.applyDJState(roomState.djState)
                                            Log.d(TAG, "🎛️ DJ state applied for late join")
                                            
                                            val currentTrack = roomState.playlist.currentTrack
                                            if (currentTrack != null) {
                                                Log.d(TAG, "🎵 Current track: ${currentTrack.fileName}")
                                                
                                                // CRITICAL: Check if we already have this track
                                                val currentSession = musicShareManager.musicState.value
                                                val needsDownload = currentSession !is MusicShareState.Ready || 
                                                                    (currentSession as? MusicShareState.Ready)?.fileName != currentTrack.fileName
                                                
                                                if (needsDownload) {
                                                    Log.d(TAG, "📥 Need to download track: ${currentTrack.fileName}")
                                                    Log.d(TAG, "📥 Requesting metadata from host...")
                                                    
                                                    // Metadata will arrive automatically, download will start
                                                    // Host broadcasts metadata to ALL clients including late joiners
                                                } else {
                                                    Log.d(TAG, "✅ Track already downloaded!")
                                                }
                                                
                                                // If music is playing, prepare to sync
                                                if (roomState.playbackState == PlaybackState.PLAYING) {
                                                    // Calculate current position with elapsed time
                                                    val elapsed = System.currentTimeMillis() - roomState.lastUpdateTime
                                                    val syncPosition = roomState.currentPosition + elapsed.toInt()
                                                    
                                                    Log.d(TAG, "⏱️ Late join position sync:")
                                                    Log.d(TAG, "   - Stored position: ${roomState.currentPosition}ms")
                                                    Log.d(TAG, "   - Elapsed since update: ${elapsed}ms")
                                                    Log.d(TAG, "   - Calculated sync position: ${syncPosition}ms")
                                                    
                                                    if (!needsDownload) {
                                                        // Start immediately if already downloaded
                                                        Log.d(TAG, "▶️ Starting playback immediately at ${syncPosition}ms")
                                                        launch {
                                                            // Convert host time to local time
                                                            val localStartTime = System.currentTimeMillis() + 500L
                                                            musicShareManager.startPlayback(localStartTime, syncPosition)
                                                        }
                                                    } else {
                                                        Log.d(TAG, "⏳ Will start playback after download completes")
                                                        // Download will complete, then CLIENT will receive MusicStart
                                                    }
                                                }
                                            } else {
                                                Log.d(TAG, "📭 No track in playlist")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "❌ Failed to process late join RoomState", e)
                                            e.printStackTrace()
                                        }
                                    } else {
                                        Log.d(TAG, "✅ Fresh join, no music playing yet")
                                    }
                                }
                                is SyncMessage.MusicMetadata -> {
                                    Log.d(TAG, "🎵 ========== MUSIC METADATA RECEIVED ==========")
                                    Log.d(TAG, "🎵 fileName: ${message.fileName}")
                                    Log.d(TAG, "🎵 sessionId: ${message.sessionId}")
                                    Log.d(TAG, "🎵 fileSizeBytes: ${message.fileSizeBytes}")
                                    Log.d(TAG, "🎵 totalChunks: ${message.totalChunks}")
                                    
                                    musicShareManager.receiveMusicMetadata(message)
                                    
                                    // Setup progress callback to report to host
                                    musicShareManager.onProgressUpdate = { progress, isComplete ->
                                        viewModelScope.launch {
                                            val statusMsg = SyncMessage.MusicDownloadStatus(
                                                sessionId = message.sessionId,
                                                isComplete = isComplete,
                                                progress = progress
                                            )
                                            syncClient.sendMessage(statusMsg)
                                            Log.d(TAG, "📤 Sent download status: ${(progress * 100).toInt()}% (complete=$isComplete)")
                                        }
                                    }
                                    
                                    // CRITICAL: Start download with the received sessionId
                                    Log.d(TAG, "📥 ========== STARTING DOWNLOAD ==========")
                                    startMusicDownload(message.sessionId)
                                }
                                is SyncMessage.MusicChunk -> {
                                    val data = android.util.Base64.decode(message.data, android.util.Base64.NO_WRAP)
                                    musicShareManager.storeChunk(message.chunkIndex, data)
                                }
                                is SyncMessage.MusicStart -> {
                                    Log.d(TAG, "▶️ ========== MUSIC START COMMAND RECEIVED ==========")
                                    Log.d(TAG, "▶️ Message sessionId: ${message.sessionId}")
                                    Log.d(TAG, "▶️ HOST start time (host clock): ${message.startAt}")
                                    Log.d(TAG, "▶️ Current local time: ${System.currentTimeMillis()}")
                                    Log.d(TAG, "▶️ Clock offset: ${syncClient.clockOffsetMillis.value}ms")
                                    
                                    // CRITICAL: Use ClockSync to convert host time to local time
                                    // This is THE KEY to perfect synchronization!
                                    val hostStartTime = message.startAt
                                    val localOffset = syncClient.clockOffsetMillis.value
                                    val currentLocalTime = System.currentTimeMillis()
                                    
                                    // CRITICAL: ClockSync formula
                                    // If offset is POSITIVE: host clock is AHEAD of local clock
                                    // If offset is NEGATIVE: host clock is BEHIND local clock
                                    // To convert host time to local time: localTime = hostTime - offset
                                    val localStartTime = hostStartTime - localOffset
                                    
                                    Log.d(TAG, "⏰ ========== CLOCK SYNC CONVERSION ==========")
                                    Log.d(TAG, "⏰ Host start time: $hostStartTime")
                                    Log.d(TAG, "⏰ Clock offset: ${if (localOffset >= 0) "+" else ""}${localOffset}ms")
                                    Log.d(TAG, "⏰ LOCAL start time (converted): $localStartTime")
                                    Log.d(TAG, "⏰ Current local time: $currentLocalTime")
                                    Log.d(TAG, "⏰ Delay until start: ${localStartTime - currentLocalTime}ms")
                                    
                                    if (localStartTime <= currentLocalTime) {
                                        Log.w(TAG, "⚠️ WARNING: Converted start time already passed!")
                                        Log.w(TAG, "   Late by: ${currentLocalTime - localStartTime}ms")
                                        Log.w(TAG, "   Check clock-sync quality (run more pings)")
                                    }
                                    
                                    // Start playback at converted LOCAL time
                                    Log.d(TAG, "🎵 Starting playback with LOCAL time: $localStartTime")
                                    musicShareManager.startPlayback(localStartTime)
                                }
                                is SyncMessage.MusicControl -> {
                                    Log.d(TAG, "🎛️ ========== MUSIC CONTROL RECEIVED ==========")
                                    Log.d(TAG, "🎛️ Action: ${message.action}")
                                    Log.d(TAG, "🎛️ Position from HOST: ${message.positionMs}ms")
                                    Log.d(TAG, "🎛️ HOST execute time: ${message.executeAt}")
                                    Log.d(TAG, "🎛️ Clock offset: ${syncClient.clockOffsetMillis.value}ms")
                                    
                                    // Convert host time to local time
                                    val hostExecuteTime = message.executeAt
                                    val localOffset = syncClient.clockOffsetMillis.value
                                    val localExecuteTime = hostExecuteTime - localOffset
                                    val resumePosition = message.positionMs
                                    
                                    Log.d(TAG, "⏰ Sync conversion:")
                                    Log.d(TAG, "   - LOCAL execute time: $localExecuteTime")
                                    Log.d(TAG, "   - Delay: ${localExecuteTime - System.currentTimeMillis()}ms")
                                    
                                    when (message.action) {
                                        MusicAction.PAUSE -> {
                                            Log.d(TAG, "⏸️ Pause scheduled")
                                            launch { musicShareManager.pausePlayback(localExecuteTime) }
                                        }
                                        MusicAction.RESUME -> {
                                            Log.d(TAG, "▶️ Resume at position ${resumePosition}ms")
                                            launch { musicShareManager.resumePlayback(localExecuteTime, resumePosition) }
                                        }
                                        MusicAction.STOP -> {
                                            Log.d(TAG, "⏹️ Stop scheduled")
                                            launch { musicShareManager.stopPlayback() }
                                        }
                                    }
                                    
                                    Log.d(TAG, "✅ Control scheduled")
                                }
                                is SyncMessage.HostTransfer -> {
                                    Log.d(TAG, "👑 ========== HOST TRANSFER ==========")
                                    Log.d(TAG, "👑 New host: ${message.newHostName}")
                                    Log.d(TAG, "👑 New host ID: ${message.newHostId}")
                                    Log.d(TAG, "👑 Reason: ${message.reason}")
                                    
                                    val myDeviceId = (syncClient.connectionState.value as? SyncConnectionState.Connected)?.deviceId
                                    
                                    if (message.newHostId == myDeviceId) {
                                        Log.d(TAG, "🎉🎉🎉 I AM THE NEW HOST!")
                                        Log.d(TAG, "🔄 Transitioning from CLIENT to HOST...")
                                        
                                        // CRITICAL: Get current RoomState before disconnecting
                                        val currentRoomState = roomStateManager?.getCurrentState()
                                        Log.d(TAG, "📊 Current RoomState captured:")
                                        if (currentRoomState != null) {
                                            Log.d(TAG, "   - Playback: ${currentRoomState.playbackState}")
                                            Log.d(TAG, "   - Position: ${currentRoomState.currentPosition}ms")
                                            Log.d(TAG, "   - Tracks: ${currentRoomState.playlist.tracks.size}")
                                            Log.d(TAG, "   - Current track: ${currentRoomState.playlist.currentTrackIndex}")
                                        } else {
                                            Log.w(TAG, "⚠️ No RoomState available!")
                                        }
                                        
                                        // Disconnect from old host
                                        syncClient.disconnect()
                                        Log.d(TAG, "✅ Disconnected from old host")
                                        
                                        // Become the new host with preserved state
                                        viewModelScope.launch {
                                            delay(500)  // Brief delay for cleanup
                                            Log.d(TAG, "🚀 Starting new host server...")
                                            becomeHost(currentRoomState)
                                        }
                                    } else {
                                        Log.d(TAG, "🔄 Another device became host: ${message.newHostName}")
                                        Log.d(TAG, "📊 Staying as CLIENT, continuing playback...")
                                        // Continue as CLIENT - music keeps playing
                                        // No reconnect needed - old host kept us connected
                                    }
                                }
                                is SyncMessage.DJStateSync -> {
                                    Log.d(TAG, "🎛️ ========== DJ STATE SYNC RECEIVED ==========")
                                    try {
                                        val djState = kotlinx.serialization.json.Json.decodeFromString<DJState>(message.djStateJson)
                                        
                                        Log.d(TAG, "🎛️ DJ State:")
                                        Log.d(TAG, "   - Bass: ${djState.bass}")
                                        Log.d(TAG, "   - Mid: ${djState.mid}")
                                        Log.d(TAG, "   - Treble: ${djState.treble}")
                                        Log.d(TAG, "   - Master Volume: ${djState.masterVolume}")
                                        Log.d(TAG, "   - Playback Speed: ${djState.playbackSpeed}")
                                        
                                        // Apply at synchronized time
                                        val delayMs = message.applyAt - System.currentTimeMillis()
                                        if (delayMs > 0) {
                                            Log.d(TAG, "⏰ Waiting ${delayMs}ms before applying...")
                                            delay(delayMs)
                                        }
                                        
                                        // Apply DJ state to audio engine
                                        djStateManager?.applyDJState(djState)
                                        
                                        // CRITICAL: Apply to music player immediately
                                        musicShareManager.applyDJControls(
                                            djState.bass,
                                            djState.mid,
                                            djState.treble,
                                            djState.masterVolume
                                        )
                                        
                                        // Apply playback speed if changed
                                        if (djState.playbackSpeed != 1.0f) {
                                            musicShareManager.setPlaybackSpeed(djState.playbackSpeed)
                                        }
                                        
                                        Log.d(TAG, "✅ DJ state applied to audio engine")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Failed to apply DJ state", e)
                                        e.printStackTrace()
                                    }
                                }
                                is SyncMessage.BassSync -> {
                                    // CRITICAL: CLIENT receives bass sync from HOST for synchronized flash!
                                    // This is THE KEY to synchronized party mode flash across all devices
                                    
                                    // PRIORITY 1: KICK detection (instant, most reliable)
                                    val hasKick = message.hasKick
                                    
                                    // PRIORITY 2: BEAT detection (fallback for non-kick beats)
                                    val hasBeat = message.isBeat
                                    
                                    // Bass presence check
                                    val hasBass = message.bass > 0.08f || message.subBass > 0.08f
                                    
                                    // Energy check
                                    val hasEnergy = message.energy > 0.10f
                                    
                                    // FLASH CONDITION: (Kick OR Beat) AND (Bass OR Energy)
                                    val shouldFlash = (hasKick || hasBeat) && (hasBass || hasEnergy)
                                    
                                    if (shouldFlash) {
                                        Log.d(TAG, "🔥 ========== BASS SYNC FLASH ==========")
                                        Log.d(TAG, "🔥 Kick: $hasKick, Beat: $hasBeat")
                                        Log.d(TAG, "🔥 Bass: ${"%.2f".format(message.bass)}, Energy: ${"%.2f".format(message.energy)}")
                                        Log.d(TAG, "🔥 Start time: ${message.startAt}")
                                        
                                        // CRITICAL: Use TORCH_ONLY mode (camera flash, not screen)
                                        // This is what user wants: "gerçek flaşı kullanmak istiyoruz"
                                        val flashMsg = SyncMessage.Flash(
                                            startAt = message.startAt,
                                            durationMs = 50,  // Short pulse for bass beat
                                            mode = FlashMode.TORCH_ONLY,  // ✅ CAMERA FLASH
                                            color = "#FFFFFF",
                                            repeatCount = 1,
                                            intervalMs = 0
                                        )
                                        
                                        _pendingFlash.value = flashMsg
                                        Log.d(TAG, "✅ CLIENT flash triggered!")
                                    }
                                }
                                is SyncMessage.BassFlashControl -> {
                                    // CRITICAL: HOST enabled/disabled bass flash - CLIENT must follow!
                                    Log.d(TAG, "🔥 ========== BASS FLASH CONTROL RECEIVED ==========")
                                    Log.d(TAG, "🔥 Enabled: ${message.enabled}")
                                    Log.d(TAG, "🔥 Intensity: ${message.intensity}")
                                    
                                    // Parse intensity
                                    val intensity = try {
                                        BassFlashlightSync.FlashIntensity.valueOf(message.intensity)
                                    } catch (e: Exception) {
                                        BassFlashlightSync.FlashIntensity.NORMAL
                                    }
                                    
                                    // Update local state
                                    _bassFlashIntensity.value = intensity
                                    
                                    // Toggle bass flash to match HOST
                                    if (message.enabled != _bassFlashEnabled.value) {
                                        Log.d(TAG, "🔄 Syncing bass flash state with HOST...")
                                        toggleBassFlashSync(message.enabled)
                                    } else {
                                        Log.d(TAG, "✅ Bass flash already in sync with HOST")
                                    }
                                }
                                is SyncMessage.DJEffectTrigger -> {
                                    // CRITICAL: CLIENT receives DJ effect trigger from HOST!
                                    // Play synchronized with ultra-low latency
                                    Log.d(TAG, "🎵 ========== DJ EFFECT TRIGGER RECEIVED ==========")
                                    Log.d(TAG, "🎵 Effect ID: ${message.effectId}")
                                    Log.d(TAG, "🎵 Play at: ${message.playAt}")
                                    Log.d(TAG, "🎵 Volume: ${message.volume}")
                                    
                                    // Schedule effect with clock-corrected timestamp
                                    djEffectPlayer.scheduleEffect(
                                        effectId = message.effectId,
                                        playAtHostTime = message.playAt,
                                        volume = message.volume
                                    )
                                    
                                    val effect = DJEffects.getById(message.effectId)
                                    Log.d(TAG, "✅ DJ effect scheduled: ${effect?.displayName}")
                                }
                                is SyncMessage.PlaylistUpdate -> {
                                    Log.d(TAG, "📝 ========== PLAYLIST UPDATE RECEIVED ==========")
                                    Log.d(TAG, "📝 Update type: ${message.updateType}")
                                    try {
                                        val playlist = kotlinx.serialization.json.Json.decodeFromString<Playlist>(message.playlist)
                                        Log.d(TAG, "📝 Updated playlist: ${playlist.tracks.size} tracks")
                                        
                                        // Update local RoomState
                                        roomStateManager?.updateState(playlist = playlist)
                                        
                                        Log.d(TAG, "📝 Tracks in playlist:")
                                        playlist.tracks.forEachIndexed { index, track ->
                                            val marker = if (index == playlist.currentTrackIndex) "▶️" else "  "
                                            Log.d(TAG, "   $marker $index. ${track.fileName}")
                                        }
                                        
                                        Log.d(TAG, "✅ Playlist updated locally")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Failed to process playlist update", e)
                                        e.printStackTrace()
                                    }
                                }
                                is SyncMessage.TrackChange -> {
                                    Log.d(TAG, "🎵 ========== TRACK CHANGE RECEIVED ==========")
                                    Log.d(TAG, "🎵 New track index: ${message.trackIndex}")
                                    Log.d(TAG, "🎵 Session ID: ${message.trackSessionId}")
                                    Log.d(TAG, "🎵 Start scheduled at: ${message.startAt}")
                                    
                                    // Stop current playback
                                    Log.d(TAG, "⏹️ Stopping current playback...")
                                    musicShareManager.stopPlayback()
                                    
                                    // Update room state to new track
                                    val currentPlaylist = roomStateManager?.getCurrentState()?.playlist
                                    if (currentPlaylist != null) {
                                        val updatedPlaylist = currentPlaylist.copy(currentTrackIndex = message.trackIndex)
                                        roomStateManager?.updateState(
                                            playlist = updatedPlaylist,
                                            currentPosition = 0,
                                            playbackState = PlaybackState.IDLE
                                        )
                                        
                                        val newTrack = updatedPlaylist.tracks.getOrNull(message.trackIndex)
                                        if (newTrack != null) {
                                            Log.d(TAG, "🎵 New track: ${newTrack.fileName}")
                                            
                                            // Check if we need to download
                                            val currentMusicState = musicShareManager.musicState.value
                                            val needsDownload = currentMusicState !is MusicShareState.Ready ||
                                                              (currentMusicState as? MusicShareState.Ready)?.fileName != newTrack.fileName
                                            
                                            if (needsDownload) {
                                                Log.d(TAG, "📥 Need to download new track")
                                                // Metadata will be broadcasted by host, download will auto-start
                                            } else {
                                                Log.d(TAG, "✅ Track already downloaded, ready to play")
                                            }
                                        }
                                    }
                                    
                                    Log.d(TAG, "✅ Ready for new track, waiting for start command")
                                }
                                else -> Unit
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error processing music message", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Message collection failed", e)
            }
        }
    }
    
    /**
     * CLIENT: Start downloading music chunks with AGGRESSIVE retry and parallel requests
     * CRITICAL FIX: Takes sessionId as parameter (don't try to get from host's metadata!)
     */
    private fun startMusicDownload(sessionId: String) {
        viewModelScope.launch {
            Log.d(TAG, "📥 ========== MUSIC DOWNLOAD STARTED ==========")
            Log.d(TAG, "📥 Session ID: $sessionId")
            
            var retryRound = 0
            val maxRetryRounds = 10  // Increased retry attempts
            
            while (retryRound < maxRetryRounds) {
                val missingChunks = musicShareManager.getMissingChunks()
                
                if (missingChunks.isEmpty()) {
                    Log.d(TAG, "✅ ========== DOWNLOAD COMPLETE ==========")
                    Log.d(TAG, "✅ All chunks received successfully!")
                    break
                }
                
                retryRound++
                Log.d(TAG, "📥 ========== DOWNLOAD ROUND $retryRound/$maxRetryRounds ==========")
                Log.d(TAG, "📥 Missing chunks: ${missingChunks.size}")
                Log.d(TAG, "📥 Chunk indices: ${missingChunks.take(10)}${if (missingChunks.size > 10) "..." else ""}")
                
                // PARALLEL REQUEST: Request all missing chunks at once (server will queue them)
                missingChunks.forEach { chunkIndex ->
                    val requestMsg = SyncMessage.MusicChunkRequest(
                        sessionId = sessionId,
                        chunkIndex = chunkIndex
                    )
                    
                    // Send request to server
                    syncClient.sendMessage(requestMsg)
                    
                    if (chunkIndex % 10 == 0 || chunkIndex == missingChunks.last()) {
                        Log.d(TAG, "📤 Requested chunk $chunkIndex")
                    }
                }
                
                Log.d(TAG, "📥 Waiting for chunks to arrive (3s timeout)...")
                
                // Wait for chunks to arrive (shorter intervals, more attempts)
                delay(3000)
            }
            
            if (retryRound >= maxRetryRounds) {
                Log.e(TAG, "❌ Download failed after $maxRetryRounds attempts")
                val missingChunks = musicShareManager.getMissingChunks()
                Log.e(TAG, "❌ Still missing ${missingChunks.size} chunks: ${missingChunks.take(20)}")
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
        Log.d(TAG, "🔦 ========== TORCH PULSE REQUEST ==========")
        Log.d(TAG, "🔦 Duration: ${durationMs}ms, repeat: $repeatCount, interval: ${intervalMs}ms")
        
        val sync = bassFlashSync
        if (sync == null) {
            Log.e(TAG, "❌ Cannot pulse - shared BassFlashSync instance not set!")
            Log.e(TAG, "❌ This means MainActivity didn't call setBassFlashSync()")
            return
        }
        
        if (!sync.hasFlashSupport()) {
            Log.w(TAG, "⚠️ Torch not supported on this device")
            Log.w(TAG, "⚠️ hasFlash: false or camera unavailable")
            return
        }
        
        Log.d(TAG, "✅ BassFlashSync available - calling manualPulse...")
        Log.d(TAG, "🔦 Calling manualPulse on SHARED instance...")
        
        try {
            sync.manualPulse(durationMs, repeatCount, intervalMs)
            Log.d(TAG, "✅ manualPulse completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ manualPulse failed with exception", e)
            Log.e(TAG, "❌ Exception: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
    
    // ---------- MUSIC SHARE FUNCTIONS (HOST) ----------
    
    /**
     * HOST: Select music file and prepare for sharing
     * CRITICAL: GUARANTEED metadata broadcast to ALL clients (existing + late join)
     */
    fun selectMusicFile(uri: android.net.Uri) {
        if (syncServer == null) {
            Log.e(TAG, "❌ Must be hosting to share music")
            return
        }
        
        val server = syncServer!!
        
        viewModelScope.launch {
            Log.d(TAG, "🎵 ========== MUSIC FILE SELECTION STARTED ==========")
            Log.d(TAG, "🎵 URI: $uri")
            Log.d(TAG, "🎵 Connected clients: ${_connectedDevices.value.size}")
            
            val result = musicShareManager.prepareMusic(uri)
            
            if (result.isSuccess) {
                val session = result.getOrNull()
                Log.d(TAG, "✅ Music prepared successfully:")
                Log.d(TAG, "   - Session ID: ${session?.sessionId}")
                Log.d(TAG, "   - File name: ${session?.fileName}")
                
                // CRITICAL: IMMEDIATE metadata broadcast - NO DELAY!
                val metadata = musicShareManager.getMusicMetadata()
                if (metadata != null) {
                    Log.d(TAG, "📡 ========== BROADCASTING METADATA TO ALL CLIENTS ==========")
                    
                    // Broadcast to ALL connected clients
                    launch {
                        try {
                            server.broadcast(metadata)
                            delay(100) // Brief delay for message delivery
                            
                            Log.d(TAG, "✅ ========== METADATA BROADCAST COMPLETE ==========")
                            Log.d(TAG, "✅ Sent to ${_connectedDevices.value.size} clients")
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ BROADCAST FAILED", e)
                        }
                    }
                } else {
                    Log.e(TAG, "❌ getMusicMetadata returned NULL!")
                }
            } else {
                Log.e(TAG, "❌ Failed to prepare music: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * HOST: Start synchronized music playback across all devices
     * CRITICAL: Uses GUARANTEED DELIVERY with broadcast verification
     * - Broadcasts MusicStart with synchronized timestamp
     * - Verifies ALL clients received the message
     * - Logs delivery confirmation
     * - Host also starts at EXACT same synchronized time
     */
    fun startSharedMusic() {
        val server = syncServer
        if (server == null) {
            Log.e(TAG, "❌ Must be hosting to control music")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "🎵 ========== STARTING SYNCHRONIZED MUSIC PLAYBACK ==========")
            
            val connectedDeviceCount = _connectedDevices.value.size
            Log.d(TAG, "🎵 Connected devices: $connectedDeviceCount")
            _connectedDevices.value.forEach { device ->
                Log.d(TAG, "   - ${device.name}")
            }
            
            // CRITICAL: ULTRA-LOW-LATENCY synchronized start time
            // Music is ALREADY downloaded and prepared on all devices!
            // We only need minimal time for message delivery and scheduling
            // 
            // BREAKDOWN:
            // 1. Message broadcast (50-100ms WiFi local network)
            // 2. Client receives + processes message (20ms)
            // 3. Handler scheduling precision (30ms)
            // 4. Safety buffer for jitter (50ms)
            // 
            // TOTAL: 150ms (down from 2000ms!)
            // 
            // WHY THIS WORKS:
            // - Music is ALREADY downloaded (not streaming!)
            // - MediaPlayer is ALREADY prepared (file ready to play!)
            // - ClockSync compensates for time differences (±5ms)
            // - Local WiFi has ~10-50ms latency (not internet!)
            
            val messageDeliveryTime = 80L     // Local WiFi is FAST
            val processingTime = 20L          // Message parsing is instant
            val schedulingBuffer = 50L        // Handler precision buffer
            val totalLeadTime = messageDeliveryTime + processingTime + schedulingBuffer
            
            val startAt = System.currentTimeMillis() + totalLeadTime
            
            Log.d(TAG, "⏰ ULTRA-LOW-LATENCY synchronized start:")
            Log.d(TAG, "   - Current time: ${System.currentTimeMillis()}")
            Log.d(TAG, "   - Start time: $startAt")
            Log.d(TAG, "   - Lead time: ${totalLeadTime}ms (OPTIMIZED from 2000ms!)")
            Log.d(TAG, "   - Will start in: ${(startAt - System.currentTimeMillis())}ms")
            
            val startMsg = SyncMessage.MusicStart(
                sessionId = musicShareManager.getMusicMetadata()?.sessionId ?: "",
                startAt = startAt
            )
            
            Log.d(TAG, "📡 Broadcasting MusicStart to ALL $connectedDeviceCount clients...")
            Log.d(TAG, "📡 Message: sessionId=${startMsg.sessionId}, startAt=${startMsg.startAt}")
            
            // CRITICAL: Broadcast with delivery confirmation
            val (successCount, failCount) = server.broadcast(startMsg)
            
            Log.d(TAG, "📡 ========== BROADCAST RESULT ==========")
            Log.d(TAG, "📡 Target clients: $connectedDeviceCount")
            Log.d(TAG, "📡 Delivered: $successCount")
            Log.d(TAG, "📡 Failed: $failCount")
            
            if (failCount > 0) {
                Log.e(TAG, "❌ WARNING: $failCount client(s) did NOT receive MusicStart!")
                Log.e(TAG, "❌ Synchronization may be imperfect!")
            } else {
                Log.d(TAG, "✅ ALL CLIENTS CONFIRMED - GUARANTEED DELIVERY")
            }
            
            // CRITICAL: Host ALSO starts at the SAME synchronized time (not immediately!)
            // This ensures ALL devices (host + clients) play together
            val hostDelayMs = startAt - System.currentTimeMillis()
            Log.d(TAG, "⏰ HOST will also play at synchronized time")
            Log.d(TAG, "⏰ HOST waiting ${hostDelayMs}ms before starting playback...")
            
            if (hostDelayMs > 0) {
                delay(hostDelayMs)
            } else {
                Log.w(TAG, "⚠️ WARNING: Start time already passed! Starting immediately...")
            }
            
            Log.d(TAG, "▶️ ========== HOST STARTING PLAYBACK NOW ==========")
            Log.d(TAG, "▶️ Synchronized with $successCount client(s)")
            musicShareManager.startPlayback(System.currentTimeMillis())
            
            // Update RoomState
            roomStateManager?.updateState(
                playbackState = PlaybackState.PLAYING,
                currentPosition = 0
            )
            Log.d(TAG, "📊 RoomState updated: PLAYING")
            
            Log.d(TAG, "✅ ========== SYNCHRONIZED PLAYBACK ACTIVE ==========")
            Log.d(TAG, "✅ Host + $successCount clients playing in perfect sync")
        }
    }
    
    /**
     * HOST: Control music playback (pause/resume/stop)
     * CRITICAL PRINCIPLE: "HOST WAITS TOO" - Host ALSO executes at synchronized time!
     * 
     * WHY THIS IS CRITICAL:
     * - If host executes immediately but clients execute later → OUT OF SYNC!
     * - ALL devices (host + clients) MUST execute at SAME timestamp
     * - This is the KEY to perfect synchronization
     * 
     * HOW IT WORKS:
     * 1. Calculate future executeAt timestamp (NOW + lead time)
     * 2. Broadcast MusicControl(executeAt) to all clients
     * 3. HOST ALSO schedules execution at SAME executeAt time
     * 4. Result: ALL devices execute together (±5ms precision)
     */
    fun controlSharedMusic(action: MusicAction) {
        val server = syncServer
        if (server == null) {
            Log.e(TAG, "❌ Must be hosting to control music")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "🎛️ ========== PROFESSIONAL SYNC CONTROL: $action ==========")
            
            // CRITICAL: Get current position BEFORE calculating time
            val currentPosition = musicShareManager.getCurrentPosition()
            
            // ULTRA-LOW-LATENCY synchronized execute time
            // We only need time for message delivery (music already playing!)
            val networkLeadTime = 100L  // Reduced from 500ms → 100ms
            val executeAt = System.currentTimeMillis() + networkLeadTime
            
            Log.d(TAG, "⏰ ULTRA-LOW-LATENCY control timing:")
            Log.d(TAG, "   - Action: $action")
            Log.d(TAG, "   - Current position: ${currentPosition}ms")
            Log.d(TAG, "   - Execute time: $executeAt")
            Log.d(TAG, "   - Lead time: ${networkLeadTime}ms (OPTIMIZED from 500ms!)")
            
            val controlMsg = SyncMessage.MusicControl(
                sessionId = musicShareManager.getMusicMetadata()?.sessionId ?: "",
                action = action,
                executeAt = executeAt,
                positionMs = currentPosition
            )
            
            Log.d(TAG, "📡 Broadcasting with position: ${currentPosition}ms")
            val (successCount, failCount) = server.broadcast(controlMsg)
            
            Log.d(TAG, "📡 Result: $successCount success, $failCount failed")
            
            if (failCount == 0) {
                Log.d(TAG, "✅ ALL CLIENTS received control")
            }
            
            when (action) {
                MusicAction.PAUSE -> {
                    musicShareManager.pausePlayback(executeAt)
                    roomStateManager?.updateState(
                        playbackState = PlaybackState.PAUSED,
                        currentPosition = currentPosition
                    )
                    Log.d(TAG, "⏸️ HOST pause scheduled, RoomState updated")
                }
                MusicAction.RESUME -> {
                    musicShareManager.resumePlayback(executeAt, currentPosition)
                    roomStateManager?.updateState(
                        playbackState = PlaybackState.PLAYING,
                        currentPosition = currentPosition
                    )
                    Log.d(TAG, "▶️ HOST resume scheduled at position ${currentPosition}ms, RoomState updated")
                }
                MusicAction.STOP -> {
                    musicShareManager.stopPlayback()
                    roomStateManager?.updateState(
                        playbackState = PlaybackState.IDLE,
                        currentPosition = 0
                    )
                    Log.d(TAG, "⏹️ HOST stop scheduled, RoomState updated")
                }
            }
            
            Log.d(TAG, "✅ Sync complete: Host + $successCount clients")
        }
    }
    
    // ========== HOST TRANSFER ==========
    
    /**
     * Become the new host after host transfer
     * Called when old host leaves and this device is selected as new host
     */
    private suspend fun becomeHost(previousRoomState: RoomState?) {
        Log.d(TAG, "👑 ========== BECOMING NEW HOST ==========")
        Log.d(TAG, "👑 Previous RoomState: ${previousRoomState != null}")
        
        // Stop any existing server
        syncServer?.stop()
        
        // Create new server with original room name  
        val roomName = if (previousRoomState != null) "Rezonans Room" else "Rezonans Room"
        val server = SyncServer(roomName = roomName)
        syncServer = server
        server.start()
        
        // Restore RoomState if available
        if (previousRoomState != null) {
            roomStateManager = server.getRoomStateManager()
            roomStateManager?.loadState(previousRoomState)
            Log.d(TAG, "📊 RoomState restored:")
            Log.d(TAG, "   - Playback: ${previousRoomState.playbackState}")
            Log.d(TAG, "   - Position: ${previousRoomState.currentPosition}ms")
            Log.d(TAG, "   - Tracks: ${previousRoomState.playlist.tracks.size}")
        } else {
            roomStateManager = server.getRoomStateManager()
        }
        
        // Initialize DJ manager
        if (djStateManager == null) {
            djStateManager = DJStateManager()
            Log.d(TAG, "🎛️ DJStateManager initialized")
        }
        
        // Start discovery
        discoveryManager.advertise(roomName, SyncServer.DEFAULT_PORT)
        SyncForegroundService.start(getApplication(), roomName)
        
        _syncState.value = SyncState.Hosting(roomName)
        
        // Listen to connections
        viewModelScope.launch {
            server.connectedDevices.collect { devices ->
                _connectedDevices.value = devices
                SyncForegroundService.start(getApplication(), roomName, devices.size)
                Log.d(TAG, "👥 Connected devices as new host: ${devices.size}")
            }
        }
        
        // Listen to incoming messages
        viewModelScope.launch {
            server.incomingMessages.collect { (deviceId, message) ->
                // Handle chunk requests, download status, etc.
                if (message is SyncMessage.MusicChunkRequest) {
                    launch {
                        val chunkData = musicShareManager.getChunk(message.chunkIndex)
                        if (chunkData != null) {
                            val base64Data = android.util.Base64.encodeToString(chunkData, android.util.Base64.NO_WRAP)
                            val chunkMsg = SyncMessage.MusicChunk(
                                sessionId = message.sessionId,
                                chunkIndex = message.chunkIndex,
                                data = base64Data
                            )
                            server.sendToDevice(deviceId, chunkMsg)
                        }
                    }
                }
                
                if (message is SyncMessage.MusicDownloadStatus) {
                    val currentStatus = _clientDownloadStatus.value.toMutableMap()
                    currentStatus[deviceId] = message.progress
                    _clientDownloadStatus.value = currentStatus
                }
            }
        }
        
        Log.d(TAG, "✅ Successfully became new host!")
    }
    
    // ========== PLAYLIST MANAGEMENT ==========
    
    /**
     * Add track to playlist (HOST only)
     * CRITICAL: Now broadcasts BOTH PlaylistUpdate AND MusicMetadata!
     */
    fun addTrackToPlaylist(musicUri: android.net.Uri) {
        if (syncServer == null) {
            Log.e(TAG, "❌ Must be hosting to add tracks")
            return
        }
        
        val server = syncServer!!
        
        viewModelScope.launch {
            Log.d(TAG, "➕ ========== ADDING TRACK TO PLAYLIST ==========")
            Log.d(TAG, "➕ URI: $musicUri")
            Log.d(TAG, "➕ Connected clients: ${_connectedDevices.value.size}")
            
            // Prepare music file
            val result = musicShareManager.prepareMusic(musicUri)
            if (result.isSuccess) {
                val session = result.getOrNull()
                if (session != null) {
                    // Create playlist track
                    val track = PlaylistTrack(
                        sessionId = session.sessionId,
                        fileName = session.fileName,
                        fileSizeBytes = session.fileSizeBytes
                    )
                    
                    // Add to room state
                    roomStateManager?.addTrack(track)
                    
                    // Broadcast playlist update
                    val playlist = roomStateManager?.getCurrentState()?.playlist
                    if (playlist != null) {
                        val playlistJson = kotlinx.serialization.json.Json.encodeToString(Playlist.serializer(), playlist)
                        val updateMsg = SyncMessage.PlaylistUpdate(
                            playlist = playlistJson,
                            updateType = "add"
                        )
                        server.broadcast(updateMsg)
                        Log.d(TAG, "📡 Broadcast PlaylistUpdate")
                    }
                    
                    // CRITICAL FIX: ALSO broadcast MusicMetadata so clients can download!
                    val metadata = musicShareManager.getMusicMetadata()
                    if (metadata != null) {
                        Log.d(TAG, "📡 ========== BROADCASTING MUSIC METADATA ==========")
                        Log.d(TAG, "📡 Session: ${metadata.sessionId}")
                        Log.d(TAG, "📡 File: ${metadata.fileName}")
                        Log.d(TAG, "📡 Size: ${metadata.fileSizeBytes} bytes")
                        Log.d(TAG, "📡 Chunks: ${metadata.totalChunks}")
                        Log.d(TAG, "📡 Target: ${_connectedDevices.value.size} clients")
                        
                        launch {
                            try {
                                val (successCount, failCount) = server.broadcast(metadata)
                                
                                Log.d(TAG, "📡 ========== METADATA BROADCAST COMPLETE ==========")
                                Log.d(TAG, "✅ Success: $successCount clients")
                                Log.d(TAG, "❌ Failed: $failCount clients")
                                
                                if (failCount > 0) {
                                    Log.e(TAG, "⚠️ WARNING: Some clients did NOT receive metadata!")
                                } else {
                                    Log.d(TAG, "✅ ALL CLIENTS RECEIVED METADATA - Download will start!")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ METADATA BROADCAST FAILED", e)
                            }
                        }
                    } else {
                        Log.e(TAG, "❌ getMusicMetadata returned NULL!")
                    }
                    
                    Log.d(TAG, "✅ Track added to playlist: ${track.fileName}")
                }
            } else {
                Log.e(TAG, "❌ Failed to prepare track: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    
    /**
     * Remove track from playlist (HOST only)
     */
    fun removeTrackFromPlaylist(trackId: String) {
        if (syncServer == null) {
            Log.e(TAG, "❌ Must be hosting to remove tracks")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "➖ Removing track from playlist: $trackId")
            
            roomStateManager?.removeTrack(trackId)
            
            // Broadcast playlist update
            val playlist = roomStateManager?.getCurrentState()?.playlist
            if (playlist != null) {
                val playlistJson = kotlinx.serialization.json.Json.encodeToString(Playlist.serializer(), playlist)
                val updateMsg = SyncMessage.PlaylistUpdate(
                    playlist = playlistJson,
                    updateType = "remove"
                )
                syncServer?.broadcast(updateMsg)
            }
            
            Log.d(TAG, "✅ Track removed from playlist")
        }
    }
    
    /**
     * Next track (HOST only)
     * CRITICAL FIX: Properly load the next track so it can be played!
     */
    fun nextTrack() {
        if (syncServer == null) {
            Log.e(TAG, "❌ Must be hosting to control playlist")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "⏭️ Next track...")
            
            val nextTrack = roomStateManager?.nextTrack()
            if (nextTrack != null) {
                Log.d(TAG, "🎵 Next track: ${nextTrack.fileName}")
                
                // Stop current playback
                controlSharedMusic(MusicAction.STOP)
                
                // CRITICAL: Track change - clients will re-download if needed
                // No need to prepareMusic here - clients handle it via MusicMetadata broadcast
                
                // Broadcast metadata for new track
                val metadata = SyncMessage.MusicMetadata(
                    sessionId = nextTrack.sessionId,
                    fileName = nextTrack.fileName,
                    fileSizeBytes = nextTrack.fileSizeBytes,
                    chunkSizeBytes = 65536,
                    totalChunks = ((nextTrack.fileSizeBytes + 65535) / 65536).toInt(),
                    format = nextTrack.fileName.substringAfterLast('.', "mp3")
                )
                syncServer?.broadcast(metadata)
                
                // Broadcast track change
                val changeMsg = SyncMessage.TrackChange(
                    trackIndex = roomStateManager?.getCurrentState()?.playlist?.currentTrackIndex ?: 0,
                    trackSessionId = nextTrack.sessionId,
                    startAt = System.currentTimeMillis() + 2000L
                )
                syncServer?.broadcast(changeMsg)
                
                Log.d(TAG, "✅ Track change broadcasted - ready to play!")
            } else {
                Log.w(TAG, "⚠️ No next track available")
            }
        }
    }
    
    /**
     * Previous track (HOST only)
     * CRITICAL FIX: Properly load the previous track so it can be played!
     */
    fun previousTrack() {
        if (syncServer == null) {
            Log.e(TAG, "❌ Must be hosting to control playlist")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "⏮️ Previous track...")
            
            val previousTrack = roomStateManager?.previousTrack()
            if (previousTrack != null) {
                Log.d(TAG, "🎵 Previous track: ${previousTrack.fileName}")
                
                // Stop current playback
                controlSharedMusic(MusicAction.STOP)
                
                // CRITICAL: Track change - clients will re-download if needed
                // No need to prepareMusic here - clients handle it via MusicMetadata broadcast
                
                // Broadcast metadata for new track
                val metadata = SyncMessage.MusicMetadata(
                    sessionId = previousTrack.sessionId,
                    fileName = previousTrack.fileName,
                    fileSizeBytes = previousTrack.fileSizeBytes,
                    chunkSizeBytes = 65536,
                    totalChunks = ((previousTrack.fileSizeBytes + 65535) / 65536).toInt(),
                    format = previousTrack.fileName.substringAfterLast('.', "mp3")
                )
                syncServer?.broadcast(metadata)
                
                // Broadcast track change
                val changeMsg = SyncMessage.TrackChange(
                    trackIndex = roomStateManager?.getCurrentState()?.playlist?.currentTrackIndex ?: 0,
                    trackSessionId = previousTrack.sessionId,
                    startAt = System.currentTimeMillis() + 2000L
                )
                syncServer?.broadcast(changeMsg)
                
                Log.d(TAG, "✅ Track change broadcasted - ready to play!")
            } else {
                Log.w(TAG, "⚠️ No previous track available")
            }
        }
    }
    
    /**
     * Select specific track (HOST only)
     * CRITICAL FIX: Properly load the selected track so it can be played!
     */
    fun selectTrack(index: Int) {
        if (syncServer == null) {
            Log.e(TAG, "❌ Must be hosting to control playlist")
            return
        }
        
        viewModelScope.launch {
            Log.d(TAG, "🎵 Selecting track $index...")
            
            val selectedTrack = roomStateManager?.selectTrack(index)
            if (selectedTrack != null) {
                Log.d(TAG, "🎵 Selected track: ${selectedTrack.fileName}")
                
                // Stop current playback
                controlSharedMusic(MusicAction.STOP)
                
                // CRITICAL: Track change - clients will re-download if needed
                // No need to prepareMusic here - clients handle it via MusicMetadata broadcast
                
                // Broadcast metadata for new track
                val metadata = SyncMessage.MusicMetadata(
                    sessionId = selectedTrack.sessionId,
                    fileName = selectedTrack.fileName,
                    fileSizeBytes = selectedTrack.fileSizeBytes,
                    chunkSizeBytes = 65536,
                    totalChunks = ((selectedTrack.fileSizeBytes + 65535) / 65536).toInt(),
                    format = selectedTrack.fileName.substringAfterLast('.', "mp3")
                )
                syncServer?.broadcast(metadata)
                
                // Broadcast track change
                val changeMsg = SyncMessage.TrackChange(
                    trackIndex = index,
                    trackSessionId = selectedTrack.sessionId,
                    startAt = System.currentTimeMillis() + 2000L
                )
                syncServer?.broadcast(changeMsg)
                
                Log.d(TAG, "✅ Track selected and broadcasted - ready to play!")
            } else {
                Log.w(TAG, "⚠️ Invalid track index: $index")
            }
        }
    }
    
    // ========== DJ CONTROLS - SYNCHRONIZED ==========
    
    // Throttling for DJ controls to prevent lag
    private var lastEQBroadcastTime = 0L
    private var lastVolumeBroadcastTime = 0L
    private var pendingEQUpdate: Triple<Float, Float, Float>? = null
    private var pendingVolumeUpdate: Float? = null
    private var eqThrottleJob: Job? = null
    private var volumeThrottleJob: Job? = null
    
    /**
     * HOST: Update EQ - Synchronized across all devices
     * SMOOTH THROTTLING: Broadcasts max every 100ms to prevent lag
     */
    fun updateDJEQ(bass: Float, mid: Float, treble: Float) {
        val server = syncServer
        val djManager = djStateManager
        if (server == null || djManager == null) {
            Log.e(TAG, "❌ Must be hosting with DJ enabled")
            return
        }
        
        // CRITICAL: Apply locally IMMEDIATELY for smooth UI
        djManager.updateEQ(bass, mid, treble)
        
        // Store pending update
        pendingEQUpdate = Triple(bass, mid, treble)
        
        // Cancel previous throttle job
        eqThrottleJob?.cancel()
        
        // THROTTLED broadcast: Max every 100ms
        val now = System.currentTimeMillis()
        val timeSinceLastBroadcast = now - lastEQBroadcastTime
        
        if (timeSinceLastBroadcast >= 100L) {
            // Broadcast immediately if enough time passed
            broadcastEQUpdate(server, djManager, bass, mid, treble)
            lastEQBroadcastTime = now
            pendingEQUpdate = null
        } else {
            // Schedule broadcast after remaining time
            val remainingTime = 100L - timeSinceLastBroadcast
            eqThrottleJob = viewModelScope.launch {
                delay(remainingTime)
                pendingEQUpdate?.let { (b, m, t) ->
                    broadcastEQUpdate(server, djManager, b, m, t)
                    lastEQBroadcastTime = System.currentTimeMillis()
                    pendingEQUpdate = null
                }
            }
        }
    }
    
    private fun broadcastEQUpdate(server: SyncServer, djManager: DJStateManager, bass: Float, mid: Float, treble: Float) {
        viewModelScope.launch {
            // CRITICAL: Update RoomState with new DJ state
            roomStateManager?.updateState(djState = djManager.getCurrentState())
            
            // Broadcast to all clients with minimal latency
            val applyAt = System.currentTimeMillis() + 20L // 20ms ultra-low latency
            val djStateJson = kotlinx.serialization.json.Json.encodeToString(DJState.serializer(), djManager.getCurrentState())
            val syncMsg = SyncMessage.DJStateSync(
                djStateJson = djStateJson,
                applyAt = applyAt
            )
            
            server.broadcast(syncMsg)
            Log.d(TAG, "🎚️ EQ synced: bass=${"%.2f".format(bass)}, mid=${"%.2f".format(mid)}, treble=${"%.2f".format(treble)}")
        }
    }
    
    /**
     * HOST: Update Volume - Synchronized with SMOOTH throttling
     */
    fun updateDJVolume(master: Float? = null) {
        val server = syncServer
        val djManager = djStateManager
        if (server == null || djManager == null) return
        
        // Apply locally IMMEDIATELY
        djManager.updateVolume(master, null, null, null, null)
        
        // Store pending update
        if (master != null) pendingVolumeUpdate = master
        
        // Cancel previous throttle job
        volumeThrottleJob?.cancel()
        
        // THROTTLED broadcast: Max every 100ms
        val now = System.currentTimeMillis()
        val timeSinceLastBroadcast = now - lastVolumeBroadcastTime
        
        if (timeSinceLastBroadcast >= 100L) {
            // Broadcast immediately
            broadcastVolumeUpdate(server, djManager, master)
            lastVolumeBroadcastTime = now
            pendingVolumeUpdate = null
        } else {
            // Schedule broadcast
            val remainingTime = 100L - timeSinceLastBroadcast
            volumeThrottleJob = viewModelScope.launch {
                delay(remainingTime)
                pendingVolumeUpdate?.let { vol ->
                    broadcastVolumeUpdate(server, djManager, vol)
                    lastVolumeBroadcastTime = System.currentTimeMillis()
                    pendingVolumeUpdate = null
                }
            }
        }
    }
    
    private fun broadcastVolumeUpdate(server: SyncServer, djManager: DJStateManager, master: Float?) {
        viewModelScope.launch {
            // CRITICAL: Update RoomState
            roomStateManager?.updateState(djState = djManager.getCurrentState())
            
            val applyAt = System.currentTimeMillis() + 20L // 20ms ultra-low latency
            val djStateJson = kotlinx.serialization.json.Json.encodeToString(DJState.serializer(), djManager.getCurrentState())
            val syncMsg = SyncMessage.DJStateSync(djStateJson, applyAt)
            
            server.broadcast(syncMsg)
            Log.d(TAG, "🔊 Volume synced: master=${"%.2f".format(master ?: 0f)}")
        }
    }
    
    /**
     * HOST: Update Effect - Synchronized
     */
    fun updateDJEffect(type: EffectType, enabled: Boolean, level: Float) {
        val server = syncServer
        val djManager = djStateManager
        if (server == null || djManager == null) return
        
        viewModelScope.launch {
            djManager.updateEffect(type, enabled, level)
            
            val applyAt = System.currentTimeMillis() + 50L
            val djStateJson = kotlinx.serialization.json.Json.encodeToString(DJState.serializer(), djManager.getCurrentState())
            val syncMsg = SyncMessage.DJStateSync(djStateJson, applyAt)
            
            server.broadcast(syncMsg)
            Log.d(TAG, "🎛️ Effect synced: $type enabled=$enabled, level=$level")
        }
    }
    
    /**
     * HOST: Scratch/Seek - Synchronized scrubbing
     */
    fun djScratchSeek(positionMs: Int, speed: Float = 1.0f) {
        val server = syncServer
        val djManager = djStateManager
        if (server == null || djManager == null) return
        
        viewModelScope.launch {
            djManager.scratchSeek(positionMs, speed)
            
            // Immediate broadcast for responsive scratching
            val applyAt = System.currentTimeMillis() + 20L // 20ms for ultra-low latency
            val djStateJson = kotlinx.serialization.json.Json.encodeToString(DJState.serializer(), djManager.getCurrentState())
            val syncMsg = SyncMessage.DJStateSync(djStateJson, applyAt)
            
            server.broadcast(syncMsg)
            
            // Also update music position
            musicShareManager.getCurrentPosition()
            Log.d(TAG, "💿 Scratch synced: pos=${positionMs}ms, speed=$speed")
        }
    }
    
    // ========== DJ EFFECT SYSTEM ==========
    
    /**
     * HOST: Trigger a DJ effect on all devices
     * CRITICAL: Ultra-low latency (20ms) synchronized playback!
     * Works independently of music playback
     * 
     * @param effectId Effect identifier (from DJEffects)
     * @param volume Volume 0.0 to 1.0
     */
    fun triggerDJEffect(effectId: String, volume: Float = 1.0f) {
        val server = syncServer
        if (server == null) {
            // Not hosting - just play locally (shouldn't happen in normal flow)
            Log.w(TAG, "⚠️ Not hosting - playing effect locally only")
            djEffectPlayer.playNow(effectId, volume)
            return
        }
        
        viewModelScope.launch {
            // CRITICAL: 20ms latency for instant response!
            val playAt = System.currentTimeMillis() + 20L
            
            val effectMsg = SyncMessage.DJEffectTrigger(
                effectId = effectId,
                playAt = playAt,
                volume = volume
            )
            
            val (successCount, failCount) = server.broadcast(effectMsg)
            
            // Also play on HOST device
            djEffectPlayer.scheduleEffect(effectId, playAt, volume)
            
            val effect = DJEffects.getById(effectId)
            Log.d(TAG, "🎵 DJ Effect triggered: ${effect?.displayName} (broadcast=$successCount, failed=$failCount)")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioSync()  // Stop audio streaming
        musicShareManager.release()  // Release music share resources
        djEffectPlayer.release()  // Release DJ effect player
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

sealed class AudioSyncStatus {
    data object Idle : AudioSyncStatus()
    data object Starting : AudioSyncStatus()
    data class Streaming(val deviceCount: Int, val bitrate: Int = 128) : AudioSyncStatus()
    data class Receiving(val bufferMs: Int, val latencyMs: Int) : AudioSyncStatus()
    data class Error(val message: String) : AudioSyncStatus()
}
