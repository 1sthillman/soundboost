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
        val connectedDeviceCount: Int,
        /** CRITICAL: Room state for late join sync */
        val roomStateJson: String? = null
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

    /** 
     * HOST'un bass analizini tüm client'lara broadcast etmek için.
     * Manuel tetiklemeden FARKI: Müzik çalınca otomatik gönderilir.
     */
    @Serializable
    @SerialName("bass_sync")
    data class BassSync(
        /** Host saatine göre tetiklenme zamanı (epoch millis) */
        val startAt: Long,
        val bass: Float,
        val subBass: Float,
        val energy: Float,
        val hasKick: Boolean,
        val isBeat: Boolean
    ) : SyncMessage()

    /**
     * SYNCHRONIZED AUDIO STREAMING - Host'un çaldığı müzik client'larda senkron çalır!
     * Opus-encoded audio chunks over UDP for ultra-low latency
     */
    @Serializable
    @SerialName("audio_chunk")
    data class AudioChunk(
        /** Host saatine göre playback zamanı (epoch millis) */
        val timestamp: Long,
        /** Packet sequence number (for jitter buffer ordering) */
        val sequence: Long,
        /** Opus-encoded audio data (base64 encoded for JSON transport) */
        val data: String
    ) : SyncMessage()

    /**
     * MUSIC FILE SHARING - Host shares a music file metadata with clients
     * Clients will request download in chunks
     */
    @Serializable
    @SerialName("music_metadata")
    data class MusicMetadata(
        /** Unique music session ID */
        val sessionId: String,
        /** File name (e.g., "Song.mp3") */
        val fileName: String,
        /** Total file size in bytes */
        val fileSizeBytes: Long,
        /** Chunk size in bytes (for progressive download) */
        val chunkSizeBytes: Int = 65536, // 64KB chunks
        /** Total number of chunks */
        val totalChunks: Int,
        /** Audio format (mp3, m4a, flac, etc.) */
        val format: String
    ) : SyncMessage()

    /**
     * CLIENT DOWNLOAD STATUS - Client reports download progress to host
     */
    @Serializable
    @SerialName("music_download_status")
    data class MusicDownloadStatus(
        val sessionId: String,
        val isComplete: Boolean,
        val progress: Float // 0.0 to 1.0
    ) : SyncMessage()

    /**
     * REQUEST MUSIC CHUNK - Client requests a specific chunk of the music file
     */
    @Serializable
    @SerialName("music_chunk_request")
    data class MusicChunkRequest(
        val sessionId: String,
        val chunkIndex: Int
    ) : SyncMessage()

    /**
     * MUSIC CHUNK DATA - Host sends a chunk of the music file
     */
    @Serializable
    @SerialName("music_chunk")
    data class MusicChunk(
        val sessionId: String,
        val chunkIndex: Int,
        /** Base64 encoded audio data */
        val data: String
    ) : SyncMessage()

    /**
     * START MUSIC PLAYBACK - Host signals all clients to start playing at exact time
     */
    @Serializable
    @SerialName("music_start")
    data class MusicStart(
        val sessionId: String,
        /** Host saatine göre playback başlama zamanı (epoch millis) */
        val startAt: Long
    ) : SyncMessage()

    /**
     * MUSIC PLAYBACK CONTROL - Pause/Resume/Stop music across all devices
     * WITH POSITION SYNCHRONIZATION
     */
    @Serializable
    @SerialName("music_control")
    data class MusicControl(
        val sessionId: String,
        val action: MusicAction,
        /** Synchronized timestamp for pause/resume/stop (host clock) */
        val executeAt: Long = System.currentTimeMillis(),
        /** CRITICAL: Playback position in milliseconds (for resume sync) */
        val positionMs: Int = 0
    ) : SyncMessage()

    /**
     * ROOM STATE SYNC - Yeni katılan client'a mevcut oda durumu gönderilir
     */
    @Serializable
    @SerialName("room_state")
    data class RoomStateSync(
        val playlist: String,  // JSON-encoded Playlist
        val playbackState: String,  // "idle", "playing", "paused"
        val currentTrackIndex: Int,
        val currentPosition: Int,
        val serverTimestamp: Long
    ) : SyncMessage()

    /**
     * PLAYLIST UPDATE - Playlist değişikliği (şarkı ekleme/çıkarma)
     */
    @Serializable
    @SerialName("playlist_update")
    data class PlaylistUpdate(
        val playlist: String,  // JSON-encoded Playlist
        val updateType: String  // "add", "remove", "reorder", "clear"
    ) : SyncMessage()

    /**
     * TRACK CHANGE - Şarkı değişikliği (next/previous)
     */
    @Serializable
    @SerialName("track_change")
    data class TrackChange(
        val trackIndex: Int,
        val trackSessionId: String,
        val startAt: Long
    ) : SyncMessage()

    /**
     * HOST TRANSFER - Oda host'u değişiyor
     */
    @Serializable
    @SerialName("host_transfer")
    data class HostTransfer(
        val newHostId: String,
        val newHostName: String,
        val reason: String  // "host_left", "manual_transfer"
    ) : SyncMessage()

    /**
     * DJ CONTROL SYNC - Real-time audio manipulation
     */
    @Serializable
    @SerialName("dj_state_sync")
    data class DJStateSync(
        val djStateJson: String,  // JSON-encoded DJState
        val applyAt: Long
    ) : SyncMessage()

    /**
     * DJ COMMAND - Specific control (EQ, volume, effect, seek)
     */
    @Serializable
    @SerialName("dj_command")
    data class DJCommandMsg(
        val commandJson: String,  // JSON-encoded DJCommand
        val applyAt: Long
    ) : SyncMessage()

    /**
     * STEM SEPARATION STATUS - Notify clients about separation progress/completion
     * All devices process stems independently (deterministic), this syncs completion
     */
    @Serializable
    @SerialName("stem_separation_status")
    data class StemSeparationStatus(
        val sessionId: String,
        val status: String,  // "started", "processing", "completed", "error"
        val progress: Float = 0f,  // 0.0 to 1.0
        val message: String = "",
        val deviceId: String = ""
    ) : SyncMessage()

    /**
     * STEM SEPARATION COMPLETE - Notify all that stems are ready
     */
    @Serializable
    @SerialName("stem_ready")
    data class StemReady(
        val sessionId: String,
        val vocalsReady: Boolean,
        val musicReady: Boolean
    ) : SyncMessage()

    /**
     * MESSAGE ACKNOWLEDGMENT - Client confirms receipt of critical message
     */
    @Serializable
    @SerialName("ack")
    data class Ack(
        val messageId: String,
        val messageType: String,
        val success: Boolean,
        val error: String? = null,
        val clientState: String? = null  // JSON-encoded ClientState
    ) : SyncMessage()

    /**
     * HEARTBEAT - Continuous health check
     */
    @Serializable
    @SerialName("heartbeat")
    data class Heartbeat(
        val state: String,  // "idle", "downloading", "ready", "playing", "paused"
        val currentPosition: Int = 0,
        val downloadProgress: Float = 0f,
        val playerReady: Boolean = false,
        val clockOffset: Long = 0
    ) : SyncMessage()

    /**
     * READY CHECK REQUEST - Host asks if all devices are ready for action
     */
    @Serializable
    @SerialName("ready_check_request")
    data class ReadyCheckRequest(
        val requestId: String,
        val action: String,  // "music_start", "music_resume", etc.
        val timeoutMs: Long = 5000
    ) : SyncMessage()

    /**
     * READY CHECK RESPONSE - Client reports ready status
     */
    @Serializable
    @SerialName("ready_check_response")
    data class ReadyCheckResponse(
        val requestId: String,
        val ready: Boolean,
        val reason: String? = null,
        val estimatedReadyTime: Long? = null
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

@Serializable
enum class MusicAction {
    PAUSE,
    RESUME,
    STOP
}
