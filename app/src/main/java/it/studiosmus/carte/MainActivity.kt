package it.studiosmus.carte

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        container.addView(TextView(this).apply { text = "CARTE 🃏"; textSize = 32f })
        container.addView(TextView(this).apply {
            text = "Briscola e Scopa — progetto in costruzione.\nMotore offline, nessun account richiesto."
            textSize = 18f
        })
        setContentView(container)
    }
}
