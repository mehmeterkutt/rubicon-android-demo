package com.rubicon.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

/**
 * A plain `Activity`, not a ComponentActivity: the SDK cannot hear its onNewIntent, so it calls
 * setIntent() as the KDoc of DEEP_LINKS advises, and the SDK reads the link on resume. `singleTop`
 * with the deep link `rubicon://legacy`. Its screen view is named from the class: `LegacyLink`.
 */
class LegacyLinkActivity : Activity() {
    private lateinit var text: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = TextView(this).apply {
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            textSize = 16f
        }
        setContentView(text)
        show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        show()
    }

    private fun show() {
        text.text = "A plain Activity (singleTop)\n\nLink: ${intent?.data ?: "none"}\n\nOpen another rubicon://legacy link while this is shown: it arrives through onNewIntent and setIntent()."
    }
}
