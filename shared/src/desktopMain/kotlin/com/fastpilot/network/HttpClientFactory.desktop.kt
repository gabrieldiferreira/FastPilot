package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)

actual fun clearPlatformHttpCache() {}
