package com.soundboost.sync

import io.ktor.serialization.kotlinx.json.json
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val connectionMutex = Mutex()
    private val connections = mutableMapOf<String, io.ktor.websocket.WebSocketSession>()

    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<Pair<String, SyncMessage>>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Pair<String, SyncMessage>> = _incomingMessages

    private var engine: ApplicationEngine? = null

    fun start() {
        if (engine != null) return
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
                    connectionMutex.withLock { connections[deviceId] = this }

                    try {
                        send(Frame.Text(json.encodeToString(SyncMessage.serializer(), SyncMessage.Welcome(
                            deviceId = deviceId,
                            roomName = roomName,
                            connectedDeviceCount = connections.size
                        ))))

                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val text = frame.readText()
                            val message = runCatching {
                                json.decodeFromString(SyncMessage.serializer(), text)
                            }.getOrNull() ?: continue

                            when (message) {
                                is SyncMessage.Join -> {
                                    updateDeviceList(deviceId, message.deviceName)
                                }
                                is SyncMessage.Ping -> {
                                    val t1 = System.currentTimeMillis()
                                    val pong = SyncMessage.Pong(t0 = message.t0, t1 = t1, t2 = System.currentTimeMillis())
                                    send(Frame.Text(json.encodeToString(SyncMessage.serializer(), pong)))
                                }
                                else -> Unit
                            }
                            _incomingMessages.tryEmit(deviceId to message)
                        }
                    } catch (t: Throwable) {
                        // Bağlantı düştü — normal, temizleme finally'de yapılır.
                    } finally {
                        connectionMutex.withLock {
                            connections.remove(deviceId)
                        }
                        removeDeviceFromList(deviceId)
                    }
                }
            }
        }.start(wait = false)
    }

    /** Tüm bağlı client'lara flaş komutunu tek seferde, aynı anda yayınlar. */
    suspend fun broadcast(message: SyncMessage) {
        val payload = Frame.Text(json.encodeToString(SyncMessage.serializer(), message))
        connectionMutex.withLock {
            connections.values.toList()
        }.forEach { session ->
            runCatching { session.send(payload) }
        }
    }

    /** Tüm cihazların aynı flaşı, host saatine göre startAt anında görmesini sağlar. */
    suspend fun triggerFlash(pattern: FlashPattern) {
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
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 200, timeoutMillis = 1000)
        engine = null
        connections.clear()
        _connectedDevices.value = emptyList()
    }

    private fun updateDeviceList(deviceId: String, deviceName: String) {
        val current = _connectedDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == deviceId }
        val info = DeviceInfo(id = deviceId, name = deviceName, isHost = false)
        if (existingIndex >= 0) current[existingIndex] = info else current.add(info)
        _connectedDevices.value = current
    }

    private fun removeDeviceFromList(deviceId: String) {
        _connectedDevices.value = _connectedDevices.value.filterNot { it.id == deviceId }
    }

    companion object {
        const val DEFAULT_PORT = 8127
        const val FLASH_SCHEDULE_LEAD_MS = 300L
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
