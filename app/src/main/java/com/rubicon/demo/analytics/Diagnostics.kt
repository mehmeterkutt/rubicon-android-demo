package com.rubicon.demo.analytics

import android.util.Log
import com.rubicon.demo.RubiconApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel { DEBUG, INFO, WARN, ERROR }

data class LogEntry(val id: Long, val time: String, val level: LogLevel, val message: String)

/**
 * In-app log sink. Everything the analytics layer does is mirrored here so the Diagnostics screen
 * can show it without a cable and Logcat. Later the SDK's Logger and onError hooks will feed this.
 */
object Diagnostics {
    private const val MAX_ENTRIES = 500
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val ids = AtomicLong()

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries

    fun log(level: LogLevel, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d(RubiconApp.LOG_TAG, message)
            LogLevel.INFO -> Log.i(RubiconApp.LOG_TAG, message)
            LogLevel.WARN -> Log.w(RubiconApp.LOG_TAG, message)
            LogLevel.ERROR -> Log.e(RubiconApp.LOG_TAG, message)
        }
        val entry = LogEntry(ids.incrementAndGet(), fmt.format(Date()), level, message)
        _entries.update { (listOf(entry) + it).take(MAX_ENTRIES) }
    }

    fun clear() = _entries.update { emptyList() }
}
