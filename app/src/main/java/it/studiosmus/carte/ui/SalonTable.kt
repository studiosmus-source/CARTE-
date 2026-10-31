package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Portrait-adaptive overhead salon table, inspired by the supplied eight-sided
 * walnut / green felt / Persian rug reference. Wood and felt use the
 * actual supplied photographic materials, bundled offline as WebP assets.
 *
 * This view is only the background. Game cards, stock and touch targets remain
 * independent children of the FrameLayout.
 */
class SalonTable(context: Context, fireplaceIntensity: Float = 0f) : FrameLayout(context) {
    private val fireAmount = fireplaceIntensity.coerceIn(0f, 1f)
    private val density = resources.displayMetrics.density
    private val woodTexture by lazy { loadTexture("salon_wood") }
    private val feltTexture by lazy { loadTexture("salon_felt") }

    private fun loadTexture(name: String): Bitmap? = synchronized(textureCache) {
        textureCache[name] ?: try {
            context.assets.open("textures/$name.webp").use { input ->
                BitmapFactory.decodeStream(input)?.also { textureCache[name] = it }
            }
        } catch (_: java.io.IOException) {
            null // The CI gate rejects missing files; keep crash-safe fallback.
        }
    }

    companion object {
        private val textureCache = mutableMapOf<String, Bitmap>()
    }
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

            // Real user-supplied walnut texture on the wood ring only.
            woodTexture?.let { wood ->
                canvas.save()
                canvas.clipPath(outerShape)
                canvas.clipOutPath(innerShape)
                paint.style = Paint.Style.FILL
                paint.shader = null
                paint.alpha = 255
                paint.isFilterBitmap = true
                canvas.drawBitmap(wood, null, outer, paint)
                canvas.restore()
            }
            // Gentle overhead sheen adds depth without masking the photograph.
            canvas.save()
            canvas.clipPath(outerShape)
            canvas.clipOutPath(innerShape)
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(0f, outer.top, w, outer.bottom,
                intArrayOf(Color.argb(73, 255, 218, 158),
                    Color.TRANSPARENT, Color.argb(100, 25, 12, 5)),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(outer, paint)
            paint.shader = null
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
            // Real green woven felt supplied by the user.
            feltTexture?.let { textile ->
                paint.shader = null
                paint.style = Paint.Style.FILL
                paint.alpha = 255
                paint.isFilterBitmap = true
                canvas.drawBitmap(textile, null, inner, paint)
            }
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(w * .42f, h * .17f,
                maxOf(w, h) * .9f,
                intArrayOf(Color.argb(93, 247, 220, 160),
                    Color.argb(28, 208, 174, 103), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            canvas.drawRect(inner, paint)
            paint.shader = null
            paint.shader = RadialGradient(w * .5f, h * .47f,
                maxOf(w, h) * .77f,
                intArrayOf(Color.TRANSPARENT, Color.argb(66, 3, 24, 22)),
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

    /**
     * The off-screen fireplace lives to the lower left of the table. Its
     * orange spill is intentionally almost imperceptible, with low-frequency
     * irregular variation rather than fast blinking. This is a separate small
     * transparent layer: the expensive wood/felt texture is never re-painted
     * on every animation tick. Child card hit targets remain above the light.
     */
    private val fireplaceLight = object : View(context) {
        private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (fireAmount <= 0f || width <= 0 || height <= 0) return
            val w = width.toFloat()
            val h = height.toFloat()
            val seconds = SystemClock.uptimeMillis() / 1000f
            // Slow, subtle variations that are visible even at low volume.
            val phase = (.88f + .085f * sin(seconds * 2.1f) +
                .045f * sin(seconds * 5.4f) +
                .020f * sin(seconds * 1.1f)).coerceIn(.73f, 1f)
            val strength = (.52f + fireAmount * .48f) * phase
            val originX = -w * .035f
            val originY = h * .58f
            val radius = max(w * .83f, h * .53f)
            val outer = RectF(dp(2f), dp(12f), w - dp(2f), h - dp(12f))
            canvas.save()
            canvas.clipPath(octagon(outer, outer.width() * .235f, outer.height() * .175f))
            lightPaint.shader = RadialGradient(originX, originY, radius,
                intArrayOf(
                    Color.argb((150f * strength).toInt(), 255, 160, 83),
                    Color.argb((68f * strength).toInt(), 244, 138, 63),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, .40f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, lightPaint)
            lightPaint.shader = null
            canvas.restore()
            if (isAttachedToWindow && visibility == VISIBLE) postInvalidateDelayed(140L)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (fireAmount > 0f) invalidate()
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(fireplaceLight, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}
