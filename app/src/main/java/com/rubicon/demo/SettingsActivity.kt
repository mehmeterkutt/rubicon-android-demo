package com.rubicon.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import com.rubicon.demo.analytics.Diagnostics
import com.rubicon.demo.analytics.LogLevel
import com.rubicon.demo.ui.screens.SettingsScreen
import com.rubicon.demo.ui.theme.RubiconTheme

class SettingsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Diagnostics.log(LogLevel.DEBUG, "SettingsActivity.onCreate")
        val analytics = (application as RubiconApp).analytics
        val version = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        setContent {
            RubiconTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings") },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                        )
                    },
                ) { padding ->
                    SettingsScreen(analytics, version, packageName, onClose = { finish() }, modifier = Modifier.padding(padding))
                }
            }
        }
    }

    override fun onStart() { super.onStart(); Diagnostics.log(LogLevel.DEBUG, "SettingsActivity.onStart") }
    override fun onStop() { super.onStop(); Diagnostics.log(LogLevel.DEBUG, "SettingsActivity.onStop") }
}
