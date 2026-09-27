package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rubicon.demo.analytics.RubiconAnalytics
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.SectionCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EventsScreen(analytics: RubiconAnalytics, modifier: Modifier = Modifier) {
    val state by analytics.state.collectAsState()
    val scope = rememberCoroutineScope()
    var clicks by remember { mutableIntStateOf(0) }
    var customName by remember { mutableStateOf("Feature Used") }
    var customProps by remember { mutableStateOf("feature=search, source=home") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard("Single events", "One call per button. Watch them arrive on the Diagnostics tab.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { clicks++; analytics.track("Button Clicked", mapOf("count" to clicks)) }) { Text("Track") }
                FilledTonalButton(onClick = { analytics.screen("Events") }) { Text("Screen") }
                FilledTonalButton(onClick = {
                    analytics.track("Purchase Completed", mapOf("amount" to 9.99, "currency" to "USD", "items" to 2))
                }) { Text("Purchase") }
            }
        }

        SectionCard("Custom event", "Properties as key=value pairs separated by commas.") {
            OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("Event name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = customProps, onValueChange = { customProps = it }, label = { Text("Properties") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { analytics.track(customName.trim(), parseProps(customProps)) }, enabled = customName.isNotBlank()) { Text("Send") }
        }

        SectionCard("Scenarios", "Multi-event flows for queue, batching and flush tests.") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        listOf("Product Viewed", "Add To Cart", "Checkout Started", "Payment Submitted", "Order Completed").forEachIndexed { i, name ->
                            analytics.track(name, mapOf("step" to i + 1, "order_id" to "ord-1001"))
                            delay(150)
                        }
                    }
                }) { Text("Checkout funnel") }
                OutlinedButton(onClick = {
                    repeat(50) { analytics.track("Burst Event", mapOf("index" to it)) }
                }) { Text("Burst 50") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { analytics.flush() }) { Text("Flush now") }
            }
        }

        SectionCard("Session state") {
            KeyValueLine("events tracked", state.eventsTracked.toString())
            KeyValueLine("flushes requested", state.flushesRequested.toString())
            KeyValueLine("last deep link", state.lastDeepLink ?: "none")
            Text(
                "Deep link test: adb shell am start -a android.intent.action.VIEW -d \"rubicon://open?campaign=test\"",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun parseProps(text: String): Map<String, Any?> =
    text.split(',').mapNotNull { pair ->
        val (k, v) = pair.split('=', limit = 2).map { it.trim() }.takeIf { it.size == 2 } ?: return@mapNotNull null
        k to (v.toIntOrNull() ?: v.toDoubleOrNull() ?: v.toBooleanStrictOrNull() ?: v)
    }.toMap()
