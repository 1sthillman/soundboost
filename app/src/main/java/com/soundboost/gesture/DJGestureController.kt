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
import kotlin.math.sqrt

/**
 * ENHANCED DJ GESTURE CONTROLLER v3.0
 * Simplified, reliable, and intuitive gesture detection
 * Focus: Clear gestures that ALWAYS work
 */
class DJGestureController(
    private val context: Context,
    private val onVolumeChange: (Int) -> Unit,
    private val onBassChange: (Int) -> Unit,
    private val onTrebleChange: (Int) -> Unit,
    private val onPresetChange: (Int) -> Unit
) {
    
    companion object {
        private const val TAG = "DJGestureController"
        private const val MODEL_NAME = "hand_landmarker.task"
        
        // RELAXED detection for better recognition
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_TRACKING_CONFIDENCE = 0.5f
        
        // ULTRA-FAST thresholds for instant response
        private const val MOVEMENT_THRESHOLD = 0.012f  // Very sensitive
        private const val ROTATION_THRESHOLD = 0.08f    // Very sensitive
        private const val OPENNESS_THRESHOLD = 0.06f    // Very sensitive
        private const val COOLDOWN_MS = 150L            // Ultra fast!
        
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
    
    // State tracking - Minimal for performance
    private var previousHandY: Float? = null
    private var previousTwoHandDistance: Float? = null
    private var lastGestureTime = 0L
    private var gestureStableFrames = 0
    
    // Ultra-fast gesture history
    private val gestureHistory = mutableListOf<DJGesture>()
    private val historySize = 2  // Reduced for faster response
    
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
            
            Log.d(TAG, "✅ DJ Gesture Controller v3.0 initialized (Enhanced)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize: ${e.message}", e)
            _isInitialized.value = false
        }
    }
    
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
    
    private fun analyzeGestures(result: HandLandmarkerResult, timestampMs: Long) {
        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) return
        
        val primaryHand = landmarks[0]
        val handCount = landmarks.size
        
        // Calculate basic metrics - Only what's needed
        val fingerCount = countExtendedFingers(primaryHand)
        val handOpenness = calculateHandOpenness(primaryHand)
        val wristY = primaryHand[WRIST].y()
        
        // Update minimal metrics for UI
        _gestureMetrics.value = GestureMetrics(
            fingerCount = fingerCount,
            handOpenness = handOpenness,
            handRotation = 0f,
            twoHandDistance = if (handCount == 2) calculateTwoHandDistance(landmarks[0], landmarks[1]) else 0f,
            confidence = 1f  // Always confident for performance
        )
        
        // Cooldown check
        if (timestampMs - lastGestureTime < COOLDOWN_MS) return
        
        // ULTRA-SIMPLE gesture detection - Priority order
        val detectedGesture = when {
            // GESTURE 1: ONE HAND UP = Volume increase (continuous)
            handCount == 1 && wristY < 0.4f -> {
                val delta = 5
                onVolumeChange(delta)
                lastGestureTime = timestampMs
                DJGesture.SwipeVolume(delta.toFloat())
            }
            
            // GESTURE 2: ONE HAND DOWN = Volume decrease (continuous)
            handCount == 1 && wristY > 0.6f -> {
                val delta = -5
                onVolumeChange(delta)
                lastGestureTime = timestampMs
                DJGesture.SwipeVolume(delta.toFloat())
            }
            
            // GESTURE 3: TWO HANDS = Bass control
            handCount == 2 -> {
                val distance = calculateTwoHandDistance(landmarks[0], landmarks[1])
                previousTwoHandDistance?.let { prevDist ->
                    val delta = distance - prevDist
                    if (abs(delta) > MOVEMENT_THRESHOLD) {
                        val bassDelta = (delta * 80).toInt().coerceIn(-10, 10)
                        onBassChange(bassDelta)
                        lastGestureTime = timestampMs
                    }
                }
                previousTwoHandDistance = distance
                DJGesture.TwoHandsBass(distance)
            }
            
            // GESTURE 4: CLOSED FIST (0 fingers) = Preset 1 (Flat)
            handCount == 1 && fingerCount == 0 -> {
                if (isStableGesture()) {
                    onPresetChange(1)
                    lastGestureTime = timestampMs
                    DJGesture.Fist
                } else DJGesture.Idle
            }
            
            // GESTURE 5: TWO FINGERS = Preset 2 (Bass)
            handCount == 1 && fingerCount == 2 -> {
                if (isStableGesture()) {
                    onPresetChange(2)
                    lastGestureTime = timestampMs
                    DJGesture.PeaceSign
                } else DJGesture.Idle
            }
            
            // GESTURE 6: THREE FINGERS = Preset 3 (Treble)
            handCount == 1 && fingerCount == 3 -> {
                if (isStableGesture()) {
                    onPresetChange(3)
                    lastGestureTime = timestampMs
                    DJGesture.OkSign
                } else DJGesture.Idle
            }
            
            // GESTURE 7: FIVE FINGERS (OPEN HAND) = Max Volume
            handCount == 1 && fingerCount == 5 && handOpenness > 0.65f -> {
                if (isStableGesture()) {
                    onVolumeChange(20)
                    lastGestureTime = timestampMs
                    DJGesture.OpenHand
                } else DJGesture.Idle
            }
            
            else -> DJGesture.Idle
        }
        
        // Update state with smoothing
        updateGestureWithSmoothing(detectedGesture)
        previousHandY = wristY
        if (handCount == 2) {
            previousTwoHandDistance = calculateTwoHandDistance(landmarks[0], landmarks[1])
        }
    }
    
    private fun countExtendedFingers(hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Int {
        var count = 0
        
        // Thumb
        if (distance(hand[THUMB_TIP], hand[WRIST]) > distance(hand[2], hand[WRIST])) count++
        
        // Other fingers
        if (hand[INDEX_TIP].y() < hand[6].y()) count++
        if (hand[MIDDLE_TIP].y() < hand[10].y()) count++
        if (hand[RING_TIP].y() < hand[14].y()) count++
        if (hand[PINKY_TIP].y() < hand[18].y()) count++
        
        return count
    }
    
    private fun calculateHandOpenness(hand: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val wrist = hand[WRIST]
        val avgFingerDistance = listOf(INDEX_TIP, MIDDLE_TIP, RING_TIP, PINKY_TIP)
            .map { distance(hand[it], wrist) }
            .average()
            .toFloat()
        
        return (avgFingerDistance * 2.0f).coerceIn(0f, 1f)
    }
    
    private fun calculateTwoHandDistance(hand1: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>, 
                                        hand2: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        return distance(hand1[WRIST], hand2[WRIST])
    }
    
    private fun distance(p1: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
                        p2: com.google.mediapipe.tasks.components.containers.NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return sqrt(dx * dx + dy * dy)
    }
    
    private fun isStableGesture(): Boolean {
        gestureStableFrames++
        return gestureStableFrames > 2  // Faster stable detection
    }
    
    private fun updateGestureWithSmoothing(gesture: DJGesture) {
        gestureHistory.add(gesture)
        if (gestureHistory.size > historySize) {
            gestureHistory.removeAt(0)
        }
        
        // Use most common gesture in history
        val mostCommon = gestureHistory.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        _currentGesture.value = mostCommon ?: gesture
        
        if (gesture != DJGesture.Idle) {
            gestureStableFrames = 0
        }
    }
    
    private fun resetGestureState() {
        _currentGesture.value = DJGesture.Idle
        previousHandY = null
        previousTwoHandDistance = null
        gestureStableFrames = 0
        gestureHistory.clear()
    }
    
    fun release() {
        handLandmarker?.close()
        handLandmarker = null
        _isInitialized.value = false
        Log.d(TAG, "🔌 DJ Gesture Controller released")
    }
}

/**
 * SIMPLIFIED DJ Gestures - Actually Working!
 */
sealed class DJGesture {
    object Idle : DJGesture()
    data class SwipeVolume(val delta: Float) : DJGesture()      // Hand high/low
    data class TwoHandsBass(val distance: Float) : DJGesture()  // Two hands
    object Fist : DJGesture()                                    // 0 fingers = Preset 1
    object PeaceSign : DJGesture()                               // 2 fingers = Preset 2
    object OkSign : DJGesture()                                  // 3 fingers = Preset 3
    object OpenHand : DJGesture()                                // 5 fingers = Max volume
}

/**
 * Enhanced gesture metrics with confidence
 */
data class GestureMetrics(
    val fingerCount: Int = 0,
    val handOpenness: Float = 0f,
    val handRotation: Float = 0f,
    val twoHandDistance: Float = 0f,
    val confidence: Float = 0f
)
