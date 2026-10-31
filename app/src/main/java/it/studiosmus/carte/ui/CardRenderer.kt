package it.studiosmus.carte.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import it.studiosmus.carte.core.Card

/** Card rendering and bitmap caching, independent of activity and game state. */
class CardRenderer(private val context: Context, private val gold: Int, private val ivory: Int) {
    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    // Share decoded artwork between repeated cards and screens. Decode at display resolution
    // instead of allocating 40 full-size (1920x3173) bitmaps on the UI thread.
    private val cardBitmapCache = mutableMapOf<String, android.graphics.Bitmap>()
    private fun cardArtwork(card: Card): android.graphics.Bitmap? {
        cardBitmapCache[card.id]?.let { return it }
        return try {
            val path = "cards/${card.id}.jpg"
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 360 && bounds.outHeight / (sample * 2) >= 500) sample *= 2
            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(path).use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream, null, options)?.also {
                    cardBitmapCache[card.id] = it
                }
            }
        } catch (_: java.io.IOException) { null }
    }

    // HD original artwork is preferred; the painted fallback is used only if an asset is missing.
    fun cardTile(card: Card, enabled: Boolean = false, action: (() -> Unit)? = null): View =
        object : View(context) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            private val ink = if (card.suit.name in listOf("DENARI", "COPPE"))
                Color.rgb(156, 52, 38) else Color.rgb(35, 75, 65)
            // Vector suit emblems avoid font-dependent emoji and stay crisp at any density.
            private fun emblem(canvas: android.graphics.Canvas, cx: Float, cy: Float, scale: Float) {
                canvas.save()
                canvas.translate(cx, cy)
                canvas.scale(scale, scale)
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = ink
                when (card.suit.name) {
                    "DENARI" -> {
                        paint.color = Color.rgb(202, 145, 46)
                        canvas.drawCircle(0f, 0f, 11f, paint)
                        paint.color = Color.rgb(130, 47, 36)
                        canvas.drawCircle(0f, 0f, 7.5f, paint)
                        paint.color = Color.rgb(237, 190, 80)
                        canvas.drawCircle(0f, 0f, 4.5f, paint)
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = 1.2f
                        canvas.drawCircle(0f, 0f, 9.5f, paint)
                    }
                    "COPPE" -> {
                        val bowl = android.graphics.Path().apply {
                            moveTo(-11f, -7f); lineTo(11f, -7f)
                            cubicTo(10f, 6f, 5f, 8f, 2f, 9f)
                            lineTo(2f, 14f); lineTo(8f, 14f)
                            lineTo(8f, 17f); lineTo(-8f, 17f)
                            lineTo(-8f, 14f); lineTo(-2f, 14f)
                            lineTo(-2f, 9f)
                            cubicTo(-5f, 8f, -10f, 6f, -11f, -7f)
                            close()
                        }
                        canvas.drawPath(bowl, paint)
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = 2f
                        canvas.drawArc(-15f, -8f, -6f, 6f, 90f, 180f, false, paint)
                        canvas.drawArc(6f, -8f, 15f, 6f, -90f, 180f, false, paint)
                    }
                    "SPADE" -> {
                        val blade = android.graphics.Path().apply {
                            moveTo(0f, -18f); lineTo(5f, 5f); lineTo(0f, 10f)
                            lineTo(-5f, 5f); close()
                        }
                        canvas.drawPath(blade, paint)
                        canvas.drawRect(-9f, 9f, 9f, 12f, paint)
                        canvas.drawRect(-2f, 12f, 2f, 19f, paint)
                        canvas.drawCircle(0f, 19f, 2.5f, paint)
                    }
                    else -> {
                        paint.strokeWidth = 5f
                        paint.strokeCap = android.graphics.Paint.Cap.ROUND
                        canvas.drawLine(-3f, 16f, 3f, -15f, paint)
                        paint.style = android.graphics.Paint.Style.FILL
                        canvas.drawOval(-11f, -17f, -1f, -3f, paint)
                        canvas.drawOval(1f, -12f, 11f, 2f, paint)
                    }
                }
                canvas.restore()
            }
            private fun text(canvas: android.graphics.Canvas, value: String, x: Float, y: Float,
                             size: Float, color: Int, bold: Boolean = false) {
                paint.color = color
                paint.style = android.graphics.Paint.Style.FILL
                paint.textAlign = android.graphics.Paint.Align.CENTER
                paint.typeface = if (bold) Typeface.create(Typeface.SERIF, Typeface.BOLD) else Typeface.SERIF
                paint.textSize = size
                canvas.drawText(value, x, y, paint)
            }
            private val artwork: android.graphics.Bitmap? by lazy { cardArtwork(card) }
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                val bitmap = artwork
                if (bitmap != null) {
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.isFilterBitmap = true
                    canvas.drawBitmap(bitmap, null,
                        android.graphics.RectF(0f, 0f, w, h), paint)
                    return
                }
                val unit = w / 92f
                paint.style = android.graphics.Paint.Style.FILL
                paint.color = Color.rgb(251, 240, 212)
                canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, 9f * unit, 9f * unit, paint)
                paint.style = android.graphics.Paint.Style.STROKE
                paint.strokeWidth = 2f * unit
                paint.color = Color.rgb(189, 145, 76)
                canvas.drawRoundRect(5f * unit, 5f * unit, w - 5f * unit, h - 5f * unit,
                    6f * unit, 6f * unit, paint)
                paint.strokeWidth = 0.7f * unit
                canvas.drawRoundRect(9f * unit, 9f * unit, w - 9f * unit, h - 9f * unit,
                    3f * unit, 3f * unit, paint)
                paint.style = android.graphics.Paint.Style.FILL
                val rank = card.rank.number
                val title = when (rank) { 8 -> "F"; 9 -> "C"; 10 -> "R"; else -> rank.toString() }
                text(canvas, title, 18f * unit, 25f * unit, 17f * unit, ink, true)
                text(canvas, title, w - 18f * unit, h - 13f * unit, 17f * unit, ink, true)
                if (rank <= 7) {
                    val positions = when (rank) {
                        1 -> listOf(0.5f to 0.5f)
                        2 -> listOf(0.5f to 0.32f, 0.5f to 0.68f)
                        3 -> listOf(0.5f to 0.27f, 0.5f to 0.5f, 0.5f to 0.73f)
                        else -> (0 until rank).map { i ->
                            (if (i % 2 == 0) 0.32f else 0.68f) to
                                (0.25f + (i / 2) * 0.16f).coerceAtMost(0.78f)
                        }
                    }
                    positions.forEach { (px, py) ->
                        emblem(canvas, w * px, h * py, 0.75f * unit)
                    }
                } else {
                    emblem(canvas, w / 2f, h * 0.44f, 1.7f * unit)
                    val figure = when (rank) { 8 -> "FANTE"; 9 -> "CAVALLO"; else -> "RE" }
                    text(canvas, figure, w / 2f, h * 0.64f, 12f * unit, ink, true)
                }
                text(canvas, card.suit.name, w / 2f, h - 27f * unit, 9f * unit, ink, true)
            }
        }.apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(247, 237, 215))
                cornerRadius = dp(5).toFloat()
            }
            clipToOutline = true
            elevation = dp(5).toFloat()
            contentDescription = "${card.rank.number} ${card.suit.name}"
            isEnabled = enabled || action == null
            if (action != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
        }

    fun cardRow(cards: List<Card>, action: ((Card) -> Unit)? = null, dealFrom: Boolean? = null, collectTo: Boolean? = null): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        cards.forEachIndexed { index, card ->
            val tile = if (action == null) cardTile(card) else cardTile(card, true) { action(card) }
            // Match the original 1920×3173 scans without stretching the artwork.
            val params = LinearLayout.LayoutParams(dp(78), dp(129))
            params.setMargins(dp(3), 0, dp(3), 0)
            row.addView(tile, params)
            if (dealFrom != null) CardMotion.deal(tile, dealFrom, ::dp, index * 85L)
            if (collectTo != null) CardMotion.collect(tile, collectTo, ::dp, index * 45L)
        }
        return HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            // A short hand fits on a phone without forcing the player to scroll.
            isFillViewport = true
            addView(row)
        }
    }

    fun cardBackRow(count: Int, animateDeal: Boolean = false): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        repeat(count) { index ->
            val back = object : View(context) {
                private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                override fun onDraw(canvas: android.graphics.Canvas) {
                    val w = width.toFloat()
                    val h = height.toFloat()
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = Color.rgb(95, 31, 37)
                    canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, dp(7).toFloat(), dp(7).toFloat(), paint)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = dp(2).toFloat()
                    paint.color = gold
                    canvas.drawRoundRect(dp(5).toFloat(), dp(5).toFloat(),
                        w - dp(5), h - dp(5), dp(4).toFloat(), dp(4).toFloat(), paint)
                    paint.strokeWidth = dp(1).toFloat()
                    val step = dp(12).toFloat()
                    var y = dp(15).toFloat()
                    while (y < h - dp(10)) {
                        var x = dp(14).toFloat()
                        while (x < w - dp(10)) {
                            canvas.drawCircle(x, y, dp(2).toFloat(), paint)
                            x += step
                        }
                        y += step
                    }
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = ivory
                    paint.textAlign = android.graphics.Paint.Align.CENTER
                    paint.typeface = Typeface.SERIF
                    paint.textSize = dp(20).toFloat()
                    canvas.drawText("✦", w / 2f, h / 2f + dp(7), paint)
                }
            }.apply {
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.rgb(95, 31, 37))
                    cornerRadius = dp(6).toFloat()
                }
                clipToOutline = true
                elevation = dp(4).toFloat()
                contentDescription = "Carta coperta dell'avversario"
            }
            val params = LinearLayout.LayoutParams(dp(72), dp(98))
            params.setMargins(dp(4), 0, dp(4), 0)
            row.addView(back, params)
            if (animateDeal) CardMotion.deal(back, true, ::dp, index * 85L)
        }
        return row
    }


}
