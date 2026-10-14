package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates a configured HTTP client with JSON serialization and logging.
 * Delegates to [createPlatformHttpClient] which returns a bare platform client,
 * then installs shared plugins on top of it.
 */
fun createHttpClient(): HttpClient {
    return createPlatformHttpClient().config {
        installFastPilotPlugins()
    }
}

/** Plugins shared by every platform's client (also used by tests with a mock engine). */
fun HttpClientConfig<*>.installFastPilotPlugins() {
    // The free OpenF1 tier allows ~30 requests per minute and answers 429 above it;
    // Jolpica occasionally answers 5xx. OpenF1 always sends "Retry-After: 60" although the
    // limit frees up within seconds, so the header is ignored: 2, 4, 8, 16 and 16 s (46 s).
    // Installed before HttpTimeout so the timeout applies to each attempt, not to the waits.
    install(HttpRequestRetry) {
        maxRetries = 5
        retryIf { _, response ->
            response.status == HttpStatusCode.TooManyRequests || response.status.value in 500..599
        }
        exponentialDelay(base = 2.0, baseDelayMs = 2_000, maxDelayMs = 16_000, respectRetryAfterHeader = false)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 60000
        connectTimeoutMillis = 60000
        socketTimeoutMillis = 60000
    }
    // Installed after the retry plugin so every attempt is counted
    install(RequestCounter)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
            encodeDefaults = false
        })
    }
    install(Logging) {
        level = LogLevel.NONE // Set to LogLevel.INFO for debugging
    }
}

/** Counts HTTP attempts and rate-limited (429) answers, read by the load benchmark. */
object RequestStats {
    var requests: Int = 0
        private set
    var rateLimited: Int = 0
        private set

    internal fun record(status: HttpStatusCode) {
        requests++
        if (status == HttpStatusCode.TooManyRequests) rateLimited++
    }
}

private val RequestCounter = createClientPlugin("RequestCounter") {
    on(Send) { request ->
        val call = proceed(request)
        RequestStats.record(call.response.status)
        call
    }
}
