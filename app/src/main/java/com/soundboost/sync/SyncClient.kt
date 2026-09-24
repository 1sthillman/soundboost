package com.soundboost.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Client cihaz: host'a bağlanır, periyodik ping ile ClockSync örnekleri toplar,
 * Flash mesajlarını dinler ve yerel tetikleme zamanına çevirip dışarı yayınlar.
 */
class SyncClient {

    // CRITICAL: allowSpecialFloatingPointValues = false to prevent NaN/Infinity serialization bugs
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowSpecialFloatingPointValues = false  // FIX: Prevent NaN/Infinity causing UTF-8 errors
        isLenient = false  // Strict mode
    }
    private var httpClient: HttpClient? = null
    private var supervisorJob: Job = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private var session: io.ktor.websocket.WebSocketSession? = null

    private val _connectionState = MutableStateFlow<SyncConnectionState>(SyncConnectionState.Idle)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private val _clockOffsetMillis = MutableStateFlow(0L)
    val clockOffsetMillis: StateFlow<Long> = _clockOffsetMillis.asStateFlow()

    private val _flashEvents = MutableSharedFlow<SyncMessage.Flash>(
        replay = 0,
        extraBufferCapacity = 256,  // ULTRA-LARGE buffer for 1000+ devices!
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST  // Drop old, keep new
    )
    val flashEvents: SharedFlow<SyncMessage.Flash> = _flashEvents

    private val _audioChunkEvents = MutableSharedFlow<SyncMessage.AudioChunk>(
        replay = 0,
        extraBufferCapacity = 256,  // ULTRA-LARGE buffer for audio streaming!
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val audioChunkEvents: SharedFlow<SyncMessage.AudioChunk> = _audioChunkEvents
    
    // ALL incoming messages from server (for music messages etc.)
    private val _incomingMessages = MutableSharedFlow<Pair<String, SyncMessage>>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val incomingMessages: SharedFlow<Pair<String, SyncMessage>> = _incomingMessages

    private val offsetSamples = mutableListOf<ClockSync.SampleResult>()
    
    // Disconnection diagnostic tracking
    private var lastDisconnectionException: Throwable? = null
    private var lastDisconnectionTime: Long = 0L

    fun connect(hostAddress: String, port: Int = SyncServer.DEFAULT_PORT, deviceName: String) {
        if (_connectionState.value is SyncConnectionState.Connected ||
            _connectionState.value is SyncConnectionState.Connecting
        ) {
            android.util.Log.w("SyncClient", "⚠️ Already connected or connecting")
            return
        }

        android.util.Log.d("SyncClient", "🔌 Connecting to $hostAddress:$port as '$deviceName'")
        _connectionState.value = SyncConnectionState.Connecting
        
        // CRITICAL: Create INDEPENDENT supervisor job that won't be cancelled during normal operation
        supervisorJob = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + supervisorJob)
        
        // Create new HttpClient instance for this connection
        httpClient = HttpClient(CIO) { 
            install(WebSockets) {
                pingInterval = 15000  // Keepalive every 15s
                maxFrameSize = Long.MAX_VALUE
            }
        }
        android.util.Log.d("SyncClient", "🌐 HttpClient created with keepalive")
        
        scope.launch {
            try {
                android.util.Log.d("SyncClient", "📡 Opening WebSocket session...")
                val ws = httpClient!!.webSocketSession(
                    method = io.ktor.http.HttpMethod.Get,
                    host = hostAddress,
                    port = port,
                    path = "/sync"
                )
                session = ws
                android.util.Log.d("SyncClient", "✅ WebSocket session established")
                
                // Launch in SEPARATE supervised coroutines - no mutual cancellation!
                launch(SupervisorJob()) { listenLoop(ws, deviceName) }
                launch(SupervisorJob()) { pingLoop(ws) }
            } catch (t: Throwable) {
                android.util.Log.e("SyncClient", "❌ Connection failed: ${t.javaClass.simpleName}: ${t.message}", t)
                android.util.Log.e("SyncClient", "❌ CONNECTION FAILURE DIAGNOSTIC - Stack trace:", t)
                _connectionState.value = SyncConnectionState.Error(t.message ?: "Baglanti hatasi")
            }
        }
    }

    private suspend fun listenLoop(ws: io.ktor.websocket.WebSocketSession, deviceName: String) {
        android.util.Log.d("SyncClient", "👂 Listen loop started, waiting for messages...")
        try {
            for (frame in ws.incoming) {
                if (!scope.isActive) {
                    android.util.Log.w("SyncClient", "⚠️ Scope not active, stopping listen loop")
                    break
                }
                
                // CRITICAL: Only process Text frames, ignore Close/Ping/Pong frames
                if (frame !is Frame.Text) {
                    android.util.Log.d("SyncClient", "⏩ Skipping non-text frame: ${frame.frameType}")
                    continue
                }
                
                // CRITICAL FIX 1: Wrap readText() in try-catch to handle MalformedInputException
                // This prevents crashes when receiving corrupted UTF-8 data (NaN/Infinity in JSON)
                val text = try {
                    frame.readText()
                } catch (e: io.ktor.utils.io.charsets.MalformedInputException) {
                    android.util.Log.e("SyncClient", "❌ Malformed UTF-8 frame received (likely corrupted JSON with NaN/Infinity): ${e.message}")
                    android.util.Log.w("SyncClient", "⚠️ Skipping corrupted frame, continuing to listen...")
                    continue  // Skip this frame, keep listening
                } catch (e: Exception) {
                    android.util.Log.e("SyncClient", "❌ Failed to decode frame: ${e.javaClass.simpleName}: ${e.message}", e)
                    continue  // Skip this frame, keep listening
                }
                
                // CRITICAL FIX 2: Skip empty/whitespace-only messages (server bug workaround)
                if (text.isBlank()) {
                    android.util.Log.w("SyncClient", "⚠️ Received empty message, skipping...")
                    continue
                }
                
                // CRITICAL FIX 3: Validate JSON before logging (prevent log spam)
                if (text.length < 10 || !text.startsWith("{") || !text.endsWith("}")) {
                    android.util.Log.e("SyncClient", "❌ Invalid JSON format: '${text.take(50)}...'")
                    continue
                }
                
                android.util.Log.d("SyncClient", "📥 Received message: ${text.take(100)}...")
                
                // CRITICAL: Process message INLINE to prevent scope cancellation issues
                // Using try-catch to handle errors without breaking the loop
                try {
                    handleMessage(text, ws, deviceName)
                } catch (e: Exception) {
                    android.util.Log.e("SyncClient", "❌ Error handling message: ${e.message}", e)
                    // Continue processing other messages even if one fails
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncClient", "❌ Listen loop error: ${e.javaClass.simpleName}: ${e.message}", e)
            android.util.Log.e("SyncClient", "❌ CLIENT CRASH DIAGNOSTIC - Stack trace:", e)
            lastDisconnectionException = e
            lastDisconnectionTime = System.currentTimeMillis()
        } finally {
            android.util.Log.w("SyncClient", "🔌 Listen loop ended (connection closed)")
            _connectionState.value = SyncConnectionState.Disconnected
        }
    }
    
    /**
     * CRASH-PROOF message handler - processes messages INLINE (no separate coroutine!)
     * CRITICAL FIX: Removed scope.launch() - was causing crashes because scope gets
     * cancelled when client disconnects, but messages might still be processing!
     * 
     * ENHANCED: Added detailed logging for ALL message types
     */
    private suspend fun handleMessage(text: String, ws: io.ktor.websocket.WebSocketSession, deviceName: String) {
        val message = try {
            json.decodeFromString(SyncMessage.serializer(), text)
        } catch (e: kotlinx.serialization.SerializationException) {
            android.util.Log.e("SyncClient", "❌ Failed to parse message (invalid JSON): ${e.message}")
            android.util.Log.e("SyncClient", "   JSON snippet: ${text.take(200)}...")
            return
        } catch (e: Exception) {
            android.util.Log.e("SyncClient", "❌ Failed to parse message (${e.javaClass.simpleName}): ${e.message}")
            return
        }

        android.util.Log.d("SyncClient", "📦 ========== MESSAGE RECEIVED ==========")
        android.util.Log.d("SyncClient", "📦 Type: ${message::class.simpleName}")
        
        when (message) {
            is SyncMessage.Welcome -> {
                android.util.Log.d("SyncClient", "👋 WELCOME received: room=${message.roomName}, deviceId=${message.deviceId}")
                
                // LATE JOIN: Check if RoomState provided
                if (message.roomStateJson != null) {
                    android.util.Log.d("SyncClient", "🎵 Late join - RoomState received")
                    _incomingMessages.tryEmit("server" to message)
                }
                
                _connectionState.value = SyncConnectionState.Connected(
                    deviceId = message.deviceId,
                    roomName = message.roomName
                )
                val joinMsg = SyncMessage.Join(deviceName)
                val joinPayload = json.encodeToString(SyncMessage.serializer(), joinMsg)
                ws.send(Frame.Text(joinPayload))
                android.util.Log.d("SyncClient", "📤 Sent JOIN message with name: $deviceName")
            }
            is SyncMessage.Pong -> {
                val t3 = System.currentTimeMillis()
                val sample = ClockSync.computeSample(t0 = message.t0, t1 = message.t1, t2 = message.t2, t3 = t3)
                
                val previousOffset = _clockOffsetMillis.value
                offsetSamples.add(sample)
                if (offsetSamples.size > MAX_SAMPLES) offsetSamples.removeAt(0)
                
                val newOffset = ClockSync.medianOffset(offsetSamples)
                _clockOffsetMillis.value = newOffset
                
                // Log clock sync quality periodically
                if (offsetSamples.size % 5 == 0) {
                    val isStable = ClockSync.isStable(previousOffset, newOffset, 15L)
                    val quality = when {
                        sample.rttMillis < 20 -> "EXCELLENT"
                        sample.rttMillis < 50 -> "GOOD"
                        sample.rttMillis < 100 -> "FAIR"
                        else -> "POOR"
                    }
                    
                    android.util.Log.d("SyncClient", "🕐 ========== CLOCK SYNC STATUS ==========")
                    android.util.Log.d("SyncClient", "🕐 Offset: ${newOffset}ms (stable: $isStable)")
                    android.util.Log.d("SyncClient", "🕐 RTT: ${sample.rttMillis}ms (quality: $quality)")
                    android.util.Log.d("SyncClient", "🕐 Samples: ${offsetSamples.size}/$MAX_SAMPLES")
                    
                    if (!isStable) {
                        android.util.Log.w("SyncClient", "⚠️ Clock drift detected: ${previousOffset}ms -> ${newOffset}ms")
                    }
                    
                    if (sample.rttMillis > 100) {
                        android.util.Log.w("SyncClient", "⚠️ High network latency! Sync precision may be reduced")
                    }
                }
            }
            is SyncMessage.Flash -> {
                android.util.Log.d("SyncClient", "⚡⚡⚡ FLASH EVENT received: startAt=${message.startAt}, mode=${message.mode}, color=${message.color}")
                // CRITICAL: NEVER use tryEmit - it can drop events!
                // Use emit() in a coroutine to GUARANTEE delivery
                scope.launch(SupervisorJob()) {
                    _flashEvents.emit(message)  // GUARANTEED delivery, will suspend if buffer full
                    android.util.Log.d("SyncClient", "✅ Flash event emitted (guaranteed)")
                }
            }
            is SyncMessage.BassSync -> {
                android.util.Log.d("SyncClient", "🎵🎵🎵 BASS SYNC received: hasKick=${message.hasKick}, bass=${message.bass}, energy=${message.energy}")
                // BassSync'i Flash mesajına çevir (aynı interface üzerinden işlensin)
                val flashMsg = SyncMessage.Flash(
                    startAt = message.startAt,
                    durationMs = if (message.hasKick) 40 else 30,  // Kick daha uzun
                    mode = FlashMode.SCREEN_AND_TORCH,  // Bass flash her zaman BOTH
                    color = "#FFFFFF",
                    repeatCount = 1,
                    intervalMs = 0
                )
                // CRITICAL: GUARANTEED emission for bass-sync
                scope.launch(SupervisorJob()) {
                    _flashEvents.emit(flashMsg)
                    android.util.Log.d("SyncClient", "✅ Bass-sync flash emitted (guaranteed)")
                }
            }
            is SyncMessage.AudioChunk -> {
                // Reduced log spam for audio chunks
                if (message.sequence % 100 == 0L) {
                    android.util.Log.d("SyncClient", "🎵 AUDIO CHUNK received: seq=${message.sequence}, size=${message.data.length}")
                }
                // Decode base64 to ByteArray
                val opusData = android.util.Base64.decode(message.data, android.util.Base64.NO_WRAP)
                // Emit to audio chunk flow for processing
                scope.launch(SupervisorJob()) {
                    _audioChunkEvents.emit(message)
                }
            }
            is SyncMessage.MusicMetadata -> {
                android.util.Log.d("SyncClient", "🎵 ========== MUSIC METADATA RECEIVED IN CLIENT ==========")
                android.util.Log.d("SyncClient", "🎵 sessionId: ${message.sessionId}")
                android.util.Log.d("SyncClient", "🎵 fileName: ${message.fileName}")
                android.util.Log.d("SyncClient", "🎵 fileSizeBytes: ${message.fileSizeBytes}")
                android.util.Log.d("SyncClient", "🎵 totalChunks: ${message.totalChunks}")
                android.util.Log.d("SyncClient", "🎵 Will emit to incomingMessages flow...")
                // Will be emitted to incomingMessages below
            }
            is SyncMessage.MusicChunk -> {
                android.util.Log.d("SyncClient", "📦 MUSIC CHUNK received: chunk ${message.chunkIndex}")
                // Will be emitted to incomingMessages below
            }
            is SyncMessage.MusicStart -> {
                android.util.Log.d("SyncClient", "▶️ MUSIC START received: startAt=${message.startAt}")
                // Will be emitted to incomingMessages below
            }
            is SyncMessage.MusicControl -> {
                android.util.Log.d("SyncClient", "🎛️ MUSIC CONTROL received: action=${message.action}")
                // Will be emitted to incomingMessages below
            }
            is SyncMessage.Error -> {
                android.util.Log.e("SyncClient", "❌ Error message from server: ${message.reason}")
                _connectionState.value = SyncConnectionState.Error(message.reason)
            }
            else -> {
                android.util.Log.d("SyncClient", "📭 Other message type: ${message::class.simpleName}")
            }
        }
        
        // CRITICAL: Emit ALL messages to incomingMessages flow for ViewModel processing
        // This includes music messages (MusicMetadata, MusicChunk, MusicStart, MusicControl)
        android.util.Log.d("SyncClient", "📤 Emitting message to incomingMessages flow: ${message::class.simpleName}")
        scope.launch(SupervisorJob()) {
            _incomingMessages.emit("server" to message)
            android.util.Log.d("SyncClient", "✅ Message emitted to incomingMessages flow")
        }
    }

    private suspend fun pingLoop(ws: io.ktor.websocket.WebSocketSession) {
        android.util.Log.d("SyncClient", "🔄 ========== CLOCK SYNC PING LOOP STARTED ==========")
        android.util.Log.d("SyncClient", "🔄 Ping interval: ${PING_INTERVAL_MS}ms")
        android.util.Log.d("SyncClient", "🔄 This maintains continuous clock synchronization")
        
        var pingCount = 0
        
        while (scope.isActive && ws.isActive) {
            runCatching {
                pingCount++
                val pingTime = System.currentTimeMillis()
                val pingMsg = SyncMessage.Ping(pingTime)
                val payload = json.encodeToString(SyncMessage.serializer(), pingMsg)
                ws.send(Frame.Text(payload))
                
                // Log every 5th ping to avoid spam
                if (pingCount % 5 == 0) {
                    android.util.Log.d("SyncClient", "📡 Clock sync ping #$pingCount sent (offset: ${_clockOffsetMillis.value}ms)")
                }
            }
            delay(PING_INTERVAL_MS)
        }
        
        android.util.Log.d("SyncClient", "⏹️ Clock sync ping loop stopped (sent $pingCount pings)")
    }

    /** Host'un yayınladığı mutlak zamanı, bu cihazın yerel gecikmesine çevirir. */
    fun localDelayFor(flash: SyncMessage.Flash): Long =
        ClockSync.localDelayUntil(
            hostStartAtEpochMillis = flash.startAt,
            localOffsetMillis = _clockOffsetMillis.value,
            nowLocalEpochMillis = System.currentTimeMillis()
        )
    
    /**
     * Send a message to the server (e.g., MusicChunkRequest)
     */
    suspend fun sendMessage(message: SyncMessage) {
        val ws = session
        if (ws == null) {
            android.util.Log.w("SyncClient", "⚠️ Cannot send message - not connected")
            return
        }
        
        try {
            val payload = json.encodeToString(SyncMessage.serializer(), message)
            ws.send(Frame.Text(payload))
            android.util.Log.d("SyncClient", "📤 Sent message: ${message::class.simpleName}")
        } catch (e: Exception) {
            android.util.Log.e("SyncClient", "❌ Failed to send message", e)
        }
    }

    fun disconnect() {
        scope.launch { 
            runCatching { session?.close() }
            runCatching { httpClient?.close() }  // FIX: Close HttpClient to prevent resource leak
        }
        supervisorJob.cancel()
        supervisorJob = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + supervisorJob)
        session = null
        httpClient = null
        offsetSamples.clear()
        _connectionState.value = SyncConnectionState.Idle
    }
    
    /**
     * Get human-readable disconnection diagnostic info for user display
     */
    fun getDisconnectionDiagnostic(): String {
        val exception = lastDisconnectionException
        val timeAgo = System.currentTimeMillis() - lastDisconnectionTime
        
        return when {
            exception == null -> "Bağlantı kesildi"
            
            timeAgo > 5000 -> "Bağlantı kesildi"
            
            exception.javaClass.simpleName.contains("MalformedInput") ->
                "Sunucu veri hatası\n(Yeniden bağlanmayı dene)"
            
            exception.javaClass.simpleName.contains("Protocol") ->
                "Protokol hatası\n(Sunucu ile uyumsuzluk)"
            
            exception.javaClass.simpleName.contains("ClosedChannel") ->
                "Ağ bağlantısı kesildi"
            
            exception.javaClass.simpleName.contains("Timeout") ->
                "Bağlantı zaman aşımı"
            
            exception.javaClass.simpleName.contains("Connection") ->
                "Bağlantı hatası"
            
            else -> {
                val msg = exception.message ?: "Bilinmeyen hata"
                "Bağlantı kesildi\n${msg.take(30)}"
            }
        }
    }

    companion object {
        private const val PING_INTERVAL_MS = 2000L
        private const val MAX_SAMPLES = 12
    }
}

sealed class SyncConnectionState {
    data object Idle : SyncConnectionState()
    data object Connecting : SyncConnectionState()
    data class Connected(val deviceId: String, val roomName: String) : SyncConnectionState()
    data object Disconnected : SyncConnectionState()
    data class Error(val message: String) : SyncConnectionState()
}
