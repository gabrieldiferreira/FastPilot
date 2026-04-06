package com.fastpilot.db

import kotlin.Long
import kotlin.String

public data class Cached_session(
  public val session_key: Long,
  public val session_name: String,
  public val session_type: String,
  public val meeting_key: Long,
  public val circuit_short_name: String,
  public val country_name: String,
  public val country_code: String,
  public val date_start: String,
  public val date_end: String,
  public val year: Long,
  public val cached_at: Long,
)
