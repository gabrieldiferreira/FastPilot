package com.fastpilot.domain.usecase

import com.fastpilot.model.ConstructorStandingEntry
import com.fastpilot.model.DriverStandingEntry
import com.fastpilot.model.Standings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateChampionshipGapsUseCaseTest {

    private val useCase = CalculateChampionshipGapsUseCase()

    private fun driver(position: Int, code: String, points: Double) = DriverStandingEntry(
        position = position, driverId = code.lowercase(), code = code, fullName = code,
        constructorId = "team", constructorName = "Team", points = points, wins = 0
    )

    private fun team(position: Int, id: String, points: Double) = ConstructorStandingEntry(
        position = position, constructorId = id, name = id, points = points, wins = 0
    )

    @Test
    fun `computes gaps to leader and to the position ahead`() {
        val standings = Standings(
            season = 2025, round = 24,
            drivers = listOf(driver(1, "NOR", 423.0), driver(2, "VER", 421.0), driver(3, "PIA", 410.0)),
            constructors = emptyList()
        )

        val rows = useCase(standings).drivers.rows

        assertEquals(listOf(0.0, 2.0, 13.0), rows.map { it.gapToLeader })
        assertEquals(listOf(0.0, 2.0, 11.0), rows.map { it.gapToAhead })
    }

    @Test
    fun `orders rows by championship position`() {
        val standings = Standings(
            season = 2024, round = 10,
            drivers = emptyList(),
            constructors = listOf(team(3, "ferrari", 300.0), team(1, "mclaren", 400.0), team(2, "red_bull", 350.0))
        )

        val table = useCase(standings).constructors

        assertEquals(listOf("mclaren", "red_bull", "ferrari"), table.rows.map { it.entry.constructorId })
        assertEquals(400.0, table.leaderPoints)
        assertEquals(listOf(0.0, 50.0, 100.0), table.rows.map { it.gapToLeader })
    }

    @Test
    fun `supports fractional points`() {
        val standings = Standings(
            season = 2021, round = 12,
            drivers = listOf(driver(1, "HAM", 12.5), driver(2, "VER", 5.0)),
            constructors = emptyList()
        )

        assertEquals(7.5, useCase(standings).drivers.rows[1].gapToLeader, 1e-9)
    }

    @Test
    fun `empty standings produce empty tables`() {
        val tables = useCase(Standings(season = 2026, round = 0, drivers = emptyList(), constructors = emptyList()))

        assertTrue(tables.drivers.rows.isEmpty())
        assertTrue(tables.constructors.rows.isEmpty())
        assertEquals(0.0, tables.drivers.leaderPoints)
    }
}
