package com.rubicon.demo.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubicon.analytics.Autocapture
import com.rubicon.demo.LegacyLinkActivity
import com.rubicon.demo.ProductActivity
import com.rubicon.demo.RemoteService
import com.rubicon.demo.SettingsActivity
import com.rubicon.demo.demo
import com.rubicon.demo.net.ApiCall
import com.rubicon.demo.sdk.Calls
import com.rubicon.demo.ui.components.CodeLine
import com.rubicon.demo.ui.components.Expect
import com.rubicon.demo.ui.components.Flow
import com.rubicon.demo.ui.components.KeyValueLine
import com.rubicon.demo.ui.components.SectionCard
import com.rubicon.demo.ui.components.StatusPill
import com.rubicon.demo.ui.theme.Danger
import com.rubicon.demo.ui.theme.Success
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.demo
    val enabled = app.settings.autocapture
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionCard("Automatic capture", "What the SDK captures by itself (Lifecycle 6); change it in Settings.") {
            Flow { Autocapture.entries.forEach { StatusPill(it.name.lowercase(), if (it in enabled) Success else Danger) } }
        }
        Screens(context)
        DeepLinks(context)
        Lifecycle(context)
        Network()
    }
}

@Composable
private fun Screens(context: Context) {
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Calls.show(if (granted) "Granted: the home screen resumed without a new screen view" else "Denied: the home screen resumed without a new screen view")
        }
    SectionCard("Screens", "Lifecycle 3: one screen view per activity shown; tabs send their own with screen() (Settings).") {
        Flow {
            Button(onClick = { context.startActivity(Intent(context, ProductActivity::class.java)) }) { Text("Annotated activity") }
            FilledTonalButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) { Text("Class-named activity") }
            FilledTonalButton(onClick = { context.startActivity(Intent(context, LegacyLinkActivity::class.java)) }) { Text("Plain Activity") }
            if (Build.VERSION.SDK_INT >= 33) {
                OutlinedButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("System dialog") }
            }
        }
        Expect("product_detail (annotation), Settings (class), LegacyLink (class); back here: home again.")
        Expect("A system dialog pauses and resumes this screen: no second home. Rotate the device: no new screen view either.")
    }
}

@Composable
private fun DeepLinks(context: Context) {
    fun open(uri: String, referrer: Uri? = null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(context.packageName)
        if (referrer != null) intent.putExtra(Intent.EXTRA_REFERRER, referrer)
        context.startActivity(intent)
    }
    SectionCard("Deep links", "Privacy 5b: scheme, host, path, utm_* and the referrer only. DEEP_LINKS is off by default in the SDK, on in this demo.") {
        Flow {
            Button(onClick = { open("rubicon://open/promo?utm_source=newsletter&utm_campaign=fall&token=secret#top", Uri.parse("android-app://com.google.android.gm")) }) {
                Text("Campaign link")
            }
            FilledTonalButton(onClick = { open("rubicon://open/reset-password/abc123?utm_medium=email") }) { Text("Token in path") }
            FilledTonalButton(onClick = { open("rubicon://legacy/offer?utm_content=banner") }) { Text("setIntent activity") }
        }
        Expect("This screen is singleTop: a link while it is open arrives through onNewIntent (ComponentActivity). Never token, never #top.")
        Expect("Token in path: the mask hook turns it into /reset-password/:token. setIntent activity: a plain Activity that calls setIntent.")
        Text("From a terminal, with the app closed (cold start):", style = MaterialTheme.typography.bodySmall)
        CodeLine("adb shell am start -a android.intent.action.VIEW -d \"rubicon://open/promo?utm_source=adb&secret=1\"")
    }
}

@Composable
private fun Lifecycle(context: Context) {
    val app = context.demo
    SectionCard(
        "Lifecycle, sessions and the process",
        "Lifecycle 1, 2 and 4; Session 3 to 6. Session timeout: ${app.settings.sessionTimeoutMs / 1000} s.",
    ) {
        Expect("Home key: Application Backgrounded and an upload. Back: Application Opened (from_background true), same session within the timeout; after it: Session Ended (backdated) and a new session.")
        Expect("First launch: Application Installed. Install a higher versionCode over it: Application Updated with the previous version.")
        Flow {
            OutlinedButton(onClick = {
                app.analytics.track("Before Kill", mapOf("note" to "stored, then the process dies"))
                Process.killProcess(Process.myPid())
            }) { Text("Track, then kill") }
            OutlinedButton(onClick = {
                repeat(300) { app.analytics.track("Killed While Writing", mapOf("index" to it)) }
                Process.killProcess(Process.myPid())
            }) { Text("300 events, kill at once") }
            OutlinedButton(onClick = {
                context.startService(Intent(context, RemoteService::class.java))
                Calls.show("Tracked from the :remote process: nothing may arrive")
            }) { Text(":remote process") }
            OutlinedButton(onClick = { app.restart() }) { Text("Restart app") }
        }
        Expect("After a kill, open the app again: everything stored arrives once; events still in memory at the kill are lost (Queue 1).")
        Expect(":remote: the SDK does nothing in another process, writes no file, sends nothing (Disk 2d).")
        Text("Offline and online, from a terminal:", style = MaterialTheme.typography.bodySmall)
        CodeLine("adb shell svc wifi disable && adb shell svc data disable")
        CodeLine("adb shell svc wifi enable && adb shell svc data enable")
    }
}

@Composable
private fun Network() {
    val app = LocalContext.current.demo
    val scope = rememberCoroutineScope()
    val results = remember { mutableStateMapOf<ApiCall, String>() }
    SectionCard(
        "HTTP calls",
        "Privacy 5c: the app's own OkHttp client with RubiconNetworkInterceptor; Network Request events appear on the Logs and Server tabs.",
    ) {
        KeyValueLine("API base", app.settings.apiBase)
        KeyValueLine("captured hosts", app.settings.captureHosts.joinToString().ifEmpty { "none" })
        KeyValueLine("mask path ids", app.settings.maskPathIds.toString())
        Text("The API calls go to localhost, another name for the fake server; once per emulator start:", style = MaterialTheme.typography.bodySmall)
        CodeLine("adb reverse tcp:8080 tcp:8080")
        ApiCall.entries.forEach { call ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                OutlinedButton(onClick = {
                    results[call] = "running…"
                    scope.launch { results[call] = app.api.run(call) }
                }) { Text(call.title) }
                Expect(call.expectation)
                results[call]?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
