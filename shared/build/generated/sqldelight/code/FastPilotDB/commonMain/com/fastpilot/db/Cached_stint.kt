package com.fastpilot.db

import kotlin.Long
import kotlin.String

public data class Cached_stint(
  public val driver_number: Long,
  public val session_key: Long,
  public val stint_number: Long,
  public val compound: String,
  public val lap_start: Long,
  public val lap_end: Long,
  public val tyre_age_at_start: Long,
  public val cached_at: Long,
)
