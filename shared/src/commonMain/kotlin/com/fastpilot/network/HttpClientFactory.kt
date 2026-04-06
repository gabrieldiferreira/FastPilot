package com.fastpilot.network

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(): HttpClient
