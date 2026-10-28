package com.goalbuddy.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.goalbuddy.app.avatar.AvatarPose
import com.goalbuddy.app.avatar.AvatarRenderer
import com.goalbuddy.app.data.AvatarConfig
import kotlin.math.sin

/** Draws the avatar with a given pose. Size the modifier as width x (width * 1.4). */
@Composable
fun AvatarView(cfg: AvatarConfig, pose: AvatarPose, modifier: Modifier = Modifier) {
    val renderer = remember { AvatarRenderer() }
    Canvas(modifier) {
        drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width, cfg, pose) }
    }
}

/** Avatar with its own idle animation (blinking, little waves, talking) used in the editor. */
@Composable
fun AnimatedAvatar(cfg: AvatarConfig, modifier: Modifier = Modifier, expression: Int = 0, talking: Boolean = false) {
    var t by remember { mutableFloatStateOf(0f) }
    var blinkAt by remember { mutableFloatStateOf(2f) }
    var waveUntil by remember { mutableFloatStateOf(-1f) }
    var nextWave by remember { mutableIntStateOf(4) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) t += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
            }
            if (t > blinkAt + 0.14f) blinkAt = t + 2f + (t * 7.3f % 3f)
            if (t > nextWave) { waveUntil = t + 1.6f; nextWave = (t + 6f + (t % 4f)).toInt() }
        }
    }
    val waving = t < waveUntil
    val pose = AvatarPose(
        blink = if (t in blinkAt..(blinkAt + 0.14f)) 1f else 0f,
        mouthOpen = if (talking) kotlin.math.abs(sin(t * 13f)) else 0f,
        lookX = sin(t * 0.7f) * 0.6f,
        lookY = sin(t * 0.43f) * 0.3f,
        armL = 12f + sin(t * 2f) * 4f,
        armR = if (waving) 150f + sin(t * 14f) * 22f else 12f + sin(t * 2f + 1f) * 4f,
        expression = expression,
        time = t
    )
    AvatarView(cfg, pose, modifier)
}
