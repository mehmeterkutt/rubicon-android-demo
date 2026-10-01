package com.rubicon.demo.config

import com.rubicon.analytics.BeforeSend
import com.rubicon.analytics.EventSource
import com.rubicon.analytics.IdentifyEvent
import com.rubicon.analytics.ScreenEvent
import com.rubicon.analytics.TrackEvent

/**
 * Ready-made `beforeSend` hooks that show each rule of Public API 4. They run in this order.
 */
enum class HookPreset(
    val title: String,
    val description: String,
) {
    MASK_EMAIL(
        "Mask email values",
        "Any event: an `email` property or trait keeps its key, its value becomes \"***\". Allowed for automatic events too.",
    ),
    DROP_DEBUG_EVENTS(
        "Drop \"Debug Event\"",
        "Returns null for the app's \"Debug Event\": dropped and counted as before_send.",
    ),
    MASK_RESET_TOKEN(
        "Mask deep link tokens",
        "Deep Link Opened: /reset-password/<token> becomes /reset-password/:token (Privacy 5b).",
    ),
    TAG_APP_EVENTS(
        "Tag app events",
        "Adds hooked=true to the app's track and screen events (an app event may change freely).",
    ),
    THROW_ON_HOOK_TEST(
        "Throw on \"Hook Test\"",
        "Throws for the event \"Hook Test\": the event is dropped (fail-closed); with debug on the SDK throws.",
    ),
    RENAME_APP_OPENED(
        "Rename Application Opened",
        "Breaks the rule for automatic events: they are dropped (HOOK_VIOLATION); with debug on the app crashes on purpose.",
    ),
    ;

    companion object {
        private val resetToken = Regex("/reset-password/[^/]+")

        fun hooks(selected: Set<HookPreset>): List<BeforeSend> = entries.filter { it in selected }.map { it.hook() }
    }

    private fun hook(): BeforeSend =
        when (this) {
            MASK_EMAIL ->
                BeforeSend { e ->
                    when (e) {
                        is TrackEvent -> if ("email" in e.properties) e.copy(properties = e.properties + ("email" to "***")) else e
                        is ScreenEvent -> if ("email" in e.properties) e.copy(properties = e.properties + ("email" to "***")) else e
                        is IdentifyEvent -> if ("email" in e.traits) e.copy(traits = e.traits + ("email" to "***")) else e
                    }
                }
            DROP_DEBUG_EVENTS -> BeforeSend { e -> if (e is TrackEvent && e.source == EventSource.APP && e.name == "Debug Event") null else e }
            MASK_RESET_TOKEN ->
                BeforeSend { e ->
                    val path = (e as? TrackEvent)?.properties?.get("path") as? String
                    if (e is TrackEvent && e.name == "Deep Link Opened" && path != null) {
                        e.copy(properties = e.properties + ("path" to path.replace(resetToken, "/reset-password/:token")))
                    } else {
                        e
                    }
                }
            TAG_APP_EVENTS ->
                BeforeSend { e ->
                    when {
                        e.source != EventSource.APP -> e
                        e is TrackEvent -> e.copy(properties = e.properties + ("hooked" to true))
                        e is ScreenEvent -> e.copy(properties = e.properties + ("hooked" to true))
                        else -> e
                    }
                }
            THROW_ON_HOOK_TEST ->
                BeforeSend { e ->
                    if (e is TrackEvent && e.name == "Hook Test") throw IllegalStateException("A hook failed on purpose")
                    e
                }
            RENAME_APP_OPENED ->
                BeforeSend { e -> if (e is TrackEvent && e.name == "Application Opened") e.copy(name = "App Opened") else e }
        }
}
