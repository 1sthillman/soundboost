package com.soundboost.gesture

import android.content.Context
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot

/**
 * DJ Gesture Controller - Professional hand gesture recognition for audio control
 * Uses MediaPipe Hand Landmarker for real-time tracking
 */
class DJGestureController(
    private val context: Context,
    private val onVolumeChange: (Int) -> Unit,
    private val onBassChange: (Int) -> Unit
) {
    
    companion object {
        private const val TAG = "DJGestureController"
        private const val MODEL_NAME = "hand_landmarker.task"
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
        private const val GESTURE_THRESHOLD = 0.03f // Minimum movement to trigger
    }
    
    private var handLandmarker: HandLandmarker? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _currentGesture = MutableStateFlow<DJGesture>(DJGesture.None)
    val currentGesture: StateFlow<DJGesture> = _currentGesture.asStateFlow()
    
    // Track previous positions for smooth gestures
    private var previousHandY: Float? = null
    private var previousHandX: Float? = null
    private var gestureStartTime = 0L
    
    /**
     * Initialize MediaPipe Hand Landmarker
     */
    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .build()
            
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(MIN_DETECTION_CONFIDENCE)
                .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
                .setNumHands(2)
                .setRunningMode(RunningMode.VIDEO)
                .build()
            
            handLandmarker = HandLandmarker.createFromOptions(context, options)
            _isInitialized.value = true
            
            Log.d(TAG, "✅ DJ Gesture Controller initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize: ${e.message}", e)
            _isInitialized.value = false
        }
    }
    
    /**
     * Process camera frame for gesture detection
     */
    fun processFrame(bitmap: android.graphics.Bitmap, timestampMs: Long) {
        val landmarker = handLandmarker ?: return
        
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = landmarker.detectForVideo(mpImage, timestampMs)
            
            if (result.landmarks().isNotEmpty()) {
                analyzeGesture(result)
            } else {
                _currentGesture.value = DJGesture.None
                previousHandY = null
                previousHandX = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
        }
    }
    
    /**
     * Analyze hand landmarks and detect gestures
     */
    private fun analyzeGesture(result: HandLandmarkerResult) {
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) return
        
        val primaryHand = landmarks[0]
        val handCount = landmarks.size
        
        // Get wrist position (landmark 0)
        val wrist = primaryHand[0]
        val indexTip = primaryHand[8]
        
        // Calculate hand center
        val handCenterY = (wrist.y() + indexTip.y()) / 2f
        val handCenterX = (wrist.x() + indexTip.x()) / 2f
        
        // GESTURE 1: Volume Fader (Single hand vertical movement)
        if (handCount == 1) {
            previousHandY?.let { prevY ->
                val deltaY = handCenterY - prevY
                if (abs(deltaY) > GESTURE_THRESHOLD) {
                    // Inverted: up = increase volume
                    val volumeDelta = (-deltaY * 200).toInt().coerceIn(-20, 20)
                    if (volumeDelta != 0) {
                        onVolumeChange(volumeDelta)
                        _currentGesture.value = DJGesture.VolumeFader(volumeDelta)
                    }
                }
            }
            previousHandY = handCenterY
        }
        
        // GESTURE 2: Bass Crossfader (Two hands horizontal)
        if (handCount == 2) {
            val secondHand = landmarks[1]
            val hand1X = primaryHand[0].x()
            val hand2X = secondHand[0].x()
            
            val handDistance = abs(hand2X - hand1X)
            
            // Only trigger if hands are reasonably separated
            if (handDistance > 0.1f) {
                previousHandX?.let { prevX ->
                    val avgX = (hand1X + hand2X) / 2f
                    val deltaX = avgX - prevX
                    
                    if (abs(deltaX) > GESTURE_THRESHOLD) {
                        // Right = increase bass
                        val bassDelta = (deltaX * 200).toInt().coerceIn(-20, 20)
                        if (bassDelta != 0) {
                            onBassChange(bassDelta)
                            _currentGesture.value = DJGesture.BassCrossfader(bassDelta)
                        }
                    }
                }
                previousHandX = (hand1X + hand2X) / 2f
            }
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        handLandmarker?.close()
        handLandmarker = null
        _isInitialized.value = false
        Log.d(TAG, "🔌 DJ Gesture Controller released")
    }
}

/**
 * DJ Gesture Types
 */
sealed class DJGesture {
    object None : DJGesture()
    data class VolumeFader(val delta: Int) : DJGesture()
    data class BassCrossfader(val delta: Int) : DJGesture()
}
