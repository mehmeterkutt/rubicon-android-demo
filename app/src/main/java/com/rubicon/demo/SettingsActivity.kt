package com.rubicon.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rubicon.demo.ui.SecondaryScaffold
import com.rubicon.demo.ui.screens.SettingsScreen
import com.rubicon.demo.ui.theme.RubiconTheme

/** The SDK's settings; no annotation, so its screen view is named from the class: `Settings`. */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RubiconTheme {
                SecondaryScaffold("Settings", onBack = { finish() }) { SettingsScreen(it) }
            }
        }
    }
}
