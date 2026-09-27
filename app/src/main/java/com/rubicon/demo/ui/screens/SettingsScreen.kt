package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubicon.demo.analytics.RubiconAnalytics
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.SectionCard

/** Hosted by a separate Activity so Activity-level lifecycle callbacks can be observed. */
@Composable
fun SettingsScreen(analytics: RubiconAnalytics, versionName: String, packageName: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard("About") {
            KeyValueLine("app", "Rubicon $versionName")
            KeyValueLine("package", packageName)
            KeyValueLine("sdk", "not attached yet")
        }
        SectionCard("Lifecycle", "This screen lives in SettingsActivity. Opening and closing it exercises Activity callbacks and screen tracking.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { analytics.track("Settings Changed", mapOf("theme" to "system")) }) { Text("Track here") }
                OutlinedButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
