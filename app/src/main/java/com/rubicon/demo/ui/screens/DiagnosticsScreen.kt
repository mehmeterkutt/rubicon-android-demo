package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rubicon.demo.analytics.Diagnostics
import com.rubicon.demo.analytics.LogLevel
import com.rubicon.demo.analytics.RubiconAnalytics
import com.rubicon.demo.net.BackendProbe
import com.rubicon.demo.ui.components.LogLine
import com.rubicon.demo.ui.components.SectionCard
import com.rubicon.demo.ui.components.StatTile
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(analytics: RubiconAnalytics, modifier: Modifier = Modifier) {
    val state by analytics.state.collectAsState()
    val entries by Diagnostics.entries.collectAsState()
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf("http://10.0.2.2:8080") }
    var pingResult by remember { mutableStateOf<String?>(null) }
    var warningsOnly by remember { mutableStateOf(false) }

    val visible = if (warningsOnly) entries.filter { it.level >= LogLevel.WARN } else entries

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("tracked", state.eventsTracked.toString(), Modifier.weight(1f))
            StatTile("flushes", state.flushesRequested.toString(), Modifier.weight(1f))
            StatTile("log lines", entries.size.toString(), Modifier.weight(1f))
        }

        SectionCard("Backend", "The fake server on your Mac. From the emulator it is 10.0.2.2.") {
            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    pingResult = "..."
                    scope.launch {
                        pingResult = BackendProbe.ping(baseUrl).fold({ it }, { "failed: ${it.message}" })
                        Diagnostics.log(if (pingResult!!.startsWith("HTTP 2")) LogLevel.INFO else LogLevel.ERROR, "ping $baseUrl -> $pingResult")
                    }
                }) { Text("Ping") }
            }
            pingResult?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !warningsOnly, onClick = { warningsOnly = false }, label = { Text("All") })
                FilterChip(selected = warningsOnly, onClick = { warningsOnly = true }, label = { Text("Warnings") })
            }
            TextButton(onClick = { Diagnostics.clear() }) { Text("Clear") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(visible, key = { it.id }) { LogLine(it) }
        }
    }
}
