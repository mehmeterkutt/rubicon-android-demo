package com.rubicon.demo.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A received event, as the fake server stored it. */
data class ServerEvent(
    val index: Int,
    val json: JSONObject,
) {
    val label: String get() = json.optString("event").ifEmpty { json.optString("name").ifEmpty { json.optString("type") } }
    val type: String get() = json.optString("type")
    val source: String get() = json.optString("source")
}

/**
 * Talks to the fake backend's control endpoints (`fake-server/server.py`), separately from the SDK:
 * the SDK's own requests are what is being tested. Plain HttpURLConnection, on the IO dispatcher.
 */
class FakeBackend(
    private val base: String,
) {
    data class Response(
        val code: Int,
        val body: String,
    )

    suspend fun control(query: String = ""): Response = call("GET", "/control$query")

    suspend fun reset(): Response = call("GET", "/control/reset")

    suspend fun remoteConfig(key: String): Response = call("GET", "/control/config?key=$key")

    suspend fun saveRemoteConfig(
        key: String,
        json: JSONObject,
    ): Response = call("POST", "/control/config?key=$key", json.toString())

    suspend fun deleteRemoteConfig(key: String): Response = call("DELETE", "/control/config?key=$key")

    suspend fun events(): List<ServerEvent> {
        val array = JSONArray(call("GET", "/events").body)
        return (0 until array.length()).map { i -> array.getJSONObject(i).let { ServerEvent(it.getInt("index"), it.getJSONObject("event")) } }
    }

    suspend fun clearEvents(): Response = call("GET", "/events/clear")

    suspend fun requests(): List<JSONObject> {
        val array = JSONArray(call("GET", "/requests").body)
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private suspend fun call(
        method: String,
        path: String,
        body: String? = null,
    ): Response =
        withContext(Dispatchers.IO) {
            val connection = URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 3_000
                connection.readTimeout = 5_000
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { it.write(body.toByteArray()) }
                }
                val code = connection.responseCode
                val text = (if (code < 400) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                Response(code, text)
            } finally {
                connection.disconnect()
            }
        }
}
