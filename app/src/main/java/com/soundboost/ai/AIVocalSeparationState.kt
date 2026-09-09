package com.soundboost.ai

/**
 * UI State for AI Vocal Separation feature
 */
data class AIVocalSeparationState(
    // Capture state
    val isCapturing: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val captureError: String? = null,
    
    // Volume controls
    val vocalVolume: Float = 1.0f,      // 0.0 - 2.0
    val musicVolume: Float = 1.0f,      // 0.0 - 2.0
    
    // Performance metrics
    val captureLatencyMs: Int = 0,
    val processingLatencyMs: Int = 0,
    val totalLatencyMs: Int = 0,
    val bufferFillPercent: Int = 0,
    val droppedFrames: Int = 0,
    val cpuUsagePercent: Int = 0,
    
    // App detection
    val detectedApp: String? = null,
    val isAppSupported: Boolean? = null,
    
    // Model state
    val isModelLoaded: Boolean = false,
    val modelLoadError: String? = null,
    
    // Processing mode
    val processingMode: ProcessingMode = ProcessingMode.PASS_THROUGH,
    
    // Warnings
    val warnings: List<String> = emptyList()
) {
    val hasError: Boolean
        get() = captureError != null || modelLoadError != null
    
    val canStart: Boolean
        get() = !isCapturing && isModelLoaded && !hasError
    
    val isHealthy: Boolean
        get() = isCapturing && 
                totalLatencyMs < 500 && 
                droppedFrames < 10 &&
                bufferFillPercent > 20
}

enum class ProcessingMode {
    /**
     * Pass audio through without separation (testing)
     */
    PASS_THROUGH,
    
    /**
     * Full AI separation (vocals + music)
     */
    AI_SEPARATION,
    
    /**
     * Fast mode with lower quality
     */
    FAST_MODE
}

/**
 * Supported/unsupported app information
 */
data class AppCompatibility(
    val packageName: String,
    val appName: String,
    val isSupported: Boolean,
    val reason: String? = null
) {
    companion object {
        val SUPPORTED_APPS = listOf(
            AppCompatibility("com.google.android.youtube", "YouTube", true),
            AppCompatibility("com.google.android.apps.youtube.music", "YouTube Music", true),
            AppCompatibility("com.amazon.mp3", "Amazon Music", true),
            AppCompatibility("com.apple.android.music", "Apple Music", true),
            AppCompatibility("com.deezer.android.app", "Deezer", true),
            AppCompatibility("com.maxmpz.audioplayer", "Poweramp", true)
        )
        
        val UNSUPPORTED_APPS = listOf(
            AppCompatibility(
                "com.spotify.music", 
                "Spotify", 
                false, 
                "DRM protected (ALLOW_CAPTURE_BY_NONE)"
            ),
            AppCompatibility(
                "com.android.chrome", 
                "Chrome", 
                false, 
                "Some sites block capture"
            ),
            AppCompatibility(
                "com.soundcloud.android", 
                "SoundCloud", 
                false, 
                "DRM protected"
            )
        )
        
        fun findApp(packageName: String): AppCompatibility? {
            return SUPPORTED_APPS.find { it.packageName == packageName }
                ?: UNSUPPORTED_APPS.find { it.packageName == packageName }
        }
    }
}

/**
 * Processing statistics for monitoring
 */
data class ProcessingStats(
    val timestamp: Long = System.currentTimeMillis(),
    val capturedSamples: Long = 0,
    val processedSamples: Long = 0,
    val droppedSamples: Long = 0,
    val averageProcessingTimeMs: Float = 0f,
    val peakProcessingTimeMs: Int = 0,
    val bufferUnderruns: Int = 0,
    val bufferOverruns: Int = 0
) {
    val processingEfficiency: Float
        get() = if (capturedSamples > 0) {
            (processedSamples.toFloat() / capturedSamples) * 100f
        } else 0f
    
    val dropRate: Float
        get() = if (capturedSamples > 0) {
            (droppedSamples.toFloat() / capturedSamples) * 100f
        } else 0f
}
