package com.rubicon.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.Autocapture
import com.rubicon.analytics.Consent
import com.rubicon.analytics.ContextField
import com.rubicon.analytics.LogLevel
import com.rubicon.demo.config.DemoSettings
import com.rubicon.demo.config.HookPreset
import com.rubicon.demo.demo
import com.rubicon.demo.ui.components.ChoiceChips
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.Field
import com.rubicon.demo.ui.components.Flow
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.MultiChips
import com.rubicon.demo.ui.components.SectionCard
import com.rubicon.demo.ui.components.SwitchRow

/**
 * Every setting of the SDK (Configuration 2), plus the demo's own. The SDK's configuration is fixed
 * once created, so these are saved and the process restarts, which also shows that the SDK keeps
 * its state across a restart.
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.demo
    var s by remember { mutableStateOf(app.settings) }
    var immediate by remember { mutableStateOf(s.immediateEvents.joinToString()) }
    var headers by remember { mutableStateOf(s.headers.entries.joinToString { "${it.key}=${it.value}" }) }
    var hosts by remember { mutableStateOf(s.captureHosts.joinToString()) }

    fun withText(): DemoSettings =
        s.copy(
            immediateEvents = immediate.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            headers = headers.split(',').mapNotNull { p -> p.split('=', limit = 2).map { it.trim() }.takeIf { it.size == 2 && it[0].isNotEmpty() } }.associate { it[0] to it[1] },
            captureHosts = hosts.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard("Apply", "The SDK reads its configuration once. Saving restarts the app with the new values.") {
            app.startupError?.let { Text("The SDK refused the last configuration: $it", color = MaterialTheme.colorScheme.error) }
            Flow {
                Button(onClick = { app.restart(withText()) }) { Text("Save and restart") }
                OutlinedButton(onClick = { app.restart(DemoSettings()) }) { Text("Defaults and restart") }
            }
        }

        SectionCard("Connection") {
            Field("API key", s.apiKey, supporting = "A new key starts a new queue; the old one is dropped as api_key_changed") { s = s.copy(apiKey = it) }
            Field("Host", s.host, supporting = "http:// works with a WARN; an invalid host disables the SDK") { s = s.copy(host = it) }
            Field("Extra headers", headers, supporting = "name=value, comma separated; the SDK's own headers cannot be replaced") { headers = it }
            SwitchRow("debug", s.debug, "Mistakes throw instead of logging; shown here as messages") { s = s.copy(debug = it) }
        }

        SectionCard("Logging") {
            ChoiceChips(LogLevel.entries, s.logLevel, { it.name.lowercase() }) { s = s.copy(logLevel = it) }
            SwitchRow("In-app logger", s.inAppLogger, "Off: the SDK's default Logcat logger, tag Rubicon") { s = s.copy(inAppLogger = it) }
            SwitchRow("Send SDK Diagnostics", s.sendDiagnostics, "Counts of drops and issues, at most hourly") { s = s.copy(sendDiagnostics = it) }
        }

        SectionCard("Flushing and sessions", "Queue 3, Session 4.") {
            Field("flush.at", s.flushAt.toString(), supporting = "1 to 100; outside is clamped with a WARN") { s = s.copy(flushAt = it.toIntOrNull() ?: s.flushAt) }
            Field("flush.intervalMs", s.flushIntervalMs.toString(), supporting = "1000 to 600000") { s = s.copy(flushIntervalMs = it.toLongOrNull() ?: s.flushIntervalMs) }
            Field("flush.immediateEvents", immediate, supporting = "Comma separated event names") { immediate = it }
            Field("session.backgroundTimeoutMs", s.sessionTimeoutMs.toString(), supporting = "10000 to 7200000") {
                s = s.copy(sessionTimeoutMs = it.toLongOrNull() ?: s.sessionTimeoutMs)
            }
        }

        SectionCard("Automatic capture", "autocapture.enabled (Lifecycle 6).") {
            MultiChips(Autocapture.entries, s.autocapture, { it.name.lowercase() }) { s = s.copy(autocapture = it) }
            SwitchRow("Tab screens with screen()", s.tabScreens, "The tabs send tab_<name>; the activity's screen stays automatic") { s = s.copy(tabScreens = it) }
        }

        SectionCard("Context fields switched off", "context.disabledFields: never read, never sent (Context 6).") {
            MultiChips(ContextField.entries, s.disabledFields, { it.name.lowercase() }) { s = s.copy(disabledFields = it) }
            OutlinedButton(onClick = {
                s = s.copy(disabledFields = s.disabledFields + setOf(ContextField.NETWORK_CARRIER, ContextField.LOCALE, ContextField.TIMEZONE))
            }) { Text("Child-directed checklist") }
        }

        SectionCard("Consent", "Privacy 2 and 4.") {
            ChoiceChips(Consent.entries, s.defaultConsent, { it.name.lowercase() }) { s = s.copy(defaultConsent = it) }
            SwitchRow("Store the user's choice", s.persistConsent, "Off: the app owns consent and calls optIn or optOut at every launch") { s = s.copy(persistConsent = it) }
        }

        SectionCard("beforeSend hooks", "Public API 4.") {
            HookPreset.entries.forEach { hook ->
                SwitchRow(hook.title, hook in s.hooks, hook.description) { on -> s = s.copy(hooks = if (on) s.hooks + hook else s.hooks - hook) }
            }
        }

        SectionCard("Network interceptor", "RubiconNetworkInterceptor(analytics, hosts, maskPathIds) on the app's own OkHttp client.") {
            Field("API base", s.apiBase, supporting = "The app's own calls; needs adb reverse tcp:8080 tcp:8080 for localhost") { s = s.copy(apiBase = it) }
            Field("Captured hosts", hosts, supporting = "Exact names or *.domain.tld, comma separated") { hosts = it }
            SwitchRow("Mask ids in paths", s.maskPathIds) { s = s.copy(maskPathIds = it) }
        }

        SectionCard("This build") {
            val info = LocalContext.current.packageManager.getPackageInfo(LocalContext.current.packageName, 0)
            KeyValueLine("app version", "${info.versionName}")
            KeyValueLine("package", LocalContext.current.packageName)
            Expect("Install a build with a higher versionCode over this one to see Application Updated.")
        }
    }
}
