package com.fastpilot.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

class DatabaseDriverFactory {
    fun create(): SqlDriver {
        val dbPath = File(System.getProperty("user.home"), ".fastpilot")
        dbPath.mkdirs()
        val dbFile = File(dbPath, "fastpilot.db")
        // Creates or migrates the schema based on PRAGMA user_version
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            properties = Properties(),
            schema = FastPilotDB.Schema
        )
    }
}
