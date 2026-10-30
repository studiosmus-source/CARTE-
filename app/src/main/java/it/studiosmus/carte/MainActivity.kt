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
import it.studiosmus.carte.audio.AmbienceSettings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Initial visual shell. Gameplay screens will connect to the pure Kotlin engines.
 * Uses Android platform widgets to keep the prototype dependency-free.
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
        val scroll = ScrollView(this).apply { fillViewport = true }
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
        root.addView(label("Versione iniziale · Partite in preparazione", 12f, ivory))
        setContentView(scroll)
    }

    private fun showAudioSettings() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            background = panel(forest)
        }
        val scroll = ScrollView(this)
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
        slider("Camino", ambience.fireplace) { ambience = ambience.copy(fireplace = it) }
        slider("Jazz", ambience.jazz) { ambience = ambience.copy(jazz = it) }
        slider("Conversazioni", ambience.conversations) { ambience = ambience.copy(conversations = it) }
        slider("Bicchieri", ambience.glassware) { ambience = ambience.copy(glassware = it) }
        slider("Porte", ambience.doors) { ambience = ambience.copy(doors = it) }
        slider("Carte", ambience.cards) { ambience = ambience.copy(cards = it) }
        root.addView(label("Il camino è sintetizzato offline. Jazz, voci e altri effetti saranno udibili quando saranno inclusi i file audio. Il volume delle carte è predisposto per i futuri effetti.", 13f, gold))
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
        val scroll = ScrollView(this)
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
        root.addView(label(tableText, 21f, ivory, true).apply {
            background = panel(Color.rgb(23, 83, 64))
            setPadding(dp(12), dp(20), dp(12), dp(20))
        }, LinearLayout.LayoutParams(-1, -2))
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
            match.hands[0].forEach { card ->
                root.addView(Button(this).apply {
                    text = cardName(card)
                    isEnabled = match.turn == 0
                    setOnClickListener { playScopaCard(card) }
                }, LinearLayout.LayoutParams(-1, dp(60)))
            }
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
        val scroll = ScrollView(this)
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
        root.addView(label("🂠  🂠  🂠".take(match.hands[1].size * 3), 34f, ivory))
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
        root.addView(table, LinearLayout.LayoutParams(-1, dp(125)))
        root.addView(label(match.lastTrick, 15f, gold))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(16)))
        if (match.finished) {
            root.addView(label(match.result(), 27f, gold, true))
            root.addView(label("Punteggio finale: ${match.points[0]} - ${match.points[1]}", 18f, ivory))
            val again = Button(this).apply { text = "NUOVA PARTITA"; setOnClickListener { startBriscola() } }
            root.addView(again)
        } else {
            root.addView(label("LE TUE CARTE · TOCCA PER GIOCARE", 14f, gold, true))
            val hand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            match.hands[0].forEach { card ->
                val button = Button(this).apply {
                    text = cardName(card)
                    isEnabled = match.turn == 0
                    setTextColor(forest)
                    setOnClickListener {
                        if (match.play(0, card)) {
                            advanceComputer()
                            renderBriscola()
                        }
                    }
                }
                hand.addView(button, LinearLayout.LayoutParams(-1, dp(58)))
            }
            root.addView(hand)
        }
        setContentView(scroll)
    }

}
