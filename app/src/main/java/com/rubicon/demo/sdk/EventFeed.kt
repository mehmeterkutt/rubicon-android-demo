package com.rubicon.demo.sdk

import com.rubicon.analytics.DropReason
import com.rubicon.analytics.Event
import com.rubicon.analytics.EventObserver
import com.rubicon.analytics.EventUpdate
import com.rubicon.analytics.IdentifyEvent
import com.rubicon.analytics.ScreenEvent
import com.rubicon.analytics.TrackEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** One line of the event feed. */
data class FeedEntry(
    val id: Long,
    val time: String,
    val kind: Kind,
    val title: String,
    val detail: String,
    val messageIds: List<String>,
) {
    enum class Kind { QUEUED, SENT, DROPPED }
}

data class FeedCounts(
    val queued: Int = 0,
    val sent: Int = 0,
    val dropped: Map<DropReason, Int> = emptyMap(),
) {
    val droppedTotal: Int get() = dropped.values.sum()
}

/**
 * The SDK's `eventObserver` (Logging 4c): what became of every event, for the Logs tab. Called on
 * the SDK's worker thread, so it only appends to a state flow.
 */
class EventFeed : EventObserver {
    private val ids = AtomicLong()
    private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _entries = MutableStateFlow<List<FeedEntry>>(emptyList())
    private val _counts = MutableStateFlow(FeedCounts())

    val entries: StateFlow<List<FeedEntry>> = _entries
    val counts: StateFlow<FeedCounts> = _counts

    override fun onUpdate(update: EventUpdate) {
        val entry =
            when (update) {
                is EventUpdate.Queued -> {
                    _counts.update { it.copy(queued = it.queued + 1) }
                    FeedEntry(next(), now(), FeedEntry.Kind.QUEUED, title(update.event), detail(update.event), listOf(update.event.messageId))
                }
                is EventUpdate.Sent -> {
                    _counts.update { it.copy(sent = it.sent + update.messageIds.size) }
                    FeedEntry(next(), now(), FeedEntry.Kind.SENT, "${update.messageIds.size} sent", "the server accepted the batch", update.messageIds)
                }
                is EventUpdate.Dropped -> {
                    _counts.update { c -> c.copy(dropped = c.dropped + (update.reason to (c.dropped[update.reason] ?: 0) + update.messageIds.size)) }
                    FeedEntry(
                        next(),
                        now(),
                        FeedEntry.Kind.DROPPED,
                        "${update.messageIds.size} dropped",
                        "reason: ${update.reason.name.lowercase()}",
                        update.messageIds,
                    )
                }
            }
        _entries.update { (listOf(entry) + it).take(MAX) }
    }

    fun clear() {
        _entries.value = emptyList()
        _counts.value = FeedCounts()
    }

    private fun next() = ids.incrementAndGet()

    private fun now() = synchronized(format) { format.format(Date()) }

    private fun title(event: Event): String =
        when (event) {
            is TrackEvent -> event.name
            is ScreenEvent -> "screen ${event.name}"
            is IdentifyEvent -> "identify ${event.userId}"
        }

    private fun detail(event: Event): String {
        val values =
            when (event) {
                is TrackEvent -> event.properties
                is ScreenEvent -> event.properties
                is IdentifyEvent -> event.traits
            }
        val session = event.context.session?.id?.takeLast(6)?.let { "session …$it" } ?: "no session"
        val shown = values.entries.take(6).joinToString { "${it.key}=${short(it.value)}" }
        return "${event.source.name.lowercase()} · $session · ${values.size} ${if (values.size == 1) "property" else "properties"}" + if (shown.isEmpty()) "" else "\n$shown"
    }

    private fun short(value: Any?): String = value.toString().let { if (it.length > 40) it.take(40) + "…" else it }

    private companion object {
        const val MAX = 400
    }
}
