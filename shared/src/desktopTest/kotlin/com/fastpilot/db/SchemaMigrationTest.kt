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
 * cache (Classificação screen), v2 the raw samples cache (telemetry and track position).
 */
class SchemaMigrationTest {

    private val dbFile = File.createTempFile("fastpilot-migration", ".db").apply { delete() }
    private val url = "jdbc:sqlite:${dbFile.absolutePath}"

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    /** Builds a v2 database: full current schema minus the table added in 2.sqm. */
    private fun createV2Database() {
        val driver = JdbcSqliteDriver(url)
        FastPilotDB.Schema.create(driver)
        driver.execute(null, "DROP TABLE cached_samples", 0)
        driver.execute(null, "PRAGMA user_version = 2", 0)
        driver.close()
    }

    /** Builds a v1 database: full current schema minus the tables added in 1.sqm and 2.sqm. */
    private fun createV1Database(userVersion: Int) {
        val driver = JdbcSqliteDriver(url)
        FastPilotDB.Schema.create(driver)
        driver.execute(null, "DROP TABLE cached_driver_standing", 0)
        driver.execute(null, "DROP TABLE cached_constructor_standing", 0)
        driver.execute(null, "DROP TABLE cached_samples", 0)
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
    fun `schema version is 3`() {
        assertEquals(3, FastPilotDB.Schema.version)
    }

    @Test
    fun `v1 database is migrated to v3 (Android and iOS drivers)`() {
        createV1Database(userVersion = 1)

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        assertTrue("cached_driver_standing" in tableNames(driver))
        assertTrue("cached_constructor_standing" in tableNames(driver))
        assertTrue("cached_samples" in tableNames(driver))
        FastPilotDB(driver).fastPilotDBQueries.insertConstructorStanding(2025, 1, 24, "mclaren", "McLaren", 833.0, 14, 0)
        driver.close()
    }

    @Test
    fun `v2 database is migrated to v3`() {
        createV2Database()

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        assertTrue("cached_samples" in tableNames(driver))
        FastPilotDB(driver).fastPilotDBQueries.insertSamples("car_data", 9662, 1, "a", "b", "[]", 0)
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
