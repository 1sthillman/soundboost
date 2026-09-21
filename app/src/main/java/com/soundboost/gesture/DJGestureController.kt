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
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * PROFESSIONAL DJ GESTURE CONTROLLER v2.0
 * Advanced hand gesture recognition with 6 distinct gesture types
 * Optimized for real-time audio control with MediaPipe
 */
class DJGestureController(
    private val context: Context,
    private val onVolumeChange: (Int) -> Unit,
    private val onBassChange: (Int) -> Unit,
    private val onTrebleChange: (Int) -> Unit,
    private val onPresetChange: (Int) -> Unit  // 1-5 based on finger count
) {
    
    companion object {
        private const val TAG = "DJGestureController"
        private const val MODEL_NAME = "hand_landmarker.task"
        private const val MIN_DETECTION_CONFIDENCE = 0.6f
        private const val MIN_TRACKING_CONFIDENCE = 0.6f
        
        // Gesture thresholds
        private const val MOVEMENT_THRESHOLD = 0.02f
        private const val ROTATION_THRESHOLD = 0.15f
        private const val OPENNESS_THRESHOLD = 0.12f
        private const val COOLDOWN_MS = 300L
        
        // Landmark indices
        private const val WRIST = 0
        private const val THUMB_TIP = 4
        private const val INDEX_TIP = 8
        private const val MIDDLE_TIP = 12
        private const val RING_TIP = 16
        private const val PINKY_TIP = 20
    }
    
    private var handLandmarker: HandLandmarker? = null
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _currentGesture = MutableStateFlow<DJGesture>(DJGesture.Idle)
    val currentGesture: StateFlow<DJGesture> = _currentGesture.asStateFlow()
    
    private val _gestureMetrics = MutableStateFlow(GestureMetrics())
    val gestureMetrics: StateFlow<GestureMetrics> = _gestureMetrics.asStateFlow()
    
    // State tracking
    private var previousHandY: Float? = null
    private var previousHandRotation: Float? = null
    private var previousHandOpenness: Float? = null
    private var previousTwoHandDistance: Float? = null
    private var lastGestureTime = 0L
    private var consecutiveFramesWithSameGesture = 0
    
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
            
            Log.d(TAG, "✅ DJ Gesture Controller v2.0 initialized")
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
                analyzeGestures(result, timestampMs)
            } else {
                resetGestureState()
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Frame processing error: ${e.message}")
        }
    }
    
    /**
     * Advanced gesture analysis with multiple detection algorithms
     */
    private fun analyzeGestures(result: HandLandmarkerResult, timestampMs: Long) {
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) return
        
        val primaryHand = landmarks[0]
        val handCount = landmarks.size
        
        // Calculate hand metrics
        val fingerCount = countExtendedFingers(primaryHand)
        val handOpenness = calculateHandOpenness(primaryHand)
        val handRotation = calculateHandRotation(primaryHand)
        val wristY = primaryHand[WRIST].y()
        
        // Update metrics for UI
        _gestureMetrics.value = GestureMetrics(
            fingerCount = fingerCount,
            handOpenness = handOpenness,
            handRotation = handRotation,
            twoHandDistance = if (handCount == 2) calculateTwoHandDistance(landmarks[0], landmarks[1]) else 0f
        )
        
        // Cooldown check
        if (timestampMs - lastGestureTime < COOLDOWN_MS) return
        
        // Priority-based gesture detection
        val detectedGesture = when {
            // GESTURE 1: Finger Count Presets (Highest Priority - Static)
            fingerCount in 1..5 && handCount == 1 -> {
                if (isHandStable(wristY, handOpenness, handRotation)) {
                    consecutiveFramesWithSameGesture++
                    if (consecutiveFramesWithSameGesture > 5) {
                        onPresetChange(fingerCount)
                        lastGestureTime = timestampMs
                        DJGesture.PresetSelect(fingerCount)
                    } else DJGesture.Idle
                } else {
                    consecutiveFramesWithSameGesture = 0
                    DJGesture.Idle
                }
            }
            
            // GESTURE 2: Hand Openness = Volume Control
            handCount == 1 && previousHandOpenness != null -> {
                val opennessDelta = handOpenness - previousHandOpenness!!
                if (abs(opennessDelta) > OPENNESS_THRESHOLD) {
                    val volumeDelta = (opennessDelta * 100).toInt().coerceIn(-15, 15)
                    onVolumeChange(volumeDelta)
                    lastGestureTime = timestampMs
                    DJGesture.VolumeControl(handOpenness)
                } else DJGesture.Idle
            }
            
            // GESTURE 3: Two Hand Distance = Bass Boost
            handCount == 2 && previousTwoHandDistance != null -> {
                val distance = calculateTwoHandDistance(landmarks[0], landmarks[1])
                val distanceDelta = distance - previousTwoHandDistance!!
                if (abs(distanceDelta) > MOVEMENT_THRESHOLD) {
                    val bassDelta = (distanceDelta * 150).toInt().coerceIn(-20, 20)
                    onBassChange(bassDelta)
                    lastGestureTime = timestampMs
                    DJGesture.BassBoost(distance)
                } else DJGesture.Idle
            }
            
            // GESTURE 4: Hand Rotation = Treble Control
            handCount == 1 && previousHandRotation != null -> {
                val rotationDelta = handRotation - previousHandRotation!!
                if (abs(rotationDelta) > ROTATION_THRESHOLD) {
                    val trebleDelta = (rotationDelta * 80).toInt().coerceIn(-15, 15)
                    onTrebleChange(trebleDelta)
                    lastGestureTime = timestampMs
                    DJGesture.TrebleControl(handRotation)
                } else DJGesture.Idle
            }
            
            // GESTURE 5: Vertical Movement = Master Fader
            handCount == 1 && previousHandY != null -> {
                val deltaY = wristY - previousHandY!!
                if (abs(deltaY) > MOVEMENT_THRESHOLD) {
                    val volumeDelta = (-deltaY * 150).toInt().coerceIn(-20, 20)
                    onVolumeChange(volumeDelta)
                    lastGestureTime = timestampMs
                    DJGesture.MasterFader(deltaY)
                } else DJGesture.Idle
            }
            
            else -> DJGesture.Idle
        }
        
        _currentGesture.value = detectedGesture
        
        // Update state
        previousHandY = wristY
        previousHandOpenness = handOpenness
        previousHandRotation = handRotation
        if (handCount == 2) {
            previousTwoHandDistance = calculateTwoHandDistance(landmarks[0], landmarks[1])
        }
    }
    
    /**
     * Count extended fingers (1-5)
     */
    private fun countExtendedFingers(hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Int {
        var count = 0
        
        // Thumb - check if tip is farther from wrist than base
        if (distance(hand[THUMB_TIP], hand[WRIST]) > distance(hand[2], hand[WRIST])) count++
        
        // Other fingers - check if tip is higher than middle joint
        if (hand[INDEX_TIP].y() < hand[6].y()) count++
        if (hand[MIDDLE_TIP].y() < hand[10].y()) count++
        if (hand[RING_TIP].y() < hand[14].y()) count++
        if (hand[PINKY_TIP].y() < hand[18].y()) count++
        
        return count
    }
    
    /**
     * Calculate hand openness (0.0 = closed, 1.0 = open)
     */
    private fun calculateHandOpenness(hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val wrist = hand[WRIST]
        val avgFingerDistance = listOf(INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP)
            .map { distance(hand[it], wrist) }
            .average()
            .toFloat()
        
        return (avgFingerDistance * 2.5f).coerceIn(0f, 1f)
    }
    
    /**
     * Calculate hand rotation angle (-1.0 to 1.0)
     */
    private fun calculateHandRotation(hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val wrist = hand[WRIST]
        val middleFinger = hand[MIDDLE_TIP]
        val angle = atan2(
            (middleFinger.x() - wrist.x()).toDouble(),
            (middleFinger.y() - wrist.y()).toDouble()
        ).toFloat()
        return (angle / Math.PI.toFloat()).coerceIn(-1f, 1f)
    }
    
    /**
     * Calculate distance between two hands
     */
    private fun calculateTwoHandDistance(hand1: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>, 
                                        hand2: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        return distance(hand1[WRIST], hand2[WRIST])
    }
    
    /**
     * Distance helper
     */
    private fun distance(p1: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
                        p2: com.google.mediapipe.tasks.components.containers.NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Check if hand is stable (not moving)
     */
    private fun isHandStable(currentY: Float, currentOpenness: Float, currentRotation: Float): Boolean {
        val yStable = previousHandY?.let { abs(currentY - it) < MOVEMENT_THRESHOLD / 2 } ?: true
        val opennessStable = previousHandOpenness?.let { abs(currentOpenness - it) < OPENNESS_THRESHOLD / 2 } ?: true
        val rotationStable = previousHandRotation?.let { abs(currentRotation - it) < ROTATION_THRESHOLD / 2 } ?: true
        return yStable && opennessStable && rotationStable
    }
    
    /**
     * Reset gesture state
     */
    private fun resetGestureState() {
        _currentGesture.value = DJGesture.Idle
        previousHandY = null
        previousHandOpenness = null
        previousHandRotation = null
        previousTwoHandDistance = null
        consecutiveFramesWithSameGesture = 0
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
 * DJ Gesture Types - Professional Audio Control
 */
sealed class DJGesture {
    object Idle : DJGesture()
    data class PresetSelect(val fingerCount: Int) : DJGesture()  // 1-5 fingers
    data class VolumeControl(val openness: Float) : DJGesture()  // Hand open/close
    data class BassBoost(val distance: Float) : DJGesture()      // Two hand distance
    data class TrebleControl(val rotation: Float) : DJGesture()  // Hand rotation
    data class MasterFader(val deltaY: Float) : DJGesture()      // Vertical movement
}

/**
 * Real-time gesture metrics for UI display
 */
data class GestureMetrics(
    val fingerCount: Int = 0,
    val handOpenness: Float = 0f,
    val handRotation: Float = 0f,
    val twoHandDistance: Float = 0f
)
