package com.rubicon.demo.sdk

import android.util.Log
import com.rubicon.analytics.LogLevel
import com.rubicon.analytics.RubiconLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class LogLine(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val message: String,
    val throwable: String?,
)

/**
 * The SDK's `logging.logger` (Logging 1): every line goes to the in-app log and to Logcat with the
 * tag `Rubicon`, as the SDK's own default would. The demo's own notes use [note].
 */
class SdkLog : RubiconLogger {
    private val ids = AtomicLong()
    private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines

    override fun log(
        level: LogLevel,
        message: String,
        throwable: Throwable?,
    ) {
        when (level) {
            LogLevel.DEBUG -> Log.d(TAG, message, throwable)
            LogLevel.INFO -> Log.i(TAG, message, throwable)
            LogLevel.WARN -> Log.w(TAG, message, throwable)
            LogLevel.ERROR -> Log.e(TAG, message, throwable)
            LogLevel.OFF -> return
        }
        val line = LogLine(ids.incrementAndGet(), synchronized(format) { format.format(Date()) }, level, message, throwable?.let { "${it.javaClass.simpleName}: ${it.message}" })
        _lines.update { (listOf(line) + it).take(MAX) }
    }

    /** A line from the demo itself, marked as such. */
    fun note(message: String) = log(LogLevel.INFO, "[demo] $message", null)

    fun clear() {
        _lines.value = emptyList()
    }

    companion object {
        const val TAG = "Rubicon"
        private const val MAX = 600
    }
}
