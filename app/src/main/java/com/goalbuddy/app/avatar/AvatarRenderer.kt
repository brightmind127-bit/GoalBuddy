package com.goalbuddy.app.avatar

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.goalbuddy.app.data.AvatarConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Live animation state for one frame of the avatar. */
data class AvatarPose(
    val blink: Float = 0f,          // 0 open … 1 closed
    val mouthOpen: Float = 0f,      // talking 0..1
    val lookX: Float = 0f,          // -1..1
    val lookY: Float = 0f,          // -1..1
    val armL: Float = 12f,          // degrees, 0 = straight down, positive = outwards
    val armR: Float = 12f,
    val walk: Float = 0f,           // walk cycle phase (radians)
    val walking: Boolean = false,
    /** 0 normal, 1 happy, 2 surprised, 3 dizzy, 4 sleepy */
    val expression: Int = 0,
    val shadow: Boolean = true,
    val time: Float = 0f
)

/**
 * Draws the cartoon avatar with plain android.graphics so the very same drawing is used
 * inside the app (Compose canvas) and in widgets / notifications (Bitmap).
 *
 * The avatar is drawn in a 100 x 140 unit box (width x height) that is scaled to the target size.
 */
class AvatarRenderer {

    companion object {
        const val BOX_RATIO = 1.4f
        private const val INK_DARK = 0xFF5B1A2A.toInt()
        private const val PINK = 0xFFFF8FAB.toInt()
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val BLACK = 0xFF151515.toInt()
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private lateinit var c: Canvas
    private var ow = 2.2f      // outline width
    private var oc = BLACK     // outline color
    private val sides = floatArrayOf(-1f, 1f)

    fun bitmap(a: AvatarConfig, pose: AvatarPose, widthPx: Int): Bitmap {
        val w = max(16, widthPx)
        val b = Bitmap.createBitmap(w, (w * BOX_RATIO).toInt(), Bitmap.Config.ARGB_8888)
        draw(Canvas(b), w.toFloat(), a, pose)
        return b
    }

    // ------------------------------------------------------------------ helpers

    private fun col(l: Long): Int = l.toInt()

    private fun shade(color: Int, f: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = min(255, (((color shr 16) and 0xFF) * f).toInt())
        val g = min(255, (((color shr 8) and 0xFF) * f).toInt())
        val b = min(255, ((color and 0xFF) * f).toInt())
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun alpha(color: Int, a: Float): Int =
        ((a.coerceIn(0f, 1f) * 255).toInt() shl 24) or (color and 0x00FFFFFF)

    /** Fill a path and outline it. */
    private fun fs(p: Path, color: Int, outline: Boolean = true, w: Float = ow) {
        fill.color = color
        c.drawPath(p, fill)
        if (outline && w > 0f) {
            line.color = oc
            line.strokeWidth = w
            c.drawPath(p, line)
        }
    }

    private fun ovalPath(l: Float, t: Float, r: Float, b: Float): Path =
        Path().apply { addOval(RectF(l, t, r, b), Path.Direction.CW) }

    private fun oval(l: Float, t: Float, r: Float, b: Float, color: Int, outline: Boolean = true, w: Float = ow) =
        fs(ovalPath(l, t, r, b), color, outline, w)

    private fun circle(cx: Float, cy: Float, r: Float, color: Int, outline: Boolean = true, w: Float = ow) =
        oval(cx - r, cy - r, cx + r, cy + r, color, outline, w)

    private fun rrect(l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, outline: Boolean = true, w: Float = ow) =
        fs(Path().apply { addRoundRect(RectF(l, t, r, b), rad, rad, Path.Direction.CW) }, color, outline, w)

    private fun stroke(p: Path, color: Int, w: Float) {
        line.color = color
        line.strokeWidth = w
        c.drawPath(p, line)
    }

    private fun ln(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, w: Float) {
        line.color = color
        line.strokeWidth = w
        c.drawLine(x1, y1, x2, y2, line)
    }

    /** Thick rounded line with an outline (arms, legs, tails…). */
    private fun limb(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, w: Float) {
        if (ow > 0f) ln(x1, y1, x2, y2, oc, w + ow * 2f)
        ln(x1, y1, x2, y2, color, w)
    }

    private fun limbPath(p: Path, color: Int, w: Float) {
        if (ow > 0f) stroke(p, oc, w + ow * 2f)
        stroke(p, color, w)
    }

    private fun poly(vararg pts: Float): Path = Path().apply {
        moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            lineTo(pts[i], pts[i + 1]); i += 2
        }
        close()
    }

    private fun starPath(cx: Float, cy: Float, ro: Float, ri: Float, points: Int = 5): Path = Path().apply {
        val step = Math.PI / points
        for (i in 0 until points * 2) {
            val rad = if (i % 2 == 0) ro else ri
            val ang = -Math.PI / 2 + i * step
            val x = cx + (cos(ang) * rad).toFloat()
            val y = cy + (sin(ang) * rad).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun heartPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
        moveTo(cx, cy + r * 0.9f)
        cubicTo(cx - r * 1.6f, cy - r * 0.2f, cx - r * 0.7f, cy - r * 1.3f, cx, cy - r * 0.45f)
        cubicTo(cx + r * 0.7f, cy - r * 1.3f, cx + r * 1.6f, cy - r * 0.2f, cx, cy + r * 0.9f)
        close()
    }

    /** Union of circles with a single clean outline (afro, curls, clouds). */
    private fun blob(circles: List<FloatArray>, color: Int) {
        if (ow > 0f) {
            fill.color = oc
            for (cc in circles) c.drawCircle(cc[0], cc[1], cc[2] + ow * 0.6f, fill)
        }
        fill.color = color
        for (cc in circles) c.drawCircle(cc[0], cc[1], cc[2], fill)
    }

    // ------------------------------------------------------------------ main draw

    fun draw(canvas: Canvas, width: Float, a: AvatarConfig, pose: AvatarPose) {
        c = canvas
        ow = a.outlineWidth
        oc = col(a.outlineColor)
        canvas.save()
        val u = width / 100f
        canvas.scale(u, u)

        val bs = a.bodyScale.coerceIn(0.7f, 1.4f)
        val r = 29f * a.headScale.coerceIn(0.7f, 1.4f)
        val bb = 126f                      // body bottom
        val bt = bb - 36f * bs             // body top (shoulders)
        val bw = 20f * bs                  // body half width
        val bob = if (pose.walking) -abs(sin(pose.walk)) * 1.6f else 0f
        val hy = bt + 6f - r * 0.95f + bob // head center
        val sw = when (a.bodyShape) { 2 -> bw * 0.72f; 5 -> bw * 0.62f; 4 -> bw * 0.6f; else -> bw * 0.85f }
        val sy = bt + 7f

        val skin = col(a.skinColor)
        val outfit = col(a.outfitColor)
        val outfit2 = col(a.outfitColor2)
        val acc = col(a.accessoryColor)
        val fur = col(a.furColor)
        val hairC = col(a.hairColor)

        // shadow
        if (pose.shadow) {
            fill.color = 0x33000000
            c.drawOval(RectF(28f, 131f, 72f, 139f), fill)
        }

        canvas.save()
        canvas.translate(0f, bob)

        // ---- behind the body
        drawBack(a, bt, bb, bw, sw, acc, outfit2, pose)
        drawTail(a, bb, bw, fur)

        // ---- legs + shoes
        val liftL = if (pose.walking) max(0f, sin(pose.walk)) * 3f else 0f
        val liftR = if (pose.walking) max(0f, -sin(pose.walk)) * 3f else 0f
        val pants = col(a.pantsColor)
        val shoe = col(a.shoeColor)
        val legW = 6.5f * bs.coerceAtMost(1.15f)
        limb(42f, bb - 3f, 42f, 130f - liftL, pants, legW)
        limb(58f, bb - 3f, 58f, 130f - liftR, pants, legW)
        oval(34f, 127f - liftL, 47f, 135f - liftL, shoe)
        oval(53f, 127f - liftR, 66f, 135f - liftR, shoe)

        // ---- body
        val body = bodyPath(a, bt, bb, bw)
        val bodyColor = when (a.outfit) { 4 -> outfit2; 8 -> outfit; else -> outfit }
        fs(body, bodyColor)
        drawOutfit(a, body, bt, bb, bw, sw, skin, outfit, outfit2, acc)
        drawPattern(a, body, bt, bb, bw, outfit2)
        if (ow > 0f) stroke(body, oc, ow) // re-outline over details
        drawNeck(a, bt, sw, acc)

        // ---- arms (left = viewer's left)
        val sleeve = when (a.outfit) { 8 -> skin; 4 -> outfit2; else -> outfit }
        val swing = if (pose.walking) sin(pose.walk) * 14f else 0f
        val armLen = 17f
        val aL = Math.toRadians((pose.armL + swing).toDouble())
        val aR = Math.toRadians((pose.armR - swing).toDouble())
        val lsx = 50f - sw; val rsx = 50f + sw
        val lhx = lsx - (sin(aL) * armLen).toFloat(); val lhy = sy + (cos(aL) * armLen).toFloat()
        val rhx = rsx + (sin(aR) * armLen).toFloat(); val rhy = sy + (cos(aR) * armLen).toFloat()
        limb(lsx, sy, lhx, lhy, sleeve, 7f)
        limb(rsx, sy, rhx, rhy, sleeve, 7f)
        circle(lhx, lhy, 4.2f, skin)
        drawHandItem(a, rhx, rhy, acc, pose)
        circle(rhx, rhy, 4.2f, skin)
        if (a.back == 3) { // backpack straps on top
            ln(50f - sw * 0.6f, bt + 1f, 50f - sw * 0.7f, bt + 18f, shade(acc, 0.7f), 2.4f)
            ln(50f + sw * 0.6f, bt + 1f, 50f + sw * 0.7f, bt + 18f, shade(acc, 0.7f), 2.4f)
        }

        // ---- head
        drawEarsBehind(a, r, hy, skin, fur)
        drawHairBack(a, r, hy, hairC)
        if (a.outfit == 1) { // hoodie hood around the neck
            rrect(50f - sw * 0.95f, bt - 5f, 50f + sw * 0.95f, bt + 5f, 6f, shade(outfit, 0.82f))
        }
        val head = headPath(a.headShape, r, hy)
        fs(head, skin)
        drawCheeks(a, r, hy, skin)
        drawEyes(a, r, hy, pose)
        drawNose(a, r, hy, skin)
        drawBeardBehindMouth(a, r, hy, hairC)
        drawMouth(a, r, hy, pose)
        drawMustache(a, r, hy, hairC)
        drawHairFront(a, r, hy, hairC)
        if (a.ears == 6) drawDogEars(r, hy, fur)
        drawGlasses(a, r, hy, head)
        drawHeadwear(a, r, hy, head, acc)

        canvas.restore()
        canvas.restore()
    }

    // ------------------------------------------------------------------ body

    private fun bodyPath(a: AvatarConfig, bt: Float, bb: Float, bw: Float): Path {
        if (a.outfit == 3) { // dress flares out
            return Path().apply {
                moveTo(50f - bw * 0.72f, bt)
                lineTo(50f + bw * 0.72f, bt)
                lineTo(50f + bw * 1.38f, bb + 1f)
                quadTo(50f, bb + 5f, 50f - bw * 1.38f, bb + 1f)
                close()
            }
        }
        return when (a.bodyShape) {
            0 -> ovalPath(50f - bw * 1.05f, bt, 50f + bw * 1.05f, bb + 2f)
            2 -> Path().apply { addRoundRect(RectF(50f - bw * 0.75f, bt - 3f, 50f + bw * 0.75f, bb), bw * 0.5f, bw * 0.5f, Path.Direction.CW) }
            3 -> Path().apply { addRoundRect(RectF(50f - bw * 1.05f, bt, 50f + bw * 1.05f, bb), 5f, 5f, Path.Direction.CW) }
            4 -> Path().apply {
                moveTo(50f - bw * 0.6f, bt)
                lineTo(50f + bw * 0.6f, bt)
                cubicTo(50f + bw * 0.95f, bt + 8f, 50f + bw * 1.3f, bb - 4f, 50f + bw * 1.1f, bb)
                lineTo(50f - bw * 1.1f, bb)
                cubicTo(50f - bw * 1.3f, bb - 4f, 50f - bw * 0.95f, bt + 8f, 50f - bw * 0.6f, bt)
                close()
            }
            5 -> ovalPath(50f - bw * 0.72f, bt + 5f, 50f + bw * 0.72f, bb)
            else -> Path().apply { addRoundRect(RectF(50f - bw * 0.95f, bt, 50f + bw * 0.95f, bb), bw * 0.7f, bw * 0.7f, Path.Direction.CW) }
        }
    }

    private fun drawOutfit(
        a: AvatarConfig, body: Path, bt: Float, bb: Float, bw: Float, sw: Float,
        skin: Int, outfit: Int, outfit2: Int, acc: Int
    ) {
        when (a.outfit) {
            0 -> { // t-shirt collar
                stroke(Path().apply { addArc(RectF(44f, bt - 5f, 56f, bt + 5f), 0f, 180f) }, oc, ow * 0.8f)
            }
            1 -> { // hoodie pocket + strings
                rrect(50f - bw * 0.55f, bb - 14f, 50f + bw * 0.55f, bb - 4f, 4f, shade(outfit, 0.88f), true, ow * 0.7f)
                ln(47f, bt + 2f, 47f, bt + 11f, WHITE, 1.4f)
                ln(53f, bt + 2f, 53f, bt + 11f, WHITE, 1.4f)
                circle(47f, bt + 11.5f, 1.2f, WHITE, false)
                circle(53f, bt + 11.5f, 1.2f, WHITE, false)
            }
            2 -> { // suit
                fs(poly(44f, bt, 50f, bt + 17f, 56f, bt), WHITE, true, ow * 0.7f)
                fs(poly(48.2f, bt + 1f, 51.8f, bt + 1f, 51.3f, bt + 4f, 48.7f, bt + 4f), outfit2, true, ow * 0.5f)
                fs(poly(48.7f, bt + 4f, 51.3f, bt + 4f, 52.8f, bt + 13f, 50f, bt + 16f, 47.2f, bt + 13f), outfit2, true, ow * 0.5f)
                ln(44f, bt, 47.5f, bt + 18f, oc, ow * 0.7f)
                ln(56f, bt, 52.5f, bt + 18f, oc, ow * 0.7f)
                circle(50f, bt + 21f, 1.2f, shade(outfit, 0.5f), false)
                circle(50f, bt + 26f, 1.2f, shade(outfit, 0.5f), false)
            }
            3 -> { // dress belt + bow
                c.save(); c.clipPath(body)
                fill.color = outfit2
                c.drawRect(0f, bt + 10f, 100f, bt + 14f, fill)
                c.restore()
                fs(poly(50f, bt + 12f, 45f, bt + 9f, 45f, bt + 15f), outfit2, true, ow * 0.5f)
                fs(poly(50f, bt + 12f, 55f, bt + 9f, 55f, bt + 15f), outfit2, true, ow * 0.5f)
            }
            4 -> { // overalls
                c.save(); c.clipPath(body)
                fill.color = outfit
                c.drawRect(0f, bt + 19f, 100f, 140f, fill)
                c.restore()
                rrect(50f - bw * 0.5f, bt + 7f, 50f + bw * 0.5f, bt + 21f, 2.5f, outfit, true, ow * 0.6f)
                ln(50f - sw * 0.8f, bt + 1f, 50f - bw * 0.45f, bt + 8f, outfit, 2.6f)
                ln(50f + sw * 0.8f, bt + 1f, 50f + bw * 0.45f, bt + 8f, outfit, 2.6f)
                circle(50f - bw * 0.38f, bt + 9.5f, 1.3f, 0xFFFFD54F.toInt(), false)
                circle(50f + bw * 0.38f, bt + 9.5f, 1.3f, 0xFFFFD54F.toInt(), false)
            }
            5 -> { // striped sweater
                c.save(); c.clipPath(body)
                fill.color = outfit2
                var y = bt + 5f
                while (y < bb + 4f) { c.drawRect(0f, y, 100f, y + 3f, fill); y += 7f }
                c.restore()
                stroke(Path().apply { addArc(RectF(44f, bt - 5f, 56f, bt + 5f), 0f, 180f) }, shade(outfit, 0.7f), 2.5f)
            }
            6 -> { // superhero
                circle(50f, bt + 12f, 6.5f, outfit2, true, ow * 0.7f)
                fs(starPath(50f, bt + 12f, 4.6f, 2f), outfit, false)
                c.save(); c.clipPath(body)
                fill.color = outfit2
                c.drawRect(0f, bb - 10f, 100f, bb - 7f, fill)
                c.restore()
            }
            7 -> { // robe
                fs(poly(45f, bt, 50f, bt + 9f, 55f, bt), skin, false)
                ln(45f, bt, 50f + bw * 0.5f, bb - 4f, oc, ow * 0.7f)
                c.save(); c.clipPath(body)
                fill.color = outfit2
                c.drawRect(0f, bt + 18f, 100f, bt + 22f, fill)
                c.restore()
            }
            8 -> { // tank top: show skin at the neck
                oval(44f, bt - 4f, 56f, bt + 6f, skin, false)
                ln(50f - sw * 0.75f, bt + 1f, 50f - sw * 0.6f, bt + 6f, skin, 3f)
                ln(50f + sw * 0.75f, bt + 1f, 50f + sw * 0.6f, bt + 6f, skin, 3f)
            }
            9 -> { // jersey number
                fs(poly(45f, bt, 50f, bt + 6f, 55f, bt), outfit2, false)
                text.color = outfit2
                text.textSize = 13f
                c.drawText("1", 50f, bt + 23f, text)
            }
        }
    }

    private fun drawPattern(a: AvatarConfig, body: Path, bt: Float, bb: Float, bw: Float, color: Int) {
        if (a.pattern == 0) return
        c.save()
        c.clipPath(body)
        val col = alpha(color, 0.75f)
        when (a.pattern) {
            1 -> {
                fill.color = col
                var y = bt + 4f
                while (y < bb + 4f) { c.drawRect(0f, y, 100f, y + 2.5f, fill); y += 6f }
            }
            2 -> {
                fill.color = col
                var y = bt + 3f; var row = 0
                while (y < bb + 4f) {
                    var x = 20f + if (row % 2 == 0) 0f else 3.5f
                    while (x < 80f) { c.drawCircle(x, y, 1.3f, fill); x += 7f }
                    y += 6f; row++
                }
            }
            3 -> fs(starPath(50f, bt + 15f, 6f, 2.6f), col, false)
            4 -> fs(heartPath(50f, bt + 15f, 5.5f), col, false)
            5 -> {
                for (yy in listOf(bt + 12f, bt + 22f)) {
                    val p = Path().apply {
                        moveTo(20f, yy)
                        var x = 20f; var up = true
                        while (x < 80f) { x += 4f; lineTo(x, if (up) yy - 3f else yy); up = !up }
                    }
                    stroke(p, col, 1.8f)
                }
            }
        }
        c.restore()
    }

    private fun drawNeck(a: AvatarConfig, bt: Float, sw: Float, acc: Int) {
        when (a.neck) {
            1 -> {
                rrect(50f - sw * 0.95f, bt - 2f, 50f + sw * 0.95f, bt + 5f, 3f, acc, true, ow * 0.8f)
                rrect(50f + sw * 0.25f, bt + 2f, 50f + sw * 0.6f, bt + 16f, 2f, shade(acc, 0.9f), true, ow * 0.8f)
            }
            2 -> {
                fs(poly(50f, bt + 3f, 44.5f, bt, 44.5f, bt + 6f), acc, true, ow * 0.6f)
                fs(poly(50f, bt + 3f, 55.5f, bt, 55.5f, bt + 6f), acc, true, ow * 0.6f)
                circle(50f, bt + 3f, 1.6f, shade(acc, 0.8f), true, ow * 0.5f)
            }
            3 -> {
                for (i in 0..8) {
                    val ang = Math.toRadians(20.0 + i * 17.5)
                    val x = 50f + (cos(ang) * sw * 0.7f).toFloat()
                    val y = bt + 1f + (sin(ang) * 7f).toFloat()
                    circle(x, y, 1.3f, acc, false)
                }
                circle(50f, bt + 9.5f, 2.2f, shade(acc, 1.2f), true, ow * 0.5f)
            }
            4 -> {
                ln(46f, bt, 50f, bt + 10f, 0xFF1E88E5.toInt(), 2.2f)
                ln(54f, bt, 50f, bt + 10f, 0xFFE53935.toInt(), 2.2f)
                circle(50f, bt + 13f, 4.4f, acc, true, ow * 0.7f)
                fs(starPath(50f, bt + 13f, 2.8f, 1.2f), shade(acc, 0.75f), false)
            }
            5 -> {
                rrect(50f - sw * 0.85f, bt - 1f, 50f + sw * 0.85f, bt + 3f, 2f, 0xFFE53935.toInt(), true, ow * 0.6f)
                circle(50f, bt + 5.5f, 3f, acc, true, ow * 0.6f)
                ln(48.5f, bt + 6.5f, 51.5f, bt + 6.5f, oc, 0.8f)
            }
        }
    }

    private fun drawBack(a: AvatarConfig, bt: Float, bb: Float, bw: Float, sw: Float, acc: Int, outfit2: Int, pose: AvatarPose) {
        if (a.outfit == 6 || a.back == 4) { // cape
            val capeColor = if (a.outfit == 6) outfit2 else acc
            fs(Path().apply {
                moveTo(50f - sw, bt + 1f); lineTo(50f + sw, bt + 1f)
                lineTo(50f + bw * 1.55f, bb + 7f)
                quadTo(50f, bb + 11f, 50f - bw * 1.55f, bb + 7f)
                close()
            }, shade(capeColor, 0.9f))
        }
        val flap = sin(pose.time * 6f) * 3f
        for (s in sides) {
            when (a.back) {
                1 -> fs(Path().apply {
                    moveTo(50f + s * bw * 0.5f, bt + 6f)
                    cubicTo(50f + s * (bw + 18f), bt - 14f - flap, 50f + s * (bw + 27f), bt + 8f, 50f + s * (bw + 16f), bt + 22f)
                    cubicTo(50f + s * (bw + 12f), bt + 18f, 50f + s * (bw + 6f), bt + 24f, 50f + s * bw * 0.6f, bt + 18f)
                    close()
                }, WHITE)
                2 -> fs(Path().apply {
                    moveTo(50f + s * bw * 0.5f, bt + 4f)
                    lineTo(50f + s * (bw + 24f), bt - 8f - flap)
                    quadTo(50f + s * (bw + 17f), bt + 2f, 50f + s * (bw + 16f), bt + 14f)
                    quadTo(50f + s * (bw + 10f), bt + 8f, 50f + s * (bw + 8f), bt + 18f)
                    quadTo(50f + s * (bw + 3f), bt + 12f, 50f + s * bw * 0.5f, bt + 16f)
                    close()
                }, acc)
                5 -> {
                    val wx = 50f + s * (bw + 7f)
                    c.save(); c.rotate(s * (55f + flap), wx, bt + 8f)
                    oval(wx - 7f, bt - 12f, wx + 7f, bt + 10f, alpha(acc, 0.6f), true, ow * 0.6f)
                    c.restore()
                    c.save(); c.rotate(s * 120f, wx - s * 3f, bt + 12f)
                    oval(wx - s * 3f - 5f, bt - 2f, wx - s * 3f + 5f, bt + 14f, alpha(acc, 0.6f), true, ow * 0.6f)
                    c.restore()
                }
                6 -> {
                    val x = 50f + s * (bw * 0.95f + 2f)
                    rrect(x - 5f, bt - 2f, x + 5f, bt + 22f, 4f, 0xFFB0BEC5.toInt())
                    val fl = 6f + abs(sin(pose.time * 20f)) * 4f
                    fs(poly(x - 4f, bt + 22f, x + 4f, bt + 22f, x, bt + 22f + fl), 0xFFFF9800.toInt(), false)
                    fs(poly(x - 2f, bt + 22f, x + 2f, bt + 22f, x, bt + 22f + fl * 0.6f), 0xFFFFEB3B.toInt(), false)
                }
            }
        }
        if (a.back == 3) rrect(50f - bw * 1.15f, bt + 2f, 50f + bw * 1.15f, bb - 6f, 6f, acc)
    }

    private fun drawTail(a: AvatarConfig, bb: Float, bw: Float, fur: Int) {
        val x0 = 50f + bw * 0.7f
        when (a.tail) {
            1 -> limbPath(Path().apply {
                moveTo(x0, bb - 6f)
                cubicTo(x0 + 16f, bb - 4f, x0 + 6f, bb - 26f, x0 + 16f, bb - 30f)
            }, fur, 4.5f)
            2 -> {
                val p = Path().apply {
                    moveTo(x0 - 2f, bb - 4f)
                    quadTo(x0 + 22f, bb - 2f, x0 + 18f, bb - 30f)
                    quadTo(x0 + 8f, bb - 18f, x0 - 2f, bb - 12f)
                    close()
                }
                fs(p, fur)
                c.save(); c.clipPath(p)
                fill.color = WHITE
                c.drawCircle(x0 + 18f, bb - 30f, 8f, fill)
                c.restore()
                stroke(p, oc, ow)
            }
            3 -> {
                limbPath(Path().apply {
                    moveTo(x0, bb - 6f)
                    cubicTo(x0 + 14f, bb, x0 + 14f, bb - 18f, x0 + 10f, bb - 24f)
                }, 0xFFD32F2F.toInt(), 2.2f)
                fs(poly(x0 + 10f, bb - 30f, x0 + 15f, bb - 22f, x0 + 5f, bb - 22f), 0xFFD32F2F.toInt())
            }
            4 -> blob(listOf(
                floatArrayOf(x0 + 2f, bb - 7f, 4f), floatArrayOf(x0 + 5f, bb - 9f, 3.5f), floatArrayOf(x0 + 4f, bb - 4f, 3.5f)
            ), WHITE)
        }
    }

    private fun drawHandItem(a: AvatarConfig, hx: Float, hy: Float, acc: Int, pose: AvatarPose) {
        when (a.handItem) {
            1 -> {
                limb(hx, hy, hx + 4f, hy - 15f, 0xFF8D6E63.toInt(), 1.8f)
                fs(starPath(hx + 4f, hy - 18f, 5.5f, 2.4f), acc)
            }
            2 -> {
                val sway = sin(pose.time * 2f) * 2f
                ln(hx, hy, hx + 6f + sway, hy - 28f, oc, 0.8f)
                oval(hx - 1f + sway, hy - 46f, hx + 13f + sway, hy - 28f, acc)
                fill.color = alpha(WHITE, 0.6f)
                c.drawOval(RectF(hx + 1.5f + sway, hy - 43f, hx + 4.5f + sway, hy - 38f), fill)
            }
            3 -> {
                ln(hx, hy, hx + 3f, hy - 13f, 0xFF43A047.toInt(), 1.6f)
                for (i in 0 until 5) {
                    val ang = Math.toRadians(i * 72.0)
                    circle(hx + 3f + (cos(ang) * 2.8f).toFloat(), hy - 16f + (sin(ang) * 2.8f).toFloat(), 2.3f, acc, true, ow * 0.5f)
                }
                circle(hx + 3f, hy - 16f, 1.7f, 0xFFFFEB3B.toInt(), false)
            }
            4 -> {
                rrect(hx - 4f, hy - 9f, hx + 4.5f, hy + 1f, 1.5f, WHITE)
                rrect(hx - 4f, hy - 6f, hx + 4.5f, hy - 2.5f, 0.5f, acc, false)
                val st = sin(pose.time * 3f) * 1.2f
                stroke(Path().apply { moveTo(hx - 1f, hy - 11f); quadTo(hx - 3f + st, hy - 14f, hx - 1f, hy - 17f) }, alpha(0xFF9E9E9E.toInt(), 0.8f), 1f)
                stroke(Path().apply { moveTo(hx + 2f, hy - 11f); quadTo(hx + st, hy - 14f, hx + 2f, hy - 17f) }, alpha(0xFF9E9E9E.toInt(), 0.8f), 1f)
            }
            5 -> {
                limb(hx, hy - 2f, hx + 3f, hy - 26f, 0xFFE0E0E0.toInt(), 3f)
                limb(hx - 4f, hy - 3f, hx + 4.5f, hy - 4f, acc, 2f)
                limb(hx, hy, hx - 0.5f, hy + 4f, 0xFF6D4C41.toInt(), 2f)
            }
            6 -> {
                rrect(hx - 7f, hy - 9f, hx + 6f, hy + 4f, 1.5f, acc)
                ln(hx - 5.5f, hy + 2f, hx + 4.5f, hy + 2f, WHITE, 1.5f)
            }
            7 -> {
                ln(hx, hy + 3f, hx, hy - 26f, 0xFF6D4C41.toInt(), 1.5f)
                val wv = sin(pose.time * 5f) * 1.5f
                fs(Path().apply {
                    moveTo(hx, hy - 26f)
                    quadTo(hx + 7f, hy - 27f + wv, hx + 14f, hy - 22f)
                    quadTo(hx + 7f, hy - 18f - wv, hx, hy - 17f)
                    close()
                }, acc, true, ow * 0.7f)
            }
            8 -> {
                fs(poly(hx - 4f, hy - 6f, hx + 4f, hy - 6f, hx, hy + 5f), 0xFFE0A35A.toInt())
                circle(hx, hy - 9f, 5f, acc)
                circle(hx - 1.5f, hy - 15f, 1.2f, 0xFFE53935.toInt(), false)
            }
            9 -> {
                stroke(Path().apply { addArc(RectF(hx - 9f, hy - 15f, hx - 2f, hy - 8f), 90f, 180f) }, acc, 1.6f)
                stroke(Path().apply { addArc(RectF(hx + 2f, hy - 15f, hx + 9f, hy - 8f), 270f, 180f) }, acc, 1.6f)
                fs(Path().apply {
                    moveTo(hx - 5f, hy - 16f); lineTo(hx + 5f, hy - 16f)
                    quadTo(hx + 5f, hy - 6f, hx, hy - 6f)
                    quadTo(hx - 5f, hy - 6f, hx - 5f, hy - 16f); close()
                }, acc)
                rrect(hx - 4f, hy - 2f, hx + 4f, hy + 1f, 1f, shade(acc, 0.75f))
                ln(hx, hy - 6f, hx, hy - 2f, shade(acc, 0.75f), 1.6f)
            }
        }
    }

    // ------------------------------------------------------------------ head

    private fun headPath(shape: Int, r: Float, hy: Float): Path = when (shape) {
        1 -> ovalPath(50f - r * 0.85f, hy - r * 1.05f, 50f + r * 0.85f, hy + r)
        2 -> ovalPath(50f - r * 1.15f, hy - r * 0.9f, 50f + r * 1.15f, hy + r * 0.9f)
        3 -> Path().apply { addRoundRect(RectF(50f - r, hy - r, 50f + r, hy + r * 0.95f), r * 0.45f, r * 0.45f, Path.Direction.CW) }
        4 -> Path().apply {
            val top = hy - r * 1.05f
            moveTo(50f, top)
            cubicTo(50f + r * 0.7f, top, 50f + r * 1.05f, hy + r * 0.2f, 50f + r * 0.85f, hy + r * 0.6f)
            cubicTo(50f + r * 0.6f, hy + r * 1.05f, 50f - r * 0.6f, hy + r * 1.05f, 50f - r * 0.85f, hy + r * 0.6f)
            cubicTo(50f - r * 1.05f, hy + r * 0.2f, 50f - r * 0.7f, top, 50f, top)
            close()
        }
        5 -> Path().apply {
            moveTo(50f, hy - r)
            cubicTo(50f + r * 1.2f, hy - r, 50f + r * 1.15f, hy + r * 0.45f, 50f, hy + r * 1.05f)
            cubicTo(50f - r * 1.15f, hy + r * 0.45f, 50f - r * 1.2f, hy - r, 50f, hy - r)
            close()
        }
        else -> ovalPath(50f - r, hy - r, 50f + r, hy + r)
    }

    private fun drawEarsBehind(a: AvatarConfig, r: Float, hy: Float, skin: Int, fur: Int) {
        val inner = PINK
        for (s in sides) {
            when (a.ears) {
                1 -> {
                    oval(50f + s * r * 0.98f - r * 0.17f, hy - r * 0.18f, 50f + s * r * 0.98f + r * 0.17f, hy + r * 0.28f, skin)
                    stroke(Path().apply { addArc(RectF(50f + s * r * 0.98f - r * 0.08f, hy - r * 0.06f, 50f + s * r * 0.98f + r * 0.08f, hy + r * 0.16f), if (s < 0) 90f else 270f, 180f) }, shade(skin, 0.75f), 1.2f)
                }
                2 -> fs(poly(50f + s * r * 0.88f, hy - r * 0.18f, 50f + s * r * 1.5f, hy - r * 0.5f, 50f + s * r * 0.9f, hy + r * 0.22f), skin)
                3 -> {
                    fs(poly(50f + s * r * 0.82f, hy - r * 0.45f, 50f + s * r * 0.2f, hy - r * 0.92f, 50f + s * r * 0.75f, hy - r * 1.38f), fur)
                    fs(poly(50f + s * r * 0.7f, hy - r * 0.6f, 50f + s * r * 0.38f, hy - r * 0.86f, 50f + s * r * 0.7f, hy - r * 1.15f), inner, false)
                }
                4 -> {
                    circle(50f + s * r * 0.7f, hy - r * 0.78f, r * 0.3f, fur)
                    circle(50f + s * r * 0.7f, hy - r * 0.78f, r * 0.15f, shade(fur, 1.35f), false)
                }
                5 -> {
                    c.save(); c.rotate(s * 12f, 50f + s * r * 0.35f, hy - r * 0.8f)
                    oval(50f + s * r * 0.35f - r * 0.18f, hy - r * 2.0f, 50f + s * r * 0.35f + r * 0.18f, hy - r * 0.6f, fur)
                    oval(50f + s * r * 0.35f - r * 0.08f, hy - r * 1.85f, 50f + s * r * 0.35f + r * 0.08f, hy - r * 0.85f, inner, false)
                    c.restore()
                }
                7 -> {
                    circle(50f + s * r * 0.9f, hy - r * 0.7f, r * 0.42f, fur)
                    circle(50f + s * r * 0.9f, hy - r * 0.7f, r * 0.24f, inner, false)
                }
            }
        }
    }

    private fun drawDogEars(r: Float, hy: Float, fur: Int) {
        for (s in sides) {
            c.save(); c.rotate(s * 18f, 50f + s * r * 0.9f, hy - r * 0.5f)
            oval(50f + s * r * 0.95f - r * 0.2f, hy - r * 0.6f, 50f + s * r * 0.95f + r * 0.2f, hy + r * 0.45f, fur)
            c.restore()
        }
    }

    private fun drawCheeks(a: AvatarConfig, r: Float, hy: Float, skin: Int) {
        val cc = col(a.cheekColor)
        for (s in sides) {
            val cx = 50f + s * r * 0.62f
            val cy = hy + r * 0.42f
            if (a.cheeks == 1 || a.cheeks == 3) {
                fill.color = alpha(cc, 0.55f)
                c.drawOval(RectF(cx - r * 0.17f, cy - r * 0.1f, cx + r * 0.17f, cy + r * 0.1f), fill)
            }
            if (a.cheeks == 2 || a.cheeks == 3) {
                fill.color = shade(skin, 0.62f)
                c.drawCircle(cx - 2f, cy - 1.2f, 0.65f, fill)
                c.drawCircle(cx + 1.5f, cy - 1.8f, 0.65f, fill)
                c.drawCircle(cx, cy + 0.8f, 0.65f, fill)
            }
            when (a.cheeks) {
                4 -> {
                    ln(cx - 2.5f, cy - 1.5f, cx + 2.5f, cy - 1.5f, cc, 1.6f)
                    ln(cx - 2.5f, cy + 1.5f, cx + 2.5f, cy + 1.5f, cc, 1.6f)
                }
                5 -> fs(heartPath(cx, cy, r * 0.09f), alpha(cc, 0.85f), false)
                6 -> {
                    val p = Path()
                    for (i in 0..24) {
                        val t = i / 24f
                        val ang = t * 4f * Math.PI.toFloat()
                        val rad = r * 0.1f * t
                        val x = cx + cos(ang) * rad
                        val y = cy + sin(ang) * rad
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    stroke(p, cc, 1.1f)
                }
            }
        }
    }

    private fun drawEyes(a: AvatarConfig, r: Float, hy: Float, pose: AvatarPose) {
        val ex = r * 0.36f * a.eyeSpacing
        val ey = hy + r * 0.08f
        var er = r * 0.15f * a.eyeSize
        var style = a.eyes
        when (pose.expression) {
            1 -> style = 3
            2 -> { style = if (style == 7) 7 else 0; er *= 1.25f }
            3 -> style = -1
            4 -> style = 2
        }
        val lx = pose.lookX.coerceIn(-1f, 1f) * er * 0.4f
        val ly = pose.lookY.coerceIn(-1f, 1f) * er * 0.4f
        val eyeC = col(a.eyeColor)
        val closed = pose.blink > 0.5f && style != 3 && style != -1

        if (style == 7) { // cyclops
            if (closed) closedEye(50f, ey, er * 1.7f) else roundEye(50f, ey - r * 0.02f, er * 1.75f, lx * 1.5f, ly * 1.5f, eyeC, pose.expression == 2)
        } else {
            for (s in sides) {
                val x = 50f + s * ex
                if (closed) { closedEye(x, ey, er); continue }
                when (style) {
                    -1 -> { // dizzy spiral
                        val p = Path()
                        for (i in 0..30) {
                            val t = i / 30f
                            val ang = t * 5f * Math.PI.toFloat() * s
                            val rad = er * 1.05f * t
                            val px = x + cos(ang) * rad
                            val py = ey + sin(ang) * rad
                            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        }
                        stroke(p, oc, 1.5f)
                    }
                    0 -> roundEye(x, ey, er, lx, ly, eyeC, pose.expression == 2)
                    1 -> {
                        oval(x - er * 1.1f, ey - er * 1.35f, x + er * 1.1f, ey + er * 1.35f, WHITE)
                        circle(x + lx, ey + ly, er * 0.88f, eyeC, false)
                        circle(x + lx, ey + ly, er * 0.45f, BLACK, false)
                        circle(x + lx - er * 0.35f, ey + ly - er * 0.4f, er * 0.3f, WHITE, false)
                        circle(x + lx + er * 0.35f, ey + ly + er * 0.35f, er * 0.15f, WHITE, false)
                    }
                    2 -> {
                        roundEye(x, ey, er, lx, ly * 0.3f + er * 0.15f, eyeC, false)
                        c.save()
                        c.clipRect(x - er * 1.3f, ey - er * 1.5f, x + er * 1.3f, ey - er * 0.05f)
                        oval(x - er * 0.9f, ey - er * 1.15f, x + er * 0.9f, ey + er * 1.15f, col(a.skinColor), false)
                        c.restore()
                        ln(x - er * 0.9f, ey - er * 0.05f, x + er * 0.9f, ey - er * 0.05f, oc, ow * 0.9f)
                    }
                    3 -> stroke(Path().apply { addArc(RectF(x - er, ey - er * 0.5f, x + er, ey + er * 1.1f), 200f, 140f) }, oc, 2.2f)
                    4 -> {
                        circle(x + lx * 0.5f, ey + ly * 0.5f, er * 0.55f, BLACK, false)
                        circle(x + lx * 0.5f - er * 0.18f, ey + ly * 0.5f - er * 0.2f, er * 0.17f, WHITE, false)
                    }
                    5 -> {
                        val white = RectF(x - er * 0.95f, ey - er * 1.5f, x + er * 0.95f, ey + er * 1.1f)
                        oval(white.left, white.top, white.right, white.bottom, WHITE, false)
                        oval(x + lx - er * 0.7f, ey + ly - er * 1.1f, x + lx + er * 0.7f, ey + ly + er * 0.95f, eyeC, false)
                        oval(x + lx - er * 0.38f, ey + ly - er * 0.55f, x + lx + er * 0.38f, ey + ly + er * 0.5f, shade(eyeC, 0.35f), false)
                        circle(x + lx - er * 0.3f, ey + ly - er * 0.55f, er * 0.32f, WHITE, false)
                        circle(x + lx + er * 0.25f, ey + ly + er * 0.4f, er * 0.13f, WHITE, false)
                        stroke(Path().apply { addArc(white, 195f, 150f) }, oc, ow * 1.4f)
                        ln(x + s * er * 0.85f, ey - er * 1.05f, x + s * er * 1.3f, ey - er * 1.35f, oc, ow * 0.8f)
                    }
                    6 -> {
                        val p = Path().apply {
                            moveTo(x + s * er, ey - er * 0.55f)
                            lineTo(x - s * er, ey - er * 0.2f)
                            quadTo(x - s * er, ey + er * 0.8f, x, ey + er * 0.8f)
                            quadTo(x + s * er * 1.05f, ey + er * 0.75f, x + s * er, ey - er * 0.55f)
                            close()
                        }
                        fill.color = WHITE; c.drawPath(p, fill)
                        c.save(); c.clipPath(p)
                        circle(x + lx, ey + ly + er * 0.15f, er * 0.6f, eyeC, false)
                        circle(x + lx, ey + ly + er * 0.15f, er * 0.3f, BLACK, false)
                        c.restore()
                        stroke(p, oc, ow)
                        ln(x + s * er * 1.1f, ey - er * 0.62f, x - s * er * 1.05f, ey - er * 0.25f, oc, ow * 1.5f)
                    }
                    8 -> {
                        fs(starPath(x + lx * 0.4f, ey + ly * 0.4f, er * 1.15f, er * 0.5f), eyeC)
                        circle(x + lx * 0.4f - er * 0.2f, ey + ly * 0.4f - er * 0.25f, er * 0.18f, WHITE, false)
                    }
                    9 -> {
                        fs(heartPath(x + lx * 0.4f, ey + ly * 0.4f, er * 1.05f), eyeC)
                        circle(x + lx * 0.4f - er * 0.35f, ey + ly * 0.4f - er * 0.3f, er * 0.18f, WHITE, false)
                    }
                }
            }
        }
        drawBrows(a, r, ex, ey, er, pose)
    }

    private fun roundEye(x: Float, y: Float, er: Float, lx: Float, ly: Float, eyeC: Int, smallPupil: Boolean) {
        oval(x - er * 0.9f, y - er * 1.15f, x + er * 0.9f, y + er * 1.15f, WHITE)
        circle(x + lx, y + ly, er * 0.62f, eyeC, false)
        circle(x + lx, y + ly, er * (if (smallPupil) 0.2f else 0.32f), BLACK, false)
        circle(x + lx - er * 0.25f, y + ly - er * 0.3f, er * 0.18f, WHITE, false)
    }

    private fun closedEye(x: Float, y: Float, er: Float) {
        stroke(Path().apply { moveTo(x - er, y); quadTo(x, y + er * 0.7f, x + er, y) }, oc, 1.9f)
    }

    private fun drawBrows(a: AvatarConfig, r: Float, ex: Float, ey: Float, er: Float, pose: AvatarPose) {
        if (a.brows == 0) return
        val bc = shade(col(a.hairColor), 0.9f)
        var by = ey - er * 1.55f - r * 0.05f
        if (pose.expression == 2) by -= 2.5f
        val bw = er * 1.15f
        if (a.brows == 5) {
            stroke(Path().apply {
                moveTo(50f - ex - bw, by + 1f)
                quadTo(50f, by - 1.5f, 50f + ex + bw, by + 1f)
            }, bc, 3.2f)
            return
        }
        for (s in sides) {
            val x = 50f + s * ex
            when (a.brows) {
                1 -> stroke(Path().apply { moveTo(x - bw, by + 1f); quadTo(x, by - 2f, x + bw, by + 1f) }, bc, 1.5f)
                2 -> stroke(Path().apply { moveTo(x - bw, by + 1f); quadTo(x, by - 2f, x + bw, by + 1f) }, bc, 3.6f)
                3 -> ln(x + s * bw, by - 1.5f, x - s * bw, by + 2f, bc, 3f)
                4 -> ln(x + s * bw, by + 2f, x - s * bw, by - 2f, bc, 2.6f)
                6 -> oval(x - bw, by - 2.2f, x + bw, by + 1.6f, bc, false)
            }
        }
    }

    private fun drawNose(a: AvatarConfig, r: Float, hy: Float, skin: Int) {
        val ny = hy + r * 0.36f
        val nr = r * 0.07f
        when (a.nose) {
            1 -> circle(50f, ny, nr * 0.8f, shade(skin, 0.72f), false)
            2 -> oval(50f - nr * 1.3f, ny - nr, 50f + nr * 1.3f, ny + nr, shade(skin, 0.92f), true, ow * 0.6f)
            3 -> stroke(Path().apply { moveTo(50f - nr * 0.2f, ny - nr * 1.8f); lineTo(50f + nr * 1.2f, ny + nr * 0.6f); lineTo(50f - nr * 0.5f, ny + nr * 0.8f) }, oc, 1.4f)
            4 -> circle(50f, ny, nr * 2f, shade(skin, 0.9f), true, ow * 0.7f)
            5 -> {
                oval(50f - nr * 1.8f, ny - nr * 1.2f, 50f + nr * 1.8f, ny + nr * 1.3f, 0xFFFFA8B8.toInt(), true, ow * 0.7f)
                oval(50f - nr * 0.9f, ny - nr * 0.4f, 50f - nr * 0.3f, ny + nr * 0.5f, 0xFF8E3B4A.toInt(), false)
                oval(50f + nr * 0.3f, ny - nr * 0.4f, 50f + nr * 0.9f, ny + nr * 0.5f, 0xFF8E3B4A.toInt(), false)
            }
            6 -> {
                circle(50f, ny, nr * 1.9f, 0xFFE53935.toInt(), true, ow * 0.7f)
                circle(50f - nr * 0.6f, ny - nr * 0.6f, nr * 0.5f, alpha(WHITE, 0.8f), false)
            }
        }
    }

    private fun smileY(t: Float, my: Float, mw: Float): Float = (my - 1f) + 2f * t * (1f - t) * (0.9f * mw + 1f)

    private fun drawMouth(a: AvatarConfig, r: Float, hy: Float, pose: AvatarPose) {
        val my = hy + r * 0.58f
        val mw = r * 0.3f
        if (pose.mouthOpen > 0.15f && pose.expression != 3) { // talking
            val h = mw * (0.25f + 0.75f * pose.mouthOpen)
            val p = ovalPath(50f - mw * 0.6f, my - h * 0.3f, 50f + mw * 0.6f, my + h)
            fs(p, INK_DARK, true, ow * 0.8f)
            c.save(); c.clipPath(p)
            fill.color = PINK
            c.drawCircle(50f, my + h, mw * 0.45f, fill)
            c.restore()
            return
        }
        var style = a.mouth
        when (pose.expression) {
            1 -> style = 1
            2 -> style = 7
            3 -> style = -1
            4 -> style = 7
        }
        when (style) {
            -1 -> {
                val p = Path().apply {
                    moveTo(50f - mw, my)
                    var x = 50f - mw; var up = true
                    while (x < 50f + mw) { x += mw / 3f; quadTo(x - mw / 6f, if (up) my - 2f else my + 2f, x, my); up = !up }
                }
                stroke(p, oc, 1.6f)
            }
            0 -> stroke(Path().apply { moveTo(50f - mw, my - 1f); quadTo(50f, my + mw * 0.9f, 50f + mw, my - 1f) }, oc, 2f)
            1 -> {
                val p = Path().apply {
                    moveTo(50f - mw * 1.2f, my - 2f)
                    arcTo(RectF(50f - mw * 1.2f, my - 2f - mw * 1.2f, 50f + mw * 1.2f, my - 2f + mw * 1.2f), 180f, -180f, false)
                    close()
                }
                fs(p, INK_DARK, true, ow * 0.8f)
                c.save(); c.clipPath(p)
                fill.color = WHITE
                c.drawRect(40f, my - 3f, 60f, my - 2f + mw * 0.35f, fill)
                fill.color = PINK
                c.drawCircle(50f, my - 2f + mw * 1.2f, mw * 0.6f, fill)
                c.restore()
            }
            2 -> {
                val p = ovalPath(50f - mw * 0.6f, my - mw * 0.3f, 50f + mw * 0.6f, my + mw * 0.8f)
                fs(p, INK_DARK, true, ow * 0.8f)
                c.save(); c.clipPath(p)
                fill.color = PINK
                c.drawCircle(50f, my + mw * 0.8f, mw * 0.45f, fill)
                c.restore()
            }
            3 -> stroke(Path().apply {
                moveTo(50f - mw * 0.9f, my)
                quadTo(50f - mw * 0.45f, my + mw * 0.6f, 50f, my)
                quadTo(50f + mw * 0.45f, my + mw * 0.6f, 50f + mw * 0.9f, my)
            }, oc, 1.8f)
            4 -> stroke(Path().apply { moveTo(50f - mw * 0.8f, my + 1f); quadTo(50f + mw * 0.2f, my + mw * 0.5f, 50f + mw, my - mw * 0.35f) }, oc, 2f)
            5 -> {
                rrect(50f - mw, my - mw * 0.25f, 50f + mw, my + mw * 0.5f, 2.5f, WHITE, true, ow * 0.8f)
                ln(50f - mw, my + mw * 0.12f, 50f + mw, my + mw * 0.12f, oc, 0.6f)
                for (i in 1..3) {
                    val x = 50f - mw + i * (mw / 2f)
                    ln(x, my - mw * 0.25f, x, my + mw * 0.5f, oc, 0.6f)
                }
            }
            6 -> {
                rrect(50f - mw * 0.1f, my + 0.5f, 50f + mw * 0.6f, my + mw * 1.05f, 3f, PINK, true, ow * 0.7f)
                ln(50f + mw * 0.25f, my + 1f, 50f + mw * 0.25f, my + mw * 0.7f, shade(PINK, 0.8f), 0.7f)
                stroke(Path().apply { moveTo(50f - mw, my - 1f); quadTo(50f, my + mw * 0.9f, 50f + mw, my - 1f) }, oc, 2f)
            }
            7 -> oval(50f - mw * 0.3f, my - mw * 0.1f, 50f + mw * 0.3f, my + mw * 0.5f, INK_DARK, true, ow * 0.8f)
            8 -> ln(50f - mw * 0.7f, my + 1f, 50f + mw * 0.7f, my + 1f, oc, 2f)
            9 -> {
                for (s in sides) {
                    val x = 50f + s * mw * 0.45f
                    val yy = smileY(0.275f, my, mw) - 0.3f
                    fs(poly(x - 1.6f, yy, x + 1.6f, yy, x, yy + 3.2f), WHITE, true, ow * 0.4f)
                }
                stroke(Path().apply { moveTo(50f - mw, my - 1f); quadTo(50f, my + mw * 0.9f, 50f + mw, my - 1f) }, oc, 2f)
            }
            10 -> {
                val yc = smileY(0.5f, my, mw) - 0.6f
                rrect(50f - 2.8f, yc, 50f + 2.8f, yc + 3.6f, 0.8f, WHITE, true, ow * 0.4f)
                ln(50f, yc, 50f, yc + 3.6f, oc, 0.5f)
                stroke(Path().apply { moveTo(50f - mw, my - 1f); quadTo(50f, my + mw * 0.9f, 50f + mw, my - 1f) }, oc, 2f)
            }
        }
    }

    private fun drawBeardBehindMouth(a: AvatarConfig, r: Float, hy: Float, hc: Int) {
        when (a.facialHair) {
            3 -> fs(Path().apply {
                moveTo(50f - r * 0.93f, hy + r * 0.05f)
                quadTo(50f - r * 0.88f, hy + r * 1.2f, 50f, hy + r * 1.22f)
                quadTo(50f + r * 0.88f, hy + r * 1.2f, 50f + r * 0.93f, hy + r * 0.05f)
                lineTo(50f + r * 0.72f, hy + r * 0.3f)
                quadTo(50f + r * 0.55f, hy + r * 0.88f, 50f, hy + r * 0.9f)
                quadTo(50f - r * 0.55f, hy + r * 0.88f, 50f - r * 0.72f, hy + r * 0.3f)
                close()
            }, hc)
            4 -> oval(50f - r * 0.14f, hy + r * 0.8f, 50f + r * 0.14f, hy + r * 1.08f, hc)
            5 -> {
                fill.color = alpha(hc, 0.55f)
                for (i in 0 until 28) {
                    val t = i / 27f
                    val ang = Math.PI * (0.12 + 0.76 * t)
                    val rr = r * (0.78f + 0.12f * ((i * 7) % 3) / 2f)
                    val x = 50f + (cos(ang) * rr).toFloat()
                    val y = hy + r * 0.15f + (sin(ang) * rr * 0.9f).toFloat()
                    c.drawCircle(x, y, 0.55f, fill)
                }
            }
        }
    }

    private fun drawMustache(a: AvatarConfig, r: Float, hy: Float, hc: Int) {
        if (a.facialHair != 1 && a.facialHair != 2 && a.facialHair != 3) return
        val y = hy + r * 0.47f
        val curl = a.facialHair == 2
        for (s in sides) {
            fs(Path().apply {
                moveTo(50f, y - 1f)
                cubicTo(50f + s * r * 0.15f, y - r * 0.12f, 50f + s * r * 0.38f, y - r * 0.02f, 50f + s * r * (if (curl) 0.44f else 0.36f), y + (if (curl) -r * 0.14f else r * 0.08f))
                cubicTo(50f + s * r * 0.22f, y + r * 0.08f, 50f + s * r * 0.1f, y + r * 0.07f, 50f, y + r * 0.03f)
                close()
            }, hc, true, ow * 0.8f)
        }
    }

    private fun capPath(r: Float, hy: Float, fringe: Int): Path = Path().apply {
        arcTo(RectF(50f - r * 1.07f, hy - r * 1.1f, 50f + r * 1.07f, hy + r * 0.98f), 180f, 180f, true)
        when (fringe) {
            1 -> { // bowl
                lineTo(50f + r * 1.05f, hy - r * 0.05f)
                lineTo(50f + r * 0.9f, hy - r * 0.38f)
                lineTo(50f - r * 0.9f, hy - r * 0.38f)
                lineTo(50f - r * 1.05f, hy - r * 0.05f)
            }
            2 -> { // side swept
                lineTo(50f + r * 0.98f, hy - r * 0.1f)
                quadTo(50f + r * 0.5f, hy - r * 0.75f, 50f - r * 0.3f, hy - r * 0.45f)
                quadTo(50f - r * 0.8f, hy - r * 0.25f, 50f - r * 1.02f, hy + r * 0.2f)
            }
            3 -> { // middle part
                lineTo(50f + r * 1.0f, hy + r * 0.2f)
                quadTo(50f + r * 0.6f, hy - r * 0.6f, 50f, hy - r * 0.82f)
                quadTo(50f - r * 0.6f, hy - r * 0.6f, 50f - r * 1.0f, hy + r * 0.2f)
            }
            4 -> { // zigzag bangs
                lineTo(50f + r * 1.02f, hy + r * 0.1f)
                lineTo(50f + r * 0.85f, hy - r * 0.3f)
                for (i in 1..6) {
                    val x = 50f + r * 0.85f - i * (r * 1.7f / 6f)
                    lineTo(x, hy - if (i % 2 == 0) r * 0.3f else r * 0.45f)
                }
                lineTo(50f - r * 1.02f, hy + r * 0.1f)
            }
            else -> { // short
                lineTo(50f + r * 1.0f, hy + r * 0.05f)
                quadTo(50f + r * 0.75f, hy - r * 0.55f, 50f + r * 0.35f, hy - r * 0.5f)
                quadTo(50f + r * 0.05f, hy - r * 0.35f, 50f - r * 0.2f, hy - r * 0.55f)
                quadTo(50f - r * 0.7f, hy - r * 0.65f, 50f - r * 1.0f, hy + r * 0.05f)
            }
        }
        close()
    }

    private fun drawHairBack(a: AvatarConfig, r: Float, hy: Float, hc: Int) {
        val tie = col(a.headwearColor)
        when (a.hair) {
            4 -> rrect(50f - r * 1.12f, hy - r * 1.05f, 50f + r * 1.12f, hy + r * 1.45f, r * 0.9f, hc)
            5 -> {
                c.save(); c.rotate(-22f, 50f + r * 1.05f, hy + r * 0.2f)
                oval(50f + r * 0.78f, hy - r * 0.55f, 50f + r * 1.32f, hy + r * 1.0f, hc)
                c.restore()
                circle(50f + r * 0.86f, hy - r * 0.5f, r * 0.11f, tie)
            }
            6 -> for (s in sides) {
                oval(50f + s * r * 1.15f - r * 0.3f, hy - r * 0.1f, 50f + s * r * 1.15f + r * 0.3f, hy + r * 0.95f, hc)
                circle(50f + s * r * 1.02f, hy - r * 0.08f, r * 0.1f, tie)
            }
            7 -> {
                val list = mutableListOf(floatArrayOf(50f, hy - r * 0.2f, r * 1.3f))
                for (i in 0 until 10) {
                    val ang = Math.toRadians(i * 36.0)
                    list += floatArrayOf(50f + (cos(ang) * r * 1.25f).toFloat(), hy - r * 0.2f + (sin(ang) * r * 1.2f).toFloat(), r * 0.38f)
                }
                blob(list, hc)
            }
            9 -> circle(50f, hy - r * 1.12f, r * 0.38f, hc)
            10 -> {
                val list = mutableListOf<FloatArray>()
                for (i in 0 until 12) {
                    val ang = Math.toRadians(150.0 + i * 20.0)
                    list += floatArrayOf(50f + (cos(ang) * r * 1.02f).toFloat(), hy - r * 0.05f + (sin(ang) * r * 1.02f).toFloat(), r * 0.3f)
                }
                list += floatArrayOf(50f - r * 1.0f, hy + r * 0.45f, r * 0.26f)
                list += floatArrayOf(50f + r * 1.0f, hy + r * 0.45f, r * 0.26f)
                blob(list, hc)
            }
            12 -> rrect(50f - r * 1.12f, hy - r * 1.05f, 50f + r * 1.12f, hy + r * 0.75f, r * 0.6f, hc)
        }
    }

    private fun spike(r: Float, hy: Float, angDeg: Float, half: Float, len: Float): Path {
        val cy = hy - r * 0.06f
        fun pt(a: Float, rad: Float): FloatArray {
            val t = Math.toRadians(a.toDouble())
            return floatArrayOf(50f + (cos(t) * rad).toFloat(), cy + (sin(t) * rad).toFloat())
        }
        val p1 = pt(angDeg - half, r * 0.95f)
        val tip = pt(angDeg, r * len)
        val p2 = pt(angDeg + half, r * 0.95f)
        return poly(p1[0], p1[1], tip[0], tip[1], p2[0], p2[1])
    }

    private fun drawHairFront(a: AvatarConfig, r: Float, hy: Float, hc: Int) {
        when (a.hair) {
            0 -> return
            1 -> fs(capPath(r, hy, 0), hc)
            2 -> {
                var ang = 195f
                while (ang <= 345f) { fs(spike(r, hy, ang, 13f, 1.5f), hc); ang += 25f }
                fs(capPath(r, hy, 0), hc)
            }
            3 -> fs(capPath(r, hy, 1), hc)
            4, 6 -> fs(capPath(r, hy, 3), hc)
            5 -> fs(capPath(r, hy, 0), hc)
            7 -> {
                val list = mutableListOf(floatArrayOf(50f, hy - r * 0.6f, r * 0.62f))
                var ang = 185.0
                while (ang <= 355.0) {
                    val t = Math.toRadians(ang)
                    list += floatArrayOf(50f + (cos(t) * r * 0.88f).toFloat(), hy - r * 0.1f + (sin(t) * r * 0.8f).toFloat(), r * 0.3f)
                    ang += 21.25
                }
                blob(list, hc)
            }
            8 -> {
                for (ang in listOf(235f, 252f, 270f, 288f, 305f)) fs(spike(r, hy, ang, 9f, 1.7f), hc)
                c.save(); c.clipPath(headPath(a.headShape, r, hy))
                fill.color = alpha(hc, 0.25f)
                c.drawPath(capPath(r, hy, 1), fill)
                c.restore()
            }
            9 -> fs(capPath(r, hy, 3), hc)
            10 -> {
                val list = mutableListOf(floatArrayOf(50f, hy - r * 0.62f, r * 0.55f))
                var ang = 190.0
                while (ang <= 350.0) {
                    val t = Math.toRadians(ang)
                    list += floatArrayOf(50f + (cos(t) * r * 0.85f).toFloat(), hy - r * 0.12f + (sin(t) * r * 0.78f).toFloat(), r * 0.24f)
                    ang += 20.0
                }
                blob(list, hc)
            }
            11 -> fs(capPath(r, hy, 2), hc)
            12 -> fs(capPath(r, hy, 4), hc)
        }
        if (a.hairShine && a.hair != 8) {
            stroke(Path().apply { addArc(RectF(50f - r * 0.8f, hy - r * 0.95f, 50f + r * 0.8f, hy + r * 0.6f), 205f, 40f) }, alpha(WHITE, 0.45f), 2.4f)
        }
    }

    private fun drawGlasses(a: AvatarConfig, r: Float, hy: Float, head: Path) {
        if (a.glasses == 0) return
        val gc = col(a.glassesColor)
        val ex = r * 0.36f * a.eyeSpacing
        val ey = hy + r * 0.08f
        val gr = max(r * 0.2f, r * 0.15f * a.eyeSize * 1.5f)
        val cyc = a.eyes == 7
        val xs = if (cyc) floatArrayOf(50f) else floatArrayOf(50f - ex, 50f + ex)
        fun bridge() { if (!cyc) ln(50f - ex + gr, ey, 50f + ex - gr, ey, gc, 1.6f) }
        fun temples() {
            val l = xs.first() - gr; val rr = xs.last() + gr
            ln(l, ey - 1f, 50f - r * 0.98f, ey - 2f, gc, 1.4f)
            ln(rr, ey - 1f, 50f + r * 0.98f, ey - 2f, gc, 1.4f)
        }
        when (a.glasses) {
            1 -> { temples(); bridge(); for (x in xs) { fill.color = alpha(0xFFB3E5FC.toInt(), 0.25f); c.drawCircle(x, ey, gr, fill); line.color = gc; line.strokeWidth = 1.8f; c.drawCircle(x, ey, gr, line) } }
            2 -> { temples(); bridge(); for (x in xs) { fill.color = alpha(0xFFB3E5FC.toInt(), 0.25f); val rf = RectF(x - gr * 1.1f, ey - gr * 0.85f, x + gr * 1.1f, ey + gr * 0.85f); c.drawRoundRect(rf, 2f, 2f, fill); line.color = gc; line.strokeWidth = 2f; c.drawRoundRect(rf, 2f, 2f, line) } }
            3 -> {
                temples(); bridge()
                for (x in xs) {
                    rrect(x - gr * 1.15f, ey - gr * 0.8f, x + gr * 1.15f, ey + gr * 0.9f, gr * 0.5f, 0xEE1B1B1B.toInt(), false)
                    line.color = gc; line.strokeWidth = 1.6f
                    c.drawRoundRect(RectF(x - gr * 1.15f, ey - gr * 0.8f, x + gr * 1.15f, ey + gr * 0.9f), gr * 0.5f, gr * 0.5f, line)
                    ln(x - gr * 0.6f, ey - gr * 0.35f, x - gr * 0.1f, ey - gr * 0.55f, alpha(WHITE, 0.8f), 1.2f)
                }
            }
            4 -> {
                val x = xs.last()
                line.color = gc; line.strokeWidth = 1.8f
                c.drawCircle(x, ey, gr, line)
                stroke(Path().apply { moveTo(x + gr * 0.7f, ey + gr * 0.7f); quadTo(x + gr * 1.5f, ey + r * 0.6f, x + gr * 0.4f, ey + r * 0.9f) }, gc, 0.8f)
            }
            5 -> { temples(); bridge(); for (x in xs) { val p = starPath(x, ey, gr * 1.35f, gr * 0.75f); fill.color = alpha(gc, 0.3f); c.drawPath(p, fill); stroke(p, gc, 1.8f) } }
            6 -> { temples(); bridge(); for (x in xs) { val p = heartPath(x, ey, gr * 1.1f); fill.color = alpha(0xFFFF4081.toInt(), 0.35f); c.drawPath(p, fill); stroke(p, gc, 1.8f) } }
            7 -> {
                c.save(); c.clipPath(head)
                fill.color = gc
                c.drawRect(0f, ey - gr * 0.45f, 100f, ey + gr * 0.45f, fill)
                c.restore()
                for (x in xs) {
                    circle(x, ey, gr * 1.15f, shade(gc, 0.7f))
                    circle(x, ey, gr * 0.85f, alpha(0xFF81D4FA.toInt(), 0.75f), true, ow * 0.6f)
                    circle(x - gr * 0.3f, ey - gr * 0.3f, gr * 0.2f, alpha(WHITE, 0.8f), false)
                }
            }
            8 -> {
                c.save(); c.clipPath(head)
                ln(50f - r * 1.1f, hy - r * 0.55f, 50f + r * 1.1f, hy + r * 0.35f, BLACK, 1.4f)
                c.restore()
                val x = xs.first()
                oval(x - gr * 1.05f, ey - gr * 0.9f, x + gr * 1.05f, ey + gr * 0.95f, BLACK)
            }
        }
    }

    private fun drawHeadwear(a: AvatarConfig, r: Float, hy: Float, head: Path, acc: Int) {
        val hw = col(a.headwearColor)
        when (a.headwear) {
            1 -> {
                fs(Path().apply { arcTo(RectF(50f - r * 1.0f, hy - r * 1.15f, 50f + r * 1.0f, hy + r * 0.45f), 180f, 180f, true); close() }, hw)
                rrect(50f + r * 0.05f, hy - r * 0.47f, 50f + r * 1.55f, hy - r * 0.27f, 3f, shade(hw, 0.85f))
                circle(50f, hy - r * 1.14f, 1.6f, shade(hw, 0.8f))
                circle(50f - r * 0.1f, hy - r * 0.75f, r * 0.13f, acc, false)
            }
            2 -> {
                fs(Path().apply { arcTo(RectF(50f - r * 1.02f, hy - r * 1.25f, 50f + r * 1.02f, hy + r * 0.35f), 180f, 180f, true); close() }, hw)
                rrect(50f - r * 1.06f, hy - r * 0.58f, 50f + r * 1.06f, hy - r * 0.32f, 3f, shade(hw, 0.82f))
                circle(50f, hy - r * 1.28f, r * 0.2f, acc)
            }
            3 -> {
                val base = hy - r * 0.75f
                val top = hy - r * 1.38f
                fs(poly(
                    50f - r * 0.72f, base, 50f - r * 0.78f, top, 50f - r * 0.4f, base - r * 0.3f,
                    50f, top - r * 0.1f, 50f + r * 0.4f, base - r * 0.3f, 50f + r * 0.78f, top, 50f + r * 0.72f, base
                ), hw)
                circle(50f, base - r * 0.15f, r * 0.08f, acc, true, ow * 0.5f)
                circle(50f - r * 0.45f, base - r * 0.1f, r * 0.06f, shade(acc, 0.8f), false)
                circle(50f + r * 0.45f, base - r * 0.1f, r * 0.06f, shade(acc, 0.8f), false)
            }
            4 -> {
                oval(50f - r * 1.05f, hy - r * 0.97f, 50f + r * 1.05f, hy - r * 0.7f, hw)
                rrect(50f - r * 0.62f, hy - r * 1.9f, 50f + r * 0.62f, hy - r * 0.83f, 2f, hw)
                rrect(50f - r * 0.62f, hy - r * 1.08f, 50f + r * 0.62f, hy - r * 0.9f, 0f, acc, true, ow * 0.6f)
            }
            5 -> {
                fs(Path().apply {
                    moveTo(50f - r * 1.05f, hy - r * 0.6f)
                    quadTo(50f - r * 0.3f, hy - r * 1.3f, 50f + r * 0.3f, hy - r * 2.2f)
                    quadTo(50f + r * 0.3f, hy - r * 1.3f, 50f + r * 1.05f, hy - r * 0.6f)
                    quadTo(50f, hy - r * 0.35f, 50f - r * 1.05f, hy - r * 0.6f)
                    close()
                }, hw)
                fs(starPath(50f - r * 0.1f, hy - r * 1.05f, r * 0.13f, r * 0.06f), acc, false)
                fs(starPath(50f + r * 0.35f, hy - r * 0.75f, r * 0.09f, r * 0.04f), acc, false)
                fs(starPath(50f + r * 0.12f, hy - r * 1.55f, r * 0.08f, r * 0.035f), acc, false)
            }
            6 -> {
                c.save(); c.clipPath(head)
                fill.color = hw
                c.drawRect(0f, hy - r * 0.62f, 100f, hy - r * 0.4f, fill)
                c.restore()
                fs(poly(50f + r * 0.9f, hy - r * 0.5f, 50f + r * 1.3f, hy - r * 0.75f, 50f + r * 1.25f, hy - r * 0.3f), hw, true, ow * 0.6f)
            }
            7 -> {
                val cx = 50f + r * 0.55f; val cy = hy - r * 0.88f
                fs(poly(cx, cy, cx - r * 0.42f, cy - r * 0.27f, cx - r * 0.38f, cy + r * 0.25f), hw)
                fs(poly(cx, cy, cx + r * 0.42f, cy - r * 0.27f, cx + r * 0.38f, cy + r * 0.25f), hw)
                circle(cx, cy, r * 0.1f, shade(hw, 0.8f))
            }
            8 -> {
                val p = Path().apply {
                    moveTo(50f - r * 0.5f, hy - r * 0.82f)
                    lineTo(50f + r * 0.05f, hy - r * 1.95f)
                    lineTo(50f + r * 0.55f, hy - r * 0.78f)
                    quadTo(50f, hy - r * 0.66f, 50f - r * 0.5f, hy - r * 0.82f)
                    close()
                }
                fs(p, hw)
                c.save(); c.clipPath(p)
                var k = 0f
                while (k < 4f) {
                    ln(50f - r, hy - r * (1.0f + k * 0.3f), 50f + r, hy - r * (0.7f + k * 0.3f), acc, 2.2f); k += 1f
                }
                c.restore()
                stroke(p, oc, ow)
                circle(50f + r * 0.05f, hy - r * 1.95f, r * 0.12f, acc)
            }
            9 -> for (s in sides) {
                fs(Path().apply {
                    moveTo(50f + s * r * 0.35f, hy - r * 0.88f)
                    quadTo(50f + s * r * 0.7f, hy - r * 1.35f, 50f + s * r * 0.5f, hy - r * 1.62f)
                    quadTo(50f + s * r * 0.98f, hy - r * 1.3f, 50f + s * r * 0.72f, hy - r * 0.72f)
                    close()
                }, hw)
            }
            10 -> {
                line.color = oc; line.strokeWidth = 3.4f + ow
                val rf = RectF(50f - r * 0.6f, hy - r * 1.55f, 50f + r * 0.6f, hy - r * 1.28f)
                c.drawOval(rf, line)
                line.color = hw; line.strokeWidth = 3.4f
                c.drawOval(rf, line)
                line.color = alpha(WHITE, 0.6f); line.strokeWidth = 1f
                c.drawOval(rf, line)
            }
            11 -> for (s in sides) {
                ln(50f + s * r * 0.3f, hy - r * 0.95f, 50f + s * r * 0.55f, hy - r * 1.6f, oc, 1.6f)
                circle(50f + s * r * 0.55f, hy - r * 1.62f, r * 0.13f, acc)
            }
            12 -> {
                val cx = 50f - r * 0.6f; val cy = hy - r * 0.75f
                for (i in 0 until 5) {
                    val ang = Math.toRadians(i * 72.0 - 90.0)
                    circle(cx + (cos(ang) * r * 0.17f).toFloat(), cy + (sin(ang) * r * 0.17f).toFloat(), r * 0.13f, hw, true, ow * 0.6f)
                }
                circle(cx, cy, r * 0.1f, acc, true, ow * 0.6f)
            }
        }
    }
}
