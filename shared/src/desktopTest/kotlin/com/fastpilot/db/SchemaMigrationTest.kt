package com.fastpilot.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Databases created before the standings cache (schema v1) must be upgraded
 * in place, otherwise the Classificação screen would crash on existing installs.
 */
class SchemaMigrationTest {

    private val dbFile = File.createTempFile("fastpilot-migration", ".db").apply { delete() }
    private val url = "jdbc:sqlite:${dbFile.absolutePath}"

    @AfterTest
    fun cleanup() {
        dbFile.delete()
    }

    /** Builds a v1 database: full current schema minus the tables added in 1.sqm. */
    private fun createV1Database(userVersion: Int) {
        val driver = JdbcSqliteDriver(url)
        FastPilotDB.Schema.create(driver)
        driver.execute(null, "DROP TABLE cached_driver_standing", 0)
        driver.execute(null, "DROP TABLE cached_constructor_standing", 0)
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
    fun `schema version is 2`() {
        assertEquals(2, FastPilotDB.Schema.version)
    }

    @Test
    fun `v1 database is migrated to v2 (Android and iOS drivers)`() {
        createV1Database(userVersion = 1)

        val driver = JdbcSqliteDriver(url, Properties(), FastPilotDB.Schema)

        assertTrue("cached_driver_standing" in tableNames(driver))
        assertTrue("cached_constructor_standing" in tableNames(driver))
        FastPilotDB(driver).fastPilotDBQueries.insertConstructorStanding(2025, 1, 24, "mclaren", "McLaren", 833.0, 14, 0)
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
