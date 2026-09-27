package com.rubicon.demo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rubicon.demo.analytics.Diagnostics
import com.rubicon.demo.analytics.LogLevel
import com.rubicon.demo.analytics.LoggingAnalytics
import com.rubicon.demo.ui.RubiconScaffold
import com.rubicon.demo.ui.theme.RubiconTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as RubiconApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Diagnostics.log(LogLevel.DEBUG, "MainActivity.onCreate")
        handleDeepLink(intent)
        setContent {
            RubiconTheme {
                RubiconScaffold(
                    analytics = app.analytics,
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        (app.analytics as? LoggingAnalytics)?.deepLink(uri.toString())
        app.analytics.track("Deep Link Opened", mapOf("url" to uri.toString(), "campaign" to uri.getQueryParameter("campaign")))
    }

    override fun onStart() { super.onStart(); Diagnostics.log(LogLevel.DEBUG, "MainActivity.onStart") }
    override fun onStop() { super.onStop(); Diagnostics.log(LogLevel.DEBUG, "MainActivity.onStop") }
    override fun onDestroy() { super.onDestroy(); Diagnostics.log(LogLevel.DEBUG, "MainActivity.onDestroy") }
}
