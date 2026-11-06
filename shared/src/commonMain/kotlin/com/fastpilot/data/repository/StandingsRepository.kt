package com.fastpilot.data.repository

import com.fastpilot.db.FastPilotDB
import com.fastpilot.model.ConstructorStandingEntry
import com.fastpilot.model.DriverStandingEntry
import com.fastpilot.model.Standings
import com.fastpilot.network.JolpicaApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock

/**
 * Cache-first repository for championship standings (Jolpica).
 * Standings only change after each race, so the cache lives for 7 days.
 * [db] is null on platforms without a local database (Web), which keeps them in memory only.
 */
class StandingsRepository(
    private val api: JolpicaApi,
    private val db: FastPilotDB?
) {
    companion object {
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
        // The current season changes after every race weekend
        private const val CURRENT_SEASON_TTL_MS = 60 * 60 * 1000L // 1 hour
    }

    /** The standings of [season] loaded in this run: the only cache on Web. */
    private val memory = mutableMapOf<Int, Pair<Standings, Long>>()
    private val memoryMutex = Mutex()

    /** Forgets the standings kept in memory (the benchmark's loads without cache). */
    suspend fun clearMemory() = memoryMutex.withLock { memory.clear() }

    /**
     * The cached standings right away, then fresh ones when the cache is missing, stale or
     * [forceRefresh]: Jolpica can take 20 s or more to answer, so the table never waits for it
     * when there is something to show. Throws only when there is nothing cached and the
     * request fails.
     */
    fun observeStandings(season: Int, forceRefresh: Boolean = false): Flow<Standings> = flow {
        val cached = readCache(season)
        if (cached != null) emit(cached.first)
        if (!forceRefresh && cached != null && isFresh(season, cached.second)) return@flow
        val remote = try {
            fetchRemote(season)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keeps showing the cached table
            if (cached != null) return@flow else throw e
        }
        writeCache(remote)
        if (remote != cached?.first) emit(remote)
    }

    private fun isFresh(season: Int, cachedAt: Long): Boolean {
        val age = Clock.System.now().toEpochMilliseconds() - cachedAt
        val ttl = if (season >= availableSeasons().first()) CURRENT_SEASON_TTL_MS else CACHE_TTL_MS
        return age < ttl
    }

    suspend fun getStandings(season: Int, forceRefresh: Boolean = false): Standings {
        val cached = readCache(season)
        if (!forceRefresh && cached != null && isFresh(season, cached.second)) return cached.first

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
    private suspend fun readCache(season: Int): Pair<Standings, Long>? {
        memoryMutex.withLock { memory[season] }?.let { return it }
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
        return (standings to cachedAt).also { memoryMutex.withLock { memory[season] = it } }
    }

    private suspend fun writeCache(standings: Standings) {
        memoryMutex.withLock { memory[standings.season] = standings to Clock.System.now().toEpochMilliseconds() }
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
