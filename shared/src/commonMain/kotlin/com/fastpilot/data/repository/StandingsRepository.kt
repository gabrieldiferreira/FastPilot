package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.ConstructorStandingEntry
import com.fastpilot.model.DriverStandingEntry
import com.fastpilot.model.Standings
import com.fastpilot.network.JolpicaApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock

/**
 * Cache-first repository for championship standings (Jolpica).
 * Standings only change after each race, so the cache lives for 7 days.
 * [db] is null on platforms without a local database (Web).
 */
class StandingsRepository(
    private val api: JolpicaApi,
    private val db: FastPilotDB?
) {
    companion object {
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    suspend fun getStandings(season: Int, forceRefresh: Boolean = false): Standings {
        val cached = readCache(season)
        if (!forceRefresh && cached != null) {
            val age = Clock.System.now().toEpochMilliseconds() - cached.second
            if (age < CACHE_TTL_MS) return cached.first
        }

        return try {
            val remote = fetchRemote(season)
            writeCache(remote)
            remote
        } catch (e: Exception) {
            // Fallback to stale cached data on network error
            cached?.first ?: throw e
        }
    }

    private suspend fun fetchRemote(season: Int): Standings = coroutineScope {
        val driversDeferred = async { api.getDriverStandings(season) }
        val constructorsDeferred = async { api.getConstructorStandings(season) }
        val driverList = driversDeferred.await().mrData?.standingsTable?.standingsLists?.firstOrNull()
        val constructorList = constructorsDeferred.await().mrData?.standingsTable?.standingsLists?.firstOrNull()

        Standings(
            season = season,
            round = driverList?.round?.toIntOrNull() ?: constructorList?.round?.toIntOrNull() ?: 0,
            drivers = driverList?.driverStandings.orEmpty().mapIndexed { index, s ->
                val team = s.constructors.lastOrNull()
                DriverStandingEntry(
                    // positionText can be "-" for excluded drivers; fall back to list order
                    position = s.position?.toIntOrNull() ?: (index + 1),
                    driverId = s.driver?.driverId ?: "",
                    code = s.driver?.code ?: s.driver?.familyName?.take(3)?.uppercase() ?: "",
                    fullName = "${s.driver?.givenName ?: ""} ${s.driver?.familyName ?: ""}".trim(),
                    constructorId = team?.constructorId ?: "",
                    constructorName = team?.name ?: "",
                    points = s.points?.toDoubleOrNull() ?: 0.0,
                    wins = s.wins?.toIntOrNull() ?: 0
                )
            },
            constructors = constructorList?.constructorStandings.orEmpty().mapIndexed { index, s ->
                ConstructorStandingEntry(
                    position = s.position?.toIntOrNull() ?: (index + 1),
                    constructorId = s.constructor?.constructorId ?: "",
                    name = s.constructor?.name ?: "",
                    points = s.points?.toDoubleOrNull() ?: 0.0,
                    wins = s.wins?.toIntOrNull() ?: 0
                )
            }
        )
    }

    /** Returns the cached standings and their cache timestamp, or null if nothing is cached. */
    private fun readCache(season: Int): Pair<Standings, Long>? {
        val queries = db?.fastPilotDBQueries ?: return null
        val drivers = queries.selectDriverStandingsBySeason(season.toLong()).executeAsList()
        val constructors = queries.selectConstructorStandingsBySeason(season.toLong()).executeAsList()
        if (drivers.isEmpty() && constructors.isEmpty()) return null

        val standings = Standings(
            season = season,
            round = (drivers.firstOrNull()?.round ?: constructors.first().round).toInt(),
            drivers = drivers.map {
                DriverStandingEntry(
                    position = it.position.toInt(),
                    driverId = it.driver_id,
                    code = it.code,
                    fullName = it.full_name,
                    constructorId = it.constructor_id,
                    constructorName = it.constructor_name,
                    points = it.points,
                    wins = it.wins.toInt()
                )
            },
            constructors = constructors.map {
                ConstructorStandingEntry(
                    position = it.position.toInt(),
                    constructorId = it.constructor_id,
                    name = it.name,
                    points = it.points,
                    wins = it.wins.toInt()
                )
            }
        )
        val cachedAt = (drivers.map { it.cached_at } + constructors.map { it.cached_at }).min()
        return standings to cachedAt
    }

    private fun writeCache(standings: Standings) {
        val queries = db?.fastPilotDBQueries ?: return
        val season = standings.season.toLong()
        val round = standings.round.toLong()
        val now = Clock.System.now().toEpochMilliseconds()
        queries.transaction {
            queries.deleteDriverStandingsBySeason(season)
            queries.deleteConstructorStandingsBySeason(season)
            standings.drivers.forEach {
                queries.insertDriverStanding(
                    season = season,
                    position = it.position.toLong(),
                    round = round,
                    driver_id = it.driverId,
                    code = it.code,
                    full_name = it.fullName,
                    constructor_id = it.constructorId,
                    constructor_name = it.constructorName,
                    points = it.points,
                    wins = it.wins.toLong(),
                    cached_at = now
                )
            }
            standings.constructors.forEach {
                queries.insertConstructorStanding(
                    season = season,
                    position = it.position.toLong(),
                    round = round,
                    constructor_id = it.constructorId,
                    name = it.name,
                    points = it.points,
                    wins = it.wins.toLong(),
                    cached_at = now
                )
            }
        }
    }
}
