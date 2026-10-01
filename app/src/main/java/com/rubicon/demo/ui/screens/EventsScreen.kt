package com.rubicon.demo.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.Analytics
import com.rubicon.demo.config.HookPreset
import com.rubicon.demo.demo
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.ui.components.ChoiceChips
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.Flow
import com.rubicon.demo.ui.components.SectionCard
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date

private enum class Tier { GOLD, SILVER }

private enum class PropertyType(val label: String) {
    TEXT("text"),
    INT("int"),
    LONG("long"),
    DOUBLE("double"),
    BOOL("bool"),
    NULL("null"),
    DATE("date"),
    LIST("list"),
    MAP("map"),
}

private class PropertyRow(key: String, type: PropertyType, value: String) {
    var key by mutableStateOf(key)
    var type by mutableStateOf(type)
    var value by mutableStateOf(value)

    fun toValue(): Any? =
        when (type) {
            PropertyType.TEXT -> value
            PropertyType.INT -> value.trim().toIntOrNull() ?: 0
            PropertyType.LONG -> value.trim().toLongOrNull() ?: 0L
            PropertyType.DOUBLE -> value.trim().toDoubleOrNull() ?: 0.0
            PropertyType.BOOL -> value.trim().equals("true", ignoreCase = true)
            PropertyType.NULL -> null
            PropertyType.DATE -> Date()
            PropertyType.LIST -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            PropertyType.MAP -> value.split(',').mapNotNull { pair -> pair.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap()
        }
}

@Composable
fun EventsScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.demo
    val analytics = app.analytics
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        QuickEvents(analytics, app.settings.immediateEvents)
        EventBuilder(analytics)
        ValueTypes(analytics)
        Validation(analytics)
        Volume(analytics)
        Hooks(analytics, app.settings.hooks)
    }
}

@Composable
private fun QuickEvents(
    analytics: Analytics,
    immediate: Set<String>,
) {
    SectionCard("Quick events", "Watch them on the Logs tab (queued, then sent) and on the Server tab (as received).") {
        Flow {
            Button(onClick = { Calls.run("Tracked Button Clicked") { analytics.track("Button Clicked", mapOf("button" to "quick")) } }) { Text("Track") }
            FilledTonalButton(onClick = { Calls.run("Sent screen manual_screen") { analytics.screen("manual_screen", mapOf("origin" to "events_tab")) } }) {
                Text("Screen")
            }
            FilledTonalButton(onClick = {
                Calls.run("Tracked Purchase Completed") { analytics.track("Purchase Completed", mapOf("amount" to 9.99, "currency" to "USD", "items" to 2)) }
            }) { Text("Purchase") }
            OutlinedButton(onClick = { Calls.run("Flush requested") { analytics.flush() } }) { Text("Flush") }
        }
        Expect(
            if ("Purchase Completed" in immediate) {
                "Purchase Completed is an immediate event: it is uploaded as soon as it is stored."
            } else {
                "Immediate events: ${immediate.ifEmpty { setOf("none") }.joinToString()} (Settings)."
            },
        )
    }
}

@Composable
private fun EventBuilder(analytics: Analytics) {
    var name by remember { mutableStateOf("Feature Used") }
    val rows =
        remember {
            mutableStateListOf(
                PropertyRow("feature", PropertyType.TEXT, "search"),
                PropertyRow("count", PropertyType.INT, "3"),
                PropertyRow("premium", PropertyType.BOOL, "true"),
            )
        }
    SectionCard("Build an event", "Any name and typed properties; date is now, list is comma separated, map is k=v pairs.") {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        rows.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = row.key, onValueChange = { row.key = it }, label = { Text("Key") }, singleLine = true, modifier = Modifier.weight(1f))
                    if (row.type != PropertyType.NULL && row.type != PropertyType.DATE) {
                        OutlinedTextField(
                            value = row.value,
                            onValueChange = { row.value = it },
                            label = { Text("Value") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                    }
                    IconButton(onClick = { rows.remove(row) }) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                }
                ChoiceChips(PropertyType.entries, row.type, { it.label }) { row.type = it }
            }
        }
        TextButton(onClick = { rows.add(PropertyRow("key${rows.size + 1}", PropertyType.TEXT, "")) }) { Text("Add property") }
        Flow {
            Button(onClick = { Calls.run("Tracked $name") { analytics.track(name, rows.associate { it.key to it.toValue() }) } }) { Text("Send as track") }
            FilledTonalButton(onClick = { Calls.run("Sent screen $name") { analytics.screen(name, rows.associate { it.key to it.toValue() }) } }) {
                Text("Send as screen")
            }
        }
    }
}

@Composable
private fun ValueTypes(analytics: Analytics) {
    SectionCard("Every value type", "Serialization 2: what the SDK accepts and how it converts it.") {
        Button(onClick = {
            Calls.run("Tracked All Types") {
                analytics.track(
                    "All Types",
                    mapOf(
                        "string" to "text",
                        "int" to 42,
                        "long" to 9_007_199_254_740_993L,
                        "float" to 1.1f,
                        "double" to 2.5,
                        "boolean" to true,
                        "null" to null,
                        "byte" to 7.toByte(),
                        "short" to 300.toShort(),
                        "big_decimal" to BigDecimal("12.75"),
                        "big_integer" to BigInteger("123456789"),
                        "char" to 'x',
                        "date" to Date(0),
                        "enum" to Tier.GOLD,
                        "list" to listOf("a", 1, null, listOf(true)),
                        "set" to setOf("x", "y"),
                        "int_array" to intArrayOf(1, 2, 3),
                        "map" to mapOf("level1" to mapOf("level2" to mapOf("level3" to "deepest allowed"))),
                        "unicode" to "ğüşİıöç 日本 😀",
                    ),
                )
            }
        }) { Text("Track every allowed type") }
        Expect("Float 1.1, Byte and Short as numbers, BigDecimal as a double, Char as text, Date as 1970-01-01T00:00:00.000Z, enum as GOLD, arrays and sets as lists.")
    }
}

@Composable
private fun Validation(analytics: Analytics) {
    SectionCard(
        "Validation",
        "Each one breaks a rule. With debug off: a WARN and the bad part dropped (Logs). With debug on: the SDK throws, shown here as a message.",
    ) {
        Case("Unsupported value types", "JSONObject, an object, NaN, Infinity" + if (Build.VERSION.SDK_INT >= 26) ", java.time.Instant" else "") {
            val values =
                mutableMapOf<String, Any?>(
                    "json_object" to JSONObject().put("a", 1),
                    "object" to Any(),
                    "nan" to Double.NaN,
                    "infinity" to Double.POSITIVE_INFINITY,
                    "kept" to "this one stays",
                )
            if (Build.VERSION.SDK_INT >= 26) values["instant"] = java.time.Instant.now()
            analytics.track("Bad Values", values)
        }
        Case("Bad keys", "A blank key and a nested map with a number as key") {
            analytics.track("Bad Keys", mapOf(" " to 1, "nested" to mapOf(1 to "x", "ok" to 2), "kept" to true))
        }
        Case("Nesting deeper than 3", "The fourth level is dropped") {
            analytics.track("Too Deep", mapOf("a" to mapOf("b" to mapOf("c" to mapOf("d" to "too deep")))))
        }
        Case("A string over 1024 characters", "Cut at 1024 on a code point, WARN, never throws") {
            analytics.track("Long Text", mapOf("text" to "é".repeat(1500)))
        }
        Case("300 properties", "Over 256: the rest is dropped, super properties first") {
            analytics.track("Many Properties", (1..300).associate { "p$it" to it })
        }
        Case("A name of 256 characters", "Dropped, never trimmed (INVALID_EVENT_NAME)") { analytics.track("N".repeat(256)) }
        Case("A blank name", "Dropped (INVALID_EVENT_NAME)") { analytics.track("   ") }
        Case("An event over 32 KB", "Stored as one line over 32 KB: dropped as oversize") {
            analytics.track("Huge Event", (1..40).associate { "chunk$it" to "x".repeat(1000) })
        }
        Case("An unpaired surrogate", "Becomes U+FFFD") { analytics.track("Broken Text", mapOf("text" to "a\uD800b")) }
        Case("Change the map after track", "The SDK keeps a deep copy: the server sees before") {
            val values = mutableMapOf<String, Any?>("state" to "before", "list" to mutableListOf("before"))
            analytics.track("Copied Map", values)
            values["state"] = "after"
            @Suppress("UNCHECKED_CAST")
            (values["list"] as MutableList<String>).add("after")
        }
    }
}

@Composable
private fun Case(
    title: String,
    expectation: String,
    block: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedButton(onClick = { Calls.run(title) { block() } }) { Text(title) }
        Expect(expectation)
    }
}

@Composable
private fun Volume(analytics: Analytics) {
    SectionCard("Volume and threads", "Queue 1, 3 and 5. Set the server to 503 first to see the queue fill up.") {
        Flow {
            OutlinedButton(onClick = {
                Calls.run("Funnel tracked") {
                    listOf("Product Viewed", "Add To Cart", "Checkout Started", "Payment Submitted", "Order Completed").forEachIndexed { i, step ->
                        analytics.track(step, mapOf("step" to i + 1, "order_id" to "ord-1001"))
                    }
                }
            }) { Text("Checkout funnel") }
            OutlinedButton(onClick = { Calls.run("50 events") { repeat(50) { analytics.track("Burst Event", mapOf("index" to it)) } } }) { Text("Burst 50") }
            OutlinedButton(onClick = { Calls.run("1200 events") { repeat(1200) { analytics.track("Queue Filler", mapOf("index" to it)) } } }) { Text("1200 events") }
            OutlinedButton(onClick = {
                Calls.run("8 threads started") {
                    repeat(8) { t -> Thread { repeat(200) { i -> analytics.track("Thread Event", mapOf("thread" to t, "index" to i)) } }.start() }
                }
            }) { Text("8 threads × 200") }
        }
        Expect("1200 at once can overflow the write channel (backpressure); 1200 waiting events exceed the queue of 1000 (queue_full, down to 900).")
        Expect("Per thread, events keep their order: check the index on the Server tab.")
    }
}

@Composable
private fun Hooks(
    analytics: Analytics,
    active: Set<HookPreset>,
) {
    SectionCard("beforeSend hooks", "Active: ${active.ifEmpty { null }?.joinToString { it.title } ?: "none"} (Settings).") {
        Flow {
            OutlinedButton(onClick = { Calls.run("Tracked Debug Event") { analytics.track("Debug Event", mapOf("note" to "should be dropped")) } }) {
                Text("Debug Event")
            }
            OutlinedButton(onClick = { Calls.run("Tracked Hook Test") { analytics.track("Hook Test") } }) { Text("Hook Test") }
            OutlinedButton(onClick = {
                Calls.run("Tracked Signed Up") { analytics.track("Signed Up", mapOf("email" to "ada@example.com", "plan" to "pro")) }
            }) { Text("Event with email") }
        }
        Expect("Debug Event: dropped as before_send. Hook Test: dropped (a throwing hook fails closed). Email: the value arrives as ***.")
    }
}
