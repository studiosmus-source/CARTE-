package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Portrait-adaptive overhead salon table, inspired by the supplied eight-sided
 * walnut / green felt / Persian rug reference. Procedural paint avoids large
 * bitmaps, network access, frame loops and extra asset dependencies.
 *
 * This view is only the background. Game cards, stock and touch targets remain
 * independent children of the FrameLayout.
 */
class SalonTable(context: Context) : FrameLayout(context) {
    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private fun octagon(rect: RectF, cutX: Float, cutY: Float): Path {
        val x = min(cutX, rect.width() * .46f)
        val y = min(cutY, rect.height() * .46f)
        return Path().apply {
            moveTo(rect.left + x, rect.top)
            lineTo(rect.right - x, rect.top)
            lineTo(rect.right, rect.top + y)
            lineTo(rect.right, rect.bottom - y)
            lineTo(rect.right - x, rect.bottom)
            lineTo(rect.left + x, rect.bottom)
            lineTo(rect.left, rect.bottom - y)
            lineTo(rect.left, rect.top + y)
            close()
        }
    }

    private fun corners(rect: RectF, cutX: Float, cutY: Float): List<Pair<Float, Float>> {
        val x = min(cutX, rect.width() * .46f)
        val y = min(cutY, rect.height() * .46f)
        return listOf(
            rect.left + x to rect.top, rect.right - x to rect.top,
            rect.right to rect.top + y, rect.right to rect.bottom - y,
            rect.right - x to rect.bottom, rect.left + x to rect.bottom,
            rect.left to rect.bottom - y, rect.left to rect.top + y
        )
    }

    private val surface = object : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun fill(canvas: Canvas, color: Int) {
            paint.reset()
            paint.isAntiAlias = true
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= dp(90f) || h <= dp(90f)) return

            // A dim wooden floor remains visible in the exposed rug corners.
            fill(canvas, Color.rgb(24, 17, 14))
            for (i in 0..10) {
                val y = i * h / 10f
                paint.shader = LinearGradient(0f, y, w, y + h / 10f,
                    intArrayOf(Color.rgb(48, 30, 22), Color.rgb(29, 20, 17),
                        Color.rgb(41, 27, 21)), null, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, y, w, y + h / 10f - dp(1f), paint)
                paint.shader = null
                paint.color = Color.argb(75, 8, 5, 3)
                canvas.drawRect(0f, y, w, y + dp(.7f), paint)
            }

            // Old patterned rug below the table; its patterned corners are
            // visible through the octagonal cutouts rather than under cards.
            val rug = RectF(dp(8f), dp(3f), w - dp(8f), h - dp(3f))
            paint.shader = LinearGradient(0f, 0f, w, h,
                Color.rgb(98, 51, 44), Color.rgb(42, 30, 39), Shader.TileMode.CLAMP)
            canvas.drawRect(rug, paint)
            paint.shader = null
            for (inset in listOf(5f, 11f, 16f, 22f)) {
                val frame = RectF(rug.left + dp(inset), rug.top + dp(inset),
                    rug.right - dp(inset), rug.bottom - dp(inset))
                paint.color = if (inset == 5f || inset == 16f)
                    Color.rgb(149, 116, 79) else Color.rgb(34, 34, 39)
                paint.strokeWidth = if (inset == 11f) dp(3f) else dp(1.3f)
                paint.style = Paint.Style.STROKE
                canvas.drawRect(frame, paint)
            }
            paint.style = Paint.Style.FILL
            val rugRandom = Random(1601)
            repeat(260) {
                val x = rug.left + rugRandom.nextFloat() * rug.width()
                val y = rug.top + rugRandom.nextFloat() * rug.height()
                val radius = dp(1f + rugRandom.nextFloat() * 2.2f)
                paint.color = if (it % 3 == 0) Color.argb(155, 184, 133, 88)
                    else Color.argb(120, 36, 49, 54)
                canvas.drawCircle(x, y, radius, paint)
            }

            // Corners remain proportionate on a narrow Android display.
            val marginX = dp(2f)
            val marginY = dp(12f)
            val outer = RectF(marginX, marginY, w - marginX, h - marginY)
            val rail = min(dp(34f), min(w * .095f, h * .095f))
            val cutX = outer.width() * .235f
            val cutY = outer.height() * .175f
            val inner = RectF(outer.left + rail, outer.top + rail,
                outer.right - rail, outer.bottom - rail)
            val outerShape = octagon(outer, cutX, cutY)
            val innerShape = octagon(inner, cutX * .79f, cutY * .79f)

            // Low shadow puts the entire real tabletop above the rug.
            canvas.save()
            canvas.translate(0f, dp(5f))
            paint.color = Color.argb(170, 3, 2, 1)
            paint.shader = null
            canvas.drawPath(outerShape, paint)
            canvas.restore()

            // Warm walnut veneer with strong depth and edge bevel.
            paint.shader = LinearGradient(0f, 0f, w, h,
                intArrayOf(Color.rgb(181, 111, 62), Color.rgb(104, 60, 33),
                    Color.rgb(145, 83, 43), Color.rgb(61, 33, 22)),
                null, Shader.TileMode.CLAMP)
            canvas.drawPath(outerShape, paint)
            paint.shader = null

            // Grain runs in the direction of each of the eight rail segments,
            // rather than horizontally across the whole table.
            val vertices = corners(outer, cutX, cutY)
            canvas.save()
            canvas.clipPath(outerShape)
            canvas.clipOutPath(innerShape) // API 26+, the app's minSdk.
            val grain = Random(8523)
            for (edge in 0 until vertices.size) {
                val a = vertices[edge]
                val b = vertices[(edge + 1) % vertices.size]
                val dx = b.first - a.first
                val dy = b.second - a.second
                val length = hypot(dx, dy).coerceAtLeast(1f)
                val nx = -dy / length
                val ny = dx / length
                for (line in 0 until 85) {
                    val offset = rail * 1.65f * (line + .5f) / 85f
                    val wobble = (grain.nextFloat() - .5f) * dp(5f)
                    val x1 = a.first + nx * offset
                    val y1 = a.second + ny * offset
                    val x2 = b.first + nx * offset
                    val y2 = b.second + ny * offset
                    val stroke = Path().apply {
                        moveTo(x1, y1)
                        cubicTo(x1 + dx * .33f + nx * wobble,
                            y1 + dy * .33f + ny * wobble,
                            x1 + dx * .7f - nx * wobble * .7f,
                            y1 + dy * .7f - ny * wobble * .7f,
                            x2, y2)
                    }
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = dp(if (line % 9 == 0) .9f else .32f)
                    paint.color = if (line % 4 == 0)
                        Color.argb(58, 20, 9, 6) else Color.argb(37, 241, 169, 96)
                    canvas.drawPath(stroke, paint)
                }
            }
            canvas.restore()
            paint.style = Paint.Style.STROKE
            paint.shader = null
            paint.strokeWidth = dp(2.4f)
            paint.color = Color.argb(155, 236, 174, 101)
            canvas.drawPath(outerShape, paint)
            paint.strokeWidth = dp(1.5f)
            paint.color = Color.argb(190, 32, 15, 9)
            canvas.drawPath(innerShape, paint)

            // The panno is a genuinely eight-sided insert inside the wood.
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(0f, inner.top, w * .75f, inner.bottom,
                intArrayOf(Color.rgb(43, 113, 93), Color.rgb(24, 77, 65),
                    Color.rgb(13, 51, 46)), null, Shader.TileMode.CLAMP)
            canvas.drawPath(innerShape, paint)
            paint.shader = null
            canvas.save()
            canvas.clipPath(innerShape)
            val feltRandom = Random(2244)
            repeat(2300) {
                val x = inner.left + feltRandom.nextFloat() * inner.width()
                val y = inner.top + feltRandom.nextFloat() * inner.height()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(.33f)
                paint.color = if (it % 3 == 0) Color.argb(30, 190, 199, 164)
                    else Color.argb(27, 5, 29, 23)
                canvas.drawLine(x, y, x + dp(.9f), y + dp(.5f), paint)
            }
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(w * .42f, h * .17f,
                maxOf(w, h) * .9f,
                intArrayOf(Color.argb(69, 221, 194, 124),
                    Color.argb(16, 159, 147, 93), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(inner, paint)
            paint.shader = null
            paint.shader = RadialGradient(w * .5f, h * .47f,
                maxOf(w, h) * .77f,
                intArrayOf(Color.TRANSPARENT, Color.argb(100, 3, 24, 22)),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(inner, paint)
            paint.shader = null
            canvas.restore()

            // Thin brass seam + dark inner rim define physical thickness.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(3.2f)
            paint.color = Color.argb(165, 18, 10, 6)
            canvas.drawPath(innerShape, paint)
            paint.strokeWidth = dp(.95f)
            paint.color = Color.rgb(185, 150, 100)
            canvas.drawPath(innerShape, paint)
            paint.style = Paint.Style.FILL
            paint.shader = null
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}
