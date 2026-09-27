package com.rubicon.demo.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Small connectivity check towards the fake backend, so emulator-to-Mac networking can be verified before the SDK exists. */
object BackendProbe {
    suspend fun ping(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(baseUrl.trimEnd('/') + "/control")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            try {
                val code = conn.responseCode
                val body = (if (code < 400) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
                "HTTP $code  ${body.take(120)}"
            } finally {
                conn.disconnect()
            }
        }
    }
}
