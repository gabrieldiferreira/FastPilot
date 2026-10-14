package com.fastpilot.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.Lap
import com.fastpilot.model.Session
import com.fastpilot.network.OpenF1Api
import com.fastpilot.testing.mockHttpClient
import com.fastpilot.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * TCC I §4.5: data of a finished session is downloaded once and kept (also offline),
 * live data is never cached, and race data is deleted 30 days after it was downloaded.
 */
class FinishedSessionCacheTest {

    private val now = Clock.System.now()
    private val db = FastPilotDB(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FastPilotDB.Schema.create(it) })
    private var requests = 0

    private val api = OpenF1Api(mockHttpClient { request ->
        requests++
        when {
            request.url.encodedPath.endsWith("/laps") ->
                respondJson("""[{"driver_number":1,"lap_number":1,"lap_duration":90.0}]""")
            request.url.encodedPath.endsWith("/car_data") ->
                respondJson("""[{"driver_number":1,"date":"2024-12-08T13:05:00+00:00","speed":300}]""")
            else -> respondJson("[]")
        }
    })

    private fun session(key: Int, end: Instant) =
        Session(sessionKey = key, dateStart = "${end - 2.hours}", dateEnd = "$end")

    private val finished = session(1, end = now - 3.days)
    private val endedRecently = session(2, end = now - 3.hours)
    private val live = session(3, end = now + 1.hours)

    init {
        api.sessionRegistry.track(listOf(finished, endedRecently, live))
    }

    private fun cacheLap(sessionKey: Int, downloadedAgo: Duration) {
        db.fastPilotDBQueries.insertLap(
            driver_number = 1, session_key = sessionKey.toLong(), lap_number = 1, lap_duration = 91.0,
            sector_1 = null, sector_2 = null, sector_3 = null, i1_speed = null, i2_speed = null,
            st_speed = null, is_pit_out_lap = 0, date_start = null,
            cached_at = (now - downloadedAgo).toEpochMilliseconds()
        )
    }

    private fun lap(sessionKey: Int) = Lap(driverNumber = 1, lapNumber = 1, lapDuration = 90.0, dateStart = "2024-12-08T13:03:00+00:00", sessionKey = sessionKey)

    @Test
    fun `laps downloaded after the session ended do not expire`() = runTest {
        // Older than the 1-hour TTL, but downloaded two days after the end
        cacheLap(finished.sessionKey, downloadedAgo = 1.days)

        val laps = LapRepository(api, db).getLaps(finished.sessionKey, driverNumber = 1)

        assertEquals(91.0, laps.single().lapDuration)
        assertEquals(0, requests)
    }

    @Test
    fun `laps downloaded during the session are downloaded again`() = runTest {
        // Downloaded while the session was running: incomplete
        cacheLap(endedRecently.sessionKey, downloadedAgo = 4.hours)

        val laps = LapRepository(api, db).getLaps(endedRecently.sessionKey, driverNumber = 1)

        assertEquals(90.0, laps.single().lapDuration)
        assertEquals(1, requests)
    }

    @Test
    fun `telemetry of a finished session is downloaded once`() = runTest {
        val repository = TelemetryRepository(api, db)

        repository.getLapCarData(finished.sessionKey, lap(finished.sessionKey))
        val second = repository.getLapCarData(finished.sessionKey, lap(finished.sessionKey))

        assertEquals(300, second.single().speed)
        assertEquals(1, requests)
    }

    @Test
    fun `telemetry of a live session is not cached`() = runTest {
        val repository = TelemetryRepository(api, db)

        repository.getLapCarData(live.sessionKey, lap(live.sessionKey))
        repository.getLapCarData(live.sessionKey, lap(live.sessionKey))

        assertEquals(2, requests)
    }

    @Test
    fun `race data older than 30 days is deleted`() = runTest {
        cacheLap(finished.sessionKey, downloadedAgo = 31.days)
        cacheLap(endedRecently.sessionKey, downloadedAgo = 29.days)

        CacheMaintenance(db).purgeExpired(now)

        val left = db.fastPilotDBQueries.selectLapsByDriverAndSession(1, finished.sessionKey.toLong()).executeAsList() +
            db.fastPilotDBQueries.selectLapsByDriverAndSession(1, endedRecently.sessionKey.toLong()).executeAsList()
        assertEquals(listOf(endedRecently.sessionKey.toLong()), left.map { it.session_key })
        assertTrue(requests == 0)
    }
}
