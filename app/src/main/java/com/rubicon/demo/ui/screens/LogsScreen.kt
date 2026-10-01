package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.LogLevel
import com.rubicon.demo.demo
import com.rubicon.demo.sdk.FeedEntry
import com.rubicon.demo.ui.components.ChoiceChips
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.FeedRow
import com.rubicon.demo.ui.components.Flow
import com.rubicon.demo.ui.components.LogRow
import com.rubicon.demo.ui.components.StatTile
import com.rubicon.demo.ui.components.StatusPill
import com.rubicon.demo.ui.theme.Danger
import com.rubicon.demo.ui.theme.Success

@Composable
fun LogsScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.demo
    val counts by app.feed.counts.collectAsState()
    val entries by app.feed.entries.collectAsState()
    val lines by app.sdkLog.lines.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var kind by remember { mutableStateOf<FeedEntry.Kind?>(null) }
    var level by remember { mutableStateOf(LogLevel.DEBUG) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("queued", counts.queued.toString(), Modifier.weight(1f))
            StatTile("sent", counts.sent.toString(), Modifier.weight(1f), Success)
            StatTile("dropped", counts.droppedTotal.toString(), Modifier.weight(1f), Danger)
        }
        if (counts.dropped.isNotEmpty()) {
            Row(Modifier.padding(top = 8.dp)) { Flow { counts.dropped.forEach { (reason, n) -> StatusPill("${reason.name.lowercase()} $n", Danger) } } }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            listOf("Event updates", "SDK log").forEachIndexed { i, label ->
                SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
            }
        }
        if (tab == 0) {
            ChoiceChips(listOf(null) + FeedEntry.Kind.entries, kind, { it?.name?.lowercase() ?: "all" }) { kind = it }
            TextButton(onClick = { app.feed.clear() }) { Text("Clear") }
            val shown = entries.filter { kind == null || it.kind == kind }
            if (shown.isEmpty()) Expect("Nothing yet: send an event from the Events tab. These come from logging.eventObserver.")
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { entry ->
                    FeedRow(entry)
                    HorizontalDivider()
                }
            }
        } else {
            ChoiceChips(listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR), level, { "${it.name.lowercase()}+" }) { level = it }
            TextButton(onClick = { app.sdkLog.clear() }) { Text("Clear") }
            val shown = lines.filter { it.level.ordinal >= level.ordinal }
            if (!app.settings.inAppLogger) Expect("The in-app logger is off (Settings): the SDK logs to Logcat only, tag Rubicon.")
            if (shown.isEmpty()) Expect("No lines at this level. The SDK logs at ${app.settings.logLevel.name.lowercase()} and above (Settings).")
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { line ->
                    LogRow(line)
                    HorizontalDivider()
                }
            }
        }
    }
}
