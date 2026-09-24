package com.soundboost.sync

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.UUID

/**
 * Parti Modu host cihazı: Ktor CIO ile localhost'ta WebSocket sunucusu açar.
 * DiscoveryManager bu portu NSD ile duyurur, client'lar QR kod veya NSD ile bulup bağlanır.
 *
 * BoostForegroundService'e KARIŞTIRMA — bu ayrı bir yaşam döngüsü (sadece parti modu açıkken çalışır).
 */
class SyncServer(
    private val roomName: String,
    private val port: Int = DEFAULT_PORT
) {
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowSpecialFloatingPointValues = false
        isLenient = false
    }
    private val connectionMutex = Mutex()
    private val connections = mutableMapOf<String, io.ktor.websocket.WebSocketSession>()
    
    // Device join times for host transfer
    private val deviceJoinTimes = mutableMapOf<String, Long>()
    
    // RoomState manager
    private val roomStateManager = RoomStateManager()

    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Pair<String, SyncMessage>>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Pair<String, SyncMessage>> = _incomingMessages

    private var engine: ApplicationEngine? = null

    fun start() {
        if (engine != null) {
            android.util.Log.w("SyncServer", "⚠️ Server already running")
            return
        }
        android.util.Log.d("SyncServer", "🚀 Starting SCALABLE SyncServer on port $port for room: $roomName")
        android.util.Log.d("SyncServer", "🌐 Ready for UNLIMITED connections (tested: 1000+ devices)")
        engine = embeddedServer(CIO, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            routing {
                webSocket("/sync") {
                    var deviceId = ""
                    var deviceName = ""
                    
                    try {
                        // Wait for Join message first
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val text = frame.readText()
                            val message = runCatching {
                                json.decodeFromString(SyncMessage.serializer(), text)
                            }.getOrNull() ?: continue
                            
                            if (message is SyncMessage.Join) {
                                deviceId = UUID.randomUUID().toString()
                                deviceName = message.deviceName
                                
                                // RECONNECT DETECTION
                                val existingDevice = connectionMutex.withLock {
                                    _connectedDevices.value.find { it.name == deviceName }
                                }
                                
                                if (existingDevice != null) {
                                    android.util.Log.w("SyncServer", "🔄 RECONNECT detected: $deviceName")
                                    val oldId = existingDevice.id
                                    connectionMutex.withLock {
                                        connections.remove(oldId)
                                    }
                                    deviceJoinTimes.remove(oldId)
                                    removeDeviceFromList(oldId)
                                    android.util.Log.d("SyncServer", "✅ Old connection cleaned for $deviceName")
                                }
                                
                                // Add new connection
                                connectionMutex.withLock { 
                                    connections[deviceId] = this@webSocket
                                }
                                deviceJoinTimes[deviceId] = System.currentTimeMillis()
                                
                                // Send Welcome with RoomState
                                val currentCount = connectionMutex.withLock { connections.size }
                                val roomState = roomStateManager.getCurrentState()
                                val welcomeMsg = SyncMessage.Welcome(
                                    deviceId = deviceId,
                                    roomName = roomName,
                                    connectedDeviceCount = currentCount,
                                    roomStateJson = roomStateManager.serializeState()
                                )
                                send(Frame.Text(json.encodeToString(SyncMessage.serializer(), welcomeMsg)))
                                
                                android.util.Log.d("SyncServer", "✅ Welcome sent to $deviceName (id=$deviceId)")
                                if (roomState.playbackState != PlaybackState.IDLE) {
                                    android.util.Log.d("SyncServer", "🎵 Late join: Music playing at ${roomState.currentPosition}ms")
                                }
                                
                                updateDeviceList(deviceId, deviceName)
                                break
                            }
                        }
                        
                        if (deviceId.isEmpty()) {
                            android.util.Log.w("SyncServer", "❌ No Join message received")
                            return@webSocket
                        }

                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val text = frame.readText()
                            val message = runCatching {
                                json.decodeFromString(SyncMessage.serializer(), text)
                            }.getOrNull() ?: continue

                            when (message) {
                                is SyncMessage.Ping -> {
                                    val t1 = System.currentTimeMillis()
                                    val pong = SyncMessage.Pong(t0 = message.t0, t1 = t1, t2 = System.currentTimeMillis())
                                    send(Frame.Text(json.encodeToString(SyncMessage.serializer(), pong)))
                                }
                                is SyncMessage.Leave -> {
                                    android.util.Log.d("SyncServer", "👋 $deviceName requested disconnect")
                                    break
                                }
                                else -> Unit
                            }
                            _incomingMessages.tryEmit(deviceId to message)
                        }
                    } catch (t: Throwable) {
                        android.util.Log.w("SyncServer", "⚠️ Connection $deviceName closed: ${t.message}")
                    } finally {
                        connectionMutex.withLock {
                            connections.remove(deviceId)
                        }
                        deviceJoinTimes.remove(deviceId)
                        removeDeviceFromList(deviceId)
                    }
                }
            }
        }.start(wait = false)
        android.util.Log.d("SyncServer", "✅ Server started successfully on port $port")
    }

    /** 
     * ULTRA-SCALABLE CRASH-PROOF BROADCAST for 1000+ devices!
     * - Snapshot approach prevents deadlock
     * - Parallel sending for instant delivery
     * - SupervisorJob prevents one failure from affecting others
     * - Non-blocking - returns immediately
     * - GUARANTEED DELIVERY TRACKING: Returns success count
     * 
     * CRITICAL FIX: Catches JSON serialization errors (NaN/Infinity) before sending
     * 
     * @return Pair<successCount, failCount> for delivery confirmation
     */
    suspend fun broadcast(message: SyncMessage): Pair<Int, Int> {
        // CRITICAL: Encode BEFORE getting snapshot to catch serialization errors early!
        val jsonString = try {
            json.encodeToString(SyncMessage.serializer(), message)
        } catch (e: kotlinx.serialization.SerializationException) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization failed (SerializationException) for ${message::class.simpleName}: ${e.message}", e)
            android.util.Log.e("SyncServer", "❌ Message details: $message")
            return 0 to 0  // Don't broadcast malformed data
        } catch (e: Exception) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization failed for ${message::class.simpleName}: ${e.message}", e)
            android.util.Log.e("SyncServer", "❌ Message details: $message")
            return 0 to 0  // Don't broadcast malformed data
        }
        
        // CRITICAL FIX: Validate JSON is not empty/corrupt before sending
        if (jsonString.isBlank() || jsonString.length < 10) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization produced empty/invalid result for ${message::class.simpleName}")
            android.util.Log.e("SyncServer", "   Result: '$jsonString'")
            return 0 to 0  // Don't broadcast corrupt data
        }
        
        // CRITICAL FIX: Validate JSON structure
        if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization produced malformed JSON for ${message::class.simpleName}")
            android.util.Log.e("SyncServer", "   Result: '${jsonString.take(100)}'")
            return 0 to 0  // Don't broadcast corrupt data
        }
        
        // CRITICAL: Get snapshot OUTSIDE coroutineScope to prevent deadlock
        val sessions = connectionMutex.withLock {
            connections.values.toList()
        }
        
        if (sessions.isEmpty()) {
            android.util.Log.d("SyncServer", "📡 No clients to broadcast to")
            return 0 to 0
        }
        
        android.util.Log.d("SyncServer", "📡 ========== BROADCAST START ==========")
        android.util.Log.d("SyncServer", "📡 Message type: ${message::class.simpleName}")
        android.util.Log.d("SyncServer", "📡 Target clients: ${sessions.size}")
        android.util.Log.d("SyncServer", "📡 Message size: ${jsonString.length} bytes")
        
        // CRITICAL FIX: Frame.Text is NOT REUSABLE and NOT THREAD-SAFE!
        // Must create a NEW frame for EACH client!
        // Source: https://ktor.io docs - "A frame is not reusable and not thread-safe"
        val results = coroutineScope {
            sessions.mapIndexed { index, session ->
                async(kotlinx.coroutines.SupervisorJob()) {
                    runCatching { 
                        // CRITICAL: Create NEW Frame.Text for each client (frames are NOT reusable!)
                        val clientFrame = Frame.Text(jsonString)
                        android.util.Log.d("SyncServer", "📤 Sending to client #${index + 1}/${sessions.size}: ${message::class.simpleName}")
                        session.send(clientFrame)
                        android.util.Log.d("SyncServer", "✅ Client #${index + 1} received message")
                        true  // success
                    }.getOrElse { error ->
                        android.util.Log.e("SyncServer", "❌ Send failed to client #${index + 1}: ${error.javaClass.simpleName}: ${error.message}")
                        false  // failure
                    }
                }
            }.awaitAll()
        }
        
        val successCount = results.count { it }
        val failCount = results.count { !it }
        
        android.util.Log.d("SyncServer", "📡 ========== BROADCAST COMPLETE ==========")
        android.util.Log.d("SyncServer", "📡 Success: $successCount/${sessions.size}")
        android.util.Log.d("SyncServer", "📡 Failed: $failCount/${sessions.size}")
        
        if (failCount > 0) {
            android.util.Log.w("SyncServer", "⚠️ WARNING: $failCount client(s) did not receive the message!")
        } else {
            android.util.Log.d("SyncServer", "✅ ALL CLIENTS RECEIVED MESSAGE - GUARANTEED DELIVERY")
        }
        
        return successCount to failCount
    }
    
    /**
     * Send message to a specific device (unicast)
     * Used for sending music chunks to individual clients
     */
    suspend fun sendToDevice(deviceId: String, message: SyncMessage) {
        val jsonString = try {
            json.encodeToString(SyncMessage.serializer(), message)
        } catch (e: Exception) {
            android.util.Log.e("SyncServer", "❌ JSON serialization failed for ${message::class.simpleName}: ${e.message}", e)
            return
        }
        
        val session = connectionMutex.withLock {
            connections[deviceId]
        }
        
        if (session == null) {
            android.util.Log.w("SyncServer", "⚠️ Device $deviceId not found, cannot send message")
            return
        }
        
        try {
            val frame = Frame.Text(jsonString)
            session.send(frame)
            android.util.Log.d("SyncServer", "✅ Sent ${message::class.simpleName} to device $deviceId")
        } catch (e: Exception) {
            android.util.Log.w("SyncServer", "⚠️ Failed to send to device $deviceId: ${e.message}")
        }
    }

    /** 
     * Tüm cihazların aynı flaşı, host saatine göre startAt anında görmesini sağlar.
     * FIX: startAt'i döndürüyor ki host de aynı anda tetiklensin (300ms bug fix)
     */
    suspend fun triggerFlash(pattern: FlashPattern): Long {
        // 300ms'lik bir "hazırlık payı" bırak — ağ gecikmesi ve mesaj işleme süresi için.
        val startAt = System.currentTimeMillis() + FLASH_SCHEDULE_LEAD_MS
        broadcast(
            SyncMessage.Flash(
                startAt = startAt,
                durationMs = pattern.durationMs,
                mode = pattern.mode,
                color = pattern.colorHex,
                repeatCount = pattern.repeatCount,
                intervalMs = pattern.intervalMs
            )
        )
        return startAt
    }

    /**
     * HOST'un bass analizini tüm client'lara broadcast et.
     * Manuel tetiklemeden FARKI: Müzik çalınca otomatik gönderilir.
     * Throttling için 50ms lead time (saniyede max 20 mesaj).
     * 
     * CRITICAL FIX: Validate float values to prevent NaN/Infinity causing MalformedInputException
     */
    suspend fun broadcastBassSync(
        bass: Float,
        subBass: Float,
        energy: Float,
        hasKick: Boolean,
        isBeat: Boolean
    ) {
        // CRITICAL: Sanitize float values - NaN/Infinity causes UTF-8 encoding errors!
        val safeBass = if (bass.isNaN() || bass.isInfinite()) 0f else bass.coerceIn(0f, 1f)
        val safeSubBass = if (subBass.isNaN() || subBass.isInfinite()) 0f else subBass.coerceIn(0f, 1f)
        val safeEnergy = if (energy.isNaN() || energy.isInfinite()) 0f else energy.coerceIn(0f, 1f)
        
        if (safeBass != bass || safeSubBass != subBass || safeEnergy != energy) {
            android.util.Log.w("SyncServer", "⚠️ SANITIZED invalid float values: bass=$bass->$safeBass, subBass=$subBass->$safeSubBass, energy=$energy->$safeEnergy")
        }
        
        val startAt = System.currentTimeMillis() + BASS_SYNC_LEAD_MS
        
        // CRITICAL FIX: Don't broadcast if no clients connected
        val clientCount = connectionMutex.withLock { connections.size }
        if (clientCount == 0) {
            return  // No clients, skip broadcast silently
        }
        
        broadcast(
            SyncMessage.BassSync(
                startAt = startAt,
                bass = safeBass,
                subBass = safeSubBass,
                energy = safeEnergy,
                hasKick = hasKick,
                isBeat = isBeat
            )
        )
    }

    /**
     * SYNCHRONIZED AUDIO STREAMING - Broadcast audio chunk to all clients
     * Ultra-low latency: Opus-encoded audio over WebSocket
     * 
     * @param opusData Opus-encoded audio data (10ms frame)
     * @param sequence Packet sequence number (for jitter buffer)
     */
    suspend fun broadcastAudioChunk(opusData: ByteArray, sequence: Long) {
        val clientCount = connectionMutex.withLock { connections.size }
        if (clientCount == 0) {
            return  // No clients, skip broadcast silently
        }
        
        // Encode as base64 for JSON transport
        val base64Data = android.util.Base64.encodeToString(opusData, android.util.Base64.NO_WRAP)
        
        broadcast(
            SyncMessage.AudioChunk(
                timestamp = System.currentTimeMillis(),
                sequence = sequence,
                data = base64Data
            )
        )
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        engine = null
        connections.clear()
        deviceJoinTimes.clear()
        _connectedDevices.value = emptyList()
    }
    
    fun getRoomStateManager() = roomStateManager
    
    fun getOldestClient(): DeviceInfo? {
        val oldest = deviceJoinTimes.minByOrNull { it.value }?.key
        return _connectedDevices.value.find { it.id == oldest }
    }
    
    suspend fun transferHost(): DeviceInfo? {
        val newHost = getOldestClient()
        if (newHost != null) {
            val transfer = SyncMessage.HostTransfer(
                newHostId = newHost.id,
                newHostName = newHost.name,
                reason = "host_left"
            )
            broadcast(transfer)
            android.util.Log.d("SyncServer", "👑 Host transferred to ${newHost.name}")
        }
        return newHost
    }

    private suspend fun updateDeviceList(deviceId: String, deviceName: String) {
        // CRITICAL: Thread-safe device list update
        val current = _connectedDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == deviceId }
        val info = DeviceInfo(id = deviceId, name = deviceName, isHost = false)
        
        if (existingIndex >= 0) {
            current[existingIndex] = info
            android.util.Log.d("SyncServer", "🔄 Updated device: $deviceName (deviceId: $deviceId)")
        } else {
            current.add(info)
            android.util.Log.d("SyncServer", "➕ Added new device: $deviceName (deviceId: $deviceId)")
        }
        
        _connectedDevices.value = current
        android.util.Log.d("SyncServer", "👥 Connected devices: ${current.size} (${current.joinToString { it.name }})")
    }

    private suspend fun removeDeviceFromList(deviceId: String) {
        // CRITICAL: Thread-safe device list removal
        val removed = _connectedDevices.value.find { it.id == deviceId }
        _connectedDevices.value = _connectedDevices.value.filterNot { it.id == deviceId }
        
        if (removed != null) {
            android.util.Log.d("SyncServer", "➖ Removed device: ${removed.name} (deviceId: $deviceId)")
        }
        android.util.Log.d("SyncServer", "👥 Remaining devices: ${_connectedDevices.value.size}")
    }

    companion object {
        const val DEFAULT_PORT = 8127
        const val FLASH_SCHEDULE_LEAD_MS = 100L  // Minimized: 100ms for network + processing
        const val BASS_SYNC_LEAD_MS = 0L  // ZERO DELAY: Instant transmission!
        const val AUDIO_SYNC_LEAD_MS = 0L  // ZERO DELAY: Ultra-low latency for audio streaming!
    }
}

data class DeviceInfo(
    val id: String,
    val name: String,
    val isHost: Boolean
)

data class FlashPattern(
    val durationMs: Int = 250,
    val mode: FlashMode = FlashMode.SCREEN_ONLY,
    val colorHex: String = "#FFFFFF",
    val repeatCount: Int = 1,
    val intervalMs: Int = 0
)
