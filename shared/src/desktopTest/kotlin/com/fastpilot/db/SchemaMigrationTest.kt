package com.fastpilot.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Databases created by earlier versions must be upgraded in place: v1 lacks the standings
 * cache (Classificação screen), v2 the raw samples cache (telemetry and track position),
 * v3 the circuit time zone of each session.
 */
class SchemaMigrationTest {

    private val dbFile = File.createTempFile("fastpilot-migration", ".db").apply { delete() }
    private val url = "jdbc:sqlite:${dbFile.absolutePath}"

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    /** Builds a v3 database: full current schema minus the column added in 3.sqm. */
    private fun createV3Database() {
        val driver = JdbcSqliteDriver(url)
        FastPilotDB.Schema.create(driver)
        driver.execute(null, "ALTER TABLE cached_session DROP COLUMN gmt_offset", 0)
        driver.execute(null, "PRAGMA user_version = 3", 0)
        driver.close()
    }

    /** Builds a v2 database: full current schema minus what 2.sqm and 3.sqm add. */
    private fun createV2Database() {
        val driver = JdbcSqliteDriver(url)
        FastPilotDB.Schema.create(driver)
        driver.execute(null, "DROP TABLE cached_samples", 0)
        driver.execute(null, "ALTER TABLE cached_session DROP COLUMN gmt_offset", 0)
        driver.execute(null, "PRAGMA user_version = 2", 0)
        driver.close()
    }

    /** Builds a v1 database: full current schema minus what 1.sqm, 2.sqm and 3.sqm add. */
    private fun createV1Database(userVersion: Int) {
        val driver = JdbcSqliteDriver(url)
        FastPilotDB.Schema.create(driver)
        driver.execute(null, "DROP TABLE cached_driver_standing", 0)
        driver.execute(null, "DROP TABLE cached_constructor_standing", 0)
        driver.execute(null, "DROP TABLE cached_samples", 0)
        driver.execute(null, "ALTER TABLE cached_session DROP COLUMN gmt_offset", 0)
        driver.execute(null, "PRAGMA user_version = $userVersion", 0)
        driver.close()
    }

    private fun tableNames(driver: JdbcSqliteDriver): List<String> =
        driver.executeQuery(null, "SELECT name FROM sqlite_master WHERE type = 'table'", { cursor ->
            val names = mutableListOf<String>()
            while (cursor.next().value) names += cursor.getString(0)!!
            app.cash.sqldelight.db.QueryResult.Value(names)
        }, 0).value

    @Test
    fun `schema version is 4`() {
        assertEquals(4, FastPilotDB.Schema.version)
    }

    @Test
    fun `v1 database is migrated to v4 (Android and iOS drivers)`() {
        createV1Database(userVersion = 1)

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        assertTrue("cached_driver_standing" in tableNames(driver))
        assertTrue("cached_constructor_standing" in tableNames(driver))
        assertTrue("cached_samples" in tableNames(driver))
        FastPilotDB(driver).fastPilotDBQueries.insertConstructorStanding(2025, 1, 24, "mclaren", "McLaren", 833.0, 14, 0)
        driver.close()
    }

    @Test
    fun `v2 database is migrated to v4`() {
        createV2Database()

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        assertTrue("cached_samples" in tableNames(driver))
        FastPilotDB(driver).fastPilotDBQueries.insertSamples("car_data", 9662, 1, "a", "b", "[]", 0)
        driver.close()
    }

    @Test
    fun `v3 database is migrated to v4`() {
        createV3Database()

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        val queries = FastPilotDB(driver).fastPilotDBQueries
        queries.insertSession(11388, "Race", "Race", 1296, "Singapore", "Singapore", "SGP",
            "2026-10-11T12:00:00+00:00", "2026-10-11T14:00:00+00:00", 2026, 0, "08:00:00")
        assertEquals("08:00:00", queries.selectSessionByKey(11388).executeAsOne().gmt_offset)
        driver.close()
    }

    @Test
    fun `legacy desktop database without user_version is upgraded`() {
        // The old desktop factory called Schema.create without setting user_version
        createV1Database(userVersion = 0)

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        assertTrue("cached_driver_standing" in tableNames(driver))
        assertTrue("cached_session" in tableNames(driver))
        driver.close()
    }
}
