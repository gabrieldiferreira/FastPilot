package com.fastpilot.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        return NativeSqliteDriver(
            schema = FastPilotDB.Schema,
            name = "fastpilot.db"
        )
    }
}
