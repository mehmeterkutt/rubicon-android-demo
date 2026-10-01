package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.Consent
import com.rubicon.demo.demo
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.ui.components.CodeLine
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.Field
import com.rubicon.demo.ui.components.Flow
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.SectionCard
import com.rubicon.demo.ui.components.StatusPill
import com.rubicon.demo.ui.theme.Danger
import com.rubicon.demo.ui.theme.Success
import com.rubicon.demo.ui.theme.Warning
import kotlinx.coroutines.delay

@Composable
fun IdentityScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.demo
    val analytics = app.analytics
    val person by app.person.collectAsState()
    // The SDK's getters, polled: they are cheap and have no flow.
    var deviceId by remember { mutableStateOf(analytics.deviceId) }
    var optedOut by remember { mutableStateOf(analytics.isOptedOut()) }
    LaunchedEffect(Unit) {
        while (true) {
            deviceId = analytics.deviceId
            optedOut = analytics.isOptedOut()
            delay(500)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard("This installation", "deviceId is read-only; it changes only with clearLocalData (Identity 1, Privacy 3).") {
            KeyValueLine("user (as told to the SDK)", person.userId ?: "anonymous")
            CodeLine(deviceId.ifEmpty { "SDK disabled" })
        }

        var userId by remember { mutableStateOf("user-42") }
        var traits by remember { mutableStateOf("plan=pro, email=ada@example.com") }
        SectionCard("Identify", "Identity 3 and 4: traits are sent once, with the identify event, and never stored.") {
            Field("User id", userId) { userId = it }
            Field("Traits", traits, supporting = "key=value, comma separated") { traits = it }
            Flow {
                Button(onClick = {
                    Calls.run("identify($userId)") {
                        analytics.identify(userId, parse(traits))
                        app.identified(userId)
                    }
                }) { Text("Identify") }
                FilledTonalButton(onClick = {
                    Calls.run("identify($userId) without traits") {
                        analytics.identify(userId)
                        app.identified(userId)
                    }
                }) { Text("Without traits") }
                OutlinedButton(onClick = {
                    Calls.run("reset()") {
                        analytics.reset()
                        app.personCleared()
                    }
                }) { Text("Reset") }
                OutlinedButton(onClick = { Calls.run("identify with a blank id") { analytics.identify("  ") } }) { Text("Blank id") }
            }
            Expect("Same id again without traits: nothing. Another id while identified: Session Ended for the old user, a new person, no warning.")
            Expect("Reset: the user, super properties and session go; deviceId stays; no event.")
        }

        var key by remember { mutableStateOf("plan") }
        var value by remember { mutableStateOf("pro") }
        SectionCard("Super properties", "Context 5: merged into every later event; the event's own key wins. Stored with the person.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field("Key", key, Modifier.weight(1f)) { key = it }
                Field("Value", value, Modifier.weight(1f).padding(start = 8.dp)) { value = it }
            }
            Flow {
                Button(onClick = {
                    Calls.run("register($key)") {
                        analytics.register(key, value)
                        app.registered(key, value)
                    }
                }) { Text("Register") }
                FilledTonalButton(onClick = {
                    Calls.run("register($key, null)") {
                        analytics.register(key, null)
                        app.registered(key, null)
                    }
                }) { Text("Register null") }
                OutlinedButton(onClick = {
                    Calls.run("unregister($key)") {
                        analytics.unregister(key)
                        app.unregistered(key)
                    }
                }) { Text("Unregister") }
            }
            if (person.superProperties.isEmpty()) {
                Expect("None registered.")
            } else {
                person.superProperties.forEach { (k, v) -> KeyValueLine(k, v) }
            }
        }

        SectionCard("Privacy", "Privacy 1 to 4.") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(if (optedOut) "not sending" else "sending", if (optedOut) Warning else Success)
                StatusPill("default consent ${app.settings.defaultConsent.name.lowercase()}", if (app.settings.defaultConsent == Consent.PENDING) Warning else Success)
                if (!app.settings.persistConsent) StatusPill("choice not stored", Danger)
            }
            Flow {
                Button(onClick = { Calls.run("optIn()") { analytics.optIn() } }) { Text("Opt in") }
                FilledTonalButton(onClick = { Calls.run("optOut()") { analytics.optOut() } }) { Text("Opt out") }
                OutlinedButton(onClick = {
                    Calls.run("clearLocalData()") {
                        analytics.clearLocalData()
                        app.personCleared()
                    }
                }) { Text("Clear local data") }
                OutlinedButton(onClick = {
                    val old = analytics.deviceId
                    Calls.run("Forgotten; erase $old on the server") {
                        analytics.optOut()
                        analytics.clearLocalData()
                        app.personCleared()
                    }
                }) { Text("Forget me") }
            }
            Expect("Opt out: nothing new of any kind, the open session ends silently, queued events are still sent. Opt in: resumes, no event.")
            Expect("While PENDING (Settings): events are stored, not sent; opt in sends them, opt out deletes them.")
            Expect("Clear local data: deletes queued events and the person, renews deviceId, keeps the consent choice.")
        }
    }
}

private fun parse(text: String): Map<String, Any?> =
    text
        .split(',')
        .mapNotNull { pair -> pair.split('=', limit = 2).map { it.trim() }.takeIf { it.size == 2 && it[0].isNotEmpty() } }
        .associate { (k, v) -> k to (v.toIntOrNull() ?: v.toDoubleOrNull() ?: v.toBooleanStrictOrNull() ?: v) }
