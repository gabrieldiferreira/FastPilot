package com.fastpilot.db

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class Cached_lap(
  public val driver_number: Long,
  public val session_key: Long,
  public val lap_number: Long,
  public val lap_duration: Double?,
  public val sector_1: Double?,
  public val sector_2: Double?,
  public val sector_3: Double?,
  public val i1_speed: Long?,
  public val i2_speed: Long?,
  public val st_speed: Long?,
  public val is_pit_out_lap: Long,
  public val date_start: String?,
  public val cached_at: Long,
)
