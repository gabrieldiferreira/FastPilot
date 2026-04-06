package com.fastpilot.db

import kotlin.Long
import kotlin.String

public data class Cached_driver(
  public val driver_number: Long,
  public val session_key: Long,
  public val full_name: String,
  public val name_acronym: String,
  public val team_name: String,
  public val team_colour: String,
  public val headshot_url: String?,
  public val broadcast_name: String?,
  public val cached_at: Long,
)
