package com.fastpilot.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(
            schema = FastPilotDB.Schema,
            context = context,
            name = "fastpilot.db"
        )
    }
}
