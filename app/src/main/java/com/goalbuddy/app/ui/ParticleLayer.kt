package com.goalbuddy.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.goalbuddy.app.data.Events
import com.goalbuddy.app.data.ParticleConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object ParticleOptions {
    val shapes = listOf("● Dots", "★ Stars", "♥ Hearts", "❄ Snow", "○ Bubbles", "✦ Sparkles", "▬ Confetti", "🍃 Leaves", "▲ Triangles", "🎲 Mixed")
    val motions = listOf("⬆ Float up", "⬇ Fall down", "〰 Drift", "✨ Twinkle", "🌀 Swirl", "🪲 Fireflies")
    val colorModes = listOf("Accent", "Rainbow", "Pastel", "Custom", "White & gold")
}

private class Particle {
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var size = 4f; var rot = 0f; var vrot = 0f
    var life = 0f; var maxLife = 5f
    var color = Color.White
    var shape = 0
    var phase = 0f
    var gravity = 0f
    var burst = false
    var orbitR = 0f; var orbitA = 0f
}

/**
 * Animated particles. Ambient particles appear (fade in), move and disappear (fade out);
 * confetti bursts fire when goals are completed, and tiny sparkles appear where you touch.
 */
@Composable
fun ParticleLayer(cfg: ParticleConfig, accent: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    val particles = remember { ArrayList<Particle>() }
    var frame by remember { mutableLongStateOf(0L) }
    var w by remember { mutableLongStateOf(0L) }
    var h by remember { mutableLongStateOf(0L) }
    val config by rememberUpdatedState(cfg)
    val accentColor by rememberUpdatedState(accent)

    val star = remember { starUnit(5, 0.45f) }
    val sparkle = remember { starUnit(4, 0.28f) }
    val heart = remember { heartUnit() }
    val tri = remember { Path().apply { moveTo(0f, -1f); lineTo(0.9f, 0.7f); lineTo(-0.9f, 0.7f); close() } }

    fun pickColor(c: ParticleConfig): Color {
        return when (c.colorMode) {
            0 -> accentColor.copy(alpha = 1f).let { base ->
                if (Random.nextBoolean()) base else Color(android.graphics.Color.HSVToColor(floatArrayOf(hueOf(base) + Random.nextFloat() * 30f - 15f, 0.5f, 1f)))
            }
            1 -> Color(android.graphics.Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360f, 0.75f, 1f)))
            2 -> Color(android.graphics.Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360f, 0.35f, 1f)))
            3 -> c.customColors.ifEmpty { listOf(0xFFFFFFFF) }.random().toColor()
            else -> if (Random.nextFloat() < 0.6f) Color.White else Color(0xFFFFD54F)
        }
    }

    fun spawnAmbient(p: Particle, c: ParticleConfig, initial: Boolean) {
        val W = w.toFloat().coerceAtLeast(1f)
        val H = h.toFloat().coerceAtLeast(1f)
        val sp = c.speed * density
        p.burst = false
        p.gravity = 0f
        p.size = (c.sizeMin + Random.nextFloat() * (c.sizeMax - c.sizeMin).coerceAtLeast(0f)) * density
        p.shape = if (c.shape == 9) Random.nextInt(9) else c.shape
        p.color = pickColor(c)
        p.rot = Random.nextFloat() * 360f
        p.vrot = if (c.spin) (Random.nextFloat() - 0.5f) * 120f else 0f
        p.life = 0f
        p.maxLife = c.lifetime * (0.6f + Random.nextFloat() * 0.8f)
        p.phase = Random.nextFloat() * 6.28f
        p.x = Random.nextFloat() * W
        p.y = Random.nextFloat() * H
        when (c.motion) {
            0 -> { p.vy = -(20f + Random.nextFloat() * 40f) * sp; p.vx = 0f; if (!initial) p.y = H + p.size }
            1 -> { p.vy = (20f + Random.nextFloat() * 40f) * sp; p.vx = 0f; if (!initial) p.y = -p.size }
            2 -> { p.vx = (Random.nextFloat() - 0.5f) * 30f * sp; p.vy = (Random.nextFloat() - 0.5f) * 30f * sp }
            3 -> { p.vx = 0f; p.vy = 0f; p.maxLife = 1.2f + Random.nextFloat() * c.lifetime * 0.5f }
            4 -> {
                p.orbitR = (0.1f + Random.nextFloat() * 0.45f) * min(W, H)
                p.orbitA = Random.nextFloat() * 6.28f
                p.vx = 0f; p.vy = 0f
            }
            else -> { p.vx = (Random.nextFloat() - 0.5f) * 40f * sp; p.vy = (Random.nextFloat() - 0.5f) * 40f * sp }
        }
        if (initial) p.life = Random.nextFloat() * p.maxLife
    }

    fun spawnBurst(at: Offset?, count: Int, small: Boolean) {
        val W = w.toFloat().coerceAtLeast(1f)
        val origin = at ?: Offset(W / 2f, h.toFloat() * 0.3f)
        repeat(count) {
            val p = Particle()
            p.burst = true
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val speed = (if (small) 60f + Random.nextFloat() * 90f else 180f + Random.nextFloat() * 420f) * density
            p.x = origin.x; p.y = origin.y
            p.vx = cos(ang) * speed
            p.vy = sin(ang) * speed - (if (small) 20f else 220f) * density
            p.gravity = (if (small) 120f else 700f) * density
            p.size = (if (small) 2f + Random.nextFloat() * 3f else 4f + Random.nextFloat() * 5f) * density
            p.shape = if (small) 5 else listOf(6, 6, 1, 2, 0, 8).random()
            p.color = if (small) pickColor(config) else Color(android.graphics.Color.HSVToColor(floatArrayOf(Random.nextFloat() * 360f, 0.7f, 1f)))
            p.rot = Random.nextFloat() * 360f
            p.vrot = (Random.nextFloat() - 0.5f) * 720f
            p.maxLife = if (small) 0.6f + Random.nextFloat() * 0.4f else 1.4f + Random.nextFloat() * 0.8f
            particles.add(p)
        }
    }

    LaunchedEffect(Unit) {
        Events.burst.collect { at -> if (config.burstOnDone) spawnBurst(at, config.burstCount, false) }
    }
    LaunchedEffect(Unit) {
        Events.touch.collect { at -> if (config.tapSparkles) spawnBurst(at, 8, true) }
    }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                val c = config
                val W = w.toFloat()
                val H = h.toFloat()
                if (W > 0f && H > 0f) {
                    // keep the ambient count in sync with the setting
                    val ambient = if (c.enabled) c.count else 0
                    var current = particles.count { !it.burst }
                    while (current < ambient) { particles.add(Particle().also { spawnAmbient(it, c, true) }); current++ }
                    if (current > ambient) {
                        var toRemove = current - ambient
                        val it = particles.iterator()
                        while (it.hasNext() && toRemove > 0) { if (!it.next().burst) { it.remove(); toRemove-- } }
                    }
                    val iter = particles.iterator()
                    val wind = c.wind * 60f * density
                    val respawn = ArrayList<Particle>()
                    while (iter.hasNext()) {
                        val p = iter.next()
                        p.life += dt
                        p.rot += p.vrot * dt
                        if (p.burst) {
                            p.vy += p.gravity * dt
                            p.vx *= (1f - dt * 0.8f)
                            p.x += p.vx * dt; p.y += p.vy * dt
                            if (p.life >= p.maxLife) iter.remove()
                            continue
                        }
                        when (c.motion) {
                            0, 1 -> {
                                p.x += (sin(p.life * 1.5f + p.phase) * 18f * density + wind) * dt
                                p.y += p.vy * dt
                            }
                            4 -> {
                                p.orbitA += dt * c.speed * (0.4f + 30f * density / p.orbitR.coerceAtLeast(10f))
                                p.x = W / 2f + cos(p.orbitA) * p.orbitR + wind * p.life * 0.2f
                                p.y = H / 2f + sin(p.orbitA) * p.orbitR * 0.8f
                            }
                            5 -> {
                                p.vx += (Random.nextFloat() - 0.5f) * 80f * density * dt
                                p.vy += (Random.nextFloat() - 0.5f) * 80f * density * dt
                                p.vx = p.vx.coerceIn(-40f * density, 40f * density)
                                p.vy = p.vy.coerceIn(-40f * density, 40f * density)
                                p.x += (p.vx + wind) * dt; p.y += p.vy * dt
                            }
                            else -> { p.x += (p.vx + wind) * dt; p.y += p.vy * dt }
                        }
                        val out = p.y < -60f || p.y > H + 60f || p.x < -60f || p.x > W + 60f
                        val dead = p.life >= p.maxLife && (c.fadeInOut || c.motion == 3 || c.motion == 2 || c.motion == 5 || c.motion == 4)
                        if (out || dead) respawn += p
                    }
                    for (p in respawn) spawnAmbient(p, c, false)
                }
                frame = now
            }
        }
    }

    Canvas(
        modifier
            .fillMaxSize()
            .onSizeChanged { w = it.width.toLong(); h = it.height.toLong() }
    ) {
        frame // read so the canvas redraws every frame
        val c = config
        for (p in particles) {
            val fade = if (p.burst) {
                (1f - p.life / p.maxLife).coerceIn(0f, 1f)
            } else if (c.fadeInOut || c.motion == 3) {
                val edgeT = min(p.maxLife * 0.3f, 1.2f).coerceAtLeast(0.05f)
                min(1f, min(p.life / edgeT, (p.maxLife - p.life) / edgeT)).coerceIn(0f, 1f)
            } else 1f
            val twinkle = if (c.motion == 5 && !p.burst) 0.5f + 0.5f * abs(sin(p.life * 3f + p.phase)) else 1f
            val a = (if (p.burst) 1f else c.opacity) * fade * twinkle
            if (a <= 0.01f) continue
            if (c.glow && !p.burst) drawCircle(p.color.copy(alpha = a * 0.25f), p.size * 2.2f, Offset(p.x, p.y))
            drawShape(p, a, star, sparkle, heart, tri)
        }
    }
}

private fun hueOf(c: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    return hsv[0]
}

private fun DrawScope.drawShape(p: Particle, a: Float, star: Path, sparkle: Path, heart: Path, tri: Path) {
    val col = p.color.copy(alpha = a)
    when (p.shape) {
        0 -> drawCircle(col, p.size, Offset(p.x, p.y))
        3 -> {
            drawCircle(col.copy(alpha = a * 0.35f), p.size * 1.6f, Offset(p.x, p.y))
            drawCircle(col, p.size * 0.8f, Offset(p.x, p.y))
        }
        4 -> {
            drawCircle(col, p.size, Offset(p.x, p.y), style = Stroke(width = (p.size * 0.18f).coerceAtLeast(1f)))
            drawCircle(Color.White.copy(alpha = a * 0.8f), p.size * 0.22f, Offset(p.x - p.size * 0.35f, p.y - p.size * 0.35f))
        }
        6 -> withTransform({ translate(p.x, p.y); rotate(p.rot, Offset.Zero) }) {
            drawRect(col, Offset(-p.size, -p.size * 0.45f), Size(p.size * 2f, p.size * 0.9f))
        }
        7 -> withTransform({ translate(p.x, p.y); rotate(p.rot, Offset.Zero) }) {
            drawOval(col, Offset(-p.size, -p.size * 0.45f), Size(p.size * 2f, p.size * 0.9f))
        }
        else -> {
            val path = when (p.shape) { 1 -> star; 2 -> heart; 5 -> sparkle; else -> tri }
            withTransform({
                translate(p.x, p.y)
                rotate(if (p.shape == 2) p.rot * 0.1f else p.rot, Offset.Zero)
                scale(p.size, p.size, Offset.Zero)
            }) {
                drawPath(path, col)
            }
        }
    }
}

private fun starUnit(points: Int, inner: Float): Path = Path().apply {
    val step = PI.toFloat() / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) 1f else inner
        val ang = -PI.toFloat() / 2f + i * step
        val x = cos(ang) * r
        val y = sin(ang) * r
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun heartUnit(): Path = Path().apply {
    moveTo(0f, 0.9f)
    cubicTo(-1.6f, -0.2f, -0.7f, -1.3f, 0f, -0.45f)
    cubicTo(0.7f, -1.3f, 1.6f, -0.2f, 0f, 0.9f)
    close()
}
