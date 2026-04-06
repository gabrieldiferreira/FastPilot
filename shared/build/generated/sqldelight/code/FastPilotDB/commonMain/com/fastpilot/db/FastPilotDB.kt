package com.fastpilot.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.fastpilot.db.shared.newInstance
import com.fastpilot.db.shared.schema
import kotlin.Unit

public interface FastPilotDB : Transacter {
  public val fastPilotDBQueries: FastPilotDBQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = FastPilotDB::class.schema

    public operator fun invoke(driver: SqlDriver): FastPilotDB =
        FastPilotDB::class.newInstance(driver)
  }
}
