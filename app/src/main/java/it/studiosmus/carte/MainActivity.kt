package it.studiosmus.carte

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import it.studiosmus.carte.audio.Soundscape
import it.studiosmus.carte.audio.SalonAudio
import it.studiosmus.carte.core.ScopaGame
import it.studiosmus.carte.core.BriscolaGame
import it.studiosmus.carte.core.Card
import android.widget.SeekBar
import android.widget.HorizontalScrollView
import it.studiosmus.carte.audio.AmbienceSettings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Offline Scopa and Briscola UI connected to pure Kotlin game engines.
 * Uses Android platform widgets without additional UI dependencies.
 */
class MainActivity : Activity() {
    private var game: BriscolaGame? = null
    private var scopa: ScopaGame? = null
    private val soundscape = Soundscape()
    private val salonAudio by lazy { SalonAudio(this) }
    private var fireplaceEnabled = false
    private var ambience = AmbienceSettings()
    private val ivory = Color.rgb(247, 233, 203)
    private val gold = Color.rgb(218, 174, 94)
    private val forest = Color.rgb(13, 54, 45)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 35, 30)
        window.navigationBarColor = Color.rgb(8, 35, 30)
        val prefs = getSharedPreferences("ambience", MODE_PRIVATE)
        fireplaceEnabled = prefs.getBoolean("enabled", false)
        ambience = AmbienceSettings(
            fireplace = prefs.getFloat("fireplace", 0.22f),
            jazz = prefs.getFloat("jazz", 0.20f),
            conversations = prefs.getFloat("conversations", 0.12f),
            glassware = prefs.getFloat("glassware", 0.24f),
            doors = prefs.getFloat("doors", 0.16f),
            cards = prefs.getFloat("cards", 0.55f)
        ).normalized()
        applyAmbience()
        showHome()
    }

    override fun onPause() {
        soundscape.stop()
        salonAudio.stop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (fireplaceEnabled) {
            soundscape.start()
            salonAudio.start()
        }
    }

    override fun onDestroy() {
        soundscape.release()
        salonAudio.stop()
        super.onDestroy()
    }

    private fun applyAmbience() {
        soundscape.setVolume(Soundscape.Channel.FIREPLACE, ambience.fireplace)
        salonAudio.configure(ambience)
    }

    private fun saveAmbience() {
        getSharedPreferences("ambience", MODE_PRIVATE).edit()
            .putBoolean("enabled", fireplaceEnabled)
            .putFloat("fireplace", ambience.fireplace)
            .putFloat("jazz", ambience.jazz)
            .putFloat("conversations", ambience.conversations)
            .putFloat("glassware", ambience.glassware)
            .putFloat("doors", ambience.doors)
            .putFloat("cards", ambience.cards)
            .apply()
        applyAmbience()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun panel(color: Int, radius: Int = 22, stroke: Int = gold): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(Typeface.SERIF, Typeface.BOLD)
            else typeface = Typeface.SERIF
        }

    // Self-contained painted cards: no external assets and no gameplay engine changes.
    private fun cardTile(card: Card, enabled: Boolean = false, action: (() -> Unit)? = null): View =
        object : View(this) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            private val ink = if (card.suit.name in listOf("DENARI", "COPPE"))
                Color.rgb(156, 52, 38) else Color.rgb(35, 75, 65)
            private val symbols = when (card.suit.name) {
                "DENARI" -> "●"
                "COPPE" -> "♜"
                "SPADE" -> "⚔"
                else -> "♣"
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
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
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
                        text(canvas, symbols, w * px, h * py + 9f * unit, 25f * unit, ink, true)
                    }
                } else {
                    text(canvas, symbols, w / 2f, h * 0.47f, 36f * unit, ink, true)
                    val figure = when (rank) { 8 -> "FANTE"; 9 -> "CAVALLO"; else -> "RE" }
                    text(canvas, figure, w / 2f, h * 0.64f, 12f * unit, ink, true)
                }
                text(canvas, card.suit.name, w / 2f, h - 27f * unit, 9f * unit, ink, true)
            }
        }.apply {
            contentDescription = "${card.rank.number} ${card.suit.name}"
            isEnabled = enabled || action == null
            if (action != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { action() }
            }
        }

    private fun cardRow(cards: List<Card>, action: ((Card) -> Unit)? = null): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
        cards.forEach { card ->
            val tile = if (action == null) cardTile(card) else cardTile(card, true) { action(card) }
            val params = LinearLayout.LayoutParams(dp(92), dp(126))
            params.setMargins(dp(4), 0, dp(4), 0)
            row.addView(tile, params)
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(36), dp(22), dp(22))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 30, 26), forest, Color.rgb(5, 25, 22))
            )
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)

        root.addView(label("♠  ♥  ♦  ♣", 26f, gold))
        root.addView(label("CARTE", 52f, ivory, true))
        root.addView(label("IL PIACERE DI GIOCARE", 12f, gold, true))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(42)))

        val hero = label("NAPOLETANE\n40 CARTE · INFINITE SFIDE", 24f, ivory, true).apply {
            background = panel(Color.rgb(19, 72, 57), 24)
            setPadding(dp(20), dp(44), dp(20), dp(44))
        }
        root.addView(hero, LinearLayout.LayoutParams(-1, dp(180)))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(28)))

        gameButton(root, "BRISCOLA", "Strategia, memoria e colpi di scena")
        gameButton(root, "SCOPA", "Ogni presa conta")
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(16)))
        val fireplace = label(if (fireplaceEnabled) "CAMINO ACCESO - TOCCA PER SPEGNERE" else "CAMINO: ACCENDI", 16f, ivory, true).apply {
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = panel(Color.rgb(20, 65, 54))
            setOnClickListener {
                fireplaceEnabled = !fireplaceEnabled
                saveAmbience()
                if (fireplaceEnabled) {
                    soundscape.start()
                    salonAudio.start()
                } else {
                    soundscape.stop()
                    salonAudio.stop()
                }
                text = if (fireplaceEnabled) "CAMINO ACCESO - TOCCA PER SPEGNERE"
                       else "CAMINO: ACCENDI"
            }
        }
        root.addView(fireplace, LinearLayout.LayoutParams(-1, dp(64)))
        root.addView(label("⚙ IMPOSTAZIONI AUDIO", 18f, ivory, true).apply {
            setPadding(dp(12), dp(18), dp(12), dp(18))
            background = panel(Color.rgb(20, 65, 54))
            setOnClickListener { showAudioSettings() }
        }, LinearLayout.LayoutParams(-1, dp(66)))
        root.addView(label("SOLO OFFLINE  •  NESSUN ACCOUNT", 11f, gold))
        root.addView(label("Scopa e Briscola · Gioca offline", 12f, ivory))
        setContentView(scroll)
    }

    private fun showAudioSettings() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            background = panel(forest)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        root.addView(label("‹ TORNA AL SALOTTO", 17f, gold, true).apply {
            setPadding(dp(10), dp(14), dp(10), dp(14))
            setOnClickListener { showHome() }
        })
        root.addView(label("ATMOSFERA", 34f, ivory, true))
        root.addView(label("Regola ogni suono in modo indipendente", 15f, gold))
        fun slider(title: String, initial: Float, change: (Float) -> Unit) {
            val value = label("$title · ${(initial * 100).toInt()}%", 17f, ivory, true)
            value.setPadding(0, dp(18), 0, dp(6))
            root.addView(value)
            root.addView(SeekBar(this).apply {
                max = 100
                progress = (initial * 100).toInt()
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        value.text = "$title · $progress%"
                        change(progress / 100f)
                        saveAmbience()
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                })
            })
        }
        root.addView(Button(this).apply { text = if (fireplaceEnabled) "SPEGNI CAMINO" else "PROVA IL CAMINO"; setOnClickListener { fireplaceEnabled = !fireplaceEnabled; saveAmbience(); if (fireplaceEnabled) { soundscape.start(); salonAudio.start() } else { soundscape.stop(); salonAudio.stop() }; text = if (fireplaceEnabled) "SPEGNI CAMINO" else "PROVA IL CAMINO" } })
        slider("Camino", ambience.fireplace) { ambience = ambience.copy(fireplace = it) }
        slider("Jazz", ambience.jazz) { ambience = ambience.copy(jazz = it) }
        slider("Conversazioni", ambience.conversations) { ambience = ambience.copy(conversations = it) }
        slider("Bicchieri", ambience.glassware) { ambience = ambience.copy(glassware = it) }
        slider("Porte", ambience.doors) { ambience = ambience.copy(doors = it) }
        slider("Carte", ambience.cards) { ambience = ambience.copy(cards = it) }
        val recordings = listOf(
            "Jazz" to "salon_jazz",
            "Conversazioni" to "salon_conversation",
            "Bicchieri" to "salon_glassware",
            "Porte" to "salon_door"
        )
        val available = recordings.filter { (_, resource) ->
            resources.getIdentifier(resource, "raw", packageName) != 0
        }
        val audioStatus = if (available.size == recordings.size) {
            "Registrazioni ambientali disponibili: 4/4"
        } else {
            "Registrazioni disponibili: ${available.size}/4\n" +
                recordings.filterNot { it in available }
                    .joinToString("\n") { (title, _) -> "• $title: audio non ancora incluso" }
        }
        root.addView(label("DIAGNOSTICA AUDIO", 18f, gold, true))
        root.addView(label(audioStatus, 14f, ivory).apply {
            setPadding(dp(10), dp(12), dp(10), dp(18))
        })
        root.addView(label("Camino: generato offline. Effetti delle carte: non ancora inclusi. I cursori degli audio mancanti salvano le preferenze, ma non producono suono.", 13f, gold))
        setContentView(scroll)
    }

    private fun gameButton(root: LinearLayout, title: String, subtitle: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(12), dp(22), dp(12))
            background = panel(Color.rgb(20, 65, 54))
            isClickable = true
            isFocusable = true
            setOnClickListener { if (title == "BRISCOLA") startBriscola() else startScopa() }
        }
        box.addView(label(title, 25f, ivory, true))
        box.addView(label(subtitle, 13f, gold))
        val params = LinearLayout.LayoutParams(-1, dp(100))
        params.bottomMargin = dp(14)
        root.addView(box, params)
    }

    private fun showComingSoon(title: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Il tavolo di gioco è in costruzione. Le regole sono in fase di integrazione.")
            .setPositiveButton("Torna al menu", null)
            .show()
    }
    private fun startScopa() {
        scopa = ScopaGame()
        advanceScopaComputer()
        renderScopa()
    }

    private fun advanceScopaComputer() {
        val match = scopa ?: return
        while (!match.finished && match.turn == 1) {
            val (card, capture) = match.aiMove()
            if (!match.play(1, card, capture)) break
        }
    }

    private fun playScopaCard(card: Card) {
        val match = scopa ?: return
        val choices = match.options(card)
        if (choices.size <= 1) {
            if (match.play(0, card, choices.firstOrNull())) {
                advanceScopaComputer()
                renderScopa()
            }
            return
        }
        val labels = choices.map { capture ->
            capture.joinToString(" + ") { cardName(it) }
        }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Scegli la presa")
            .setItems(labels) { _, index ->
                if (match.play(0, card, choices[index])) {
                    advanceScopaComputer()
                    renderScopa()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun renderScopa() {
        val match = scopa ?: return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(28), dp(16), dp(24))
            background = panel(forest)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        root.addView(label("‹ MENU", 15f, gold, true).apply {
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnClickListener { scopa = null; showHome() }
        })
        root.addView(label("SCOPA", 34f, ivory, true))
        root.addView(label("Scope: TU ${match.scope[0]} — ${match.scope[1]} COMPUTER", 17f, gold, true))
        root.addView(label("Carte raccolte: ${match.captured[0].size} — ${match.captured[1].size}", 14f, ivory))
        root.addView(label("AVVERSARIO · ${match.hands[1].size} carte", 14f, gold))
        root.addView(label("TAVOLO", 19f, ivory, true))
        val tableText = if (match.table.isEmpty()) "Tavolo vuoto"
            else match.table.joinToString("\n") { cardName(it) }
        if (match.table.isEmpty()) root.addView(label(tableText, 18f, ivory))
        else root.addView(cardRow(match.table))
        root.addView(label(match.lastMove, 15f, gold))
        if (match.finished) {
            val score = match.score()
            val outcome = when {
                score[0] > score[1] -> "HAI VINTO!"
                score[0] < score[1] -> "HA VINTO IL COMPUTER"
                else -> "PAREGGIO"
            }
            root.addView(label(outcome, 26f, gold, true))
            root.addView(label("Punti finali: ${score[0]} — ${score[1]}", 20f, ivory))
            root.addView(Button(this).apply {
                text = "NUOVA PARTITA"
                setOnClickListener { startScopa() }
            })
        } else {
            root.addView(label("LE TUE CARTE · TOCCA PER GIOCARE", 15f, gold, true))
            root.addView(cardRow(match.hands[0]) { card -> if (match.turn == 0) playScopaCard(card) })
        }
        setContentView(scroll)
    }

    private fun startBriscola() {
        game = BriscolaGame()
        advanceComputer()
        renderBriscola()
    }

    private fun advanceComputer() {
        val match = game ?: return
        while (!match.finished && match.turn == 1) {
            match.play(1, match.aiCard())
        }
    }

    private fun cardName(card: Card): String =
        "${card.rank.number} ${card.suit.name.lowercase().replaceFirstChar { it.uppercase() }}"

    private fun renderBriscola() {
        val match = game ?: return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(28), dp(16), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 30, 26), forest, Color.rgb(5, 25, 22))
            )
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        val back = label("‹ MENU", 15f, gold, true).apply {
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnClickListener { game = null; showHome() }
        }
        root.addView(back)
        root.addView(label("BRISCOLA", 34f, ivory, true))
        root.addView(label("TU ${match.points[0]}  —  ${match.points[1]} COMPUTER", 20f, gold, true))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(22)))
        root.addView(label("AVVERSARIO", 13f, gold))
        root.addView(label("▣  ".repeat(match.hands[1].size), 34f, gold))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(20)))
        root.addView(label("BRISCOLA: ${match.trump.name}", 17f, ivory, true))
        root.addView(label("Mazzo: ${match.remainingCards} carte", 14f, gold))
        val lead = match.lead
        val tableText = if (lead == null) "TAVOLO LIBERO" else
            "${if (lead.first == 0) "Hai giocato" else "Il computer ha giocato"}\n${cardName(lead.second)}"
        val table = label(tableText, 21f, ivory, true).apply {
            background = panel(Color.rgb(23, 83, 64), 22)
            setPadding(dp(12), dp(24), dp(12), dp(24))
        }
        root.addView(table, LinearLayout.LayoutParams(-1, dp(85)))
        if (lead != null) root.addView(cardRow(listOf(lead.second)))
        root.addView(label(match.lastTrick, 15f, gold))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(16)))
        if (match.finished) {
            root.addView(label(match.result(), 27f, gold, true))
            root.addView(label("Punteggio finale: ${match.points[0]} - ${match.points[1]}", 18f, ivory))
            val again = Button(this).apply { text = "NUOVA PARTITA"; setOnClickListener { startBriscola() } }
            root.addView(again)
        } else {
            root.addView(label("LE TUE CARTE · TOCCA PER GIOCARE", 14f, gold, true))
            root.addView(cardRow(match.hands[0]) { card ->
                if (match.turn == 0 && match.play(0, card)) {
                    advanceComputer()
                    renderBriscola()
                }
            })
        }
        setContentView(scroll)
    }

}
