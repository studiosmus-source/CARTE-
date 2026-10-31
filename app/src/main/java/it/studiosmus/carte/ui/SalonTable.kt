package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The actual supplied overhead salon-table photograph is the ONLY table
 * material. No synthetic wood, felt, simulated texture, or heavy light filters.
 * A phone viewport shows a close crop of the table, giving the cards plenty
 * of playable felt without shrinking the whole image into a tiny picture.
 *
 * Foreground cards and their click targets remain independently laid out
 * above the background and the subtle fireplace glow.
 */
class SalonTable(context: Context, fireplaceIntensity: Float = 0f) : FrameLayout(context) {
    private val fireAmount = fireplaceIntensity.coerceIn(0f, 1f)

    companion object {
        @Volatile private var tablePhoto: Bitmap? = null

        @Synchronized
        private fun photo(context: Context): Bitmap? {
            tablePhoto?.let { return it }
            return try {
                context.assets.open("textures/salon_table_reference.webp").use {
                    BitmapFactory.decodeStream(it)?.also { bitmap -> tablePhoto = bitmap }
                }
            } catch (_: java.io.IOException) {
                null
            }
        }
    }

    private val photoSurface = object : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val widthPx = width.toFloat()
            val heightPx = height.toFloat()
            if (widthPx <= 0f || heightPx <= 0f) return
            canvas.drawColor(Color.rgb(17, 35, 29))

            val bitmap = photo(context) ?: return
            // Aspect-correct CENTER_CROP plus a small intentional close-up:
            // wood rim may exit the frame, but the green playing surface does not.
            // No bitmap stretching, no duplicated material shaders.
            val zoom = 1.10f
            val scale = max(widthPx / bitmap.width, heightPx / bitmap.height) * zoom
            val sourceWidth = widthPx / scale
            val sourceHeight = heightPx / scale
            val midX = bitmap.width * .50f
            val midY = bitmap.height * .47f
            val x0 = (midX - sourceWidth / 2f)
                .coerceIn(0f, bitmap.width - sourceWidth)
            val y0 = (midY - sourceHeight / 2f)
                .coerceIn(0f, bitmap.height - sourceHeight)
            val source = Rect(x0.toInt(), y0.toInt(),
                (x0 + sourceWidth).toInt().coerceAtMost(bitmap.width),
                (y0 + sourceHeight).toInt().coerceAtMost(bitmap.height))
            canvas.drawBitmap(bitmap, source,
                RectF(0f, 0f, widthPx, heightPx), paint)
        }
    }

    /**
     * Very gentle reflection from a fireplace outside the left edge of the
     * camera. Animate only this small transparent overlay, not the photograph.
     * The fireplace OFF switch completely disables the reflection.
     */
    private val fireReflection = object : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (fireAmount <= 0f || width <= 0 || height <= 0) return

            val w = width.toFloat()
            val h = height.toFloat()
            val time = SystemClock.uptimeMillis() / 1000f
            // Off-screen flames fluctuate in amplitude AND in the location
            // of their reflected patch; avoid periodic blinking of the entire table.
            val slow = sin(time * 1.71f)
            val fast = sin(time * 4.38f)
            val flicker = (.76f + .16f * slow + .08f * fast)
                .coerceIn(.49f, 1f)
            val strength = fireAmount * flicker
            val centerY = h * (.61f + .022f * sin(time * 1.24f))
            val radius = max(w * .82f, h * .50f)
            paint.shader = RadialGradient(w * .035f, centerY, radius,
                intArrayOf(
                    Color.argb((150f * strength).toInt(), 255, 162, 77),
                    Color.argb((58f * strength).toInt(), 246, 118, 45),
                    Color.TRANSPARENT
                ), floatArrayOf(0f, .43f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
            // Local amber kick along the wooden rim catches the flame,
            // while the photographic material remains fully visible.
            paint.shader = RadialGradient(-w * .24f, h * .61f, max(w * .80f, h * .38f),
                intArrayOf(Color.argb((72f * strength).toInt(), 255, 196, 122),
                    Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            if (isAttachedToWindow && visibility == VISIBLE) {
                postInvalidateDelayed(145L)
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (fireAmount > 0f) invalidate()
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
        addView(photoSurface, LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT))
        addView(fireReflection, LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT))
    }
}
