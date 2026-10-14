package com.fastpilot.ui.i18n

/**
 * Race control messages come from the FIA in English, in a closed set of formats ("YELLOW IN
 * TRACK SECTOR 5", "FIA STEWARDS: 5 SECOND TIME PENALTY FOR CAR 44 (HAM) - CAUSING A COLLISION").
 * They are translated by rules: the message is split into its parts (prefix, cars, location,
 * status, infraction, time), each part is looked up in a [RaceControlVocabulary] and the
 * sentence is rebuilt. A message with any part outside the vocabulary is shown in English,
 * never half translated.
 */
internal fun translateRaceControl(message: String, vocabulary: RaceControlVocabulary): String =
    RaceControlTranslator(vocabulary).translate(message) ?: message

/** The words and sentence patterns of one language. Car lists ("81 (PIA) AND 12 (ANT)") arrive translated. */
internal interface RaceControlVocabulary {
    /** "FIA STEWARDS", "CORRECTION", "UPDATE". */
    val prefixes: Map<String, String>
    /** Whole messages without variable parts. */
    val phrases: Map<String, String>
    /** Infractions and other reasons ("CAUSING A COLLISION"). */
    val reasons: Map<String, String>
    /** OpenF1 session names inside messages ("RACE", "FIRST PRACTICE SESSION", "Q2"). */
    val sessions: Map<String, String>

    fun car(number: String): String
    fun cars(numbers: String): String
    val and: String
    /** "(2ND OFFENCE)". */
    fun offence(n: String): String

    fun atTurn(turn: String): String
    fun betweenTurns(first: String, second: String): String
    fun beforeTurn(turn: String): String
    val atPitEntry: String
    val inPitLane: String
    val onStartFinishStraight: String
    fun inPhase(phase: String): String
    fun onLap(lap: String): String

    /** CLEAR, YELLOW, DOUBLE YELLOW or TRACK SURFACE SLIPPERY in a sector. */
    fun sectorFlag(flag: String, sector: String): String?
    fun blueFlag(car: String, time: String): String
    fun blackAndWhiteFlag(car: String, reason: String?): String
    fun blackAndOrangeFlag(car: String): String
    fun firstCarToTakeTheFlag(car: String): String

    fun timeDeleted(car: String, time: String, detail: String): String
    fun lapDeleted(car: String, detail: String): String
    fun timeReinstated(car: String, time: String): String
    fun lapReinstated(car: String, lap: String): String
    /** "(PIT)", "(NEXT LAP)", "(NEXT LAP PIT)". */
    val notes: Map<String, String>

    fun incident(location: String?, involving: String?, status: IncidentStatus, reason: String?): String
    /** A penalty or other decision for a car; [seconds] for time and stop/go penalties. */
    fun sanction(kind: Sanction, seconds: String?, car: String, reason: String?): String
    fun penaltyServed(penalty: String): String
    fun restartProcedure(status: IncidentStatus): String
    /** "4 TRACK LIMITS". */
    fun trackLimitsCount(count: String): String
    val atPitExit: String
    val onStartStraight: String

    fun drs(enabled: Boolean, zone: String?): String
    fun marshals(location: String): String
    fun recoveryVehicle(location: String): String
    fun carOnTrack(car: String, event: CarEvent, location: String): String
    fun headPadding(color: String): String?
    /** "RISK OF RAIN FOR F1 RACE IS 20%": [session] is translated (null for "THIS SESSION"), [series] is F2/F3 or null. */
    fun riskOfRain(session: String?, series: String?, percent: String): String
    fun willStart(subject: String, time: String): String
    fun willResume(subject: String, time: String): String
    fun startDelayed(subject: String): String
    fun estimatedStart(subject: String, time: String): String
    fun estimatedResumption(subject: String, time: String): String
    fun order(kind: OrderKind, list: String): String
}

internal enum class IncidentStatus { NOTED, UNDER_INVESTIGATION, REVIEWED, NO_FURTHER_ACTION, AFTER_RACE, AFTER_SESSION, AFTER_SPRINT }

internal enum class CarEvent { OFF_TRACK, SPUN, STOPPED, MISSED_APEX }

internal enum class Sanction { TIME_PENALTY, STOP_GO_PENALTY, DRIVE_THROUGH_PENALTY, REPRIMAND, WARNING, BLACK_FLAG }

internal enum class OrderKind { RESTART, PUSH_TO_FRONT, LAPPED_CARS }

private class RaceControlTranslator(private val v: RaceControlVocabulary) {

    fun translate(message: String): String? {
        val text = message.replace('–', '-')
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""(\s)-(\S)"""), "$1- $2")
            .replace("::", ":")
            .trim()
        // "... (13:09:54)": the time stays as it is
        val timed = TIME_SUFFIX.matchEntire(text)
        val core = timed?.groupValues?.get(1) ?: text
        val translated = translateCore(core) ?: return null
        return if (timed != null) "$translated (${timed.groupValues[2]})" else translated
    }

    private fun translateCore(text: String): String? {
        for ((prefix, translation) in v.prefixes) {
            if (text.startsWith("$prefix: ")) {
                return translateCore(text.removePrefix("$prefix: "))?.let { "$translation: $it" }
            }
        }
        v.phrases[text]?.let { return it }
        for (rule in rules) {
            val match = rule.first.matchEntire(text) ?: continue
            return rule.second(match.groupValues)
        }
        return null
    }

    // ── Parts ───────────────────────────────────────────────────────────

    private fun car(number: String, code: String) = v.car(if (code.isEmpty()) number else "$number ($code)")

    private fun carList(list: String) = v.cars(list.replace(" AND ", " ${v.and} "))

    /** "CAUSING A COLLISION", "FALSE START - MOVING BEFORE SIGNAL", "TRACK LIMITS (2ND OFFENCE)". */
    private fun reason(text: String): String? {
        val parts = text.split(" - ").map { it.trim() }
        val translated = parts.map { part ->
            val offence = OFFENCE.matchEntire(part)
            val count = TRACK_LIMITS_COUNT.matchEntire(part)
            if (count != null) {
                v.trackLimitsCount(count.groupValues[1])
            } else if (offence != null) {
                val base = v.reasons[offence.groupValues[1]] ?: return null
                "$base ${v.offence(offence.groupValues[2])}"
            } else {
                v.reasons[part] ?: return null
            }
        }
        return translated.joinToString(" - ")
    }

    private fun location(text: String): String? {
        TURN.matchEntire(text)?.let { return v.atTurn(it.groupValues[1]) }
        LAP_TURN.matchEntire(text)?.let { return "${v.onLap(it.groupValues[1])}, ${v.atTurn(it.groupValues[2])}" }
        BETWEEN.matchEntire(text)?.let { return v.betweenTurns(it.groupValues[1], it.groupValues[2]) }
        PHASE.matchEntire(text)?.let { return v.inPhase(text) }
        return when (text) {
            "PIT LANE" -> v.inPitLane
            "PIT ENTRY" -> v.atPitEntry
            "PIT EXIT" -> v.atPitExit
            "START STRAIGHT" -> v.onStartStraight
            "START/FINISH STRAIGHT" -> v.onStartFinishStraight
            else -> null
        }
    }

    /** "TRACK LIMITS AT TURN 5 LAP 4 21:34:30 (PIT)". */
    private fun deletionDetail(text: String): String? {
        val m = DELETION_DETAIL.matchEntire(text) ?: return null
        val (reasonText, where, lap, clock, note) = m.groupValues.drop(1)
        val parts = buildList {
            add(reason(reasonText) ?: return null)
            add(if (where == "PIT ENTRY") v.atPitEntry else v.atTurn(where.removePrefix("TURN ")))
            if (lap.isNotEmpty()) add(v.onLap(lap))
            if (clock.isNotEmpty()) add(clock)
            if (note.isNotEmpty()) add(v.notes[note] ?: return null)
        }
        return parts.joinToString(" ")
    }

    /** "5 SECOND TIME PENALTY" -> (TIME_PENALTY, "5"). */
    private fun sanction(text: String): Pair<Sanction, String?>? {
        SECONDS_PENALTY.matchEntire(text)?.let { m ->
            val kind = if (m.groupValues[2] == "TIME") Sanction.TIME_PENALTY else Sanction.STOP_GO_PENALTY
            return kind to m.groupValues[1]
        }
        return when (text) {
            "DRIVE THROUGH PENALTY" -> Sanction.DRIVE_THROUGH_PENALTY to null
            "STOP-AND-GO PENALTY" -> Sanction.STOP_GO_PENALTY to null
            "REPRIMAND (DRIVING)", "REPRIMAND" -> Sanction.REPRIMAND to null
            "WARNING" -> Sanction.WARNING to null
            "BLACK FLAG" -> Sanction.BLACK_FLAG to null
            else -> null
        }
    }

    private fun subject(text: String): String? = v.sessions[text] ?: if (PHASE.matches(text)) text else null

    // ── Rules ───────────────────────────────────────────────────────────

    private fun rule(pattern: String, translate: (List<String>) -> String?) = Regex(pattern) to translate

    private val rules: List<Pair<Regex, (List<String>) -> String?>> = listOf(
        rule("""(CLEAR|YELLOW|DOUBLE YELLOW|TRACK SURFACE SLIPPERY) IN TRACK SECTOR (\d+)""") { g ->
            v.sectorFlag(g[1], g[2])
        },
        rule("""WAVED BLUE FLAG FOR $CAR TIMED AT (\S+)""") { g -> v.blueFlag(car(g[1], g[2]), g[3]) },
        rule("""BLACK AND WHITE FLAG FOR $CAR(?: - (.+))?""") { g ->
            val reason = if (g[3].isEmpty()) null else reason(g[3]) ?: return@rule null
            v.blackAndWhiteFlag(car(g[1], g[2]), reason)
        },
        rule("""BLACK AND ORANGE FLAG FOR $CAR""") { g -> v.blackAndOrangeFlag(car(g[1], g[2])) },
        rule("""FIRST CAR TO TAKE THE FLAG - $CAR""") { g -> v.firstCarToTakeTheFlag(car(g[1], g[2])) },
        // Lap deletions and reinstatements
        rule("""$CAR TIME (\S+) DELETED - (.+)""") { g ->
            deletionDetail(g[4])?.let { v.timeDeleted(car(g[1], g[2]), g[3], it) }
        },
        rule("""$CAR LAP DELETED - (.+)""") { g ->
            deletionDetail(g[3])?.let { v.lapDeleted(car(g[1], g[2]), it) }
        },
        rule("""$CAR TIME (\S+) WILL BE REINSTATED""") { g -> v.timeReinstated(car(g[1], g[2]), g[3]) },
        rule("""$CAR LAP (\d+) WILL BE REINSTATED""") { g -> v.lapReinstated(car(g[1], g[2]), g[3]) },
        // Incidents: "TURN 5 INCIDENT INVOLVING CARS 81 (PIA) AND 12 (ANT) UNDER INVESTIGATION - CAUSING A COLLISION"
        rule(
            """(?:(LAP \d+ TURN \d+|TURN \d+|BETWEEN TURNS? \d+ AND (?:TURN )?\d+|PIT LANE|PIT ENTRY|PIT EXIT|START STRAIGHT|START/FINISH STRAIGHT|S?Q\d) )?INCIDENT(?: INVOLVING (CARS?) ([^-]+?))? """ +
                """($STATUS)(?: - (.+))?"""
        ) { g ->
            val location = if (g[1].isEmpty()) null else location(g[1]) ?: return@rule null
            val involving = when {
                g[2].isEmpty() -> null
                g[2] == "CAR" -> CAR_NUMBER.matchEntire(g[3])?.let { car(it.groupValues[1], it.groupValues[2]) } ?: return@rule null
                else -> carList(g[3])
            }
            val reason = if (g[5].isEmpty()) null else reason(g[5]) ?: return@rule null
            v.incident(location, involving, STATUSES.getValue(g[4]), reason)
        },
        // "LAP 5 TURN 3 NOTED": an incident without the word
        rule("""(LAP \d+ TURN \d+|TURN \d+) ($STATUS)""") { g ->
            location(g[1])?.let { v.incident(it, null, STATUSES.getValue(g[2]), null) }
        },
        rule("""(PENALTY SERVED - )?($SANCTION) FOR $CAR(?: - (.+))?""") { g ->
            val reason = if (g[5].isEmpty()) null else reason(g[5]) ?: return@rule null
            val (kind, seconds) = sanction(g[2]) ?: return@rule null
            val penalty = v.sanction(kind, seconds, car(g[3], g[4]), reason)
            if (g[1].isEmpty()) penalty else v.penaltyServed(penalty)
        },
        rule("""RESTART PROCEDURE ($STATUS)""") { g -> v.restartProcedure(STATUSES.getValue(g[1])) },
        // Track, cars and marshals
        rule("""DRS (ENABLED|DISABLED)(?: IN ZONE (\d+))?""") { g -> v.drs(g[1] == "ENABLED", g[2].ifEmpty { null }) },
        rule("""MARSHALS ON TRACK AT (TURN \d+|PIT ENTRY)""") { g -> location(g[1])?.let { v.marshals(it) } },
        rule("""RECOVERY VEHICLE ON TRACK AT TURN (\d+)""") { g -> v.recoveryVehicle(v.atTurn(g[1])) },
        rule("""RECOVERY VEHICLE ON TRACK BETWEEN TURNS? (\d+) AND (?:TURN )?(\d+)""") { g ->
            v.recoveryVehicle(v.betweenTurns(g[1], g[2]))
        },
        rule("""RECOVERY VEHICLE ON TRACK BEFORE TURN (\d+)""") { g -> v.recoveryVehicle(v.beforeTurn(g[1])) },
        rule("""$CAR (OFF TRACK AND CONTINUED|SPUN AND CONTINUED|STOPPED) AT TURN (\d+)""") { g ->
            val event = when (g[3]) { "STOPPED" -> CarEvent.STOPPED; "SPUN AND CONTINUED" -> CarEvent.SPUN; else -> CarEvent.OFF_TRACK }
            v.carOnTrack(car(g[1], g[2]), event, v.atTurn(g[4]))
        },
        rule("""$CAR STOPPED ON START/FINISH STRAIGHT""") { g ->
            v.carOnTrack(car(g[1], g[2]), CarEvent.STOPPED, v.onStartFinishStraight)
        },
        rule("""CARS (.+) STOPPED AT TURN (\d+)""") { g -> v.carOnTrack(carList(g[1]), CarEvent.STOPPED, v.atTurn(g[2])) },
        rule("""$CAR MISSED THE APEX OF TURN (\d+)""") { g ->
            v.carOnTrack(car(g[1], g[2]), CarEvent.MISSED_APEX, v.atTurn(g[3]))
        },
        rule("""(PINK|BLUE|LIGHT BLUE) HEAD PADDING MATERIAL MUST BE USED""") { g -> v.headPadding(g[1]) },
        // Weather and schedule
        rule("""RISK OF RAIN FOR (?:THE )?(?:(F\d) )?(.+?) IS (\d+) ?%""") { g ->
            val series = g[1].takeIf { it.isNotEmpty() && it != "F1" }
            if (g[2] == "THIS SESSION") v.riskOfRain(null, series, g[3])
            else v.sessions[g[2]]?.let { v.riskOfRain(it, series, g[3]) }
        },
        rule("""(.+?) WILL START AT (\S+)""") { g -> subject(g[1])?.let { v.willStart(it, g[2]) } },
        rule("""(.+?) WILL RESUME AT (\S+)(?: - (STANDING|ROLLING) START PROCEDURE)?""") { g ->
            val resume = subject(g[1])?.let { v.willResume(it, g[2]) } ?: return@rule null
            if (g[3].isEmpty()) resume else "$resume - ${v.phrases.getValue("${g[3]} START PROCEDURE")}"
        },
        rule("""START OF (.+) WILL BE DELAYED""") { g -> subject(g[1])?.let { v.startDelayed(it) } },
        rule("""ESTIMATED TIME OF SESSION START - (\S+)""") { g -> v.estimatedStart(v.sessions.getValue("SESSION"), g[1]) },
        rule("""ESTIMATED TIME OF (S?Q\d) RESUMPTION - (\S+)""") { g -> v.estimatedResumption(g[1], g[2]) },
        // Restart orders: the list of car numbers stays as it is
        rule("""(?:ORDER FOR RESTART|RESUMPTION ORDER|RESTART ORDER): (.+)""") { g -> v.order(OrderKind.RESTART, g[1]) },
        rule("""PUSH CARS TO FRONT IN ORDER: (.+)""") { g -> v.order(OrderKind.PUSH_TO_FRONT, g[1]) },
        rule("""LAPPED CARS? MAY NOW OVERTAKE THE SAFETY CAR: (.+)""") { g ->
            v.order(OrderKind.LAPPED_CARS, g[1].replace(" AND ", " ${v.and} "))
        },
    )

    private companion object {
        /** "CAR 44 (HAM)", "CAR 1 (FM1)"; groups: number, code (may be empty). */
        const val STATUS = "NOTED|UNDER INVESTIGATION|REVIEWED NO FURTHER INVESTIGATION|NO FURTHER ACTION|" +
            "WILL BE INVESTIGATED AFTER THE (?:RACE|SESSION|SPRINT)"
        const val SANCTION = "\\d+ SECOND (?:TIME|STOP/GO) PENALTY|DRIVE THROUGH PENALTY|STOP-AND-GO PENALTY|" +
            "REPRIMAND \\(DRIVING\\)|REPRIMAND|WARNING|BLACK FLAG"
        val SECONDS_PENALTY = Regex("""(\d+) SECOND (TIME|STOP/GO) PENALTY""")
        val TRACK_LIMITS_COUNT = Regex("""(\d+) TRACK LIMITS""")
        val BETWEEN = Regex("""BETWEEN TURNS? (\d+) AND (?:TURN )?(\d+)""")
        const val CAR = """CAR (\d+)(?: \(([A-Z0-9]{2,4})\))?"""
        /** "44 (HAM)" after "INVOLVING CAR". */
        val CAR_NUMBER = Regex("""(\d+)(?: \(([A-Z0-9]{2,4})\))?""")
        val TIME_SUFFIX = Regex("""(.*?) \((\d{1,2}:\d{2}(?::\d{2})?)\)""")
        val OFFENCE = Regex("""(.+) \((\d+)(?:ST|ND|RD|TH) OFFENCE\)""")
        val TURN = Regex("""TURN (\d+)""")
        val LAP_TURN = Regex("""LAP (\d+) TURN (\d+)""")
        val PHASE = Regex("""S?Q\d""")
        val DELETION_DETAIL = Regex(
            """(.+?) AT (TURN \d+|PIT ENTRY)(?: LAP (\d+))?(?: (\d{1,2}:\d{2}:\d{2}))?(?: \((PIT|NEXT LAP|NEXT LAP PIT)\))?"""
        )
        val STATUSES = mapOf(
            "NOTED" to IncidentStatus.NOTED,
            "UNDER INVESTIGATION" to IncidentStatus.UNDER_INVESTIGATION,
            "REVIEWED NO FURTHER INVESTIGATION" to IncidentStatus.REVIEWED,
            "NO FURTHER ACTION" to IncidentStatus.NO_FURTHER_ACTION,
            "WILL BE INVESTIGATED AFTER THE RACE" to IncidentStatus.AFTER_RACE,
            "WILL BE INVESTIGATED AFTER THE SESSION" to IncidentStatus.AFTER_SESSION,
            "WILL BE INVESTIGATED AFTER THE SPRINT" to IncidentStatus.AFTER_SPRINT,
        )
    }
}
