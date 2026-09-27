package com.rubicon.demo.analytics

import kotlinx.coroutines.flow.StateFlow

/**
 * The surface the app programs against. It mirrors the public API we plan for the SDK
 * (see the "Public API" and "Privacy Controls" pages), so swapping in the real SDK later is a
 * one-file change.
 */
interface RubiconAnalytics {
    val state: StateFlow<AnalyticsState>

    fun track(name: String, properties: Map<String, Any?> = emptyMap())
    fun screen(name: String, properties: Map<String, Any?> = emptyMap())
    fun identify(userId: String, traits: Map<String, Any?> = emptyMap())
    fun reset()
    fun register(key: String, value: Any?)
    fun unregister(key: String)
    fun flush()
    fun optOut()
    fun optIn()
    fun clearLocalData()
}

/** Observable snapshot of what the SDK believes right now, for the UI. */
data class AnalyticsState(
    val userId: String? = null,
    val optedOut: Boolean = false,
    val superProperties: Map<String, Any?> = emptyMap(),
    val eventsTracked: Int = 0,
    val flushesRequested: Int = 0,
    val lastDeepLink: String? = null,
)
