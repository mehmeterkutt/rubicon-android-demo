package com.rubicon.demo

import android.app.Application
import com.rubicon.demo.analytics.Diagnostics
import com.rubicon.demo.analytics.LogLevel
import com.rubicon.demo.analytics.LoggingAnalytics
import com.rubicon.demo.analytics.RubiconAnalytics

/**
 * Application entry point. The analytics SDK is created here, once, and exposed to the UI through
 * [analytics]. Today that is [LoggingAnalytics], a stand-in that only records calls; it will be
 * replaced by the real SDK instance without touching the screens.
 */
class RubiconApp : Application() {

    lateinit var analytics: RubiconAnalytics
        private set

    override fun onCreate() {
        super.onCreate()
        analytics = LoggingAnalytics()
        Diagnostics.log(LogLevel.INFO, "Application created (${packageName})")
    }

    companion object {
        const val LOG_TAG = "Rubicon"
    }
}
