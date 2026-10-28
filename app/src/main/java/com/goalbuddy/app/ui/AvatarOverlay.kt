package com.goalbuddy.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goalbuddy.app.avatar.AvatarPose
import com.goalbuddy.app.avatar.AvatarRenderer
import com.goalbuddy.app.avatar.Mood
import com.goalbuddy.app.avatar.Phrases
import com.goalbuddy.app.data.AppState
import com.goalbuddy.app.data.AvatarEvent
import com.goalbuddy.app.data.Events
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Information the avatar can talk about. */
data class PhraseContext(val goal: String?, val left: Int, val done: Int, val page: String)

/**
 * The avatar's little brain: where it is on the screen edges, what it is doing and what it says.
 * Edges: 0 bottom, 1 right, 2 top, 3 left. On the side/top edges it walks on the wall/ceiling.
 */
class BuddyBrain {
    var edge by mutableIntStateOf(0)
    var pos by mutableFloatStateOf(0.5f)
    var facing by mutableFloatStateOf(1f)
    var hopT by mutableFloatStateOf(-1f)
    var jumpT by mutableFloatStateOf(-1f)
    var spinT by mutableFloatStateOf(-1f)
    var tilt by mutableFloatStateOf(0f)
    var pose by mutableStateOf(AvatarPose())
    var bubble by mutableStateOf<String?>(null)
    var bubbleId by mutableIntStateOf(0)

    private var target = 0.5f
    private var walking = false
    private var hopFromEdge = 0
    private var hopFromPos = 0f
    private var hopToEdge = 0
    private var hopToPos = 0f
    private var time = 0f
    private var idleTimer = 2f
    private var blinkTimer = 3f
    private var blinkLeft = 0f
    private var chatTimer = 5f
    private var bubbleLeft = 0f
    private var talkLeft = 0f
    private var expression = 0
    private var exprTimer = 0f
    private var waveTimer = 0f
    private var lookX = 0f
    private var lookY = 0f
    private var lookTimer = 0f
    private var tapCount = 0
    private var tapWindow = 0f
    private var scrollAccum = 0f
    private var scrollCooldown = 0f
    private var greeted = false

    // set from the UI every frame
    var settingsState: AppState? = null
    var phraseContext: () -> PhraseContext = { PhraseContext(null, 0, 0, "day") }
    var screenW = 1f
    var screenH = 1f
    var boxW = 1f
    var boxH = 1f

    val scrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            onScroll(consumed.x, consumed.y + available.y * 0.3f)
            return Offset.Zero
        }
    }

    // ------------------------------------------------------------- talking

    fun say(text: String, seconds: Float? = null) {
        val st = settingsState?.settings ?: return
        bubble = text
        bubbleId++
        val len = seconds ?: (st.bubbleSeconds + text.length * 0.03f)
        bubbleLeft = len
        talkLeft = minOf(len * 0.6f, 0.5f + text.length * 0.05f)
    }

    fun sayMood(mood: Mood) {
        val s = settingsState ?: return
        val ctx = phraseContext()
        say(Phrases.pick(mood, s.settings, s.avatar.personality, s.avatar.name, ctx.goal, ctx.left, ctx.done, ctx.page))
    }

    private fun nextChatDelay(freq: Int): Float = when (freq) {
        0 -> 40f + Random.nextFloat() * 40f
        2 -> 8f + Random.nextFloat() * 8f
        3 -> 4f + Random.nextFloat() * 3f
        else -> 16f + Random.nextFloat() * 18f
    }

    // ------------------------------------------------------------- reactions

    fun express(e: Int, seconds: Float) { expression = e; exprTimer = seconds }
    fun jump() { if (jumpT < 0f) jumpT = 0f }
    fun spin() { if (spinT < 0f) spinT = 0f }
    fun wave(seconds: Float = 1.6f) { waveTimer = seconds }

    fun onTapAvatar() {
        val st = settingsState?.settings ?: return
        tapCount++
        tapWindow = 1.6f
        if (tapCount >= 5) {
            tapCount = 0
            express(3, 2.5f); spin()
            sayMood(Mood.POKE_SPAM)
        } else {
            jump(); express(1, 1.2f)
            if (st.reactTaps) sayMood(Mood.TAP)
        }
    }

    fun onDoubleTapAvatar() { spin(); express(1, 1.5f); sayMood(Mood.DOUBLE_TAP) }

    fun onEvent(e: AvatarEvent) {
        when (e) {
            is AvatarEvent.GoalDone -> {
                jump(); wave(2f); express(1, 2.5f)
                if (e.allDone) spin()
                val s = settingsState ?: return
                val ctx = phraseContext()
                say(
                    Phrases.pick(
                        if (e.allDone) Mood.ALL_DONE else Mood.GOAL_DONE, s.settings, s.avatar.personality,
                        s.avatar.name, e.title, ctx.left, ctx.done, ctx.page
                    )
                )
            }
            is AvatarEvent.GoalAdded -> { jump(); express(1, 1.5f); sayAboutAdded(e.title) }
            AvatarEvent.GoalDeleted -> { express(2, 1f); sayMood(Mood.GOAL_DELETED) }
            is AvatarEvent.PageChanged -> {
                jump()
                val s = settingsState ?: return
                if (Random.nextFloat() < 0.5f) {
                    say(Phrases.pick(Mood.PAGE, s.settings, s.avatar.personality, s.avatar.name, page = e.period.label.lowercase()))
                }
            }
            is AvatarEvent.Say -> say(e.text)
        }
    }

    private fun sayAboutAdded(title: String) {
        val s = settingsState ?: return
        val ctx = phraseContext()
        say(Phrases.pick(Mood.GOAL_ADDED, s.settings, s.avatar.personality, s.avatar.name, title, ctx.left, ctx.done, ctx.page))
    }

    /** Somebody touched the screen somewhere: look at it. */
    fun onGlobalTouch(p: Offset) {
        val st = settingsState?.settings ?: return
        if (!st.eyesFollowTouch) return
        val c = center()
        var dx = p.x - c.x
        var dy = p.y - c.y
        // rotate into the avatar's local frame
        val rot = Math.toRadians(-rotation().toDouble())
        val lx = (dx * cos(rot) - dy * sin(rot)).toFloat()
        val ly = (dx * sin(rot) + dy * cos(rot)).toFloat()
        val d = hypot(lx, ly).coerceAtLeast(1f)
        dx = lx / d; dy = ly / d
        lookX = dx * facing
        lookY = dy
        lookTimer = 1.8f
    }

    fun onScroll(dx: Float, dy: Float) {
        val st = settingsState?.settings ?: return
        if (!st.reactScroll) return
        val d = if (abs(dx) > abs(dy)) dx else dy
        tilt = (tilt + d * 0.04f).coerceIn(-22f, 22f)
        // scrolling "pushes" the avatar along its edge a little
        val along = if (edge == 0 || edge == 2) dx else dy
        if (hopT < 0f) pos = (pos - along / (edgeLength() * 3f)).coerceIn(0f, 1f)
        lookY = if (dy > 0) -0.8f else 0.8f
        lookTimer = 0.6f
        scrollAccum += abs(d)
        if (scrollAccum > 2200f && scrollCooldown <= 0f) {
            scrollAccum = 0f
            scrollCooldown = 12f
            express(2, 1f)
            sayMood(Mood.SCROLL)
        }
    }

    // ------------------------------------------------------------- geometry

    private fun allowedEdges(): List<Int> {
        val st = settingsState?.settings ?: return listOf(0)
        val l = mutableListOf<Int>()
        if (st.edgeBottom) l += 0
        if (st.edgeRight) l += 1
        if (st.edgeTop) l += 2
        if (st.edgeLeft) l += 3
        return l.ifEmpty { listOf(0) }
    }

    private fun edgeLength(e: Int = edge): Float = if (e == 0 || e == 2) screenW else screenH

    fun placement(e: Int, p: Float): Triple<Float, Float, Float> {
        val m = 2f
        val peek = boxH * 0.16f // side/top edges: tuck the feet a little behind the edge
        return when (e) {
            1 -> Triple(screenW - boxH / 2f - m + peek, lerp(boxW / 2f + m, screenH - boxW / 2f - boxH * 0.9f, p), -90f)
            2 -> Triple(lerp(boxW / 2f + m, screenW - boxW / 2f - m, p), boxH / 2f + m - peek, 180f)
            3 -> Triple(boxH / 2f + m - peek, lerp(boxW / 2f + m, screenH - boxW / 2f - boxH * 0.9f, p), 90f)
            else -> Triple(lerp(boxW / 2f + m, screenW - boxW / 2f - m, p), screenH - boxH / 2f - m, 0f)
        }
    }

    private fun upVector(e: Int): Offset = when (e) {
        1 -> Offset(-1f, 0f)
        2 -> Offset(0f, 1f)
        3 -> Offset(1f, 0f)
        else -> Offset(0f, -1f)
    }

    fun rotation(): Float {
        if (hopT >= 0f) {
            val a = placement(hopFromEdge, hopFromPos).third
            val b = placement(hopToEdge, hopToPos).third
            val diff = ((b - a + 540f) % 360f) - 180f
            return a + diff * ease(hopT) + (if (spinT >= 0f) spinT * 360f else 0f)
        }
        return placement(edge, pos).third + tilt + (if (spinT >= 0f) spinT * 360f else 0f)
    }

    fun center(): Offset {
        if (hopT >= 0f) {
            val a = placement(hopFromEdge, hopFromPos)
            val b = placement(hopToEdge, hopToPos)
            val t = ease(hopT)
            val x = lerp(a.first, b.first, t)
            val y = lerp(a.second, b.second, t)
            // arc toward the middle of the screen
            val mx = screenW / 2f - x
            val my = screenH / 2f - y
            val d = hypot(mx, my).coerceAtLeast(1f)
            val arc = sin(PI.toFloat() * hopT) * boxH * 0.9f
            return Offset(x + mx / d * arc, y + my / d * arc)
        }
        val pl = placement(edge, pos)
        val up = upVector(edge)
        val j = if (jumpT >= 0f) sin(PI.toFloat() * jumpT) * boxH * 0.35f else 0f
        return Offset(pl.first + up.x * j, pl.second + up.y * j)
    }

    /** Where the speech bubble should point to (the top of the head, in screen space). */
    fun headPoint(): Offset {
        val c = center()
        val e = if (hopT >= 0f) hopToEdge else edge
        val up = upVector(e)
        return Offset(c.x + up.x * boxH * 0.42f, c.y + up.y * boxH * 0.42f)
    }

    fun currentEdge(): Int = if (hopT >= 0f) hopToEdge else edge

    private fun startHop(toEdge: Int, toPos: Float) {
        hopFromEdge = edge; hopFromPos = pos
        hopToEdge = toEdge; hopToPos = toPos
        hopT = 0f
        walking = false
    }

    private fun decideNext() {
        val edges = allowedEdges()
        if (edge !in edges) { startHop(edges.random(), Random.nextFloat()); return }
        val r = Random.nextFloat()
        when {
            r < 0.55f -> {
                target = 0.05f + Random.nextFloat() * 0.9f
                walking = true
            }
            r < 0.8f -> {
                val others = edges.filter { it != edge }
                val e = if (others.isNotEmpty()) others.random() else edge
                startHop(e, 0.1f + Random.nextFloat() * 0.8f)
                if (Random.nextFloat() < 0.35f) sayMood(Mood.HOP)
            }
            else -> {
                wave()
                idleTimer = 2f + Random.nextFloat() * 2f
            }
        }
    }

    // ------------------------------------------------------------- frame

    fun step(dt: Float, density: Float) {
        val s = settingsState ?: return
        val st = s.settings
        time += dt

        if (!greeted && time > 0.8f) { greeted = true; sayMood(Mood.GREET); wave(2f) }

        blinkTimer -= dt
        if (blinkTimer <= 0f) { blinkLeft = 0.13f; blinkTimer = 2f + Random.nextFloat() * 4f }
        if (blinkLeft > 0f) blinkLeft -= dt

        if (bubble != null) {
            bubbleLeft -= dt
            if (bubbleLeft <= 0f) bubble = null
        }
        if (talkLeft > 0f) talkLeft -= dt

        chatTimer -= dt
        if (chatTimer <= 0f) {
            chatTimer = nextChatDelay(st.chatFrequency)
            if (bubble == null) sayMood(Mood.IDLE)
        }

        if (exprTimer > 0f) { exprTimer -= dt; if (exprTimer <= 0f) expression = 0 }
        if (waveTimer > 0f) waveTimer -= dt
        if (lookTimer > 0f) lookTimer -= dt else { lookX *= 0.95f; lookY *= 0.95f }
        if (tapWindow > 0f) { tapWindow -= dt; if (tapWindow <= 0f) tapCount = 0 }
        if (scrollCooldown > 0f) scrollCooldown -= dt
        scrollAccum = (scrollAccum - dt * 300f).coerceAtLeast(0f)
        tilt *= (1f - (dt * 5f).coerceAtMost(1f))

        if (jumpT >= 0f) { jumpT += dt / 0.55f; if (jumpT >= 1f) jumpT = -1f }
        if (spinT >= 0f) { spinT += dt / 0.7f; if (spinT >= 1f) spinT = -1f }

        if (hopT >= 0f) {
            hopT += dt / 0.9f
            if (hopT >= 1f) {
                edge = hopToEdge; pos = hopToPos; target = pos
                hopT = -1f
                idleTimer = 1f + Random.nextFloat() * 3f
            }
        } else if (walking) {
            val speedPx = 55f * density * st.avatarSpeed
            val frac = speedPx / edgeLength().coerceAtLeast(1f) * dt
            val dir = if (target > pos) 1f else -1f
            // on the top/right edges the avatar is rotated, so "forward" is mirrored
            facing = if (edge == 2 || edge == 1) -dir else dir
            pos += dir * frac
            if ((dir > 0 && pos >= target) || (dir < 0 && pos <= target)) {
                pos = target
                walking = false
                idleTimer = 1.5f + Random.nextFloat() * 4f / st.avatarSpeed.coerceAtLeast(0.2f)
            }
        } else {
            idleTimer -= dt
            if (idleTimer <= 0f) decideNext()
        }

        val talking = talkLeft > 0f
        pose = AvatarPose(
            blink = if (blinkLeft > 0f) 1f else 0f,
            mouthOpen = if (talking) abs(sin(time * 14f)) else 0f,
            lookX = lookX,
            lookY = lookY,
            armL = if (hopT >= 0f || jumpT >= 0f) 70f else 12f + sin(time * 2f) * 3f,
            armR = when {
                waveTimer > 0f -> 150f + sin(time * 14f) * 25f
                hopT >= 0f || jumpT >= 0f -> 70f
                else -> 12f + sin(time * 2f + 1f) * 3f
            },
            walk = time * 11f,
            walking = walking && hopT < 0f,
            expression = if (hopT >= 0f && expression == 0) 2 else expression,
            shadow = hopT < 0f && jumpT < 0f,
            time = time
        )
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun ease(t: Float) = (t * t * (3f - 2f * t)).coerceIn(0f, 1f)
}

/**
 * Full-screen layer with the walking avatar and its speech bubble.
 * It only catches touches on the avatar itself; the rest of the screen stays usable.
 */
@Composable
fun AvatarOverlay(
    brain: BuddyBrain,
    state: AppState,
    phraseContext: () -> PhraseContext,
    onOpenEditor: () -> Unit
) {
    val density = LocalDensity.current
    val st = state.settings
    val sizePx = with(density) { st.avatarSize.dp.toPx() }
    brain.settingsState = state
    brain.phraseContext = phraseContext
    brain.boxW = sizePx
    brain.boxH = sizePx * AvatarRenderer.BOX_RATIO
    val openEditor by rememberUpdatedState(onOpenEditor)

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                brain.step(dt, density.density)
            }
        }
    }
    LaunchedEffect(Unit) { Events.avatar.collect { brain.onEvent(it) } }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { brain.screenW = it.width.toFloat(); brain.screenH = it.height.toFloat() }
    ) {
        val c = brain.center()
        val rot = brain.rotation()
        val wDp = st.avatarSize.dp
        val hDp = (st.avatarSize * AvatarRenderer.BOX_RATIO).dp
        AvatarView(
            cfg = state.avatar,
            pose = brain.pose,
            modifier = Modifier
                .offset { IntOffset((c.x - brain.boxW / 2f).roundToInt(), (c.y - brain.boxH / 2f).roundToInt()) }
                .size(wDp, hDp)
                .graphicsLayer {
                    rotationZ = rot
                    scaleX = brain.facing
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { brain.onTapAvatar() },
                        onDoubleTap = { brain.onDoubleTapAvatar() },
                        onLongPress = { openEditor() }
                    )
                }
        )

        SpeechBubble(brain, st.bubbleSide)
    }
}

@Composable
private fun SpeechBubble(brain: BuddyBrain, sideSetting: Int) {
    val text = brain.bubble ?: return
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val anim = remember(brain.bubbleId) { Animatable(0.4f) }
    LaunchedEffect(brain.bubbleId) {
        anim.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }
    val head = brain.headPoint()
    val e = brain.currentEdge()
    val gap = with(density) { 6.dp.toPx() }
    val margin = with(density) { 6.dp.toPx() }
    val towardRight = when (sideSetting) {
        1 -> false
        2 -> true
        else -> head.x < brain.screenW / 2f
    }
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    var x: Float
    var y: Float
    var tailDir: Int // 0 tail points down, 1 up, 2 left, 3 right
    when (e) {
        2 -> { // hanging from the top: bubble below
            y = head.y + gap
            x = if (towardRight) head.x - w * 0.15f else head.x - w * 0.85f
            tailDir = 1
        }
        3 -> { // left wall: bubble to the right
            x = head.x + gap
            y = head.y - h * 0.5f
            tailDir = 2
        }
        1 -> { // right wall: bubble to the left
            x = head.x - w - gap
            y = head.y - h * 0.5f
            tailDir = 3
        }
        else -> {
            y = head.y - h - gap
            x = if (towardRight) head.x - w * 0.15f else head.x - w * 0.85f
            tailDir = 0
        }
    }
    x = x.coerceIn(margin, (brain.screenW - w - margin).coerceAtLeast(margin))
    y = y.coerceIn(margin, (brain.screenH - h - margin).coerceAtLeast(margin))
    val tailX = (head.x - x).coerceIn(18f, (w - 18f).coerceAtLeast(18f))
    val tailY = (head.y - y).coerceIn(14f, (h - 14f).coerceAtLeast(14f))
    val pal = LocalPalette.current
    val bubbleColor = if (pal.dark) Color(0xFFFDFBFF) else Color.White

    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .onSizeChanged { size = it }
            .graphicsLayer {
                scaleX = anim.value; scaleY = anim.value
                alpha = ((anim.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
            }
            .widthIn(max = 220.dp)
            .drawBehind {
                val t = 9.dp.toPx()
                val p = Path()
                when (tailDir) {
                    0 -> { p.moveTo(tailX - t, this.size.height - 2f); p.lineTo(tailX, this.size.height + t); p.lineTo(tailX + t * 0.3f, this.size.height - 2f) }
                    1 -> { p.moveTo(tailX - t, 2f); p.lineTo(tailX, -t); p.lineTo(tailX + t * 0.3f, 2f) }
                    2 -> { p.moveTo(2f, tailY - t); p.lineTo(-t, tailY); p.lineTo(2f, tailY + t * 0.3f) }
                    else -> { p.moveTo(this.size.width - 2f, tailY - t); p.lineTo(this.size.width + t, tailY); p.lineTo(this.size.width - 2f, tailY + t * 0.3f) }
                }
                p.close()
                drawPath(p, bubbleColor)
            }
            .shadow(4.dp, RoundedCornerShape(16.dp), clip = false)
            .background(bubbleColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color(0xFF1E1B2E), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
