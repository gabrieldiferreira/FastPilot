package com.fastpilot.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Real OpenF1 race control messages (2023-2026). Measured on 12,372 messages of 159 sessions,
 * the rules translate 99.9%; the rest are one-off messages, shown in English.
 */
class RaceControlTranslationTest {

    private fun assertTranslated(message: String, pt: String, es: String) {
        assertEquals(pt, PtBrStrings.raceControl(message))
        assertEquals(es, EsStrings.raceControl(message))
        assertEquals(message, EnUsStrings.raceControl(message))
    }

    @Test
    fun flagsAndTrackStatus() {
        assertTranslated("DOUBLE YELLOW IN TRACK SECTOR 15",
            "BANDEIRA AMARELA DUPLA NO SETOR 15", "DOBLE BANDERA AMARILLA EN EL SECTOR 15")
        assertTranslated("WAVED BLUE FLAG FOR CAR 23 (ALB) TIMED AT 13:09:54",
            "BANDEIRA AZUL PARA O CARRO 23 (ALB) ÀS 13:09:54", "BANDERA AZUL PARA EL COCHE 23 (ALB) A LAS 13:09:54")
        assertTranslated("SAFETY CAR DEPLOYED", "SAFETY CAR NA PISTA", "SAFETY CAR EN PISTA")
        assertTranslated("DRS ENABLED IN ZONE 2", "DRS HABILITADO NA ZONA 2", "DRS HABILITADO EN LA ZONA 2")
    }

    @Test
    fun incidentsWithCarsStatusReasonAndTime() {
        assertTranslated(
            "FIA STEWARDS: TURN 6 INCIDENT INVOLVING CARS 44 (HAM) AND 63 (RUS) UNDER INVESTIGATION - CAUSING A COLLISION (15:04:29)",
            "COMISSÁRIOS DA FIA: INCIDENTE NA CURVA 6 ENVOLVENDO OS CARROS 44 (HAM) E 63 (RUS) SOB INVESTIGAÇÃO - CAUSAR UMA COLISÃO (15:04:29)",
            "COMISARIOS DEPORTIVOS DE LA FIA: INCIDENTE EN LA CURVA 6 CON LOS COCHES 44 (HAM) Y 63 (RUS) BAJO INVESTIGACIÓN - PROVOCAR UNA COLISIÓN (15:04:29)"
        )
        assertTranslated(
            "INCIDENT INVOLVING CAR 10 (GAS) NOTED -SPEEDING IN THE PIT LANE",
            "INCIDENTE ENVOLVENDO O CARRO 10 (GAS) REGISTRADO - EXCESSO DE VELOCIDADE NO PIT LANE",
            "INCIDENTE CON EL COCHE 10 (GAS) ANOTADO - EXCESO DE VELOCIDAD EN EL PIT LANE"
        )
    }

    @Test
    fun penaltiesAndLapDeletions() {
        assertTranslated(
            "FIA STEWARDS: PENALTY SERVED - 5 SECOND TIME PENALTY FOR CAR 4 (NOR) - TRACK LIMITS (3RD OFFENCE)",
            "COMISSÁRIOS DA FIA: PENALIDADE CUMPRIDA - PENALIDADE DE 5 SEGUNDOS PARA O CARRO 4 (NOR) - LIMITES DE PISTA (3ª INFRAÇÃO)",
            "COMISARIOS DEPORTIVOS DE LA FIA: PENALIZACIÓN CUMPLIDA - PENALIZACIÓN DE 5 SEGUNDOS PARA EL COCHE 4 (NOR) - LÍMITES DE PISTA (3ª INFRACCIÓN)"
        )
        assertTranslated(
            "CAR 41 (LIN) LAP DELETED - TRACK LIMITS AT TURN 5 LAP 4 21:34:30 (PIT)",
            "CARRO 41 (LIN): VOLTA APAGADA - LIMITES DE PISTA NA CURVA 5 VOLTA 4 21:34:30 (BOX)",
            "COCHE 41 (LIN): VUELTA ELIMINADA - LÍMITES DE PISTA EN LA CURVA 5 VUELTA 4 21:34:30 (BOXES)"
        )
        assertTranslated(
            "FIA STEWARDS: DRIVE THROUGH PENALTY FOR CAR 63 (RUS) - LEAVING THE TRACK AND GAINING AN ADVANTAGE",
            "COMISSÁRIOS DA FIA: DRIVE-THROUGH PARA O CARRO 63 (RUS) - SAIR DA PISTA E OBTER VANTAGEM",
            "COMISARIOS DEPORTIVOS DE LA FIA: DRIVE-THROUGH PARA EL COCHE 63 (RUS) - SALIR DE LA PISTA Y OBTENER VENTAJA"
        )
    }

    @Test
    fun scheduleAndWeather() {
        assertTranslated("Q2 WILL RESUME AT 15:20", "RETOMADA DO Q2 ÀS 15:20", "REANUDACIÓN DE LA Q2 A LAS 15:20")
        assertTranslated("RISK OF RAIN FOR F1 QUALIFYING SESSION IS 40 %",
            "RISCO DE CHUVA PARA A CLASSIFICAÇÃO: 40%", "RIESGO DE LLUVIA PARA LA CLASIFICACIÓN: 40%")
    }

    @Test
    fun aMessageWithAnUnknownPartStaysInEnglish() {
        val message = "TURN 3 INCIDENT INVOLVING CAR 1 (VER) NOTED - SOMETHING NEW"
        assertEquals(message, PtBrStrings.raceControl(message))
        assertEquals(message, EsStrings.raceControl(message))
    }

    @Test
    fun placeNames() {
        assertEquals("Cidade do México", PtBrStrings.place("Mexico City"))
        assertEquals("Arabia Saudí", EsStrings.place("Saudi Arabia"))
        assertEquals("Monza", EsStrings.place("Monza"))
        assertEquals("United Kingdom", EnUsStrings.place("United Kingdom"))
    }
}
