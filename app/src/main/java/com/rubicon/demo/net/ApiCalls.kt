package com.rubicon.demo.net

import com.rubicon.analytics.Analytics
import com.rubicon.analytics.network.RubiconNetworkInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** One of the app's own HTTP calls, each showing a rule of the interceptor (Privacy 5c, Lifecycle 1). */
enum class ApiCall(
    val title: String,
    val expectation: String,
) {
    ORDERS("GET with ids and a token", "Captured: path /api/users/:id/orders/:id, no query, no fragment, status 200"),
    POST_JSON("POST a JSON body", "Captured with request_bytes; the body itself is never read"),
    NOT_FOUND("404 answer", "Captured with status_code 404"),
    SERVER_ERROR("500 answer", "Captured with status_code 500"),
    TIMEOUT("Slow answer (timeout)", "Captured with error timeout and no status_code; the call fails as usual"),
    UNKNOWN_HOST("Unknown host", "Captured only if the host is configured: unknown-host.invalid is not, so nothing"),
    REFUSED("Connection refused", "Captured with error connect (localhost:1)"),
    CANCELED("Canceled call", "Captured with error canceled"),
    SDK_HOST("Call to the SDK's own host", "Never captured, whatever the configured hosts"),
    ;

    fun url(
        apiBase: String,
        sdkHost: String,
    ): String {
        val base = apiBase.trimEnd('/')
        return when (this) {
            ORDERS -> "$base/api/users/12345/orders/3f2a9c1e-0b1c-4d2e-8f3a-7b2d9c1e0b1c?token=secret#section"
            POST_JSON -> "$base/api/cart"
            NOT_FOUND -> "$base/api/status/404"
            SERVER_ERROR -> "$base/api/status/500"
            TIMEOUT -> "$base/api/slow?ms=5000"
            UNKNOWN_HOST -> "http://unknown-host.invalid/api/ping"
            REFUSED -> "http://localhost:1/api/ping"
            CANCELED -> "$base/api/slow?ms=3000"
            SDK_HOST -> "${sdkHost.trimEnd('/')}/api/ping"
        }
    }
}

/** The app's own OkHttp client, with the SDK's interceptor as the first application interceptor. */
class ApiCalls(
    analytics: Analytics,
    hosts: Set<String>,
    maskPathIds: Boolean,
    private val apiBase: String,
    private val sdkHost: String,
) {
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor(RubiconNetworkInterceptor(analytics, hosts, maskPathIds))
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

    /** Makes [call] and describes its outcome as the app sees it. */
    suspend fun run(call: ApiCall): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(call.url(apiBase, sdkHost))
            if (call == ApiCall.POST_JSON) builder.post("""{"sku":"A1","quantity":2}""".toRequestBody("application/json".toMediaType()))
            val started = System.nanoTime()
            val okCall = client.newCall(builder.build())
            if (call == ApiCall.CANCELED) {
                Thread {
                    Thread.sleep(300)
                    okCall.cancel()
                }.start()
            }
            try {
                okCall.execute().use { response ->
                    val bytes = response.body?.bytes()?.size ?: 0
                    "HTTP ${response.code}, $bytes bytes read by the app, ${elapsed(started)} ms"
                }
            } catch (t: Throwable) {
                "Failed: ${t.javaClass.simpleName} after ${elapsed(started)} ms"
            }
        }

    private fun elapsed(started: Long) = (System.nanoTime() - started) / 1_000_000
}
