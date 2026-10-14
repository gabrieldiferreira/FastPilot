package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLCache

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin)

actual fun clearPlatformHttpCache() = NSURLCache.sharedURLCache.removeAllCachedResponses()
