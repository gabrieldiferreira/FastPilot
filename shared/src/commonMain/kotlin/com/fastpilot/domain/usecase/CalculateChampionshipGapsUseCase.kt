package com.fastpilot.domain.usecase

import com.fastpilot.model.ConstructorStandingEntry
import com.fastpilot.model.DriverStandingEntry
import com.fastpilot.model.Standings

/**
 * Points gaps for one row of a championship table.
 */
data class ChampionshipRow<T>(
    val entry: T,
    val position: Int,
    val points: Double,
    val gapToLeader: Double,
    /** Points behind the row directly above; 0 for the leader. */
    val gapToAhead: Double
)

data class ChampionshipTable<T>(
    val rows: List<ChampionshipRow<T>>,
    val leaderPoints: Double
)

data class ChampionshipTables(
    val season: Int,
    val round: Int,
    val drivers: ChampionshipTable<DriverStandingEntry>,
    val constructors: ChampionshipTable<ConstructorStandingEntry>
)

/**
 * Orders the driver and constructor standings by position and computes
 * the points gap of each entry to the leader and to the entry ahead.
 */
class CalculateChampionshipGapsUseCase {

    operator fun invoke(standings: Standings): ChampionshipTables = ChampionshipTables(
        season = standings.season,
        round = standings.round,
        drivers = buildTable(standings.drivers, { it.position }, { it.points }),
        constructors = buildTable(standings.constructors, { it.position }, { it.points })
    )

    private fun <T> buildTable(
        entries: List<T>,
        position: (T) -> Int,
        points: (T) -> Double
    ): ChampionshipTable<T> {
        val sorted = entries.sortedBy(position)
        val leaderPoints = sorted.firstOrNull()?.let(points) ?: 0.0
        val rows = sorted.mapIndexed { index, entry ->
            val pts = points(entry)
            val ahead = if (index == 0) pts else points(sorted[index - 1])
            ChampionshipRow(
                entry = entry,
                position = position(entry),
                points = pts,
                gapToLeader = leaderPoints - pts,
                gapToAhead = ahead - pts
            )
        }
        return ChampionshipTable(rows, leaderPoints)
    }
}
