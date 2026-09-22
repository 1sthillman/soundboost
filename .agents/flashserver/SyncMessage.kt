package com.soundboost.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Flash-Sync kablosuz protokolü.
 *
 * Tüm mesajlar WebSocket üzerinden Frame.Text olarak taşınır ve
 * Json.encodeToString / Json.decodeFromString ile serileştirilir.
 * Manuel string parse ETME — kırılgan olur, tip güvenliğini kaybedersin.
 *
 * Host <-> Client arasında akan mesajlar:
 *  Join      : client -> host, odaya katılma isteği
 *  Welcome   : host -> client, katılım onayı + oda bilgisi
 *  Ping/Pong : iki yönlü, ClockSync için saat farkı ölçümü
 *  Flash     : host -> tüm client'lar, senkronize tetikleme
 *  Leave     : herhangi bir yön, temiz ayrılma
 *  Error     : host -> client, oda dolu / reddedildi vb.
 */
@Serializable
sealed class SyncMessage {

    @Serializable
    @SerialName("join")
    data class Join(
        val deviceName: String,
        val protocolVersion: Int = PROTOCOL_VERSION
    ) : SyncMessage()

    @Serializable
    @SerialName("welcome")
    data class Welcome(
        val deviceId: String,
        val roomName: String,
        val connectedDeviceCount: Int
    ) : SyncMessage()

    @Serializable
    @SerialName("ping")
    data class Ping(
        /** Client'ın kendi saatiyle gönderim anı (System.currentTimeMillis) */
        val t0: Long
    ) : SyncMessage()

    @Serializable
    @SerialName("pong")
    data class Pong(
        /** Client'ın ping'i gönderdiği an (aynen geri yansıtılır) */
        val t0: Long,
        /** Host'un ping'i aldığı an */
        val t1: Long,
        /** Host'un pong'u gönderdiği an */
        val t2: Long
    ) : SyncMessage()

    @Serializable
    @SerialName("flash")
    data class Flash(
        /** Host saatine göre tetiklenme zamanı (epoch millis) */
        val startAt: Long,
        val durationMs: Int,
        val mode: FlashMode,
        /** Hex renk, örn. "#FFFFFF" — ScreenFlashOverlay bunu Color'a çevirir */
        val color: String,
        /** Art arda flaş için tekrar sayısı (strobe modunda kullanılır) */
        val repeatCount: Int = 1,
        val intervalMs: Int = 0
    ) : SyncMessage()

    @Serializable
    @SerialName("leave")
    data class Leave(val deviceId: String) : SyncMessage()

    @Serializable
    @SerialName("error")
    data class Error(val reason: String) : SyncMessage()

    companion object {
        const val PROTOCOL_VERSION = 1
    }
}

@Serializable
enum class FlashMode {
    SCREEN_ONLY,
    TORCH_ONLY,
    SCREEN_AND_TORCH
}
