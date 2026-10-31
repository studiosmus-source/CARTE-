package it.studiosmus.carte

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import it.studiosmus.carte.audio.Soundscape
import it.studiosmus.carte.audio.SalonAudio
import it.studiosmus.carte.core.ScopaGame
import it.studiosmus.carte.core.BriscolaGame
import it.studiosmus.carte.core.Card
import android.widget.SeekBar
import android.widget.HorizontalScrollView
import it.studiosmus.carte.audio.AmbienceSettings
import android.widget.FrameLayout
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Offline Scopa and Briscola UI connected to pure Kotlin game engines.
 * Uses Android platform widgets without additional UI dependencies.
 */
class MainActivity : Activity() {
    private val gameHandler = Handler(Looper.getMainLooper())
    private var computerPending = false
    private var trickPreview: Pair<Card, Card>? = null
    private var trickResolving = false
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
        gameHandler.removeCallbacksAndMessages(null)
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

    // Apply system bar safe areas to every screen without accumulating padding
    // when Android redispatches insets after configuration changes.
    private fun installScreen(view: View) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { target, insets ->
            // Legacy getters work down to the app's minSdk 26.
            @Suppress("DEPRECATION")
            target.setPadding(left + insets.systemWindowInsetLeft,
                top + insets.systemWindowInsetTop,
                right + insets.systemWindowInsetRight,
                bottom + insets.systemWindowInsetBottom)
            insets
        }
        setContentView(view)
        view.requestApplyInsets()
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

    private val cardRenderer by lazy { it.studiosmus.carte.ui.CardRenderer(this, gold, ivory) }

    private fun cardTile(card: Card, enabled: Boolean = false, action: (() -> Unit)? = null): View =
        cardRenderer.cardTile(card, enabled, action)

    private fun cardRow(cards: List<Card>, action: ((Card) -> Unit)? = null): View =
        cardRenderer.cardRow(cards, action)

    private fun cardBackRow(count: Int): View = cardRenderer.cardBackRow(count)

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
        installScreen(scroll)
    }

    private fun showAudioSettings(returnToBriscola: Boolean = false) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            background = panel(forest)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        root.addView(label("‹ TORNA AL SALOTTO", 17f, gold, true).apply {
            setPadding(dp(10), dp(14), dp(10), dp(14))
            setOnClickListener {
                if (returnToBriscola && game != null) {
                    renderBriscola()
                    advanceComputer()
                } else showHome()
            }
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
        installScreen(scroll)
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
            setPadding(dp(12), dp(12), dp(12), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(7, 30, 26), forest, Color.rgb(5, 25, 22)))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(root)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("‹ MENU", 14f, gold, true).apply {
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener { scopa = null; showHome() }
        })
        header.addView(label("SCOPA", 25f, ivory, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(label("SCOPE · TU ${match.scope[0]} — ${match.scope[1]} COMPUTER", 15f, gold, true))
        root.addView(label("Carte raccolte: ${match.captured[0].size} — ${match.captured[1].size}", 12f, ivory))
        root.addView(label("AVVERSARIO · ${match.hands[1].size} carte", 12f, gold))
        root.addView(cardBackRow(match.hands[1].size))
        val felt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
            background = panel(Color.rgb(18, 83, 62), 22, Color.rgb(121, 139, 82))
        }
        felt.addView(label("SUL TAVOLO", 13f, gold, true))
        if (match.table.isEmpty()) {
            felt.addView(label("Tavolo libero", 15f, ivory).apply {
                setPadding(dp(8), dp(24), dp(8), dp(24))
            })
        } else {
            felt.addView(cardRow(match.table))
        }
        root.addView(felt, LinearLayout.LayoutParams(-1, -2))
        root.addView(label(match.lastMove, 13f, gold))
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
            root.addView(label("LE TUE CARTE · TOCCA PER GIOCARE", 13f, gold, true))
            root.addView(cardRow(match.hands[0]) { card -> if (match.turn == 0) playScopaCard(card) })
        }
        installScreen(scroll)
    }

    private fun startBriscola() {
        gameHandler.removeCallbacksAndMessages(null)
        computerPending = false
        trickPreview = null
        trickResolving = false
        game = BriscolaGame()
        advanceComputer()
        renderBriscola()
    }

    private fun playBriscola(player: Int, card: Card) {
        val match = game ?: return
        if (trickResolving || match.finished || match.turn != player) return
        val first = match.lead
        if (first == null) {
            if (match.play(player, card)) {
                renderBriscola()
                if (match.turn == 1) advanceComputer()
            }
            return
        }
        // Keep both played cards on the felt while the trick is being awarded.
        trickPreview = first.second to card
        trickResolving = true
        renderBriscola()
        gameHandler.postDelayed({
            if (game !== match) return@postDelayed
            match.play(player, card)
            trickPreview = null
            trickResolving = false
            renderBriscola()
            if (match.turn == 1) advanceComputer()
        }, 1000L)
    }

    private fun advanceComputer() {
        val match = game ?: return
        if (match.finished || match.turn != 1 || computerPending || trickResolving) return
        computerPending = true
        gameHandler.postDelayed({
            computerPending = false
            if (game !== match || match.finished || match.turn != 1) return@postDelayed
            playBriscola(1, match.aiCard())
        }, 850L)
    }

    private fun cardName(card: Card): String =
        "${card.rank.number} ${card.suit.name.lowercase().replaceFirstChar { it.uppercase() }}"

    private fun renderBriscola() {
        val match = game ?: return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(10), dp(8), dp(10), dp(10))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(7, 30, 26), Color.rgb(20, 94, 68), forest))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(label("‹", 30f, gold, true).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener {
                gameHandler.removeCallbacksAndMessages(null)
                computerPending = false
                trickPreview = null
                trickResolving = false
                game = null
                showHome()
            }
        })
        header.addView(label("BRISCOLA", 19f, ivory, true),
            LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("⚙", 23f, gold).apply {
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setOnClickListener {
                // Finish the current trick before opening settings: a pending
                // callback must never replace the settings screen unexpectedly.
                if (!trickResolving && !computerPending) {
                    showAudioSettings(returnToBriscola = true)
                }
            }
        })
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        root.addView(cardBackRow(match.hands[1].size))
        root.addView(label("COMPUTER  ·  ${match.points[1]} PUNTI", 13f, gold, true))

        val table = FrameLayout(this).apply {
            background = panel(Color.rgb(23, 103, 76), 24, Color.rgb(73, 125, 89))
        }
        root.addView(table, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })
        // The exposed trump lies sideways underneath the covered stock.
        if (match.remainingCards > 0) {
            val trump = cardTile(match.trumpCard).apply {
                rotation = 90f
                pivotX = dp(39).toFloat()
                pivotY = dp(64).toFloat()
            }
            table.addView(trump, FrameLayout.LayoutParams(dp(78), dp(129), Gravity.START or Gravity.CENTER_VERTICAL).apply {
                leftMargin = dp(38)
            })
            if (match.remainingCards > 1) {
                val back = cardBackRow(1)
                table.addView(back, FrameLayout.LayoutParams(dp(90), dp(120),
                    Gravity.START or Gravity.CENTER_VERTICAL))
            }
            table.addView(label("${match.remainingCards}", 17f, ivory, true).apply {
                background = panel(Color.rgb(17, 63, 52), 18)
            }, FrameLayout.LayoutParams(dp(44), dp(38),
                Gravity.START or Gravity.CENTER_VERTICAL).apply {
                leftMargin = dp(22)
                topMargin = dp(64)
            })
        }
        val lead = match.lead
        val leadView = lead?.let { cardTile(it.second) }
        val replyView = trickPreview?.let { cardTile(it.second) }
        it.studiosmus.carte.ui.TrickLayout.bind(
            table, leadView, replyView, match.remainingCards > 0
        ) { dp(it) }
        val tableMessage = when {
            trickResolving -> "PRESA IN CORSO"
            lead?.first == 1 -> "IL COMPUTER HA GIOCATO"
            lead?.first == 0 -> "HAI GIOCATO"
            else -> match.lastTrick.ifEmpty { "TOCCA A TE" }
        }
        table.addView(label(tableMessage, 12f, ivory, true),
            FrameLayout.LayoutParams(-1, dp(38), Gravity.BOTTOM))
        root.addView(label("TU  ·  ${match.points[0]} PUNTI", 14f, gold, true))
        if (match.finished) {
            root.addView(label(match.result(), 24f, ivory, true))
            root.addView(Button(this).apply {
                text = "NUOVA PARTITA"
                setOnClickListener { startBriscola() }
            })
        } else {
            root.addView(cardRow(match.hands[0]) { card ->
                if (match.turn == 0 && !trickResolving) playBriscola(0, card)
            })
        }
        installScreen(root)
    }

}
