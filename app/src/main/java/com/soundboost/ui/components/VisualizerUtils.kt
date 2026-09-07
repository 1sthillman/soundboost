package com.soundboost.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/**
 * Utility functions for high-quality visualizer rendering
 * Matching HTML Canvas API features in Android Compose
 */

/**
 * Draw circle with REAL glow effect (HTML ctx.shadowBlur simulation)
 * Uses multiple expanding circles with decreasing alpha for realistic glow
 */
fun DrawScope.drawCircleWithGlow(
    color: Color,
    radius: Float,
    center: Offset,
    blur: Float = 0f,
    glowColor: Color = color
) {
    if (blur > 0f) {
        // Draw 8 glow layers for smooth, realistic blur
        for (i in 1..8) {
            val glowRadius = radius + (blur * i / 4f)
            val alpha = (blur / 20f) * (1f - i / 9f) // Exponential falloff
            drawCircle(
                color = glowColor.copy(alpha = alpha * glowColor.alpha),
                radius = glowRadius,
                center = center,
                blendMode = BlendMode.Plus
            )
        }
    }
    // Main circle
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
}

/**
 * Draw path with REAL glow effect
 */
fun DrawScope.drawPathWithGlow(
    path: Path,
    color: Color,
    blur: Float = 0f,
    strokeWidth: Float,
    cap: StrokeCap = StrokeCap.Round,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    if (blur > 0f) {
        // 6 glow layers
        for (i in 1..6) {
            val glowWidth = strokeWidth + (blur * i / 3f)
            val alpha = (blur / 20f) * (1f - i / 7f)
            drawPath(
                path,
                color.copy(alpha = alpha * color.alpha),
                style = Stroke(width = glowWidth, cap = cap),
                blendMode = BlendMode.Plus
            )
        }
    }
    // Main path
    drawPath(
        path,
        color,
        style = Stroke(width = strokeWidth, cap = cap),
        blendMode = blendMode
    )
}

/**
 * Draw line with REAL glow effect
 */
fun DrawScope.drawLineWithGlow(
    start: Offset,
    end: Offset,
    color: Color,
    blur: Float = 0f,
    strokeWidth: Float,
    cap: StrokeCap = StrokeCap.Round,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    if (blur > 0f) {
        for (i in 1..6) {
            val glowWidth = strokeWidth + (blur * i / 3f)
            val alpha = (blur / 20f) * (1f - i / 7f)
            drawLine(
                color.copy(alpha = alpha * color.alpha),
                start,
                end,
                strokeWidth = glowWidth,
                cap = cap,
                blendMode = BlendMode.Plus
            )
        }
    }
    drawLine(
        color,
        start,
        end,
        strokeWidth = strokeWidth,
        cap = cap,
        blendMode = blendMode
    )
}

/**
 * Smooth bezier path through points (matches HTML smoothPath function)
 */
fun smoothPath(path: Path, points: List<Offset>, close: Boolean = false) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return
    }
    
    path.moveTo(points[0].x, points[0].y)
    
    for (i in 1 until points.size - 1) {
        val mx = (points[i].x + points[i + 1].x) / 2f
        val my = (points[i].y + points[i + 1].y) / 2f
        path.quadraticTo(points[i].x, points[i].y, mx, my)
    }
    
    if (points.size > 1) {
        val last = points.last()
        path.lineTo(last.x, last.y)
    }
    
    if (close) {
        path.close()
    }
}

/**
 * Create radial gradient brush (matches HTML ctx.createRadialGradient)
 */
fun radialGradientBrush(
    colors: List<Color>,
    center: Offset,
    radius: Float,
    stops: List<Float>? = null
): Brush {
    return Brush.radialGradient(
        colorStops = if (stops != null) {
            colors.zip(stops).map { it.second to it.first }.toTypedArray()
        } else {
            colors.mapIndexed { i, c -> 
                (i.toFloat() / (colors.size - 1f)) to c 
            }.toTypedArray()
        },
        center = center,
        radius = radius
    )
}

/**
 * Linear interpolation between colors
 */
fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

/**
 * Get energy floor (prevents division by zero in brightness calculations)
 */
fun energyFloor(v: Float): Float = maxOf(0.05f, v)

/**
 * Analyze audio into bands (matches HTML analyze() function)
 */
data class AudioAnalysis(
    val sub: Float,        // 0-3 bins
    val bass: Float,       // 1-7 bins
    val lowMid: Float,     // 7-15 bins
    val mid: Float,        // 15-26 bins
    val highMid: Float,    // 26-36 bins
    val treble: Float,     // 36-46 bins
    val energy: Float,     // Smoothed overall energy
    val brightness: Float, // Treble / energyFloor(sub+bass)
    val beat: Boolean,     // Onset detection
    val punch: Float       // Transient strength
)

class AudioAnalyzer {
    private val fluxHistory = mutableListOf<Float>()
    private var prevBandFrame: FloatArray? = null
    private var smoothedEnergy = 0f
    private var beatCooldown = 0
    
    fun analyze(bars: FloatArray, sensitivity: Float = 0.55f): AudioAnalysis {
        val sub = bandAvg(bars, 0, 3)
        val bass = bandAvg(bars, 1, 7)
        val lowMid = bandAvg(bars, 7, 15)
        val mid = bandAvg(bars, 15, 26)
        val highMid = bandAvg(bars, 26, 36)
        val treble = bandAvg(bars, 36, 46)
        
        val frameEnergy = (sub * 1.3f + bass) / 2.3f
        smoothedEnergy += (frameEnergy - smoothedEnergy) * 0.12f
        
        // Spectral flux (onset detection)
        var flux = 0f
        val prev = prevBandFrame
        if (prev != null) {
            for (i in bars.indices) {
                flux += maxOf(0f, bars[i] - prev[i])
            }
        }
        prevBandFrame = bars.copyOf()
        
        fluxHistory.add(flux)
        if (fluxHistory.size > 30) fluxHistory.removeAt(0)
        
        val avgFlux = fluxHistory.average().toFloat()
        val fluxThresh = avgFlux * (1.55f - sensitivity * 0.4f) + 0.6f
        
        beatCooldown = maxOf(0, beatCooldown - 1)
        val beat = beatCooldown == 0 && flux > fluxThresh && frameEnergy > 0.22f
        if (beat) beatCooldown = 7
        
        val punch = maxOf(0f, minOf(1f, (flux - avgFlux) / 6f))
        val brightness = treble / energyFloor(sub + bass)
        
        return AudioAnalysis(
            sub, bass, lowMid, mid, highMid, treble,
            smoothedEnergy, brightness, beat, punch
        )
    }
    
    private fun bandAvg(bars: FloatArray, from: Int, to: Int): Float {
        val start = maxOf(0, from)
        val end = minOf(bars.size, to)
        if (start >= end) return 0f
        return bars.slice(start until end).average().toFloat()
    }
}
