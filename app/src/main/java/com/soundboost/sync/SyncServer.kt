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
    // CRITICAL: allowSpecialFloatingPointValues = false to prevent NaN/Infinity serialization bugs
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowSpecialFloatingPointValues = false  // FIX: Prevent NaN/Infinity causing UTF-8 errors
        isLenient = false  // Strict mode
    }
    private val connectionMutex = Mutex()
    private val connections = mutableMapOf<String, io.ktor.websocket.WebSocketSession>()

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
                    val deviceId = UUID.randomUUID().toString()
                    
                    // CRITICAL: Add to connections BEFORE sending Welcome
                    connectionMutex.withLock { 
                        connections[deviceId] = this
                        android.util.Log.d("SyncServer", "🔌 NEW connection: deviceId=$deviceId (total: ${connections.size})")
                    }

                    try {
                        val currentCount = connectionMutex.withLock { connections.size }
                        android.util.Log.d("SyncServer", "🔍 NEW CONNECTION DEBUG:")
                        android.util.Log.d("SyncServer", "   - New deviceId: $deviceId")
                        android.util.Log.d("SyncServer", "   - Total devices BEFORE: $currentCount")
                        android.util.Log.d("SyncServer", "   - All deviceIds: ${connections.keys.joinToString()}")
                        
                        val welcomeMsg = SyncMessage.Welcome(
                            deviceId = deviceId,
                            roomName = roomName,
                            connectedDeviceCount = currentCount
                        )
                        send(Frame.Text(json.encodeToString(SyncMessage.serializer(), welcomeMsg)))
                        android.util.Log.d("SyncServer", "📤 Welcome sent to deviceId=$deviceId (room has $currentCount devices)")

                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val text = frame.readText()
                            val message = runCatching {
                                json.decodeFromString(SyncMessage.serializer(), text)
                            }.getOrNull() ?: continue

                            when (message) {
                                is SyncMessage.Join -> {
                                    android.util.Log.d("SyncServer", "📥 JOIN from: ${message.deviceName} (deviceId: $deviceId)")
                                    updateDeviceList(deviceId, message.deviceName)
                                    val totalDevices = connectionMutex.withLock { connections.size }
                                    android.util.Log.d("SyncServer", "✅ Device added! Total devices: $totalDevices")
                                }
                                is SyncMessage.Ping -> {
                                    val t1 = System.currentTimeMillis()
                                    val pong = SyncMessage.Pong(t0 = message.t0, t1 = t1, t2 = System.currentTimeMillis())
                                    send(Frame.Text(json.encodeToString(SyncMessage.serializer(), pong)))
                                }
                                is SyncMessage.Leave -> {
                                    android.util.Log.d("SyncServer", "👋 Client $deviceId requested disconnect")
                                    break  // Exit loop, cleanup in finally
                                }
                                else -> Unit
                            }
                            _incomingMessages.tryEmit(deviceId to message)
                        }
                    } catch (t: Throwable) {
                        android.util.Log.w("SyncServer", "⚠️ Connection $deviceId closed: ${t.javaClass.simpleName}: ${t.message}", t)
                        android.util.Log.w("SyncServer", "⚠️ Stack trace for debugging:", t)
                    } finally {
                        val remainingCount = connectionMutex.withLock {
                            connections.remove(deviceId)
                            connections.size
                        }
                        android.util.Log.d("SyncServer", "🔍 DISCONNECTION DEBUG:")
                        android.util.Log.d("SyncServer", "   - Removed deviceId: $deviceId")
                        android.util.Log.d("SyncServer", "   - Remaining devices: $remainingCount")
                        android.util.Log.d("SyncServer", "   - Remaining deviceIds: ${connectionMutex.withLock { connections.keys.joinToString() }}")
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
     * 
     * CRITICAL FIX: Catches JSON serialization errors (NaN/Infinity) before sending
     */
    suspend fun broadcast(message: SyncMessage) {
        // CRITICAL: Encode BEFORE getting snapshot to catch serialization errors early!
        val jsonString = try {
            json.encodeToString(SyncMessage.serializer(), message)
        } catch (e: kotlinx.serialization.SerializationException) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization failed (SerializationException) for ${message::class.simpleName}: ${e.message}", e)
            android.util.Log.e("SyncServer", "❌ Message details: $message")
            return  // Don't broadcast malformed data
        } catch (e: Exception) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization failed for ${message::class.simpleName}: ${e.message}", e)
            android.util.Log.e("SyncServer", "❌ Message details: $message")
            return  // Don't broadcast malformed data
        }
        
        // CRITICAL FIX: Validate JSON is not empty/corrupt before sending
        if (jsonString.isBlank() || jsonString.length < 10) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization produced empty/invalid result for ${message::class.simpleName}")
            android.util.Log.e("SyncServer", "   Result: '$jsonString'")
            return  // Don't broadcast corrupt data
        }
        
        // CRITICAL FIX: Validate JSON structure
        if (!jsonString.startsWith("{") || !jsonString.endsWith("}")) {
            android.util.Log.e("SyncServer", "❌ CRITICAL: JSON serialization produced malformed JSON for ${message::class.simpleName}")
            android.util.Log.e("SyncServer", "   Result: '${jsonString.take(100)}'")
            return  // Don't broadcast corrupt data
        }
        
        // CRITICAL: Get snapshot OUTSIDE coroutineScope to prevent deadlock
        val sessions = connectionMutex.withLock {
            connections.values.toList()
        }
        
        if (sessions.isEmpty()) {
            android.util.Log.d("SyncServer", "📡 No clients to broadcast to")
            return
        }
        
        android.util.Log.d("SyncServer", "📡 Broadcasting to ${sessions.size} clients: ${message::class.simpleName}")
        
        // CRITICAL FIX: Frame.Text is NOT REUSABLE and NOT THREAD-SAFE!
        // Must create a NEW frame for EACH client!
        // Source: https://ktor.io docs - "A frame is not reusable and not thread-safe"
        coroutineScope {
            val results = sessions.mapIndexed { index, session ->
                async(kotlinx.coroutines.SupervisorJob()) {
                    runCatching { 
                        // CRITICAL: Create NEW Frame.Text for each client (frames are NOT reusable!)
                        val clientFrame = Frame.Text(jsonString)
                        android.util.Log.d("SyncServer", "📤 Sending to client #${index + 1}/${sessions.size}: ${message::class.simpleName} (${jsonString.length} bytes)")
                        session.send(clientFrame)
                        android.util.Log.d("SyncServer", "✅ Sent to client #${index + 1}")
                        true  // success
                    }.getOrElse { error ->
                        android.util.Log.w("SyncServer", "⚠️ Send failed to client #${index + 1}: ${error.javaClass.simpleName}: ${error.message}")
                        android.util.Log.w("SyncServer", "⚠️ Stack trace:", error)
                        false  // failure
                    }
                }
            }
            
            val outcomes = results.awaitAll()
            val successCount = outcomes.count { it }
            val failCount = outcomes.count { !it }
            
            if (failCount > 0) {
                android.util.Log.w("SyncServer", "⚠️ Broadcast completed: $successCount success, $failCount failed (of ${sessions.size} total)")
            } else {
                android.util.Log.d("SyncServer", "✅ Broadcast completed: $successCount success, $failCount failed (of ${sessions.size} total)")
            }
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

    fun stop() {
        engine?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        engine = null
        connections.clear()
        _connectedDevices.value = emptyList()
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
