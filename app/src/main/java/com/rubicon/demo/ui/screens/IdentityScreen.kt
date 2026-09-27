package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubicon.demo.analytics.RubiconAnalytics
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.SectionCard

@Composable
fun IdentityScreen(analytics: RubiconAnalytics, modifier: Modifier = Modifier) {
    val state by analytics.state.collectAsState()
    var userId by remember { mutableStateOf("user-42") }
    var propKey by remember { mutableStateOf("plan") }
    var propValue by remember { mutableStateOf("pro") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("User", "identify links the device to a known user; reset returns to anonymous.") {
            KeyValueLine("current user", state.userId ?: "anonymous")
            OutlinedTextField(value = userId, onValueChange = { userId = it }, label = { Text("userId") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { analytics.identify(userId.trim(), mapOf("email" to "$userId@example.com")) }, enabled = userId.isNotBlank()) { Text("Identify") }
                OutlinedButton(onClick = { analytics.reset() }) { Text("Reset") }
            }
        }

        SectionCard("Super properties", "Attached to every event until unregistered or reset.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = propKey, onValueChange = { propKey = it }, label = { Text("key") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = propValue, onValueChange = { propValue = it }, label = { Text("value") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(onClick = { analytics.register(propKey.trim(), propValue.trim()) }, enabled = propKey.isNotBlank()) { Text("Register") }
            if (state.superProperties.isEmpty()) {
                Text("none registered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.superProperties.forEach { (k, v) ->
                        AssistChip(onClick = { analytics.unregister(k) }, label = { Text("$k=$v  x") })
                    }
                }
            }
        }

        SectionCard("Consent", "Opt-out stops new events; clear removes local data but keeps the opt-out choice.") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Opted out", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.optedOut, onCheckedChange = { if (it) analytics.optOut() else analytics.optIn() })
            }
            OutlinedButton(onClick = { analytics.clearLocalData() }) { Text("Clear local data") }
        }
    }
}
