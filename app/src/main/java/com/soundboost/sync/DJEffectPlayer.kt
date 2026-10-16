package com.soundboost.sync

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * DJ EFFECT PLAYER - Synchronized sound effect playback
 * 
 * CRITICAL REQUIREMENTS:
 * - Ultra-low latency using SoundPool (preloaded sounds)
 * - Precise timestamp-based playback
 * - Independent of music playback (separate audio stream)
 * - Works on both HOST and CLIENT
 */
class DJEffectPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    private var soundPool: SoundPool? = null
    private val loadedEffects = mutableMapOf<String, Int>() // effectId -> soundId
    private val pendingEffects = mutableListOf<PendingEffect>()
    
    // Clock sync offset (CLIENT only - for timestamp correction)
    private var clockOffset: Long = 0L
    
    init {
        initializeSoundPool()
        startPlaybackScheduler()
    }
    
    private fun initializeSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(8) // Allow 8 simultaneous effects
            .setAudioAttributes(audioAttributes)
            .build()
        
        Log.d(TAG, "🎵 SoundPool initialized for DJ effects")
    }
    
    /**
     * Preload all DJ effects into memory for instant playback
     * CRITICAL: Must be called during initialization!
     */
    suspend fun preloadEffects() = withContext(Dispatchers.IO) {
        Log.d(TAG, "📦 Preloading ${DJEffects.all.size} DJ effects...")
        
        val pool = soundPool
        if (pool == null) {
            Log.e(TAG, "❌ SoundPool not initialized!")
            return@withContext
        }
        
        DJEffects.all.forEach { effect ->
            try {
                // Load from assets
                val afd = context.assets.openFd("djeffect/${effect.fileName}")
                val soundId = pool.load(afd, 1)
                
                if (soundId > 0) {
                    loadedEffects[effect.id] = soundId
                    Log.d(TAG, "✅ Loaded: ${effect.displayName} (${effect.fileName})")
                } else {
                    Log.e(TAG, "❌ Failed to load: ${effect.fileName}")
                }
                
                afd.close()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading ${effect.fileName}", e)
            }
        }
        
        // Brief delay to ensure all sounds are fully loaded
        delay(500)
        
        Log.d(TAG, "✅ Preloaded ${loadedEffects.size}/${DJEffects.all.size} effects")
    }
    
    /**
     * Set clock offset for CLIENT devices (to sync with HOST clock)
     */
    fun setClockOffset(offset: Long) {
        clockOffset = offset
        Log.d(TAG, "⏰ Clock offset set: ${offset}ms")
    }
    
    /**
     * Schedule a DJ effect to play at specific timestamp
     * CRITICAL: Uses host clock timestamp for synchronization!
     * 
     * @param effectId Effect identifier
     * @param playAtHostTime When to play (host clock epoch millis)
     * @param volume Volume 0.0 to 1.0
     */
    fun scheduleEffect(effectId: String, playAtHostTime: Long, volume: Float = 1.0f) {
        val effect = DJEffects.getById(effectId)
        if (effect == null) {
            Log.e(TAG, "❌ Unknown effect: $effectId")
            return
        }
        
        val soundId = loadedEffects[effectId]
        if (soundId == null) {
            Log.e(TAG, "❌ Effect not loaded: $effectId")
            return
        }
        
        // Convert host timestamp to local time
        val localPlayTime = playAtHostTime - clockOffset
        val now = System.currentTimeMillis()
        val delayMs = localPlayTime - now
        
        Log.d(TAG, "🎯 Scheduled: ${effect.displayName} in ${delayMs}ms (host=$playAtHostTime, local=$localPlayTime)")
        
        synchronized(pendingEffects) {
            pendingEffects.add(
                PendingEffect(
                    soundId = soundId,
                    name = effect.displayName,
                    playAtLocal = localPlayTime,
                    volume = volume
                )
            )
        }
    }
    
    /**
     * Play effect immediately (for testing)
     */
    fun playNow(effectId: String, volume: Float = 1.0f) {
        val soundId = loadedEffects[effectId]
        if (soundId == null) {
            Log.e(TAG, "❌ Effect not loaded: $effectId")
            return
        }
        
        val pool = soundPool
        if (pool == null) {
            Log.e(TAG, "❌ SoundPool not initialized!")
            return
        }
        
        pool.play(soundId, volume, volume, 1, 0, 1.0f)
        
        val effect = DJEffects.getById(effectId)
        Log.d(TAG, "🔊 Playing NOW: ${effect?.displayName}")
    }
    
    /**
     * Background scheduler - checks pending effects and plays at exact time
     * Runs every 10ms for ultra-precise timing
     */
    private fun startPlaybackScheduler() {
        scope.launch {
            while (true) {
                delay(10) // Check every 10ms
                
                val now = System.currentTimeMillis()
                val toPlay = mutableListOf<PendingEffect>()
                
                synchronized(pendingEffects) {
                    val iterator = pendingEffects.iterator()
                    while (iterator.hasNext()) {
                        val effect = iterator.next()
                        
                        // Play if within 30ms window (allows for slight timing variance)
                        if (effect.playAtLocal <= now + 30) {
                            toPlay.add(effect)
                            iterator.remove()
                        }
                    }
                }
                
                // Play scheduled effects
                toPlay.forEach { effect ->
                    playScheduledEffect(effect)
                }
            }
        }
    }
    
    private fun playScheduledEffect(effect: PendingEffect) {
        val pool = soundPool
        if (pool == null) {
            Log.e(TAG, "❌ SoundPool not initialized!")
            return
        }
        
        val now = System.currentTimeMillis()
        val latency = now - effect.playAtLocal
        
        pool.play(effect.soundId, effect.volume, effect.volume, 1, 0, 1.0f)
        
        Log.d(TAG, "🔊 Played: ${effect.name} (latency: ${latency}ms)")
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        loadedEffects.clear()
        synchronized(pendingEffects) {
            pendingEffects.clear()
        }
        Log.d(TAG, "🗑️ DJ Effect Player released")
    }
    
    private data class PendingEffect(
        val soundId: Int,
        val name: String,
        val playAtLocal: Long,
        val volume: Float
    )
    
    companion object {
        private const val TAG = "DJEffectPlayer"
    }
}
