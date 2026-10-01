package com.rubicon.demo.sdk

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Runs a call to the SDK. With `debug` on, the SDK throws on the developer's mistakes (an invalid
 * property, a blank name): the demo catches that and shows it instead of crashing, so both modes can
 * be tried. Results and notes go to [messages], shown as a snackbar.
 */
object Calls {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages

    fun show(message: String) {
        _messages.tryEmit(message)
    }

    inline fun run(
        label: String,
        block: () -> Unit,
    ) {
        try {
            block()
            show(label)
        } catch (t: Throwable) {
            show("The SDK threw ${t.javaClass.simpleName}: ${t.message}")
        }
    }
}
