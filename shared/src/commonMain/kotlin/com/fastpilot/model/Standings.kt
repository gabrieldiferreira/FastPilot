package com.fastpilot.model

/**
 * Championship standings for one season, as returned by Jolpica after [round].
 */
data class Standings(
    val season: Int,
    val round: Int,
    val drivers: List<DriverStandingEntry>,
    val constructors: List<ConstructorStandingEntry>
)

data class DriverStandingEntry(
    val position: Int,
    val driverId: String,
    val code: String,
    val fullName: String,
    val constructorId: String,
    val constructorName: String,
    val points: Double,
    val wins: Int
)

data class ConstructorStandingEntry(
    val position: Int,
    val constructorId: String,
    val name: String,
    val points: Double,
    val wins: Int
)
