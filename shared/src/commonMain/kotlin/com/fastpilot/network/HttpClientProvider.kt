package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates a configured HTTP client with JSON serialization and logging.
 * Delegates to [createPlatformHttpClient] which returns a bare platform client,
 * then installs shared plugins on top of it.
 */
fun createHttpClient(): HttpClient {
    return createPlatformHttpClient().config {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
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
}
