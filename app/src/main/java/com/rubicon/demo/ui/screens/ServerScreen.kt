package com.rubicon.demo.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.Autocapture
import com.rubicon.demo.demo
import com.rubicon.demo.net.FakeBackend
import com.rubicon.demo.net.ServerEvent
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.ui.components.ChoiceChips
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.Field
import com.rubicon.demo.ui.components.Flow
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.MultiChips
import com.rubicon.demo.ui.components.SectionCard
import com.rubicon.demo.ui.components.StatusPill
import com.rubicon.demo.ui.components.SwitchRow
import com.rubicon.demo.ui.theme.Danger
import com.rubicon.demo.ui.theme.Mono
import com.rubicon.demo.ui.theme.Success
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ServerScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.demo
    val backend = app.backend
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Connection(backend, app.settings.host)
        Responses(backend)
        RemoteConfig(backend, app.settings.apiKey)
        ReceivedEvents(backend)
        Requests(backend)
    }
}

private suspend fun attempt(block: suspend () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        Calls.show("Fake server unreachable: ${t.javaClass.simpleName}. Is python3 fake-server/server.py running?")
    }
}

@Composable
private fun Connection(
    backend: FakeBackend,
    host: String,
) {
    var state by remember { mutableStateOf<String?>(null) }
    var reachable by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun check() {
        try {
            state = backend.control().body.replace("\n", "").replace("  ", " ")
            reachable = true
        } catch (t: Throwable) {
            state = "${t.javaClass.simpleName}: ${t.message}"
            reachable = false
        }
    }
    LaunchedEffect(Unit) { check() }
    SectionCard("Fake server", "fake-server/server.py on the Mac; the SDK sends to the same host.") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(if (reachable) "reachable" else "unreachable", if (reachable) Success else Danger)
            Text(host, style = Mono)
        }
        state?.let { Text(it, style = Mono) }
        OutlinedButton(onClick = { scope.launch { check() } }) { Text("Check again") }
    }
}

@Composable
private fun Responses(backend: FakeBackend) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf(200) }
    var retryAfter by remember { mutableStateOf("") }
    var delayMs by remember { mutableStateOf("0") }
    var mode by remember { mutableStateOf("normal") }
    SectionCard("Batch responses", "How the server answers the SDK's uploads, to see Retry 1 to 5 at work.") {
        ChoiceChips(listOf(200, 400, 401, 403, 404, 408, 413, 422, 429, 500, 501, 503), status, { it.toString() }) { status = it }
        Field("Retry-After (seconds)", retryAfter, supporting = "Empty for none; used with 429 and 503") { retryAfter = it }
        Field("Delay (ms)", delayMs, supporting = "Over 20 000 the SDK times out") { delayMs = it }
        ChoiceChips(listOf("normal", "redirect", "drop"), mode, { it }) { mode = it }
        Flow {
            Button(onClick = {
                scope.launch {
                    attempt {
                        backend.control("?status=$status&retry_after=$retryAfter&delay_ms=${delayMs.toIntOrNull() ?: 0}&mode=$mode")
                        Calls.show("The server now answers $status ($mode)")
                    }
                }
            }) { Text("Apply") }
            OutlinedButton(onClick = {
                scope.launch {
                    attempt {
                        backend.reset()
                        status = 200
                        retryAfter = ""
                        delayMs = "0"
                        mode = "normal"
                        Calls.show("The server answers 200 again")
                    }
                }
            }) { Text("Reset to 200") }
        }
        Expect("5xx, 408, timeouts and drop: kept, backoff 2 s, 10 s, 30 s… 429, or 503 with Retry-After: pause as asked. 400, 422: deleted as rejected.")
        Expect("401, 403, 404, 413, 501, redirect: kept, paused for an hour, one ERROR. A restart or a new flush does not end a pause.")
    }
}

@Composable
private fun RemoteConfig(
    backend: FakeBackend,
    apiKey: String,
) {
    val app = LocalContext.current.demo
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(true) }
    var off by remember { mutableStateOf(emptySet<Autocapture>()) }
    var flushAt by remember { mutableStateOf("") }
    var flushInterval by remember { mutableStateOf("") }
    var sampleRate by remember { mutableStateOf("") }
    var diagnosticsRate by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("…") }

    fun load(json: JSONObject?) {
        current = json?.toString(2) ?: "none (the server answers 404)"
        enabled = json?.optBoolean("enabled", true) ?: true
        off = json?.optJSONArray("autocapture_off")?.let { a -> (0 until a.length()).mapNotNull { i -> Autocapture.entries.find { it.name.lowercase() == a.getString(i) } }.toSet() } ?: emptySet()
        flushAt = json?.opt("flush_at")?.toString().orEmpty()
        flushInterval = json?.opt("flush_interval_ms")?.toString().orEmpty()
        sampleRate = json?.opt("sample_rate")?.toString().orEmpty()
        diagnosticsRate = json?.opt("diagnostics_sample_rate")?.toString().orEmpty()
    }
    suspend fun refresh() {
        attempt {
            val response = backend.remoteConfig(apiKey)
            load(if (response.code == 200) JSONObject(response.body) else null)
        }
    }
    LaunchedEffect(Unit) { refresh() }

    SectionCard("Remote configuration", "GET /v1/config for $apiKey (Configuration 1). It only restricts; saved values apply at the next launch.") {
        SwitchRow("enabled", enabled, "Off is the kill switch: collection and upload stop at once, in the running process") { enabled = it }
        Text("autocapture_off", style = MaterialTheme.typography.bodyMedium)
        MultiChips(Autocapture.entries, off, { it.name.lowercase() }) { off = it }
        Expect("network_requests stops at once; the others from the next launch.")
        Field("flush_at", flushAt, supporting = "1 to 100, never below the app's value") { flushAt = it }
        Field("flush_interval_ms", flushInterval, supporting = "1000 to 600000, never below the app's value") { flushInterval = it }
        Field("sample_rate", sampleRate, supporting = "0 to 1, by deviceId") { sampleRate = it }
        Field("diagnostics_sample_rate", diagnosticsRate, supporting = "0 to 1") { diagnosticsRate = it }
        Flow {
            Button(onClick = {
                val json = JSONObject().put("enabled", enabled)
                if (off.isNotEmpty()) json.put("autocapture_off", JSONArray(off.map { it.name.lowercase() }))
                flushAt.toIntOrNull()?.let { json.put("flush_at", it) }
                flushInterval.toLongOrNull()?.let { json.put("flush_interval_ms", it) }
                sampleRate.toDoubleOrNull()?.let { json.put("sample_rate", it) }
                diagnosticsRate.toDoubleOrNull()?.let { json.put("diagnostics_sample_rate", it) }
                scope.launch {
                    attempt {
                        backend.saveRemoteConfig(apiKey, json)
                        refresh()
                        Calls.show("Saved on the server; the SDK reads it at the next launch")
                    }
                }
            }) { Text("Save") }
            FilledTonalButton(onClick = {
                scope.launch {
                    attempt {
                        backend.saveRemoteConfig(apiKey, JSONObject().put("enabled", false))
                        refresh()
                        Calls.show("Kill switch saved; restart to let the SDK read it")
                    }
                }
            }) { Text("Kill switch") }
            OutlinedButton(onClick = {
                scope.launch {
                    attempt {
                        backend.deleteRemoteConfig(apiKey)
                        refresh()
                        Calls.show("Deleted: /v1/config answers 404")
                    }
                }
            }) { Text("Delete") }
            OutlinedButton(onClick = { app.restart() }) { Text("Restart app") }
        }
        Text(current, style = Mono)
    }
}

@Composable
private fun ReceivedEvents(backend: FakeBackend) {
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<ServerEvent>>(emptyList()) }
    var open by remember { mutableStateOf<Int?>(null) }
    suspend fun refresh() = attempt { events = backend.events().asReversed() }
    LaunchedEffect(Unit) { refresh() }
    SectionCard("Received events", "What the server stored, newest first, exactly as the SDK sent it. Tap one for its JSON.") {
        Flow {
            Button(onClick = { scope.launch { refresh() } }) { Text("Refresh") }
            OutlinedButton(onClick = {
                scope.launch {
                    attempt {
                        backend.clearEvents()
                        events = emptyList()
                    }
                }
            }) { Text("Clear") }
        }
        KeyValueLine("events", events.size.toString())
        events.take(150).forEach { event ->
            HorizontalDivider()
            Column(Modifier.fillMaxWidth().clickable { open = if (open == event.index) null else event.index }.animateContentSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(event.source, if (event.source == "auto") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    Text(event.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("#${event.index}", style = MaterialTheme.typography.labelSmall)
                }
                Text(event.type + " · " + event.json.optJSONObject("properties")?.toString()?.take(120).orEmpty(), style = MaterialTheme.typography.bodySmall)
                if (open == event.index) Text(event.json.toString(2), style = Mono, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun Requests(backend: FakeBackend) {
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    SectionCard("Requests", "The server's view of each request: status, size, gzip and the headers the SDK sets (HTTP 2, 5).") {
        Button(onClick = { scope.launch { attempt { requests = backend.requests() } } }) { Text("Load the last requests") }
        requests.take(60).forEach { r ->
            HorizontalDivider()
            Text("${r.optString("time")}  ${r.optString("method")} ${r.optString("path")}  →  ${r.opt("status")}", style = Mono)
            Text(
                listOfNotNull(
                    r.optString("note").takeIf { it.isNotEmpty() },
                    "gzip=${r.optBoolean("gzip")}, ${r.optInt("bytes")} bytes",
                    r.optString("user_agent").takeIf { it.isNotEmpty() && it != "null" },
                    r.optString("if_none_match").takeIf { it.isNotEmpty() && it != "null" }?.let { "If-None-Match $it" },
                ).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
