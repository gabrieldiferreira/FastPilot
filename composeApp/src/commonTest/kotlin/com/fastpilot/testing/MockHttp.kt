package com.fastpilot.testing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

// Same helper as in :shared commonTest (KMP has no test fixtures sharing between modules)

/**
 * The engine runs on [dispatcher] so that ViewModel tests driven by a TestDispatcher
 * see requests complete inside advanceUntilIdle() instead of on real threads.
 */
fun mockHttpClient(
    dispatcher: CoroutineDispatcher,
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): HttpClient = HttpClient(MockEngine(MockEngineConfig().apply {
    this.dispatcher = dispatcher
    addHandler(handler)
})) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
}

fun MockRequestHandleScope.respondJson(body: String) =
    respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
