package com.fastpilot.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

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
    // The free OpenF1 tier allows 30 requests per minute and 3 per second (see OpenF1RateLimit);
    // Jolpica occasionally answers 5xx. OpenF1 always sends "Retry-After: 60" although the
    // limit frees up within seconds, so the header is ignored: 2, 4, 8, 16 and 16 s (46 s).
    // Installed before HttpTimeout so the timeout applies to each attempt, not to the waits.
    install(HttpRequestRetry) {
        maxRetries = 5
        retryIf { _, response ->
            response.status == HttpStatusCode.TooManyRequests || response.status.value in 500..599
        }
        // A request that fails without an answer (no network, DNS) is retried once, for a brief
        // drop. Ktor retries exceptions by default, which with the delays above made a screen
        // without network wait 46 s per request before showing the error. Timeouts are not retried.
        retryOnExceptionIf { _, cause ->
            retryCount <= 1 && cause !is HttpRequestTimeoutException &&
                cause !is ConnectTimeoutException && cause !is SocketTimeoutException
        }
        exponentialDelay(base = 2.0, baseDelayMs = 2_000, maxDelayMs = 16_000, respectRetryAfterHeader = false)
    }
    // Plugins installed first wrap the later ones. The rate limiter goes after the retry plugin, so
    // every attempt waits for its turn, and before HttpTimeout, so the time waiting for the quota
    // (over a minute when many screens open in a row) does not count against the request timeout
    install(OpenF1RateLimit)
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

/**
 * Lets at most [permits] requests start in any [windowMs] window, delaying the others.
 * A request that waits is cheaper than a 429, whose retry waits at least 2 s.
 *
 * When requests are waiting, the newest goes first: the screen the user just opened is served
 * before the backlog a screen opened earlier left in the queue (with 30 requests a minute, a
 * screen with many requests used to hold the next one back for a minute or more).
 */
internal class RateLimiter(
    private val permits: Int,
    private val windowMs: Long,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private val mutex = Mutex()
    private val starts = ArrayDeque<TimeMark>()
    /** Requests waiting for a permit, oldest first; only the last one may take the next permit. */
    private val waiting = ArrayList<Any>()
    /** Bumped whenever [waiting] changes, to wake the requests waiting for their turn. */
    private val changes = MutableStateFlow(0L)

    suspend fun acquire() {
        val ticket = Any()
        mutex.withLock { waiting.add(ticket); changes.value++ }
        var granted = false
        // The start this request already waited a whole window for: it counts as out of the window
        var waitedFor: TimeMark? = null
        try {
            while (true) {
                val seen = changes.value
                // null: a newer request goes first; 0: permit taken; otherwise ms until one frees up
                val waitMs: Long? = mutex.withLock {
                    if (waiting.last() !== ticket) return@withLock null
                    while (starts.isNotEmpty() && (starts.first() === waitedFor ||
                            starts.first().elapsedNow().inWholeMilliseconds >= windowMs)) {
                        starts.removeFirst()
                    }
                    if (starts.size < permits) {
                        starts.addLast(timeSource.markNow())
                        waiting.remove(ticket)
                        granted = true
                        changes.value++
                        0L
                    } else {
                        waitedFor = starts.first()
                        windowMs - starts.first().elapsedNow().inWholeMilliseconds
                    }
                }
                when {
                    granted -> return
                    waitMs == null -> changes.first { it != seen }
                    else -> delay(waitMs)
                }
            }
        } finally {
            // A cancelled request leaves the queue, so the next one is not held back by it
            if (!granted) withContext(NonCancellable) {
                mutex.withLock { waiting.remove(ticket); changes.value++ }
            }
        }
    }
}

/**
 * The free OpenF1 tier has two limits, both answered with 429 and "Retry-After: 60":
 * - 3 requests per second. Bursts of 3 still got 429s (the server sees them with network
 *   jitter), so requests are spread evenly: one every 400 ms, 2.5 per second.
 * - 30 requests in any 60 s (measured: 30 answers, then 429 until the first one is 60 s old).
 *   29 per 61 s leaves a margin.
 * Waiting here is cheaper than a 429, whose retries wait 2 s or more and still count.
 * Live sessions go to the proxy (another host, the Sponsor quota) and are not limited here.
 */
private val OpenF1RateLimit = createClientPlugin("OpenF1RateLimit") {
    val perMinute = RateLimiter(permits = 29, windowMs = 61_000)
    val perSecond = RateLimiter(permits = 1, windowMs = 400)
    on(Send) { request ->
        if (request.url.host == "api.openf1.org") {
            perMinute.acquire()
            perSecond.acquire()
        }
        proceed(request)
    }
}
