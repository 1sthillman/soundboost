package com.soundboost.sync

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.soundboost.audio.TFLiteStemSeparator
import com.soundboost.audio.StemSeparationState as AudioStemSeparationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.util.UUID

/**
 * MUSIC SHARE MANAGER - Manages synchronized music playback across devices
 * 
 * HOST: Shares music file, broadcasts chunks, controls playback
 * CLIENT: Downloads chunks, buffers, plays synchronized
 * 
 * AI STEM SEPARATION:
 * - TensorFlow Lite based vocal/music separation
 * - GPU accelerated, chunked processing (no freezing!)
 * - Cached stems (process once, use forever)
 * - Synchronized completion across all devices
 */
class MusicShareManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    private val _musicState = MutableStateFlow<MusicShareState>(MusicShareState.Idle)
    val musicState: StateFlow<MusicShareState> = _musicState.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()
    
    // AI STEM SEPARATION
    private val stemSeparator = TFLiteStemSeparator(context)
    val stemSeparationState: StateFlow<AudioStemSeparationState> = stemSeparator.separationState
    
    // Callback for client to report progress to host
    var onProgressUpdate: ((Float, Boolean) -> Unit)? = null
    
    // Callback for stem separation status broadcasts
    var onStemSeparationUpdate: ((String, String, Float, String) -> Unit)? = null
    
    // HOST: Source file storage
    private var hostMusicFile: File? = null
    private var currentSession: MusicSession? = null
    
    // CLIENT: Downloaded chunks storage
    private val downloadedChunks = mutableMapOf<Int, ByteArray>()
    private val downloadMutex = Mutex()
    
    // CLIENT: Current download session ID (needed for chunk requests)
    private var currentDownloadSessionId: String? = null
    
    // MediaPlayer for synchronized playback
    private var syncPlayer: SynchronizedMusicPlayer? = null
    
    // ---------- HOST FUNCTIONS ----------
    
    /**
     * HOST: Select music file and prepare for sharing
     * NOW WITH AI STEM SEPARATION!
     */
    suspend fun prepareMusic(musicUri: Uri): Result<MusicSession> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📁 Preparing music file from URI: $musicUri")
            
            // Copy file to internal storage for chunked reading
            val inputStream = context.contentResolver.openInputStream(musicUri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))
            
            val tempFile = File(context.cacheDir, "shared_music_${System.currentTimeMillis()}.tmp")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            hostMusicFile = tempFile
            val fileSize = tempFile.length()
            val chunkSize = CHUNK_SIZE_BYTES
            val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()
            
            val fileName = getFileName(musicUri) ?: "music.mp3"
            val format = fileName.substringAfterLast('.', "mp3")
            
            val session = MusicSession(
                sessionId = UUID.randomUUID().toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                chunkSizeBytes = chunkSize,
                totalChunks = totalChunks,
                format = format,
                localFile = tempFile
            )
            
            currentSession = session
            _musicState.value = MusicShareState.Prepared(session)
            
            Log.d(TAG, "✅ Music prepared: $fileName (${fileSize / 1024}KB, $totalChunks chunks)")
            
            // CRITICAL: Start AI stem separation in background
            // This processes the music file for perfect vocal/music split
            startStemSeparation(musicUri, session.sessionId)
            
            Result.success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to prepare music", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start AI stem separation (background processing)
     * MÜKEMMEL: GPU-accelerated, chunked, cached, non-blocking!
     */
    private fun startStemSeparation(musicUri: Uri, sessionId: String) {
        scope.launch(Dispatchers.Default) {
            try {
                Log.d(TAG, "🤖 ========== STARTING AI STEM SEPARATION ==========")
                Log.d(TAG, "🤖 Session: $sessionId")
                Log.d(TAG, "🤖 URI: $musicUri")
                
                // Notify start
                onStemSeparationUpdate?.invoke(sessionId, "started", 0f, "Starting AI separation...")
                
                // Monitor progress
                launch {
                    stemSeparator.separationState.collect { state ->
                        when (state) {
                            is AudioStemSeparationState.Processing -> {
                                Log.d(TAG, "🤖 Separation progress: ${(state.progress * 100).toInt()}% - ${state.status}")
                                onStemSeparationUpdate?.invoke(
                                    sessionId,
                                    "processing",
                                    state.progress,
                                    state.status
                                )
                            }
                            is AudioStemSeparationState.Complete -> {
                                Log.d(TAG, "✅ ========== SEPARATION COMPLETE ==========")
                                Log.d(TAG, "✅ Vocals: ${state.result.vocalsPath}")
                                Log.d(TAG, "✅ Music: ${state.result.musicPath}")
                                onStemSeparationUpdate?.invoke(
                                    sessionId,
                                    "completed",
                                    1.0f,
                                    "Separation complete!"
                                )
                            }
                            is AudioStemSeparationState.Error -> {
                                Log.e(TAG, "❌ Separation error: ${state.message}")
                                onStemSeparationUpdate?.invoke(
                                    sessionId,
                                    "error",
                                    0f,
                                    state.message
                                )
                            }
                            else -> Unit
                        }
                    }
                }
                
                // Start separation
                val result = stemSeparator.separateAudio(musicUri, sessionId, forceReprocess = false)
                
                if (result != null) {
                    Log.d(TAG, "✅ Separation result ready - stems cached for instant reuse")
                } else {
                    Log.w(TAG, "⚠️ Separation returned null - using fallback")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Stem separation failed", e)
                onStemSeparationUpdate?.invoke(sessionId, "error", 0f, e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * HOST: Get metadata to broadcast to clients
     */
    fun getMusicMetadata(): SyncMessage.MusicMetadata? {
        val session = currentSession ?: return null
        return SyncMessage.MusicMetadata(
            sessionId = session.sessionId,
            fileName = session.fileName,
            fileSizeBytes = session.fileSizeBytes,
            chunkSizeBytes = session.chunkSizeBytes,
            totalChunks = session.totalChunks,
            format = session.format
        )
    }
    
    /**
     * HOST: Get chunk data for client request
     */
    suspend fun getChunk(chunkIndex: Int): ByteArray? = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext null
        val file = session.localFile ?: return@withContext null
        
        if (chunkIndex < 0 || chunkIndex >= session.totalChunks) {
            Log.w(TAG, "⚠️ Invalid chunk index: $chunkIndex")
            return@withContext null
        }
        
        try {
            val offset = chunkIndex.toLong() * session.chunkSizeBytes
            val remainingBytes = session.fileSizeBytes - offset
            val chunkSize = minOf(session.chunkSizeBytes.toLong(), remainingBytes).toInt()
            
            val buffer = ByteArray(chunkSize)
            file.inputStream().use { input ->
                input.skip(offset)
                input.read(buffer)
            }
            
            Log.d(TAG, "📤 Serving chunk $chunkIndex/${session.totalChunks} (${chunkSize / 1024}KB)")
            buffer
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to read chunk $chunkIndex", e)
            null
        }
    }
    
    /**
     * HOST: Clear current music session
     */
    fun clearSession() {
        hostMusicFile?.delete()
        hostMusicFile = null
        currentSession = null
        _musicState.value = MusicShareState.Idle
        Log.d(TAG, "🧹 Music session cleared")
    }
    
    // ---------- CLIENT FUNCTIONS ----------
    
    /**
     * CLIENT: Receive music metadata and prepare for download
     */
    fun receiveMusicMetadata(metadata: SyncMessage.MusicMetadata) {
        Log.d(TAG, "📥 ========== MUSIC METADATA RECEIVED ==========")
        Log.d(TAG, "📥 fileName: ${metadata.fileName}")
        Log.d(TAG, "📥 sessionId: ${metadata.sessionId}")
        Log.d(TAG, "📥 fileSizeBytes: ${metadata.fileSizeBytes} (${metadata.fileSizeBytes / 1024}KB)")
        Log.d(TAG, "📥 totalChunks: ${metadata.totalChunks}")
        Log.d(TAG, "📥 format: ${metadata.format}")
        
        val session = MusicSession(
            sessionId = metadata.sessionId,
            fileName = metadata.fileName,
            fileSizeBytes = metadata.fileSizeBytes,
            chunkSizeBytes = metadata.chunkSizeBytes,
            totalChunks = metadata.totalChunks,
            format = metadata.format,
            localFile = null
        )
        
        currentSession = session
        currentDownloadSessionId = metadata.sessionId  // CRITICAL: Store for chunk requests!
        downloadedChunks.clear()
        _downloadProgress.value = 0f
        _musicState.value = MusicShareState.Downloading(metadata.fileName, 0f)
        
        Log.d(TAG, "✅ Ready to start download")
    }
    
    /**
     * CLIENT: Get list of missing chunks to request
     */
    suspend fun getMissingChunks(): List<Int> = downloadMutex.withLock {
        val session = currentSession ?: return emptyList()
        (0 until session.totalChunks).filter { chunkIndex ->
            !downloadedChunks.containsKey(chunkIndex)
        }
    }
    
    /**
     * CLIENT: Store received chunk with enhanced logging
     */
    suspend fun storeChunk(chunkIndex: Int, data: ByteArray) = downloadMutex.withLock {
        val session = currentSession ?: run {
            Log.e(TAG, "❌ Cannot store chunk - no session!")
            return@withLock
        }
        
        downloadedChunks[chunkIndex] = data
        
        val progress = downloadedChunks.size.toFloat() / session.totalChunks
        _downloadProgress.value = progress
        
        val percentComplete = (progress * 100).toInt()
        Log.d(TAG, "💾 Stored chunk $chunkIndex/${session.totalChunks} ($percentComplete%) - size: ${data.size} bytes")
        
        _musicState.value = MusicShareState.Downloading(session.fileName, progress)
        
        // Notify host of progress update
        onProgressUpdate?.invoke(progress, false)
        
        // Log milestone progress
        if (percentComplete % 10 == 0 && percentComplete > 0) {
            Log.d(TAG, "📊 Download milestone: $percentComplete% complete (${downloadedChunks.size}/${session.totalChunks} chunks)")
        }
        
        // Check if download complete
        if (downloadedChunks.size == session.totalChunks) {
            Log.d(TAG, "🎉 ========== ALL CHUNKS RECEIVED ==========")
            finalizeDownload()
        }
    }
    
    /**
     * CLIENT: Finalize download - combine chunks into playable file
     * THEN: Start AI stem separation for synchronized DJ controls!
     */
    private suspend fun finalizeDownload() = withContext(Dispatchers.IO) {
        val session = currentSession ?: return@withContext
        
        try {
            Log.d(TAG, "🔄 ========== FINALIZING DOWNLOAD ==========")
            Log.d(TAG, "🔄 Combining ${session.totalChunks} chunks into playable file...")
            
            val outputFile = File(context.cacheDir, "received_${session.sessionId}.${session.format}")
            
            var totalBytesWritten = 0L
            outputFile.outputStream().use { output ->
                for (i in 0 until session.totalChunks) {
                    val chunkData = downloadedChunks[i]
                        ?: throw Exception("Missing chunk $i")
                    output.write(chunkData)
                    totalBytesWritten += chunkData.size
                    
                    if (i % 10 == 0 || i == session.totalChunks - 1) {
                        Log.d(TAG, "✍️ Writing chunk $i/${session.totalChunks} (${totalBytesWritten / 1024}KB written)")
                    }
                }
            }
            
            currentSession = session.copy(localFile = outputFile)
            _musicState.value = MusicShareState.Ready(session.fileName)
            
            // Notify host that download is complete
            onProgressUpdate?.invoke(1.0f, true)
            
            Log.d(TAG, "✅ ========== DOWNLOAD COMPLETE ==========")
            Log.d(TAG, "✅ File saved: ${outputFile.absolutePath}")
            Log.d(TAG, "✅ File size: ${outputFile.length() / 1024}KB")
            Log.d(TAG, "✅ Ready to play!")
            
            // CRITICAL: CLIENT also processes stems (deterministic, same result as host!)
            Log.d(TAG, "🤖 CLIENT: Starting stem separation for DJ controls...")
            val outputUri = Uri.fromFile(outputFile)
            startStemSeparation(outputUri, session.sessionId)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ ========== FINALIZATION FAILED ==========", e)
            Log.e(TAG, "❌ Error: ${e.message}")
            e.printStackTrace()
            _musicState.value = MusicShareState.Error("Failed to finalize: ${e.message}")
        }
    }
    
    // ---------- PLAYBACK FUNCTIONS (CLIENT & HOST) ----------
    
    /**
     * Start synchronized playback at specific time
     * USES: SynchronizedMusicPlayer with position tracking
     */
    suspend fun startPlayback(startAtMillis: Long, startPosition: Int = 0) = withContext(Dispatchers.Main) {
        val session = currentSession ?: run {
            Log.e(TAG, "❌ Cannot start playback - no session")
            return@withContext
        }
        
        val file = session.localFile ?: run {
            Log.e(TAG, "❌ Cannot start playback - no file")
            return@withContext
        }
        
        try {
            Log.d(TAG, "⏰ ========== SYNCHRONIZED PLAYBACK ==========")
            Log.d(TAG, "⏰ Start time: $startAtMillis")
            Log.d(TAG, "⏰ Start position: ${startPosition}ms")
            
            // Release old player
            syncPlayer?.release()
            
            // Create and prepare new synchronized player
            val player = SynchronizedMusicPlayer(file)
            if (!player.prepare()) {
                _musicState.value = MusicShareState.Error("Failed to prepare player")
                return@withContext
            }
            
            syncPlayer = player
            
            // Start at synchronized time
            player.startSynchronized(startAtMillis, startPosition)
            _musicState.value = MusicShareState.Playing(session.fileName)
            
            Log.d(TAG, "✅ Playback scheduled")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start playback", e)
            _musicState.value = MusicShareState.Error("Playback failed: ${e.message}")
        }
    }
    
    /**
     * Pause playback at synchronized time
     * RECORDS: Exact position for synchronized resume
     */
    suspend fun pausePlayback(pauseAtMillis: Long) {
        val player = syncPlayer ?: run {
            Log.w(TAG, "⚠️ Cannot pause - no player")
            return
        }
        
        Log.d(TAG, "⏸️ Scheduling synchronized pause")
        player.pauseSynchronized(pauseAtMillis)
        
        val session = currentSession
        if (session != null) {
            _musicState.value = MusicShareState.Paused(session.fileName)
        }
    }
    
    /**
     * Resume playback at synchronized time WITH position
     * CRITICAL: All devices seek to SAME position before resuming
     */
    suspend fun resumePlayback(resumeAtMillis: Long, resumePosition: Int) {
        val player = syncPlayer ?: run {
            Log.w(TAG, "⚠️ Cannot resume - no player")
            return
        }
        
        Log.d(TAG, "▶️ Scheduling synchronized resume: time=$resumeAtMillis, position=${resumePosition}ms")
        player.resumeSynchronized(resumeAtMillis, resumePosition)
        
        val session = currentSession
        if (session != null) {
            _musicState.value = MusicShareState.Playing(session.fileName)
        }
    }
    
    /**
     * Stop playback
     */
    suspend fun stopPlayback() {
        syncPlayer?.stop()
        
        val session = currentSession
        if (session != null) {
            _musicState.value = MusicShareState.Ready(session.fileName)
        }
        
        Log.d(TAG, "⏹️ Playback stopped")
    }
    
    /**
     * Get current playback position (for pause position tracking)
     */
    fun getCurrentPosition(): Int {
        return syncPlayer?.currentPosition?.value ?: 0
    }
    
    /**
     * Apply DJ controls to synchronized player
     * INCLUDES VOCAL/MUSIC SEPARATION!
     */
    fun applyDJControls(bass: Float, mid: Float, treble: Float, masterVolume: Float, vocalBalance: Float = 0.5f) {
        syncPlayer?.applyDJControls(bass, mid, treble, masterVolume, vocalBalance)
    }
    
    /**
     * Set playback speed (for DJ scratching)
     */
    fun setPlaybackSpeed(speed: Float) {
        syncPlayer?.setPlaybackSpeed(speed)
    }
    
    /**
     * Get stem separation result (for loading stems into player)
     */
    fun getStemSeparationResult() = (stemSeparator.separationState.value as? AudioStemSeparationState.Complete)?.result
    
    /**
     * Check if stems are ready
     */
    fun areStemsReady(): Boolean {
        return stemSeparator.separationState.value is AudioStemSeparationState.Complete
    }
    
    /**
     * Clear stem cache (for testing/debugging)
     */
    fun clearStemCache(sessionId: String? = null) {
        stemSeparator.clearCache(sessionId)
    }
    
    /**
     * Release all resources
     */
    fun release() {
        syncPlayer?.release()
        syncPlayer = null
        stemSeparator.release()
        clearSession()
        downloadedChunks.clear()
        Log.d(TAG, "🧹 MusicShareManager released")
    }
    
    private fun getFileName(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst()) {
                    it.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        private const val TAG = "MusicShareManager"
        private const val CHUNK_SIZE_BYTES = 65536 // 64KB chunks for smooth download
    }
}

/**
 * Music session data
 */
data class MusicSession(
    val sessionId: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val chunkSizeBytes: Int,
    val totalChunks: Int,
    val format: String,
    val localFile: File?
)

/**
 * Music share state machine
 */
sealed class MusicShareState {
    data object Idle : MusicShareState()
    data class Prepared(val session: MusicSession) : MusicShareState()
    data class Downloading(val fileName: String, val progress: Float) : MusicShareState()
    data class Ready(val fileName: String) : MusicShareState()
    data class Playing(val fileName: String) : MusicShareState()
    data class Paused(val fileName: String) : MusicShareState()
    data class Error(val message: String) : MusicShareState()
}
