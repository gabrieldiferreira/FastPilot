package com.fastpilot.network

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient

/**
 * Drops responses kept by the platform's own HTTP cache (Jolpica sends max-age=600).
 * Only iOS has one: NSURLSession uses the shared NSURLCache; OkHttp and CIO have none
 * configured, and the browser cache is disabled by the Web benchmark driver.
 */
expect fun clearPlatformHttpCache()
