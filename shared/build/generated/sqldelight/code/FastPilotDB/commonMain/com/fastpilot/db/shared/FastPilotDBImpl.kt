package com.fastpilot.db.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.fastpilot.db.FastPilotDB
import com.fastpilot.db.FastPilotDBQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<FastPilotDB>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = FastPilotDBImpl.Schema

internal fun KClass<FastPilotDB>.newInstance(driver: SqlDriver): FastPilotDB =
    FastPilotDBImpl(driver)

private class FastPilotDBImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), FastPilotDB {
  override val fastPilotDBQueries: FastPilotDBQueries = FastPilotDBQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS cached_session (
          |    session_key INTEGER PRIMARY KEY,
          |    session_name TEXT NOT NULL,
          |    session_type TEXT NOT NULL,
          |    meeting_key INTEGER NOT NULL,
          |    circuit_short_name TEXT NOT NULL,
          |    country_name TEXT NOT NULL,
          |    country_code TEXT NOT NULL DEFAULT '',
          |    date_start TEXT NOT NULL,
          |    date_end TEXT NOT NULL,
          |    year INTEGER NOT NULL,
          |    cached_at INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS cached_driver (
          |    driver_number INTEGER NOT NULL,
          |    session_key INTEGER NOT NULL,
          |    full_name TEXT NOT NULL,
          |    name_acronym TEXT NOT NULL,
          |    team_name TEXT NOT NULL,
          |    team_colour TEXT NOT NULL,
          |    headshot_url TEXT,
          |    broadcast_name TEXT,
          |    cached_at INTEGER NOT NULL,
          |    PRIMARY KEY (driver_number, session_key)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS cached_lap (
          |    driver_number INTEGER NOT NULL,
          |    session_key INTEGER NOT NULL,
          |    lap_number INTEGER NOT NULL,
          |    lap_duration REAL,
          |    sector_1 REAL,
          |    sector_2 REAL,
          |    sector_3 REAL,
          |    i1_speed INTEGER,
          |    i2_speed INTEGER,
          |    st_speed INTEGER,
          |    is_pit_out_lap INTEGER NOT NULL DEFAULT 0,
          |    date_start TEXT,
          |    cached_at INTEGER NOT NULL,
          |    PRIMARY KEY (driver_number, session_key, lap_number)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS cached_stint (
          |    driver_number INTEGER NOT NULL,
          |    session_key INTEGER NOT NULL,
          |    stint_number INTEGER NOT NULL,
          |    compound TEXT NOT NULL,
          |    lap_start INTEGER NOT NULL,
          |    lap_end INTEGER NOT NULL,
          |    tyre_age_at_start INTEGER NOT NULL DEFAULT 0,
          |    cached_at INTEGER NOT NULL,
          |    PRIMARY KEY (driver_number, session_key, stint_number)
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
