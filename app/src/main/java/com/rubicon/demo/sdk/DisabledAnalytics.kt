package com.rubicon.demo.sdk

import com.rubicon.analytics.Analytics

/**
 * Stands in when the SDK refused the configuration with `debug` on (it throws then), so the demo
 * keeps running and shows why. With `debug` off the SDK returns its own instance that does nothing.
 */
class DisabledAnalytics : Analytics {
    override val deviceId: String = ""

    override fun track(
        name: String,
        properties: Map<String, Any?>,
    ): Unit = Unit

    override fun screen(
        name: String,
        properties: Map<String, Any?>,
    ): Unit = Unit

    override fun identify(
        userId: String,
        traits: Map<String, Any?>,
    ): Unit = Unit

    override fun reset(): Unit = Unit

    override fun register(
        key: String,
        value: Any?,
    ): Unit = Unit

    override fun unregister(key: String): Unit = Unit

    override fun flush(): Unit = Unit

    override fun optOut(): Unit = Unit

    override fun optIn(): Unit = Unit

    override fun isOptedOut(): Boolean = true

    override fun clearLocalData(): Unit = Unit
}
