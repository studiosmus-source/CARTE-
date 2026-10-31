package it.studiosmus.carte

import android.app.Activity
import android.os.Bundle
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
    private val briscolaSession by lazy { it.studiosmus.carte.ui.BriscolaSession({ if (currentScreen == 2) renderBriscola() }, { salonAudio.playCard(it); Unit }) }
    private val game get() = briscolaSession.game
    private val scopaSession by lazy { it.studiosmus.carte.ui.ScopaSession({ if (currentScreen == 3) renderScopa() }, { salonAudio.playCard(it); Unit }) }
    private val salonAudio by lazy { SalonAudio(this) }
    private var currentScreen = 0 // home=0, audio=1, briscola=2, scopa=3, table=4
    private var preludeGame = 2
    private var displayedScreen: View? = null
    private var navigationToken = 0
    private var announcedResult: Any? = null
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
        (findViewById<View>(android.R.id.content) as ViewGroup).setBackgroundColor(Color.BLACK)
        salonAudio.enterScene(SalonAudio.Scene.HOME, 0L)
        val prefs = getSharedPreferences("ambience", MODE_PRIVATE)
        fireplaceEnabled = prefs.getBoolean("enabled", true)
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
            -1 -> Unit // the transition callback installs the destination
            1 -> showAudioSettings(audioReturnsToBriscola)
            2 -> renderBriscola()
            3 -> renderScopa()
            4 -> showPrelude()
            else -> showHome()
        }
    }

    override fun onPause() {
        salonAudio.stop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        updateFireplacePlayback()
        salonAudio.start()
    }

    override fun onDestroy() {
        navigationToken++
        displayedScreen?.animate()?.cancel()
        briscolaSession.stop()
        scopaSession.stop()
        salonAudio.stop()
        super.onDestroy()
    }

    private fun applyAmbience() {
        salonAudio.configure(ambience)
        updateFireplacePlayback()
    }

    private fun updateFireplacePlayback() {
        // The room mixer silences the fireplace outside the salon automatically.
        // Do not resurrect the old noise generator when a sample is missing.
        salonAudio.setFireplaceEnabled(fireplaceEnabled)
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
        displayedScreen = view
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
                // Fireplace is independent of jazz and the other salon layers.
                updateFireplacePlayback()
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
        root.addView(label("Scopa e Briscola · Gioca offline · v0.1.7", 12f, ivory))
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
        root.addView(Button(this).apply {
            text = if (fireplaceEnabled) "SPEGNI CAMINO" else "PROVA IL CAMINO"
            setOnClickListener {
                fireplaceEnabled = !fireplaceEnabled
                saveAmbience()
                text = if (fireplaceEnabled) "SPEGNI CAMINO" else "PROVA IL CAMINO"
            }
        })
        slider("Camino", ambience.fireplace) { ambience = ambience.copy(fireplace = it) }
        slider("Jazz", ambience.jazz) { ambience = ambience.copy(jazz = it) }
        slider("Conversazioni", ambience.conversations) { ambience = ambience.copy(conversations = it) }
        slider("Bicchieri", ambience.glassware) { ambience = ambience.copy(glassware = it) }
        slider("Porte", ambience.doors) { ambience = ambience.copy(doors = it) }
        slider("Carte", ambience.cards) { ambience = ambience.copy(cards = it) }
        val recordings = listOf(
            "Jazz" to "salon_jazz",
            "Camino" to "salon_fireplace",
            "Carte: distribuzione" to "salon_card_deal_01",
            "Carte: giocata" to "salon_card_play_01",
            "Carte: raccolta" to "salon_card_collect_01",
            "Conversazioni" to "salon_conversation",
            "Bisbiglio breve 1" to "salon_whisper_01",
            "Bisbiglio breve 2" to "salon_whisper_02",
            "Bisbiglio breve 3" to "salon_whisper_03",
            "Bisbiglio breve 4" to "salon_whisper_04",
            "Bicchieri" to "salon_glassware",
            "Sedie" to "salon_chair",
            "Passi sul parquet" to "salon_footsteps",
            "Battimani" to "salon_applause",
            "Porte" to "salon_door"
        )
        val available = recordings.filter { (_, resource) ->
            resources.getIdentifier(resource, "raw", packageName) != 0
        }
        val audioStatus = "Registrazioni disponibili: ${available.size}/${recordings.size}\n" +
            recordings.joinToString("\n") { (title, resource) ->
                "$title: " + if (resources.getIdentifier(resource, "raw", packageName) != 0)
                    "presente" else "non incluso"
            }
        root.addView(Button(this).apply {
            text = "ASCOLTA UN SUONO"
            setOnClickListener {
                val samples = recordings.filter { (title, resource) ->
                    title != "Jazz" && salonAudio.hasRecording(resource)
                }
                if (samples.isEmpty()) {
                    android.widget.Toast.makeText(this@MainActivity,
                        "Non ci sono registrazioni da ascoltare", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Ascolta un suono isolato")
                        .setItems(samples.map { it.first }.toTypedArray()) { _, index ->
                            salonAudio.previewSample(samples[index].second)
                        }
                        .setNegativeButton("Chiudi", null)
                        .show()
                }
            }
        })
        root.addView(label("DIAGNOSTICA AUDIO", 18f, gold, true))
        root.addView(label(audioStatus, 14f, ivory).apply {
            setPadding(dp(10), dp(12), dp(10), dp(18))
        })
        root.addView(label("Solo registrazioni reali: i suoni non ancora inclusi restano silenziosi, senza rumori artificiali.", 13f, gold))
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
            setOnClickListener { navigateToPrelude(if (title == "BRISCOLA") 2 else 3) }
        }
        box.addView(label(title, 25f, ivory, true))
        box.addView(label(subtitle, 13f, gold))
        val params = LinearLayout.LayoutParams(-1, dp(100))
        params.bottomMargin = dp(14)
        root.addView(box, params)
    }

    private fun startScopa() {
        currentScreen = 3
        scopaSession.start()
    }

    /** Fade to genuine black before revealing an EMPTY, un-dealt table. */
    private fun navigateToPrelude(type: Int) {
        if (type != 2 && type != 3) return
        val token = ++navigationToken
        currentScreen = -1 // suppress callbacks while leaving this screen
        val outgoing = displayedScreen
        salonAudio.enterScene(SalonAudio.Scene.BLACK, 850L)
        fun reveal() {
            if (token != navigationToken || isFinishing || isDestroyed) return
            briscolaSession.stop()
            scopaSession.stop()
            preludeGame = type
            showPrelude()
            updateFireplacePlayback()
            val incoming = displayedScreen ?: return
            incoming.alpha = 0f
            salonAudio.enterScene(SalonAudio.Scene.SALON, 1600L)
            incoming.animate().alpha(1f).setDuration(1600L).start()
        }
        if (outgoing == null) reveal()
        else outgoing.animate().alpha(0f).setDuration(850L).withEndAction(::reveal).start()
    }

    /** Leaving the table always fades OUT to black, then home fades IN. */
    private fun navigateHome() {
        val token = ++navigationToken
        currentScreen = -1 // suppress callbacks while leaving this screen
        val outgoing = displayedScreen
        salonAudio.enterScene(SalonAudio.Scene.BLACK, 850L)
        fun reveal() {
            if (token != navigationToken || isFinishing || isDestroyed) return
            briscolaSession.stop()
            scopaSession.stop()
            showHome()
            updateFireplacePlayback()
            val incoming = displayedScreen ?: return
            incoming.alpha = 0f
            salonAudio.enterScene(SalonAudio.Scene.HOME, 1600L)
            incoming.animate().alpha(1f).setDuration(1600L).start()
        }
        if (outgoing == null) reveal()
        else outgoing.animate().alpha(0f).setDuration(850L).withEndAction(::reveal).start()
    }

    /** No cards are constructed until the player explicitly presses AVVIA. */
    private fun showPrelude() {
        currentScreen = 4
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(24), dp(16), dp(28))
            setBackgroundColor(Color.rgb(15, 20, 17))
        }
        root.addView(label("‹ MENU", 16f, gold, true).apply {
            setPadding(dp(8), dp(8), dp(8), dp(16))
            setOnClickListener { navigateHome() }
        })
        root.addView(label(if (preludeGame == 2) "BRISCOLA" else "SCOPA", 26f, ivory, true))
        root.addView(View(this), LinearLayout.LayoutParams(1, dp(20)))
        root.addView(it.studiosmus.carte.ui.SalonTable(this, if (fireplaceEnabled) 1f else 0f),
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(10)
                bottomMargin = dp(22)
            })
        root.addView(Button(this).apply {
            text = "AVVIA"
            textSize = 20f
            setTextColor(ivory)
            background = panel(Color.rgb(70, 48, 33), 16)
            setOnClickListener {
                if (preludeGame == 2) startBriscola() else startScopa()
            }
        }, LinearLayout.LayoutParams(-1, dp(64)))
        installScreen(root)
    }


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
        val match = scopaSession.game ?: return
        if (match.finished && announcedResult !== match) {
            announcedResult = match
            val scores = match.score()
            salonAudio.playResult(scores[0] > scores[1])
        }
        currentScreen = 3
        installScreen(it.studiosmus.carte.ui.ScopaScreen(
            this, scopaSession, cardRenderer,
            onBack = { confirmLeaveGame() },
            onNewGame = { navigateToPrelude(3) },
            onPlay = { card -> playScopaCard(card) },
            fireplaceLight = if (fireplaceEnabled) 1f else 0f
        ).render())
    }

    private fun startBriscola() {
        currentScreen = 2
        briscolaSession.start()
    }
    private fun advanceComputer() = briscolaSession.advanceComputer()

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when (currentScreen) {
            2, 3 -> confirmLeaveGame()
            4 -> navigateHome()
            -1 -> Unit // ignore back during a transition
            1 -> if (audioReturnsToBriscola && game != null) renderBriscola() else showHome()
            else -> super.onBackPressed()
        }
    }

    /** Do not discard an unfinished offline match from an accidental Back tap. */
    private fun confirmLeaveGame() {
        val unfinished = when (currentScreen) {
            2 -> briscolaSession.game?.finished == false
            3 -> scopaSession.game?.finished == false
            else -> false
        }
        if (!unfinished) {
            navigateHome()
            return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Abbandonare la partita?")
            .setMessage("La partita in corso verrà persa.")
            .setNegativeButton("Continua a giocare", null)
            .setPositiveButton("Abbandona") { _, _ -> navigateHome() }
            .show()
    }

    private fun cardName(card: Card): String =
        "${card.rank.number} ${card.suit.name.lowercase().replaceFirstChar { it.uppercase() }}"

    private fun renderBriscola() {
        val match = game ?: return
        if (match.finished && announcedResult !== match) {
            announcedResult = match
            salonAudio.playResult(match.points[0] > match.points[1])
        }
        currentScreen = 2
        installScreen(it.studiosmus.carte.ui.BriscolaScreen(
            this, briscolaSession, cardRenderer,
            onBack = { confirmLeaveGame() },
            onSettings = { showAudioSettings(returnToBriscola = true) },
            onNewGame = { navigateToPrelude(2) },
            fireplaceLight = if (fireplaceEnabled) 1f else 0f
        ).render())
    }

}
