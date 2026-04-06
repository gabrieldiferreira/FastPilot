package com.fastpilot.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class FastPilotDBQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllSessions(mapper: (
    session_key: Long,
    session_name: String,
    session_type: String,
    meeting_key: Long,
    circuit_short_name: String,
    country_name: String,
    country_code: String,
    date_start: String,
    date_end: String,
    year: Long,
    cached_at: Long,
  ) -> T): Query<T> = Query(636_176_823, arrayOf("cached_session"), driver, "FastPilotDB.sq",
      "selectAllSessions",
      "SELECT cached_session.session_key, cached_session.session_name, cached_session.session_type, cached_session.meeting_key, cached_session.circuit_short_name, cached_session.country_name, cached_session.country_code, cached_session.date_start, cached_session.date_end, cached_session.year, cached_session.cached_at FROM cached_session ORDER BY date_start DESC") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectAllSessions(): Query<Cached_session> = selectAllSessions { session_key,
      session_name, session_type, meeting_key, circuit_short_name, country_name, country_code,
      date_start, date_end, year, cached_at ->
    Cached_session(
      session_key,
      session_name,
      session_type,
      meeting_key,
      circuit_short_name,
      country_name,
      country_code,
      date_start,
      date_end,
      year,
      cached_at
    )
  }

  public fun <T : Any> selectSessionsByYear(year: Long, mapper: (
    session_key: Long,
    session_name: String,
    session_type: String,
    meeting_key: Long,
    circuit_short_name: String,
    country_name: String,
    country_code: String,
    date_start: String,
    date_end: String,
    year: Long,
    cached_at: Long,
  ) -> T): Query<T> = SelectSessionsByYearQuery(year) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectSessionsByYear(year: Long): Query<Cached_session> = selectSessionsByYear(year) {
      session_key, session_name, session_type, meeting_key, circuit_short_name, country_name,
      country_code, date_start, date_end, year_, cached_at ->
    Cached_session(
      session_key,
      session_name,
      session_type,
      meeting_key,
      circuit_short_name,
      country_name,
      country_code,
      date_start,
      date_end,
      year_,
      cached_at
    )
  }

  public fun <T : Any> selectSessionByKey(session_key: Long, mapper: (
    session_key: Long,
    session_name: String,
    session_type: String,
    meeting_key: Long,
    circuit_short_name: String,
    country_name: String,
    country_code: String,
    date_start: String,
    date_end: String,
    year: Long,
    cached_at: Long,
  ) -> T): Query<T> = SelectSessionByKeyQuery(session_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectSessionByKey(session_key: Long): Query<Cached_session> =
      selectSessionByKey(session_key) { session_key_, session_name, session_type, meeting_key,
      circuit_short_name, country_name, country_code, date_start, date_end, year, cached_at ->
    Cached_session(
      session_key_,
      session_name,
      session_type,
      meeting_key,
      circuit_short_name,
      country_name,
      country_code,
      date_start,
      date_end,
      year,
      cached_at
    )
  }

  public fun <T : Any> selectSessionsByMeeting(meeting_key: Long, mapper: (
    session_key: Long,
    session_name: String,
    session_type: String,
    meeting_key: Long,
    circuit_short_name: String,
    country_name: String,
    country_code: String,
    date_start: String,
    date_end: String,
    year: Long,
    cached_at: Long,
  ) -> T): Query<T> = SelectSessionsByMeetingQuery(meeting_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!
    )
  }

  public fun selectSessionsByMeeting(meeting_key: Long): Query<Cached_session> =
      selectSessionsByMeeting(meeting_key) { session_key, session_name, session_type, meeting_key_,
      circuit_short_name, country_name, country_code, date_start, date_end, year, cached_at ->
    Cached_session(
      session_key,
      session_name,
      session_type,
      meeting_key_,
      circuit_short_name,
      country_name,
      country_code,
      date_start,
      date_end,
      year,
      cached_at
    )
  }

  public fun <T : Any> selectDriversBySession(session_key: Long, mapper: (
    driver_number: Long,
    session_key: Long,
    full_name: String,
    name_acronym: String,
    team_name: String,
    team_colour: String,
    headshot_url: String?,
    broadcast_name: String?,
    cached_at: Long,
  ) -> T): Query<T> = SelectDriversBySessionQuery(session_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6),
      cursor.getString(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectDriversBySession(session_key: Long): Query<Cached_driver> =
      selectDriversBySession(session_key) { driver_number, session_key_, full_name, name_acronym,
      team_name, team_colour, headshot_url, broadcast_name, cached_at ->
    Cached_driver(
      driver_number,
      session_key_,
      full_name,
      name_acronym,
      team_name,
      team_colour,
      headshot_url,
      broadcast_name,
      cached_at
    )
  }

  public fun <T : Any> selectDriverByNumber(
    driver_number: Long,
    session_key: Long,
    mapper: (
      driver_number: Long,
      session_key: Long,
      full_name: String,
      name_acronym: String,
      team_name: String,
      team_colour: String,
      headshot_url: String?,
      broadcast_name: String?,
      cached_at: Long,
    ) -> T,
  ): Query<T> = SelectDriverByNumberQuery(driver_number, session_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6),
      cursor.getString(7),
      cursor.getLong(8)!!
    )
  }

  public fun selectDriverByNumber(driver_number: Long, session_key: Long): Query<Cached_driver> =
      selectDriverByNumber(driver_number, session_key) { driver_number_, session_key_, full_name,
      name_acronym, team_name, team_colour, headshot_url, broadcast_name, cached_at ->
    Cached_driver(
      driver_number_,
      session_key_,
      full_name,
      name_acronym,
      team_name,
      team_colour,
      headshot_url,
      broadcast_name,
      cached_at
    )
  }

  public fun <T : Any> selectLapsByDriverAndSession(
    driver_number: Long,
    session_key: Long,
    mapper: (
      driver_number: Long,
      session_key: Long,
      lap_number: Long,
      lap_duration: Double?,
      sector_1: Double?,
      sector_2: Double?,
      sector_3: Double?,
      i1_speed: Long?,
      i2_speed: Long?,
      st_speed: Long?,
      is_pit_out_lap: Long,
      date_start: String?,
      cached_at: Long,
    ) -> T,
  ): Query<T> = SelectLapsByDriverAndSessionQuery(driver_number, session_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getDouble(3),
      cursor.getDouble(4),
      cursor.getDouble(5),
      cursor.getDouble(6),
      cursor.getLong(7),
      cursor.getLong(8),
      cursor.getLong(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!
    )
  }

  public fun selectLapsByDriverAndSession(driver_number: Long, session_key: Long): Query<Cached_lap>
      = selectLapsByDriverAndSession(driver_number, session_key) { driver_number_, session_key_,
      lap_number, lap_duration, sector_1, sector_2, sector_3, i1_speed, i2_speed, st_speed,
      is_pit_out_lap, date_start, cached_at ->
    Cached_lap(
      driver_number_,
      session_key_,
      lap_number,
      lap_duration,
      sector_1,
      sector_2,
      sector_3,
      i1_speed,
      i2_speed,
      st_speed,
      is_pit_out_lap,
      date_start,
      cached_at
    )
  }

  public fun <T : Any> selectLapByNumber(
    driver_number: Long,
    session_key: Long,
    lap_number: Long,
    mapper: (
      driver_number: Long,
      session_key: Long,
      lap_number: Long,
      lap_duration: Double?,
      sector_1: Double?,
      sector_2: Double?,
      sector_3: Double?,
      i1_speed: Long?,
      i2_speed: Long?,
      st_speed: Long?,
      is_pit_out_lap: Long,
      date_start: String?,
      cached_at: Long,
    ) -> T,
  ): Query<T> = SelectLapByNumberQuery(driver_number, session_key, lap_number) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getDouble(3),
      cursor.getDouble(4),
      cursor.getDouble(5),
      cursor.getDouble(6),
      cursor.getLong(7),
      cursor.getLong(8),
      cursor.getLong(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12)!!
    )
  }

  public fun selectLapByNumber(
    driver_number: Long,
    session_key: Long,
    lap_number: Long,
  ): Query<Cached_lap> = selectLapByNumber(driver_number, session_key, lap_number) { driver_number_,
      session_key_, lap_number_, lap_duration, sector_1, sector_2, sector_3, i1_speed, i2_speed,
      st_speed, is_pit_out_lap, date_start, cached_at ->
    Cached_lap(
      driver_number_,
      session_key_,
      lap_number_,
      lap_duration,
      sector_1,
      sector_2,
      sector_3,
      i1_speed,
      i2_speed,
      st_speed,
      is_pit_out_lap,
      date_start,
      cached_at
    )
  }

  public fun <T : Any> selectStintsByDriverAndSession(
    driver_number: Long,
    session_key: Long,
    mapper: (
      driver_number: Long,
      session_key: Long,
      stint_number: Long,
      compound: String,
      lap_start: Long,
      lap_end: Long,
      tyre_age_at_start: Long,
      cached_at: Long,
    ) -> T,
  ): Query<T> = SelectStintsByDriverAndSessionQuery(driver_number, session_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!
    )
  }

  public fun selectStintsByDriverAndSession(driver_number: Long, session_key: Long):
      Query<Cached_stint> = selectStintsByDriverAndSession(driver_number, session_key) {
      driver_number_, session_key_, stint_number, compound, lap_start, lap_end, tyre_age_at_start,
      cached_at ->
    Cached_stint(
      driver_number_,
      session_key_,
      stint_number,
      compound,
      lap_start,
      lap_end,
      tyre_age_at_start,
      cached_at
    )
  }

  public fun <T : Any> selectStintsBySession(session_key: Long, mapper: (
    driver_number: Long,
    session_key: Long,
    stint_number: Long,
    compound: String,
    lap_start: Long,
    lap_end: Long,
    tyre_age_at_start: Long,
    cached_at: Long,
  ) -> T): Query<T> = SelectStintsBySessionQuery(session_key) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!
    )
  }

  public fun selectStintsBySession(session_key: Long): Query<Cached_stint> =
      selectStintsBySession(session_key) { driver_number, session_key_, stint_number, compound,
      lap_start, lap_end, tyre_age_at_start, cached_at ->
    Cached_stint(
      driver_number,
      session_key_,
      stint_number,
      compound,
      lap_start,
      lap_end,
      tyre_age_at_start,
      cached_at
    )
  }

  public fun insertSession(
    session_key: Long?,
    session_name: String,
    session_type: String,
    meeting_key: Long,
    circuit_short_name: String,
    country_name: String,
    country_code: String,
    date_start: String,
    date_end: String,
    year: Long,
    cached_at: Long,
  ) {
    driver.execute(436_295_442, """
        |INSERT OR REPLACE INTO cached_session(
        |    session_key, session_name, session_type, meeting_key,
        |    circuit_short_name, country_name, country_code,
        |    date_start, date_end, year, cached_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 11) {
          bindLong(0, session_key)
          bindString(1, session_name)
          bindString(2, session_type)
          bindLong(3, meeting_key)
          bindString(4, circuit_short_name)
          bindString(5, country_name)
          bindString(6, country_code)
          bindString(7, date_start)
          bindString(8, date_end)
          bindLong(9, year)
          bindLong(10, cached_at)
        }
    notifyQueries(436_295_442) { emit ->
      emit("cached_session")
    }
  }

  public fun deleteSessionByKey(session_key: Long) {
    driver.execute(-1_995_611_512, """DELETE FROM cached_session WHERE session_key = ?""", 1) {
          bindLong(0, session_key)
        }
    notifyQueries(-1_995_611_512) { emit ->
      emit("cached_session")
    }
  }

  public fun insertDriver(
    driver_number: Long,
    session_key: Long,
    full_name: String,
    name_acronym: String,
    team_name: String,
    team_colour: String,
    headshot_url: String?,
    broadcast_name: String?,
    cached_at: Long,
  ) {
    driver.execute(704_726_060, """
        |INSERT OR REPLACE INTO cached_driver(
        |    driver_number, session_key, full_name, name_acronym,
        |    team_name, team_colour, headshot_url, broadcast_name, cached_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindLong(0, driver_number)
          bindLong(1, session_key)
          bindString(2, full_name)
          bindString(3, name_acronym)
          bindString(4, team_name)
          bindString(5, team_colour)
          bindString(6, headshot_url)
          bindString(7, broadcast_name)
          bindLong(8, cached_at)
        }
    notifyQueries(704_726_060) { emit ->
      emit("cached_driver")
    }
  }

  public fun insertLap(
    driver_number: Long,
    session_key: Long,
    lap_number: Long,
    lap_duration: Double?,
    sector_1: Double?,
    sector_2: Double?,
    sector_3: Double?,
    i1_speed: Long?,
    i2_speed: Long?,
    st_speed: Long?,
    is_pit_out_lap: Long,
    date_start: String?,
    cached_at: Long,
  ) {
    driver.execute(2_036_864_023, """
        |INSERT OR REPLACE INTO cached_lap(
        |    driver_number, session_key, lap_number, lap_duration,
        |    sector_1, sector_2, sector_3, i1_speed, i2_speed, st_speed,
        |    is_pit_out_lap, date_start, cached_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 13) {
          bindLong(0, driver_number)
          bindLong(1, session_key)
          bindLong(2, lap_number)
          bindDouble(3, lap_duration)
          bindDouble(4, sector_1)
          bindDouble(5, sector_2)
          bindDouble(6, sector_3)
          bindLong(7, i1_speed)
          bindLong(8, i2_speed)
          bindLong(9, st_speed)
          bindLong(10, is_pit_out_lap)
          bindString(11, date_start)
          bindLong(12, cached_at)
        }
    notifyQueries(2_036_864_023) { emit ->
      emit("cached_lap")
    }
  }

  public fun insertStint(
    driver_number: Long,
    session_key: Long,
    stint_number: Long,
    compound: String,
    lap_start: Long,
    lap_end: Long,
    tyre_age_at_start: Long,
    cached_at: Long,
  ) {
    driver.execute(-1_071_733_398, """
        |INSERT OR REPLACE INTO cached_stint(
        |    driver_number, session_key, stint_number, compound,
        |    lap_start, lap_end, tyre_age_at_start, cached_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindLong(0, driver_number)
          bindLong(1, session_key)
          bindLong(2, stint_number)
          bindString(3, compound)
          bindLong(4, lap_start)
          bindLong(5, lap_end)
          bindLong(6, tyre_age_at_start)
          bindLong(7, cached_at)
        }
    notifyQueries(-1_071_733_398) { emit ->
      emit("cached_stint")
    }
  }

  public fun clearSessionCache(cached_at: Long) {
    driver.execute(-456_361_874, """DELETE FROM cached_session WHERE cached_at < ?""", 1) {
          bindLong(0, cached_at)
        }
    notifyQueries(-456_361_874) { emit ->
      emit("cached_session")
    }
  }

  public fun clearDriverCache(cached_at: Long) {
    driver.execute(-1_951_355_368, """DELETE FROM cached_driver WHERE cached_at < ?""", 1) {
          bindLong(0, cached_at)
        }
    notifyQueries(-1_951_355_368) { emit ->
      emit("cached_driver")
    }
  }

  public fun clearLapCache(cached_at: Long) {
    driver.execute(929_379_721, """DELETE FROM cached_lap WHERE cached_at < ?""", 1) {
          bindLong(0, cached_at)
        }
    notifyQueries(929_379_721) { emit ->
      emit("cached_lap")
    }
  }

  public fun clearStintCache(cached_at: Long) {
    driver.execute(-869_400_426, """DELETE FROM cached_stint WHERE cached_at < ?""", 1) {
          bindLong(0, cached_at)
        }
    notifyQueries(-869_400_426) { emit ->
      emit("cached_stint")
    }
  }

  public fun clearAll() {
    driver.execute(1_976_755_583, """DELETE FROM cached_session""", 0)
    notifyQueries(1_976_755_583) { emit ->
      emit("cached_session")
    }
  }

  private inner class SelectSessionsByYearQuery<out T : Any>(
    public val year: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_session", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_session", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(463_421_752,
        """SELECT cached_session.session_key, cached_session.session_name, cached_session.session_type, cached_session.meeting_key, cached_session.circuit_short_name, cached_session.country_name, cached_session.country_code, cached_session.date_start, cached_session.date_end, cached_session.year, cached_session.cached_at FROM cached_session WHERE year = ? ORDER BY date_start DESC""",
        mapper, 1) {
      bindLong(0, year)
    }

    override fun toString(): String = "FastPilotDB.sq:selectSessionsByYear"
  }

  private inner class SelectSessionByKeyQuery<out T : Any>(
    public val session_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_session", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_session", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(126_656_601,
        """SELECT cached_session.session_key, cached_session.session_name, cached_session.session_type, cached_session.meeting_key, cached_session.circuit_short_name, cached_session.country_name, cached_session.country_code, cached_session.date_start, cached_session.date_end, cached_session.year, cached_session.cached_at FROM cached_session WHERE session_key = ?""",
        mapper, 1) {
      bindLong(0, session_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectSessionByKey"
  }

  private inner class SelectSessionsByMeetingQuery<out T : Any>(
    public val meeting_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_session", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_session", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-283_727_008,
        """SELECT cached_session.session_key, cached_session.session_name, cached_session.session_type, cached_session.meeting_key, cached_session.circuit_short_name, cached_session.country_name, cached_session.country_code, cached_session.date_start, cached_session.date_end, cached_session.year, cached_session.cached_at FROM cached_session WHERE meeting_key = ? ORDER BY date_start""",
        mapper, 1) {
      bindLong(0, meeting_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectSessionsByMeeting"
  }

  private inner class SelectDriversBySessionQuery<out T : Any>(
    public val session_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_driver", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_driver", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_828_435_845,
        """SELECT cached_driver.driver_number, cached_driver.session_key, cached_driver.full_name, cached_driver.name_acronym, cached_driver.team_name, cached_driver.team_colour, cached_driver.headshot_url, cached_driver.broadcast_name, cached_driver.cached_at FROM cached_driver WHERE session_key = ?""",
        mapper, 1) {
      bindLong(0, session_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectDriversBySession"
  }

  private inner class SelectDriverByNumberQuery<out T : Any>(
    public val driver_number: Long,
    public val session_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_driver", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_driver", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-794_201_137,
        """SELECT cached_driver.driver_number, cached_driver.session_key, cached_driver.full_name, cached_driver.name_acronym, cached_driver.team_name, cached_driver.team_colour, cached_driver.headshot_url, cached_driver.broadcast_name, cached_driver.cached_at FROM cached_driver WHERE driver_number = ? AND session_key = ?""",
        mapper, 2) {
      bindLong(0, driver_number)
      bindLong(1, session_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectDriverByNumber"
  }

  private inner class SelectLapsByDriverAndSessionQuery<out T : Any>(
    public val driver_number: Long,
    public val session_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_lap", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_lap", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(971_290_589, """
    |SELECT cached_lap.driver_number, cached_lap.session_key, cached_lap.lap_number, cached_lap.lap_duration, cached_lap.sector_1, cached_lap.sector_2, cached_lap.sector_3, cached_lap.i1_speed, cached_lap.i2_speed, cached_lap.st_speed, cached_lap.is_pit_out_lap, cached_lap.date_start, cached_lap.cached_at FROM cached_lap
    |WHERE driver_number = ? AND session_key = ?
    |ORDER BY lap_number
    """.trimMargin(), mapper, 2) {
      bindLong(0, driver_number)
      bindLong(1, session_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectLapsByDriverAndSession"
  }

  private inner class SelectLapByNumberQuery<out T : Any>(
    public val driver_number: Long,
    public val session_key: Long,
    public val lap_number: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_lap", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_lap", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_521_221_012, """
    |SELECT cached_lap.driver_number, cached_lap.session_key, cached_lap.lap_number, cached_lap.lap_duration, cached_lap.sector_1, cached_lap.sector_2, cached_lap.sector_3, cached_lap.i1_speed, cached_lap.i2_speed, cached_lap.st_speed, cached_lap.is_pit_out_lap, cached_lap.date_start, cached_lap.cached_at FROM cached_lap
    |WHERE driver_number = ? AND session_key = ? AND lap_number = ?
    """.trimMargin(), mapper, 3) {
      bindLong(0, driver_number)
      bindLong(1, session_key)
      bindLong(2, lap_number)
    }

    override fun toString(): String = "FastPilotDB.sq:selectLapByNumber"
  }

  private inner class SelectStintsByDriverAndSessionQuery<out T : Any>(
    public val driver_number: Long,
    public val session_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_stint", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_stint", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_383_623_958, """
    |SELECT cached_stint.driver_number, cached_stint.session_key, cached_stint.stint_number, cached_stint.compound, cached_stint.lap_start, cached_stint.lap_end, cached_stint.tyre_age_at_start, cached_stint.cached_at FROM cached_stint
    |WHERE driver_number = ? AND session_key = ?
    |ORDER BY stint_number
    """.trimMargin(), mapper, 2) {
      bindLong(0, driver_number)
      bindLong(1, session_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectStintsByDriverAndSession"
  }

  private inner class SelectStintsBySessionQuery<out T : Any>(
    public val session_key: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("cached_stint", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("cached_stint", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_446_213_805, """
    |SELECT cached_stint.driver_number, cached_stint.session_key, cached_stint.stint_number, cached_stint.compound, cached_stint.lap_start, cached_stint.lap_end, cached_stint.tyre_age_at_start, cached_stint.cached_at FROM cached_stint
    |WHERE session_key = ?
    |ORDER BY driver_number, stint_number
    """.trimMargin(), mapper, 1) {
      bindLong(0, session_key)
    }

    override fun toString(): String = "FastPilotDB.sq:selectStintsBySession"
  }
}
