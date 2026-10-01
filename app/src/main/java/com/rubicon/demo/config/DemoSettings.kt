package com.rubicon.demo.config

import android.content.Context
import com.rubicon.analytics.AnalyticsConfig
import com.rubicon.analytics.Autocapture
import com.rubicon.analytics.Consent
import com.rubicon.analytics.ContextField
import com.rubicon.analytics.EventObserver
import com.rubicon.analytics.LogLevel
import com.rubicon.analytics.RubiconLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything the demo can set on the SDK. The SDK's configuration is fixed once the instance exists,
 * so these are saved and applied on the next start of the process (Settings > Save and restart).
 */
data class DemoSettings(
    val apiKey: String = "rk_demo",
    val host: String = "http://10.0.2.2:8080",
    val debug: Boolean = false,
    val logLevel: LogLevel = LogLevel.DEBUG,
    /** Send the SDK's log lines to the in-app log as well as Logcat; off uses the SDK's Logcat logger. */
    val inAppLogger: Boolean = true,
    val sendDiagnostics: Boolean = true,
    val flushAt: Int = 30,
    val flushIntervalMs: Long = 60_000,
    val immediateEvents: Set<String> = setOf("Purchase Completed"),
    val sessionTimeoutMs: Long = 300_000,
    val autocapture: Set<Autocapture> = Autocapture.entries.toSet(),
    val disabledFields: Set<ContextField> = emptySet(),
    val defaultConsent: Consent = Consent.GRANTED,
    val persistConsent: Boolean = true,
    val headers: Map<String, String> = mapOf("X-Demo-Client" to "rubicon-demo"),
    val hooks: Set<HookPreset> = setOf(HookPreset.MASK_EMAIL, HookPreset.DROP_DEBUG_EVENTS, HookPreset.MASK_RESET_TOKEN),
    /** Base URL of the app's own HTTP calls; another name for the fake server, see the README. */
    val apiBase: String = "http://localhost:8080",
    val captureHosts: Set<String> = setOf("localhost"),
    val maskPathIds: Boolean = true,
    /** Each tab sends `screen("tab_<name>")`; the activity's own screen view stays automatic. */
    val tabScreens: Boolean = true,
) {
    fun toConfig(
        observer: EventObserver,
        logger: RubiconLogger,
    ): AnalyticsConfig =
        AnalyticsConfig(apiKey).also { c ->
            c.host = host
            c.debug = debug
            c.beforeSend = HookPreset.hooks(hooks)
            c.flush.at = flushAt
            c.flush.intervalMs = flushIntervalMs
            c.flush.immediateEvents = immediateEvents
            c.session.backgroundTimeoutMs = sessionTimeoutMs
            c.autocapture.enabled = autocapture
            c.context.disabledFields = disabledFields
            c.privacy.defaultConsent = defaultConsent
            c.privacy.persistConsent = persistConsent
            c.logging.level = logLevel
            c.logging.sendDiagnostics = sendDiagnostics
            c.logging.eventObserver = observer
            if (inAppLogger) c.logging.logger = logger
            c.network.headers = headers
        }

    fun toJson(): JSONObject =
        JSONObject()
            .put("apiKey", apiKey)
            .put("host", host)
            .put("debug", debug)
            .put("logLevel", logLevel.name)
            .put("inAppLogger", inAppLogger)
            .put("sendDiagnostics", sendDiagnostics)
            .put("flushAt", flushAt)
            .put("flushIntervalMs", flushIntervalMs)
            .put("immediateEvents", JSONArray(immediateEvents.toList()))
            .put("sessionTimeoutMs", sessionTimeoutMs)
            .put("autocapture", JSONArray(autocapture.map { it.name }))
            .put("disabledFields", JSONArray(disabledFields.map { it.name }))
            .put("defaultConsent", defaultConsent.name)
            .put("persistConsent", persistConsent)
            .put("headers", JSONObject(headers))
            .put("hooks", JSONArray(hooks.map { it.name }))
            .put("apiBase", apiBase)
            .put("captureHosts", JSONArray(captureHosts.toList()))
            .put("maskPathIds", maskPathIds)
            .put("tabScreens", tabScreens)

    companion object {
        private const val PREFS = "demo_settings"
        private const val KEY = "settings"

        fun load(context: Context): DemoSettings {
            val text = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return DemoSettings()
            return runCatching { fromJson(JSONObject(text)) }.getOrDefault(DemoSettings())
        }

        fun save(
            context: Context,
            settings: DemoSettings,
        ) {
            // commit, not apply: the process is killed right after to restart.
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, settings.toJson().toString()).commit()
        }

        private fun fromJson(o: JSONObject): DemoSettings {
            val d = DemoSettings()
            return DemoSettings(
                apiKey = o.optString("apiKey", d.apiKey),
                host = o.optString("host", d.host),
                debug = o.optBoolean("debug", d.debug),
                logLevel = enumOr(o.optString("logLevel"), d.logLevel),
                inAppLogger = o.optBoolean("inAppLogger", d.inAppLogger),
                sendDiagnostics = o.optBoolean("sendDiagnostics", d.sendDiagnostics),
                flushAt = o.optInt("flushAt", d.flushAt),
                flushIntervalMs = o.optLong("flushIntervalMs", d.flushIntervalMs),
                immediateEvents = o.optJSONArray("immediateEvents")?.strings()?.toSet() ?: d.immediateEvents,
                sessionTimeoutMs = o.optLong("sessionTimeoutMs", d.sessionTimeoutMs),
                autocapture = o.optJSONArray("autocapture")?.enums<Autocapture>() ?: d.autocapture,
                disabledFields = o.optJSONArray("disabledFields")?.enums<ContextField>() ?: d.disabledFields,
                defaultConsent = enumOr(o.optString("defaultConsent"), d.defaultConsent),
                persistConsent = o.optBoolean("persistConsent", d.persistConsent),
                headers = o.optJSONObject("headers")?.let { h -> h.keys().asSequence().associateWith { h.getString(it) } } ?: d.headers,
                hooks = o.optJSONArray("hooks")?.enums<HookPreset>() ?: d.hooks,
                apiBase = o.optString("apiBase", d.apiBase),
                captureHosts = o.optJSONArray("captureHosts")?.strings()?.toSet() ?: d.captureHosts,
                maskPathIds = o.optBoolean("maskPathIds", d.maskPathIds),
                tabScreens = o.optBoolean("tabScreens", d.tabScreens),
            )
        }

        private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

        private inline fun <reified E : Enum<E>> JSONArray.enums(): Set<E> = strings().mapNotNull { name -> enumValues<E>().find { it.name == name } }.toSet()

        private inline fun <reified E : Enum<E>> enumOr(
            name: String,
            default: E,
        ): E = enumValues<E>().find { it.name == name } ?: default
    }
}
