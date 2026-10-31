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
import kotlin.math.sin

/**
 * A quiet, material table: dark walnut rail, deep woven green felt and warm
 * overhead light. All details are drawn once per view, without bitmap assets,
 * frame loops or expensive runtime blur.
 */
class SalonTable(context: Context) : FrameLayout(context) {
    private val density = resources.displayMetrics.density
    private val rim = 14f * density
    private val surface = object : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) return
            val outer = RectF(0f, 0f, w, h)
            val felt = RectF(rim, rim, w - rim, h - rim)
            val radius = 28f * density
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(0f, 0f, w, h,
                intArrayOf(Color.rgb(106, 65, 36), Color.rgb(46, 27, 18),
                    Color.rgb(85, 49, 29), Color.rgb(30, 19, 15)),
                null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(outer, radius, radius, paint)
            paint.shader = null
            paint.color = Color.argb(115, 13, 9, 7)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * density
            canvas.drawRoundRect(RectF(rim * .45f, rim * .45f,
                w - rim * .45f, h - rim * .45f), radius * .8f, radius * .8f, paint)
            paint.style = Paint.Style.FILL

            // Subtle wood grain, deterministic and deliberately non-repeating.
            canvas.save()
            val railClip = Path().apply { addRoundRect(outer, radius, radius, Path.Direction.CW) }
            canvas.clipPath(railClip)
            for (i in 0 until 26) {
                val y = h * i / 26f
                paint.color = Color.argb(if (i % 3 == 0) 21 else 11, 198, 139, 77)
                paint.strokeWidth = density * .65f
                paint.style = Paint.Style.STROKE
                val path = Path().apply {
                    moveTo(0f, y)
                    cubicTo(w * .3f, y + sin(i * 2.1).toFloat() * density * 5,
                        w * .7f, y - density * 4, w, y + density * 2)
                }
                canvas.drawPath(path, paint)
            }
            canvas.restore()
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(13, 45, 35)
            canvas.drawRoundRect(felt, radius * .65f, radius * .65f, paint)
            paint.shader = LinearGradient(0f, rim, w, h - rim,
                intArrayOf(Color.rgb(30, 83, 65), Color.rgb(23, 67, 54),
                    Color.rgb(16, 49, 42)), null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(felt, radius * .65f, radius * .65f, paint)
            paint.shader = null

            canvas.save()
            val feltClip = Path().apply {
                addRoundRect(felt, radius * .65f, radius * .65f, Path.Direction.CW)
            }
            canvas.clipPath(feltClip)
            // Woven micro-fibres, subtle enough not to compete with the cards.
            val step = 6f * density
            var y = rim
            var row = 0
            while (y < h - rim) {
                var x = rim + (row % 2) * step * .5f
                while (x < w - rim) {
                    paint.color = Color.argb(19, 150, 177, 135)
                    paint.strokeWidth = .55f * density
                    canvas.drawLine(x, y, x + 1.7f * density, y + .8f * density, paint)
                    x += step
                }
                y += step
                row++
            }
            paint.shader = RadialGradient(w * .39f, h * .19f,
                maxOf(w, h) * .83f,
                intArrayOf(Color.argb(53, 248, 189, 111),
                    Color.argb(10, 181, 145, 88), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(felt, paint)
            paint.shader = null
            paint.shader = RadialGradient(w * .5f, h * .43f,
                maxOf(w, h) * .74f,
                intArrayOf(Color.TRANSPARENT, Color.argb(105, 4, 15, 13)),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(felt, paint)
            paint.shader = null
            canvas.restore()
        }
    }

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}
