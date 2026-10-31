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
            // The user-supplied photograph already contains the portrait playing-field
            // crop. Fit it without extra zoom so the side wood remains visible.
            // No bitmap stretching, no duplicated material shaders.
            val zoom = 1.0f
            val scale = max(widthPx / bitmap.width, heightPx / bitmap.height) * zoom
            val sourceWidth = widthPx / scale
            val sourceHeight = heightPx / scale
            val midX = bitmap.width * .50f
            val midY = bitmap.height * .50f
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
            // Fireplace reflected from outside the left side. Two moving pools
            // oscillate at different rates so the glow feels alive, but the
            // table never flashes as one uniform sheet of orange.
            val slow = sin(time * 2.17f)
            val fast = sin(time * 5.73f + .63f)
            val drift = sin(time * .91f)
            val flame = (.70f + .23f * slow + .10f * fast)
                .coerceIn(.36f, 1f) * fireAmount
            val centerY = h * (.62f + .037f * drift)
            val centerX = w * (.025f + .028f * sin(time * 1.37f))
            val radius = max(w * .78f, h * .52f)
            paint.shader = RadialGradient(centerX, centerY, radius,
                intArrayOf(
                    Color.argb((184f * flame).toInt(), 255, 169, 83),
                    Color.argb((81f * flame).toInt(), 244, 126, 48),
                    Color.TRANSPARENT
                ), floatArrayOf(0f, .39f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)

            // Warmer intermittent gleam on the left wooden rail.
            val ember = (.68f + .24f * sin(time * 3.32f + 1.1f)
                + .08f * sin(time * 7.12f)).coerceIn(.38f, 1f) * fireAmount
            paint.shader = RadialGradient(-w * .12f,
                h * (.43f + .028f * sin(time * 1.04f)),
                max(w * .66f, h * .32f),
                intArrayOf(Color.argb((101f * ember).toInt(), 255, 191, 107),
                    Color.argb((26f * ember).toInt(), 240, 130, 51),
                    Color.TRANSPARENT),
                floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            if (isAttachedToWindow && visibility == VISIBLE) {
                postInvalidateDelayed(110L)
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
