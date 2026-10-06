package com.fastpilot.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class DatabaseDriverFactory {
    fun create(): SqlDriver {
        val dbPath = File(System.getProperty("user.home"), ".fastpilot")
        dbPath.mkdirs()
        val dbFile = File(dbPath, "fastpilot.db")
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            FastPilotDB.Schema.create(driver)
        }
        return driver
    }
}

