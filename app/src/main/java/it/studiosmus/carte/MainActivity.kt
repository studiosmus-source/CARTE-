package it.studiosmus.carte

import android.app.Activity
import android.os.Bundle
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import it.studiosmus.carte.audio.Soundscape
import it.studiosmus.carte.audio.SalonAudio
import it.studiosmus.carte.core.Card
import android.widget.SeekBar
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
    private val briscolaSession by lazy { it.studiosmus.carte.ui.BriscolaSession({ renderBriscola() }, { soundscape.playCard(it) }) }
    private val game get() = briscolaSession.game
    private val scopaSession by lazy { it.studiosmus.carte.ui.ScopaSession({ renderScopa() }, { soundscape.playCard(it) }) }
    private val soundscape = Soundscape()
    private val salonAudio by lazy { SalonAudio(this) }
    private var currentScreen = 0 // home=0, audio=1, briscola=2, scopa=3
    private var audioReturnsToBriscola = false
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (currentScreen) {
            1 -> showAudioSettings(audioReturnsToBriscola)
            2 -> renderBriscola()
            3 -> renderScopa()
            else -> showHome()
        }
    }

    override fun onPause() {
        soundscape.stop()
        salonAudio.stop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        soundscape.setFireplaceEnabled(fireplaceEnabled)
        if (fireplaceEnabled) salonAudio.start()
    }

    override fun onDestroy() {
        briscolaSession.stop()
        scopaSession.stop()
        soundscape.release()
        salonAudio.stop()
        super.onDestroy()
    }

    private fun applyAmbience() {
        soundscape.setVolume(Soundscape.Channel.FIREPLACE, ambience.fireplace)
        soundscape.setVolume(Soundscape.Channel.CARDS, ambience.cards)
        soundscape.setFireplaceEnabled(fireplaceEnabled)
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

    private fun showHome() {
        currentScreen = 0
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
                    soundscape.setFireplaceEnabled(true)
                    salonAudio.start()
                } else {
                    soundscape.setFireplaceEnabled(false)
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
        currentScreen = 1
        audioReturnsToBriscola = returnToBriscola
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
        root.addView(Button(this).apply { text = if (fireplaceEnabled) "SPEGNI CAMINO" else "PROVA IL CAMINO"; setOnClickListener { fireplaceEnabled = !fireplaceEnabled; saveAmbience(); if (fireplaceEnabled) { soundscape.setFireplaceEnabled(true); salonAudio.start() } else { soundscape.setFireplaceEnabled(false); salonAudio.stop() }; text = if (fireplaceEnabled) "SPEGNI CAMINO" else "PROVA IL CAMINO" } })
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
        root.addView(label("Camino e carte: effetti offline indipendenti. Le registrazioni ambientali non incluse restano silenziose; le preferenze vengono salvate.", 13f, gold))
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

    private fun startScopa() = scopaSession.start()

    private fun playScopaCard(card: Card) {
        val match = scopaSession.game ?: return
        val choices = scopaSession.options(card)
        if (choices.size <= 1) {
            scopaSession.play(card, choices.firstOrNull())
            return
        }
        val labels = choices.map { capture ->
            capture.joinToString(" + ") { cardName(it) }
        }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Scegli la presa")
            .setItems(labels) { _, index ->
                if (scopaSession.game === match) scopaSession.play(card, choices[index])
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun renderScopa() {
        if (scopaSession.game == null) return
        currentScreen = 3
        installScreen(it.studiosmus.carte.ui.ScopaScreen(
            this, scopaSession, cardRenderer,
            onBack = { scopaSession.stop(); showHome() },
            onNewGame = { startScopa() },
            onPlay = { card -> playScopaCard(card) }
        ).render())
    }

    private fun startBriscola() = briscolaSession.start()
    private fun advanceComputer() = briscolaSession.advanceComputer()

    private fun cardName(card: Card): String =
        "${card.rank.number} ${card.suit.name.lowercase().replaceFirstChar { it.uppercase() }}"

    private fun renderBriscola() {
        if (game == null) return
        currentScreen = 2
        installScreen(it.studiosmus.carte.ui.BriscolaScreen(
            this, briscolaSession, cardRenderer,
            onBack = { briscolaSession.stop(); showHome() },
            onSettings = { showAudioSettings(returnToBriscola = true) },
            onNewGame = { startBriscola() }
        ).render())
    }

}
