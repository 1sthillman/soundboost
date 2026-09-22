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

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val httpClient = HttpClient(CIO) { install(WebSockets) }
    private var supervisorJob: Job = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private var session: io.ktor.websocket.WebSocketSession? = null

    private val _connectionState = MutableStateFlow<SyncConnectionState>(SyncConnectionState.Idle)
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    private val _clockOffsetMillis = MutableStateFlow(0L)
    val clockOffsetMillis: StateFlow<Long> = _clockOffsetMillis.asStateFlow()

    private val _flashEvents = MutableSharedFlow<SyncMessage.Flash>(extraBufferCapacity = 8)
    val flashEvents: SharedFlow<SyncMessage.Flash> = _flashEvents

    private val offsetSamples = mutableListOf<ClockSync.SampleResult>()

    fun connect(hostAddress: String, port: Int = SyncServer.DEFAULT_PORT, deviceName: String) {
        if (_connectionState.value is SyncConnectionState.Connected ||
            _connectionState.value is SyncConnectionState.Connecting
        ) return

        _connectionState.value = SyncConnectionState.Connecting
        scope.launch {
            try {
                val ws = httpClient.webSocketSession(
                    method = io.ktor.http.HttpMethod.Get,
                    host = hostAddress,
                    port = port,
                    path = "/sync"
                )
                session = ws
                launch { listenLoop(ws, deviceName) }
                launch { pingLoop(ws) }
            } catch (t: Throwable) {
                _connectionState.value = SyncConnectionState.Error(t.message ?: "Baglanti hatasi")
            }
        }
    }

    private suspend fun listenLoop(ws: io.ktor.websocket.WebSocketSession, deviceName: String) {
        for (frame in ws.incoming) {
            if (frame !is Frame.Text) continue
            val message = runCatching {
                json.decodeFromString(SyncMessage.serializer(), frame.readText())
            }.getOrNull() ?: continue

            when (message) {
                is SyncMessage.Welcome -> {
                    _connectionState.value = SyncConnectionState.Connected(
                        deviceId = message.deviceId,
                        roomName = message.roomName
                    )
                    ws.send(Frame.Text(json.encodeToString(SyncMessage.serializer(), SyncMessage.Join(deviceName))))
                }
                is SyncMessage.Pong -> {
                    val t3 = System.currentTimeMillis()
                    val sample = ClockSync.computeSample(t0 = message.t0, t1 = message.t1, t2 = message.t2, t3 = t3)
                    offsetSamples.add(sample)
                    if (offsetSamples.size > MAX_SAMPLES) offsetSamples.removeAt(0)
                    _clockOffsetMillis.value = ClockSync.medianOffset(offsetSamples)
                }
                is SyncMessage.Flash -> _flashEvents.tryEmit(message)
                is SyncMessage.Error -> _connectionState.value = SyncConnectionState.Error(message.reason)
                else -> Unit
            }
        }
        _connectionState.value = SyncConnectionState.Disconnected
    }

    private suspend fun pingLoop(ws: io.ktor.websocket.WebSocketSession) {
        while (scope.isActive && ws.isActive) {
            runCatching {
                ws.send(Frame.Text(json.encodeToString(SyncMessage.serializer(), SyncMessage.Ping(System.currentTimeMillis()))))
            }
            delay(PING_INTERVAL_MS)
        }
    }

    /** Host'un yayınladığı mutlak zamanı, bu cihazın yerel gecikmesine çevirir. */
    fun localDelayFor(flash: SyncMessage.Flash): Long =
        ClockSync.localDelayUntil(
            hostStartAtEpochMillis = flash.startAt,
            localOffsetMillis = _clockOffsetMillis.value,
            nowLocalEpochMillis = System.currentTimeMillis()
        )

    fun disconnect() {
        scope.launch { runCatching { session?.close() } }
        supervisorJob.cancel()
        supervisorJob = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + supervisorJob)
        session = null
        offsetSamples.clear()
        _connectionState.value = SyncConnectionState.Idle
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
