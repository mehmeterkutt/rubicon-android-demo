package com.rubicon.demo.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Stand-in implementation: records every call, keeps a little state, sends nothing. */
class LoggingAnalytics : RubiconAnalytics {

    private val _state = MutableStateFlow(AnalyticsState())
    override val state: StateFlow<AnalyticsState> = _state

    override fun track(name: String, properties: Map<String, Any?>) {
        if (dropIfOptedOut("track($name)")) return
        _state.update { it.copy(eventsTracked = it.eventsTracked + 1) }
        Diagnostics.log(LogLevel.DEBUG, "track  $name ${format(properties)}")
    }

    override fun screen(name: String, properties: Map<String, Any?>) {
        if (dropIfOptedOut("screen($name)")) return
        _state.update { it.copy(eventsTracked = it.eventsTracked + 1) }
        Diagnostics.log(LogLevel.DEBUG, "screen $name ${format(properties)}")
    }

    override fun identify(userId: String, traits: Map<String, Any?>) {
        val current = _state.value.userId
        if (current != null && current != userId) {
            Diagnostics.log(LogLevel.WARN, "identify($userId) while already identified as $current; call reset() first")
            return
        }
        _state.update { it.copy(userId = userId) }
        Diagnostics.log(LogLevel.INFO, "identify $userId ${format(traits)}")
    }

    override fun reset() {
        _state.update { it.copy(userId = null, superProperties = emptyMap()) }
        Diagnostics.log(LogLevel.INFO, "reset: identity and super properties cleared")
    }

    override fun register(key: String, value: Any?) {
        _state.update { it.copy(superProperties = it.superProperties + (key to value)) }
        Diagnostics.log(LogLevel.INFO, "register $key=$value")
    }

    override fun unregister(key: String) {
        _state.update { it.copy(superProperties = it.superProperties - key) }
        Diagnostics.log(LogLevel.INFO, "unregister $key")
    }

    override fun flush() {
        _state.update { it.copy(flushesRequested = it.flushesRequested + 1) }
        Diagnostics.log(LogLevel.INFO, "flush requested (no transport yet)")
    }

    override fun optOut() {
        _state.update { it.copy(optedOut = true) }
        Diagnostics.log(LogLevel.WARN, "optOut: new events will be dropped")
    }

    override fun optIn() {
        _state.update { it.copy(optedOut = false) }
        Diagnostics.log(LogLevel.INFO, "optIn: tracking resumed")
    }

    override fun clearLocalData() {
        _state.update { AnalyticsState(optedOut = it.optedOut) }
        Diagnostics.log(LogLevel.WARN, "clearLocalData: queue and identity removed, opt-out preserved")
    }

    fun deepLink(url: String) = _state.update { it.copy(lastDeepLink = url) }

    private fun dropIfOptedOut(call: String): Boolean {
        if (!_state.value.optedOut) return false
        Diagnostics.log(LogLevel.DEBUG, "$call dropped: opted out")
        return true
    }

    private fun format(map: Map<String, Any?>): String =
        if (map.isEmpty()) "" else map.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
}
