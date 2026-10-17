package com.fastpilot.network

/**
 * Base URL of the OpenF1 proxy (Vercel function api/openf1.mjs), ending in "/openf1/v1".
 *
 * OpenF1 serves real-time data (from 30 min before a session until 30 min after it)
 * only to paid accounts. The proxy keeps the account's credentials on the server,
 * adds the access token and forwards the request, so no secret ships with the app.
 * Historical data still goes straight to OpenF1, where each user has the free quota.
 *
 * Blank = no proxy: live sessions are requested directly and only show data once
 * OpenF1 makes it free, after the session.
 */
const val OPENF1_PROXY_URL = ""

/**
 * The Jolpica proxy (Vercel function api/jolpica.mjs): Jolpica answers through the Vercel CDN,
 * which keeps each answer and refreshes it in the background, so the standings do not wait for
 * a slow Jolpica. Absolute, as the mobile apps use it too.
 */
const val JOLPICA_PROXY_URL = "https://fastpilot.vercel.app/api/jolpica"
