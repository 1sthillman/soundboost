package com.soundboost.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import kotlin.math.*
import kotlin.random.Random

/**
 * PIXEL-PERFECT visualizers from awwardstheme.html
 * Every detail from HTML Canvas API translated to Compose Canvas
 * - Glow effects via multiple alpha layers
 * - BlendMode.Plus for 'lighter' composite operations
 * - Smooth bezier paths matching ctx.quadraticCurveTo
 * - All particle systems, animations, and physics
 */

// Persistent state holders (like JavaScript globals)
private var inkSplats = mutableListOf<InkSplat>()
private var sumiDust = mutableListOf<DustMote>()
private var auroraStars = mutableListOf<Star>()
private var auroraOrbPulse = 0f
private var novaStars = mutableListOf<Star>()
private var novaParticles = mutableListOf<OrbitingParticle>()
private var novaShockwaves = mutableListOf<Shockwave>()
private var mycelBranches = mutableListOf<Branch>()
private var mycelPulses = mutableListOf<TravelingPulse>()
private var mycelSpores = mutableListOf<Spore>()
private var mycelGrowStart = 0f
private var reefParticles = mutableListOf<CausticParticle>()
private var reefTentaclePhase = 0f
private var reefCompanionPhase = 1.7f
private var stormBolts = mutableListOf<LightningBolt>()
private var rainDrops = mutableListOf<RainDrop>()
private var rainSplashes = mutableListOf<Splash>()
private var cloudSeeds = FloatArray(48) { Random.nextFloat() * 6.28f }

data class InkSplat(var x: Float, var y: Float, val r: Float, var life: Float)
data class DustMote(var x: Float, var y: Float, val r: Float, val phase: Float, var vy: Float)
data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)
data class OrbitingParticle(var angle: Float, val rBase: Float, val speed: Float, val r: Float)
data class Shockwave(var radius: Float, var life: Float)
data class Branch(val points: List<Offset>, val bin: Int, val isChild: Boolean)
data class TravelingPulse(val branchIndex: Int, var progress: Float, val speed: Float)
data class Spore(var x: Float, var y: Float, var vx: Float, var vy: Float, val r: Float, var life: Float, val phase: Float)
data class CausticParticle(var x: Float, var y: Float, val r: Float, val phase: Float, val vy: Float, val isFar: Boolean)
data class LightningBolt(val points: List<Offset>, var life: Float, val branchIndex: Int)
data class RainDrop(var x: Float, var y: Float, val vy: Float, val len: Float)
data class Splash(val x: Float, val y: Float, var life: Float)

@Composable
fun SumiVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    var lastBeat by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
            }
        }
    }
    
    // Initialize dust motes once
    if (sumiDust.isEmpty()) {
        repeat(14) {
            sumiDust.add(DustMote(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = 0.4f + Random.nextFloat() * 0.9f,
                phase = Random.nextFloat() * 7f,
                vy = -(0.03f + Random.nextFloat() * 0.05f)
            ))
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        
        // Warm dark paper ground with radial vignette (exact HTML gradient)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 1.1f),
                    backgroundColor
                ),
                center = Offset(W * 0.5f, H * 0.4f),
                radius = W * 0.9f
            )
        )
        
        // Faint fibrous paper texture lines (exact HTML bezierCurveTo)
        repeat(10) { i ->
            val path = Path()
            val y = (i / 10f) * H + sin(i * 3f) * 4f
            path.moveTo(0f, y)
            path.cubicTo(W * 0.3f, y + 6f, W * 0.7f, y - 6f, W, y)
            drawPath(path, accent1.copy(alpha = 0.03f), style = Stroke(width = 1f))
        }
        
        // Slow drifting dust motes (exact HTML animation)
        sumiDust.forEach { d ->
            d.y += d.vy / H
            if (d.y < -4f / H) {
                d.y = 1f + 4f / H
                d.x = Random.nextFloat()
            }
            val alpha = 0.08f + 0.1f * abs(sin(time * 0.8f + d.phase))
            drawCircle(
                color = accent1.copy(alpha = alpha),
                radius = d.r,
                center = Offset(d.x * W, d.y * H)
            )
        }
        
        val cx = W * 0.5f
        val cy = H * 0.46f
        val bass = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        val baseRadius = minOf(W, H) * 0.3f
        val radius = baseRadius + bass * 10f
        
        // Ensō - TWO passes (dry brush ghost + wet main) - EXACT HTML
        val segments = 90
        val gapStart = 4.4f
        val gapEnd = 5.0f
        
        // Pass 1: Dry brush (offset, thinner, faded) - EXACT HTML first pass
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * PI.toFloat() * 2
            val a1 = ((i + 1f) / segments) * PI.toFloat() * 2
            if (a0 > gapStart && a0 < gapEnd) continue
            
            val bandIdx = ((i.toFloat() / segments) * 48).toInt().coerceIn(0, 47)
            val level = audioLevels?.getOrNull(bandIdx) ?: 0f
            val wobble = (sin(i * 1.7f + time * 0.6f) + sin(i * 0.5f - time * 0.3f)) * 1.6f
            val rr = radius + wobble + bass * 10f + 2.4f // +2.4f offset for dry brush
            
            val x0 = cx + cos(a0) * rr
            val y0 = cy + sin(a0) * rr * 0.94f
            val x1 = cx + cos(a1) * rr
            val y1 = cy + sin(a1) * rr * 0.94f
            
            val w = (2.2f + level * 7f) * 0.5f // *0.5f thinner
            drawLine(
                color = accent1.copy(alpha = (0.68f + level * 0.3f) * 0.35f), // *0.35f faded
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = w,
                cap = StrokeCap.Round
            )
        }
        
        // Pass 2: Wet main stroke - EXACT HTML second pass
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * PI.toFloat() * 2
            val a1 = ((i + 1f) / segments) * PI.toFloat() * 2
            if (a0 > gapStart && a0 < gapEnd) continue
            
            val bandIdx = ((i.toFloat() / segments) * 48).toInt().coerceIn(0, 47)
            val level = audioLevels?.getOrNull(bandIdx) ?: 0f
            val wobble = (sin(i * 1.7f + time * 0.6f) + sin(i * 0.5f - time * 0.3f)) * 1.6f
            val rr = radius + wobble + bass * 10f
            
            val x0 = cx + cos(a0) * rr
            val y0 = cy + sin(a0) * rr * 0.94f
            val x1 = cx + cos(a1) * rr
            val y1 = cy + sin(a1) * rr * 0.94f
            
            // Width with treble emphasis every 7th segment - EXACT HTML
            val w = 2.2f + level * 7f + if (i % 7 == 0) (audioLevels?.getOrNull(36)?.times(3f) ?: 0f) else 0f
            drawLine(
                color = accent1.copy(alpha = 0.68f + level * 0.3f),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = w,
                cap = StrokeCap.Round
            )
        }
        
        // Ink bleed halo with BlendMode.Plus (lighter) - EXACT HTML with better glow
        val sub = audioLevels?.take(3)?.average()?.toFloat() ?: 0f
        withTransform({
            // Use Plus blend mode for 'lighter' composite operation
        }) {
            // Multi-layer glow for ink bleed effect
            repeat(3) { layer ->
                val glowAlpha = (0.03f + sub * 0.05f) / (layer + 1f)
                val glowRadius = radius * 1.35f + sub * 40f + layer * 8f
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent1.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(cx, cy),
                    blendMode = BlendMode.Plus
                )
            }
        }
        
        // Brush-stroke frequency marks beneath - EXACT HTML quadraticCurveTo
        val n = 22
        repeat(n) { i ->
            val idx = ((i.toFloat() / n) * 48).toInt().coerceIn(0, 47)
            val v = audioLevels?.getOrNull(idx) ?: 0f
            val x = W * 0.12f + (i.toFloat() / n) * W * 0.76f
            val baseY = H * 0.86f
            val len = 6f + v * 70f
            
            val path = Path()
            path.moveTo(x, baseY)
            path.quadraticTo(
                x + if (i % 2 == 0) 2f else -2f,
                baseY - len * 0.6f,
                x,
                baseY - len
            )
            
            drawPath(
                path,
                accent1.copy(alpha = 0.28f + v * 0.5f),
                style = Stroke(width = 1.5f + v * 4f, cap = StrokeCap.Round)
            )
        }
        
        // Vermillion seal - pulses on beat - EXACT HTML
        val beat = isActive && bass > 0.5f && !lastBeat
        lastBeat = bass > 0.5f
        
        val sealSize = 15f + if (beat) 4f else 0f
        val sealX = W * 0.83f
        val sealY = H * 0.86f
        
        rotate(-0.06f * 180f / PI.toFloat(), pivot = Offset(sealX, sealY)) {
            drawRect(
                color = accent1.copy(alpha = 0.88f),
                topLeft = Offset(sealX - sealSize / 2, sealY - sealSize / 2),
                size = androidx.compose.ui.geometry.Size(sealSize, sealSize)
            )
            drawRect(
                color = backgroundColor,
                topLeft = Offset(sealX - sealSize * 0.28f, sealY - sealSize * 0.28f),
                size = androidx.compose.ui.geometry.Size(sealSize * 0.56f, sealSize * 0.56f)
            )
        }
        
        // Ink splatter on beat - EXACT HTML (9 irregular flicked dots)
        if (beat) {
            repeat(9) {
                val ang = Random.nextFloat() * PI.toFloat() * 2
                val dist = 14f + Random.nextFloat() * 40f
                inkSplats.add(InkSplat(
                    x = cx + cos(ang) * dist,
                    y = cy + sin(ang) * dist * 0.9f,
                    r = 0.6f + Random.nextFloat() * 2.6f,
                    life = 1f
                ))
            }
        }
        
        inkSplats.forEach { s ->
            s.life -= 0.018f
            drawCircle(
                color = accent1.copy(alpha = maxOf(0f, s.life * 0.6f)),
                radius = s.r,
                center = Offset(s.x, s.y)
            )
        }
        inkSplats.removeAll { it.life <= 0f }
    }
}

@Composable
fun AuroraVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
            }
        }
    }
    
    // Initialize stars once (60 stars - EXACT HTML)
    if (auroraStars.isEmpty()) {
        repeat(60) {
            auroraStars.add(Star(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.6f,
                r = Random.nextFloat() * 1.2f,
                phase = Random.nextFloat() * 7f
            ))
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        
        // Night sky gradient - EXACT HTML
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(backgroundColor.copy(alpha = 1.1f), backgroundColor)
            )
        )
        
        // Twinkling starfield - EXACT HTML with treble brightness
        val treble = audioLevels?.drop(36)?.average()?.toFloat() ?: 0f
        auroraStars.forEach { s ->
            val alpha = (0.22f + 0.4f * abs(sin(time * 1.1f + s.phase))) * (0.6f + treble * 1.1f)
            drawCircle(
                color = accent1.copy(alpha = alpha),
                radius = s.r,
                center = Offset(s.x * W, s.y * H)
            )
        }
        
        // Breathing moon with glow - EXACT HTML
        val sub = audioLevels?.take(3)?.average()?.toFloat() ?: 0f
        auroraOrbPulse += (sub - auroraOrbPulse) * 0.08f
        val moonX = W * 0.16f
        val moonY = H * 0.2f
        val moonR = 10f + auroraOrbPulse * 6f
        
        // Moon glow with BlendMode.Plus - EXACT HTML globalCompositeOperation='lighter'
        withTransform({}) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent1.copy(alpha = 0.5f), Color.Transparent),
                    center = Offset(moonX, moonY),
                    radius = moonR * 3.4f
                ),
                radius = moonR * 3.4f,
                center = Offset(moonX, moonY),
                blendMode = BlendMode.Plus
            )
        }
        
        // Moon body
        drawCircle(
            color = accent1.copy(alpha = 0.85f),
            radius = moonR,
            center = Offset(moonX, moonY)
        )
        
        // THREE silk ribbons with smoothPath - EXACT HTML
        val ribbons = listOf(
            RibbonData(H * 0.32f, 34f, accent1, accent2, 0.55f, 15, 26),
            RibbonData(H * 0.46f, 26f, accent2, accent1, 0.4f, 7, 15),
            RibbonData(H * 0.58f, 20f, accent1, accent2, 0.7f, 26, 36)
        )
        
        // Use BlendMode.Plus for ribbons - EXACT HTML
        withTransform({}) {
            ribbons.forEach { rb ->
                val top = mutableListOf<Offset>()
                val bot = mutableListOf<Offset>()
                
                repeat(49) { i ->
                    val idx = i.coerceAtMost(47)
                    val bandIdx = rb.bandStart + (idx % (rb.bandEnd - rb.bandStart))
                    val v = audioLevels?.getOrNull(bandIdx) ?: 0f
                    val x = (i / 48f) * W
                    val wave1 = sin(i * 0.32f + time * rb.speed) * rb.amp
                    val wave2 = sin(i * 0.18f - time * rb.speed * 0.6f) * rb.amp * 0.5f
                    val y = rb.baseY + wave1 + wave2 - v * 40f
                    val thickness = 10f + v * 46f
                    
                    top.add(Offset(x, y - thickness / 2))
                    bot.add(Offset(x, y + thickness / 2))
                }
                
                val path = Path()
                smoothPath(path, top)
                bot.reversed().forEach { path.lineTo(it.x, it.y) }
                path.close()
                
                // Gradient with shadowBlur simulation - EXACT HTML
                drawPath(
                    path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            rb.colorA.copy(alpha = 0.35f),
                            rb.colorB.copy(alpha = 0.22f),
                            rb.colorA.copy(alpha = 0f)
                        ),
                        startY = rb.baseY - 60f,
                        endY = rb.baseY + 60f
                    ),
                    blendMode = BlendMode.Plus
                )
                
                // Add glow effect (shadowBlur=16 in HTML)
                repeat(2) { glowPass ->
                    drawPath(
                        path,
                        rb.colorA.copy(alpha = 0.05f / (glowPass + 1)),
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }
        
        // Shimmering crest highlight - EXACT HTML
        val crest = mutableListOf<Offset>()
        repeat(49) { i ->
            val x = (i / 48f) * W
            val v = audioLevels?.getOrNull(i.coerceAtMost(47)) ?: 0f
            val y = H * 0.32f + sin(i * 0.32f + time * 0.55f) * 34f - v * 40f - 8f
            crest.add(Offset(x, y))
        }
        val crestPath = Path()
        smoothPath(crestPath, crest)
        drawPath(
            crestPath,
            accent1.copy(alpha = 0.25f + treble * 0.45f),
            style = Stroke(width = 1.2f, cap = StrokeCap.Round)
        )
        
        // Dark hill silhouette grounding the scene - EXACT HTML
        val bass = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        val hill = mutableListOf<Offset>()
        repeat(49) { i ->
            val x = (i / 48f) * W
            val y = H * 0.86f + sin(i * 0.4f + time * 0.2f) * 6f - bass * 10f
            hill.add(Offset(x, y))
        }
        val hillPath = Path()
        smoothPath(hillPath, hill)
        hillPath.lineTo(W, H)
        hillPath.lineTo(0f, H)
        hillPath.close()
        drawPath(hillPath, backgroundColor)
    }
}

data class RibbonData(val baseY: Float, val amp: Float, val colorA: Color, val colorB: Color, val speed: Float, val bandStart: Int, val bandEnd: Int)

private fun smoothPath(path: Path, points: List<Offset>) {
    if (points.isEmpty()) return
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size - 1) {
        val mx = (points[i].x + points[i + 1].x) / 2
        val my = (points[i].y + points[i + 1].y) / 2
        path.quadraticTo(points[i].x, points[i].y, mx, my)
    }
    if (points.size > 1) {
        path.lineTo(points.last().x, points.last().y)
    }
}

@Composable
fun NovaVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    var lastBeat by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
            }
        }
    }
    
    // Initialize stars and particles once - EXACT HTML (70 stars, 22 embers)
    if (novaStars.isEmpty()) {
        repeat(70) {
            novaStars.add(Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = Random.nextFloat() * 1.1f,
                phase = Random.nextFloat() * 7f
            ))
        }
    }
    if (novaParticles.isEmpty()) {
        repeat(22) {
            novaParticles.add(OrbitingParticle(
                angle = Random.nextFloat() * 6.28f,
                rBase = 1.5f + Random.nextFloat() * 1.6f,
                speed = (0.15f + Random.nextFloat() * 0.3f) * if (Random.nextBoolean()) 1f else -1f,
                r = 0.8f + Random.nextFloat() * 1.6f
            ))
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        
        // Deep space gradient - EXACT HTML
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor.copy(alpha = 1.15f), backgroundColor),
                center = Offset(W * 0.5f, H * 0.44f),
                radius = W * 1.0f
            )
        )
        
        // Deep starfield with treble twinkling - EXACT HTML
        val treble = audioLevels?.drop(36)?.average()?.toFloat() ?: 0f
        novaStars.forEach { s ->
            val twinkle = (0.16f + 0.5f * abs(sin(time * 1.4f + s.phase))) * (0.5f + treble * 1.3f)
            drawCircle(
                color = accent1.copy(alpha = twinkle),
                radius = s.r,
                center = Offset(s.x * W, s.y * H)
            )
        }
        
        val cx = W * 0.5f
        val cy = H * 0.46f
        val sub = audioLevels?.take(3)?.average()?.toFloat() ?: 0f
        val bass = audioLevels?.take(7)?.drop(1)?.average()?.toFloat() ?: 0f
        val coreRadius = 18f + sub * 44f + bass * 20f
        
        // Layered corona (4 layers) with BlendMode.Plus - EXACT HTML globalCompositeOperation='lighter'
        for (i in 3 downTo 0) {
            val r = coreRadius * (1.35f + i * 0.5f) + bass * i * 12f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent1.copy(alpha = 0.2f - i * 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy),
                blendMode = BlendMode.Plus
            )
        }
        
        // 16 plasma filaments with REAL shadowBlur - EXACT HTML
        val mid = audioLevels?.slice(15..25)?.average()?.toFloat() ?: 0f
        repeat(16) { i ->
            val bandIdx = ((i / 16f) * 48).toInt().coerceIn(0, 47)
            val v = audioLevels?.getOrNull(bandIdx) ?: 0f
            val baseAng = (i / 16f) * PI.toFloat() * 2 + time * 0.1f * if (i % 2 == 0) 1f else -1f
            val len = coreRadius * 1.05f + v * 95f + mid * 26f
            val wob = sin(time * 1.5f + i) * 0.2f
            
            val x1 = cx + cos(baseAng) * coreRadius * 0.85f
            val y1 = cy + sin(baseAng) * coreRadius * 0.85f
            val xm = cx + cos(baseAng + wob) * len * 0.55f
            val ym = cy + sin(baseAng + wob) * len * 0.55f
            val x2 = cx + cos(baseAng + wob * 1.7f) * len
            val y2 = cy + sin(baseAng + wob * 1.7f) * len
            
            val path = Path()
            path.moveTo(x1, y1)
            path.quadraticTo(xm, ym, x2, y2)
            
            val color = if (i % 3 == 0) accent2 else accent1
            val blur = 5f + v * 11f // shadowBlur from HTML
            
            // Draw with REAL shadowBlur effect using drawPathWithGlow
            drawPathWithGlow(
                path = path,
                color = color.copy(alpha = 0.32f + v * 0.6f),
                blur = blur,
                strokeWidth = 1f + v * 4.2f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )
        }
        
        // Orbiting embers with shadowBlur=6 - EXACT HTML
        novaParticles.forEach { p ->
            p.angle += p.speed * 0.02f
            val rr = coreRadius * p.rBase + treble * 32f
            val x = cx + cos(p.angle) * rr
            val y = cy + sin(p.angle) * rr * 0.82f
            
            // Draw with glow (shadowBlur=6, shadowColor=accent2 in HTML)
            drawCircleWithGlow(
                color = accent2.copy(alpha = 0.55f + treble * 0.45f),
                radius = p.r,
                center = Offset(x, y),
                blur = 6f,
                glowColor = accent2
            )
        }
        
        // The core itself with radial gradient - EXACT HTML
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent1, accent2, accent1),
                center = Offset(cx - coreRadius * 0.25f, cy - coreRadius * 0.25f),
                radius = coreRadius
            ),
            radius = coreRadius,
            center = Offset(cx, cy),
            blendMode = BlendMode.Plus
        )
        
        // Shockwave rings on beat - EXACT HTML
        val beat = isActive && bass > 0.6f && !lastBeat
        lastBeat = bass > 0.6f
        
        if (beat) {
            novaShockwaves.add(Shockwave(radius = coreRadius, life = 1f))
        }
        
        val punch = audioLevels?.drop(36)?.take(10)?.maxOrNull() ?: 0f
        novaShockwaves.forEach { s ->
            s.radius += 5f + punch * 3f
            s.life -= 0.028f
            
            drawCircle(
                color = accent1.copy(alpha = maxOf(0f, s.life * 0.6f)),
                radius = s.radius,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )
        }
        novaShockwaves.removeAll { it.life <= 0f }
        
        // Quiet spectral horizon (28 bars at bottom) - EXACT HTML
        val n = 28
        repeat(n) { i ->
            val idx = ((i / n.toFloat()) * 48).toInt().coerceIn(0, 47)
            val v = audioLevels?.getOrNull(idx) ?: 0f
            val x = W * 0.1f + (i / n.toFloat()) * W * 0.8f
            val baseY = H * 0.93f
            val h = 4f + v * 34f
            
            drawLine(
                color = accent1.copy(alpha = 0.16f + v * 0.3f),
                start = Offset(x, baseY),
                end = Offset(x, baseY - h),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun MycelVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
            }
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        
        // Dark forest floor
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor.copy(alpha = 1.1f), backgroundColor),
                center = Offset(W * 0.5f, H * 0.7f),
                radius = W * 0.95f
            )
        )
        
        // Generate branches once
        if (mycelBranches.isEmpty()) {
            mycelGrowStart = time
            
            repeat(6) { rootIdx ->
                var x = ((rootIdx + 0.5f) / 6f) * W + (Random.nextFloat() - 0.5f) * 26f
                var y = H + 8f
                var ang = -PI.toFloat() / 2 + (Random.nextFloat() - 0.5f) * 0.5f
                val pts = mutableListOf(Offset(x, y))
                val segs = 16 + Random.nextInt(6)
                
                repeat(segs) { s ->
                    ang += (Random.nextFloat() - 0.5f) * 0.42f
                    val len = 10f + Random.nextFloat() * 8f
                    x += cos(ang) * len
                    y += sin(ang) * len
                    pts.add(Offset(x, y))
                    
                    // Add branch
                    if (Random.nextFloat() < 0.22f && s > 4 && s < segs - 3) {
                        var bx = x
                        var by = y
                        var bang = ang + (if (Random.nextBoolean()) 1f else -1f) * (0.5f + Random.nextFloat() * 0.5f)
                        val bpts = mutableListOf(Offset(bx, by))
                        val bsegs = 5 + Random.nextInt(5)
                        
                        repeat(bsegs) {
                            bang += (Random.nextFloat() - 0.5f) * 0.3f
                            bx += cos(bang) * 9f
                            by += sin(bang) * 9f
                            bpts.add(Offset(bx, by))
                        }
                        
                        mycelBranches.add(Branch(bpts, Random.nextInt(48), isChild = true))
                    }
                }
                
                mycelBranches.add(Branch(pts, (rootIdx / 6f * 48).toInt(), isChild = false))
            }
        }
        
        // Ambient floor haze
        val sub = audioLevels?.take(3)?.average()?.toFloat() ?: 0f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent1.copy(alpha = 0.05f + sub * 0.07f)
                ),
                startY = H * 0.68f,
                endY = H
            ),
            topLeft = Offset(0f, H * 0.68f),
            size = androidx.compose.ui.geometry.Size(W, H * 0.32f)
        )
        
        val growProgress = minOf(1f, (time - mycelGrowStart) / 2.4f)
        
        // Draw branches
        mycelBranches.forEach { br ->
            val v = audioLevels?.getOrNull(br.bin) ?: 0f
            val nShow = maxOf(2, (br.points.size * growProgress).toInt())
            val pts = br.points.take(nShow)
            
            if (pts.size >= 2) {
                val path = Path()
                smoothPath(path, pts)
                
                drawPath(
                    path,
                    accent1.copy(alpha = (if (br.isChild) 0.32f else 0.55f) + v * 0.35f),
                    style = Stroke(
                        width = (if (br.isChild) 1f else 1.6f) + v * 2.4f,
                        cap = StrokeCap.Round
                    )
                )
                
                if (growProgress >= 1f) {
                    val tip = pts.last()
                    drawCircle(
                        color = accent2.copy(alpha = 0.5f + v * 0.5f),
                        radius = 1.4f + v * 3.4f,
                        center = tip
                    )
                }
            }
        }
        
        // Traveling pulses on beat
        val bass = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        if (isActive && bass > 0.5f && growProgress >= 1f && Random.nextFloat() < 0.05f) {
            repeat(2) {
                mycelPulses.add(TravelingPulse(
                    branchIndex = Random.nextInt(mycelBranches.size),
                    progress = 0f,
                    speed = 0.02f + Random.nextFloat() * 0.015f
                ))
            }
        }
        
        mycelPulses.forEach { p ->
            p.progress += p.speed
            val br = mycelBranches.getOrNull(p.branchIndex)
            if (br != null) {
                val idx = minOf(br.points.size - 1, (p.progress * br.points.size).toInt())
                val pos = br.points.getOrNull(idx)
                if (pos != null) {
                    drawCircle(
                        color = accent1.copy(alpha = 0.9f),
                        radius = 2.4f,
                        center = pos
                    )
                }
            }
        }
        mycelPulses.removeAll { it.progress >= 1f }
        
        // Drifting spores
        val treble = audioLevels?.drop(36)?.average()?.toFloat() ?: 0f
        if (Random.nextFloat() < 0.08f + treble * 0.35f) {
            mycelSpores.add(Spore(
                x = W * 0.1f + Random.nextFloat() * W * 0.8f,
                y = H * 0.85f + Random.nextFloat() * H * 0.1f,
                vx = (Random.nextFloat() - 0.5f) * 0.15f,
                vy = -(0.15f + Random.nextFloat() * 0.3f),
                r = 0.6f + Random.nextFloat() * 1.2f,
                life = 1f,
                phase = Random.nextFloat() * 7f
            ))
        }
        
        mycelSpores.forEach { s ->
            s.y += s.vy
            s.x += s.vx + sin(time + s.phase) * 0.05f
            s.life -= 0.006f
            
            drawCircle(
                color = accent2.copy(alpha = s.life * 0.7f),
                radius = s.r,
                center = Offset(s.x, s.y)
            )
        }
        mycelSpores.removeAll { it.life <= 0f }
        
        // Floor glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.05f + sub * 0.12f),
                    Color.Transparent
                ),
                center = Offset(W / 2, H * 0.95f),
                radius = W * 0.9f
            ),
            radius = W * 0.9f,
            center = Offset(W / 2, H * 0.95f)
        )
    }
}

@Composable
fun ReefVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
                reefTentaclePhase += 0.05f
                reefCompanionPhase += 0.032f
            }
        }
    }
    
    // Initialize particles once
    if (reefParticles.isEmpty()) {
        repeat(50) {
            val far = Random.nextBoolean()
            reefParticles.add(CausticParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = (if (far) 0.4f else 0.9f) + Random.nextFloat() * 1.2f,
                phase = Random.nextFloat() * 7f,
                vy = -(0.06f + Random.nextFloat() * (if (far) 0.1f else 0.22f)),
                isFar = far
            ))
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        
        // Deep ocean gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(backgroundColor.copy(alpha = 1.15f), backgroundColor),
                center = Offset(W * 0.5f, H * 0.35f),
                radius = W * 0.95f
            )
        )
        
        // Light shafts
        repeat(3) { i ->
            val bx = W * (0.2f + i * 0.3f) + sin(time * 0.3f + i) * 10f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent1.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(bx - 24f, 0f),
                size = androidx.compose.ui.geometry.Size(48f, H)
            )
        }
        
        // Caustic particles
        val treble = audioLevels?.drop(36)?.average()?.toFloat() ?: 0f
        reefParticles.forEach { p ->
            p.y = (p.y * H + p.vy) / H
            p.x = ((p.x * W + sin(time * 0.4f + p.phase) * (if (p.isFar) 0.08f else 0.18f)) / W).coerceIn(0f, 1f)
            if (p.y < -5f / H) {
                p.y = 1f + 5f / H
                p.x = Random.nextFloat()
            }
            
            val glow = (0.2f + 0.35f * abs(sin(time * 2f + p.phase)) + treble * 0.3f) * (if (p.isFar) 0.5f else 1f)
            drawCircle(
                color = accent1.copy(alpha = glow),
                radius = p.r + treble * (if (p.isFar) 0.5f else 1.2f),
                center = Offset(p.x * W, p.y * H)
            )
        }
        
        // Draw companion jellyfish (smaller, in background)
        val compX = W * (0.78f + sin(reefCompanionPhase * 0.4f) * 0.06f)
        val compY = H * (0.68f + sin(reefCompanionPhase * 0.6f) * 0.04f)
        val bass = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        drawJellyfish(compX, compY, 0.52f, accent2, audioLevels, bass, 0f, reefTentaclePhase)
        
        // Draw main jellyfish
        val contraction = if (bass > 0.6f) 1f else 0f
        drawJellyfish(W * 0.5f, H * 0.36f, 1f, accent1, audioLevels, bass, contraction, reefTentaclePhase)
    }
}

private fun DrawScope.drawJellyfish(
    cx: Float,
    cy: Float,
    scale: Float,
    hue: Color,
    audioLevels: FloatArray?,
    bass: Float,
    contraction: Float,
    tentaclePhase: Float
) {
    val bellW = (66f - contraction * 10f + bass * 10f) * scale
    val bellH = (40f + contraction * 8f) * scale
    
    // Bell glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(hue.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(cx, cy),
            radius = bellW * 1.6f
        ),
        radius = bellW * 1.6f,
        center = Offset(cx, cy)
    )
    
    // Bell body
    drawOval(
        color = hue.copy(alpha = 0.5f),
        topLeft = Offset(cx - bellW, cy),
        size = androidx.compose.ui.geometry.Size(bellW * 2, bellH)
    )
    
    // Bell rings
    repeat(4) { i ->
        val path = Path()
        path.addOval(Rect(
            left = cx - (bellW - i * 10f * scale),
            top = cy - i * 3f * scale,
            right = cx + (bellW - i * 10f * scale),
            bottom = cy + (bellH - i * 4f * scale)
        ))
        drawPath(path, hue.copy(alpha = 0.25f), style = Stroke(width = 1f))
    }
    
    // Tentacles
    repeat(8) { i ->
        val idx = 4 + i * 4
        val v = audioLevels?.getOrNull(idx % 48) ?: 0f
        val startX = cx + (i - 3.5f) * (bellW * 1.7f / 8)
        val pts = mutableListOf<Offset>()
        
        repeat(11) { s ->
            val frac = s / 10f
            val sway = sin(tentaclePhase * 1.4f + i * 0.7f + frac * 4) * (6f + v * 22f) * frac * scale
            pts.add(Offset(
                x = startX + sway,
                y = cy + bellH * 0.3f + frac * (90f + v * 40f) * scale
            ))
        }
        
        val path = Path()
        smoothPath(path, pts)
        
        drawPath(
            path,
            if (i % 2 == 0) hue else Color(0xFFff6bcf),
            alpha = (0.45f + v * 0.4f) * scale,
            style = Stroke(width = 1.4f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun MonsoonVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
            }
        }
    }
    
    // Initialize rain once
    if (rainDrops.isEmpty()) {
        repeat(70) {
            rainDrops.add(RainDrop(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vy = 4f + Random.nextFloat() * 3f,
                len = 8f + Random.nextFloat() * 10f
            ))
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        
        // Storm sky
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(backgroundColor.copy(alpha = 1.1f), backgroundColor)
            )
        )
        
        // TWO cloud layers
        listOf(
            CloudLayer(H * 0.34f, 1f, backgroundColor.copy(alpha = 0.9f), 0.15f),
            CloudLayer(H * 0.5f, 0.7f, backgroundColor.copy(alpha = 0.95f), 0.08f)
        ).forEach { cloud ->
            val path = Path()
            var first = true
            
            repeat(49) { i ->
                val idx = i.coerceAtMost(47)
                val x = (i / 48f) * W
                val level = audioLevels?.getOrNull(idx) ?: 0f
                val bump = sin(cloudSeeds[idx] + time * cloud.speed + i * 0.5f) * 10f
                val y = cloud.y - level * 40f * cloud.amp - bump
                
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
            }
            
            path.lineTo(W, 0f)
            path.lineTo(0f, 0f)
            path.close()
            
            drawPath(path, cloud.color)
        }
        
        // Rain with ground splashes
        val energy = audioLevels?.average()?.toFloat() ?: 0f
        val activeRain = (20 + energy * 160).toInt().coerceIn(0, 70)
        val groundY = H * 0.9f
        
        repeat(activeRain) { i ->
            val d = rainDrops[i]
            val wasBelow = d.y * H > groundY
            d.y = ((d.y * H + d.vy) / H).coerceIn(0f, 1.2f)
            d.x = ((d.x * W - 1.2f) / W).coerceIn(-0.1f, 1.1f)
            
            if (!wasBelow && d.y * H > groundY && Random.nextFloat() < 0.5f) {
                rainSplashes.add(Splash(x = d.x * W, y = groundY, life = 1f))
            }
            
            if (d.y > 1f) {
                d.y = -10f / H
                d.x = Random.nextFloat()
            }
            
            drawLine(
                color = accent1.copy(alpha = 0.22f),
                start = Offset(d.x * W, d.y * H),
                end = Offset(d.x * W - 3f, d.y * H + d.len),
                strokeWidth = 1f
            )
        }
        
        // Splashes
        rainSplashes.forEach { s ->
            s.life -= 0.09f
            val w = (1f - s.life) * 5f
            val h = (1f - s.life) * 1.6f
            drawOval(
                color = accent1.copy(alpha = s.life * 0.5f),
                topLeft = Offset(s.x - w, s.y - h),
                size = androidx.compose.ui.geometry.Size(w * 2, h * 2),
                style = Stroke(width = 1f)
            )
        }
        rainSplashes.removeAll { it.life <= 0f }
        
        // Lightning on strong beat/punch
        val bass = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        val punch = audioLevels?.drop(36)?.take(10)?.maxOrNull() ?: 0f
        
        if ((bass > 0.4f && Random.nextFloat() < 0.01f) || punch > 0.75f) {
            val startX = W * (0.2f + Random.nextFloat() * 0.6f)
            val pts = mutableListOf(Offset(startX, 0f))
            var cx = startX
            var cy = 0f
            
            while (cy < H * 0.62f) {
                cx += (Random.nextFloat() - 0.5f) * 40f
                cy += 18f + Random.nextFloat() * 22f
                pts.add(Offset(cx, cy))
            }
            
            val branchIdx = if (Random.nextFloat() < 0.7f) (pts.size * 0.4f).toInt() else -1
            stormBolts.add(LightningBolt(pts, life = 1f, branchIndex = branchIdx))
        }
        
        stormBolts.forEach { bolt ->
            bolt.life -= 0.09f
            
            // Main bolt
            val path = Path()
            bolt.points.forEachIndexed { i, p ->
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            
            drawPath(
                path,
                accent1.copy(alpha = maxOf(0f, bolt.life)),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
            
            // Branch
            if (bolt.branchIndex >= 0 && bolt.branchIndex < bolt.points.size) {
                val bp = bolt.points[bolt.branchIndex]
                drawLine(
                    color = accent2.copy(alpha = maxOf(0f, bolt.life)),
                    start = bp,
                    end = Offset(bp.x + 40f + Random.nextFloat() * 20f, bp.y + 50f),
                    strokeWidth = 1.3f,
                    cap = StrokeCap.Round
                )
            }
        }
        stormBolts.removeAll { it.life <= 0f }
        
        // Flash overlay on fresh bolt
        val freshBolt = stormBolts.find { it.life > 0.85f }
        if (freshBolt != null) {
            drawRect(
                color = accent1.copy(alpha = (freshBolt.life - 0.85f) * 1.3f)
            )
        }
        
        // Distant horizon glow
        val sub = audioLevels?.take(3)?.average()?.toFloat() ?: 0f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent2.copy(alpha = 0.08f + sub * 0.1f),
                    Color.Transparent
                ),
                center = Offset(W / 2, H * 0.7f),
                radius = W * 0.7f
            ),
            radius = W * 0.7f,
            center = Offset(W / 2, H * 0.7f)
        )
    }
}

data class CloudLayer(val y: Float, val amp: Float, val color: Color, val speed: Float)

// ============ MEHTAP VISUALIZER ============
// Simple moonlight/sunlight theme with calm water reflection - EXACT HTML drawMehtap

@Composable
fun MehtapVisualizer(
    audioLevels: FloatArray?,
    isActive: Boolean,
    isDarkMode: Boolean = true,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                time += 0.045f
            }
        }
    }
    
    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        val horizonY = H * 0.6f
        
        // Sky gradient - EXACT HTML
        drawRect(
            brush = Brush.verticalGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFF03070d),
                        Color(0xFF0a1b2c),
                        Color(0xFF123049)
                    )
                } else {
                    listOf(
                        Color(0xFFbfe0e8),
                        Color(0xFFeaf3ee),
                        Color(0xFFfbe6c4)
                    )
                },
                startY = 0f,
                endY = horizonY
            )
        )
        
        // Stars (dark mode only) - EXACT HTML
        if (isDarkMode) {
            val starColor = Color(0xFFeaf1ff)
            repeat(40) { i ->
                val x = (i * 37.5f) % W
                val y = (i * 19.3f) % horizonY
                val size = 0.6f + (i % 3) * 0.3f
                
                drawCircle(
                    color = starColor.copy(alpha = 0.6f),
                    radius = size,
                    center = Offset(x, y)
                )
            }
        }
        
        // Moon/Sun with breathing effect
        val bass = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        val bodyX = W * 0.52f
        val bodyY = horizonY * 0.26f
        val bodyR = minOf(W, horizonY) * 0.12f + bass * 6f
        
        // Glow - EXACT HTML
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFFbfd4e6).copy(alpha = 0.5f),
                        Color(0xFFbfd4e6).copy(alpha = 0.1f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color(0xFFffdf9e).copy(alpha = 0.5f),
                        Color(0xFFffdf9e).copy(alpha = 0.1f),
                        Color.Transparent
                    )
                },
                center = Offset(bodyX, bodyY),
                radius = bodyR * 5f
            ),
            radius = bodyR * 5f,
            center = Offset(bodyX, bodyY)
        )
        
        // Moon/Sun body - EXACT HTML
        drawCircle(
            color = if (isDarkMode) Color(0xFFdfe8ee) else Color(0xFFfff2cf),
            radius = bodyR,
            center = Offset(bodyX, bodyY)
        )
        
        // Water gradient - EXACT HTML
        drawRect(
            brush = Brush.verticalGradient(
                colors = if (isDarkMode) {
                    listOf(
                        Color(0xFF0d2436),
                        Color(0xFF030a12)
                    )
                } else {
                    listOf(
                        Color(0xFFbfe4df),
                        Color(0xFF4c8a92)
                    )
                },
                startY = horizonY,
                endY = H
            ),
            topLeft = Offset(0f, horizonY),
            size = androidx.compose.ui.geometry.Size(W, H - horizonY)
        )
        
        // Golden reflection bands - EXACT HTML
        val energy = audioLevels?.take(7)?.average()?.toFloat() ?: 0f
        val treble = audioLevels?.drop(36)?.average()?.toFloat() ?: 0f
        val reflectColor = if (isDarkMode) Color(0xFFd9b46a) else Color(0xFFc98b2e)
        
        repeat(15) { i ->
            val p = i / 15f
            val y = horizonY + 10f + p * (H - horizonY) * 0.6f
            val fade = 1f - p * 0.75f
            val w = (W * 0.08f) * (0.6f + energy * 0.4f) * fade
            val h = 1.5f * fade
            val alpha = ((fade * 50f + treble * 50f) / 255f).coerceIn(0f, 1f)
            
            drawRect(
                color = reflectColor.copy(alpha = alpha),
                topLeft = Offset(bodyX - w / 2, y),
                size = androidx.compose.ui.geometry.Size(w, h)
            )
        }
        
        // Simple wave layer - EXACT HTML
        val path = Path()
        path.moveTo(0f, H)
        val waveY = horizonY + (H - horizonY) * 0.1f
        
        repeat(31) { i ->
            val p = i / 30f
            val amp = 3f + bass * 12f
            val y = waveY + sin(p * 8f + time * 0.5f) * amp
            path.lineTo(p * W, y)
        }
        path.lineTo(W, H)
        path.close()
        
        drawPath(
            path,
            color = if (isDarkMode) {
                Color(0xFF0d2436).copy(alpha = 0.7f)
            } else {
                Color(0xFFbfe4df).copy(alpha = 0.7f)
            }
        )
    }
}
