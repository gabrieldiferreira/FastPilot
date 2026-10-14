package com.fastpilot.ui.i18n

/** Race control messages in Spanish, upper case like the FIA originals. */
internal object RaceControlEs : RaceControlVocabulary {
    override val prefixes = mapOf(
        "FIA STEWARDS" to "COMISARIOS DEPORTIVOS DE LA FIA",
        "CORRECTION" to "CORRECCIÓN",
        "UPDATE" to "ACTUALIZACIÓN",
    )

    override val phrases = mapOf(
        "GREEN LIGHT - PIT EXIT OPEN" to "LUZ VERDE - SALIDA DE BOXES ABIERTA",
        "SESSION STARTED" to "SESIÓN INICIADA",
        "SESSION FINISHED" to "SESIÓN FINALIZADA",
        "SESSION ABORTED" to "SESIÓN INTERRUMPIDA",
        "SESSION WILL NOT BE RESUMED" to "LA SESIÓN NO SE REANUDARÁ",
        "CHEQUERED FLAG" to "BANDERA A CUADROS",
        "TRACK CLEAR" to "PISTA DESPEJADA",
        "PIT EXIT OPEN" to "SALIDA DE BOXES ABIERTA",
        "PIT EXIT CLOSED" to "SALIDA DE BOXES CERRADA",
        "PIT LANE ENTRY OPEN" to "ENTRADA DE BOXES ABIERTA",
        "PIT LANE ENTRY CLOSED" to "ENTRADA DE BOXES CERRADA",
        "PIT LANE CLEAR" to "PIT LANE DESPEJADO",
        "YELLOW IN PIT LANE" to "BANDERA AMARILLA EN EL PIT LANE",
        "RED FLAG" to "BANDERA ROJA",
        "RED FLAG - RACE SUSPENDED" to "BANDERA ROJA - CARRERA SUSPENDIDA",
        "SAFETY CAR DEPLOYED" to "SAFETY CAR EN PISTA",
        "SAFETY CAR IN THIS LAP" to "EL SAFETY CAR ENTRA EN BOXES EN ESTA VUELTA",
        "SAFETY CAR LIGHTS ON" to "LUCES DEL SAFETY CAR ENCENDIDAS",
        "SAFETY CAR LIGHTS OFF" to "LUCES DEL SAFETY CAR APAGADAS",
        "SAFETY CAR THROUGH THE PIT LANE" to "EL SAFETY CAR PASA POR EL PIT LANE",
        "SAFETY CAR WILL USE START/FINISH STRAIGHT" to "EL SAFETY CAR USARÁ LA RECTA DE SALIDA/META",
        "SAFETY CAR WILL ENTER PITS: ROLLING START PROCEDURE" to
            "EL SAFETY CAR ENTRARÁ EN BOXES: PROCEDIMIENTO DE SALIDA LANZADA",
        "ALL CARS MAY OVERTAKE THE SAFETY CAR" to "TODOS LOS COCHES PUEDEN ADELANTAR AL SAFETY CAR",
        "ALL CARS THROUGH THE PIT LANE" to "TODOS LOS COCHES PASAN POR EL PIT LANE",
        "ALL CARS TO FOLLOW THE SAFETY CAR THROUGH THE PIT LANE" to "TODOS LOS COCHES SIGUEN AL SAFETY CAR POR EL PIT LANE",
        "ALL CARS USE START/FINISH STRAIGHT" to "TODOS LOS COCHES USAN LA RECTA DE SALIDA/META",
        "VIRTUAL SAFETY CAR DEPLOYED" to "VIRTUAL SAFETY CAR ACTIVADO",
        "VIRTUAL SAFETY CAR ENDING" to "FINAL DEL VIRTUAL SAFETY CAR",
        "VSC DEPLOYED" to "VSC ACTIVADO",
        "VSC ENDING" to "FINAL DEL VSC",
        "MEDICAL CAR DEPLOYED" to "COCHE MÉDICO EN PISTA",
        "DRS ENABLED" to "DRS HABILITADO",
        "DRS DISABLED" to "DRS DESHABILITADO",
        "OVERTAKE ENABLED" to "MODO ADELANTAMIENTO HABILITADO",
        "OVERTAKE DISABLED" to "MODO ADELANTAMIENTO DESHABILITADO",
        "STRAIGHT MODE - LOW GRIP" to "MODO RECTA - BAJA ADHERENCIA",
        "STRAIGHT MODE - NORMAL GRIP" to "MODO RECTA - ADHERENCIA NORMAL",
        "STRAIGHT MODE - DISABLED" to "MODO RECTA - DESHABILITADO",
        "LOW GRIP CONDITIONS" to "CONDICIONES DE BAJA ADHERENCIA",
        "NORMAL GRIP CONDITIONS" to "CONDICIONES NORMALES DE ADHERENCIA",
        "LOW GRIP DELTA ACTIVE" to "DELTA DE BAJA ADHERENCIA ACTIVO",
        "NORMAL GRIP DELTA ACTIVE" to "DELTA DE ADHERENCIA NORMAL ACTIVO",
        "WET TRACK" to "PISTA MOJADA",
        "CHANGE IN CLIMATIC CONDITIONS" to "CAMBIO EN LAS CONDICIONES CLIMÁTICAS",
        "USE OF INTERMEDIATE TYRES IS MANDATORY" to "USO OBLIGATORIO DE NEUMÁTICOS INTERMEDIOS",
        "WEATHER RADAR SYSTEM NOT AVAILABLE" to "RADAR METEOROLÓGICO NO DISPONIBLE",
        "WEATHER RADAR SYSTEM NOW OPERATIONAL" to "RADAR METEOROLÓGICO OPERATIVO",
        "ALL PASS HOLDERS MAY ACCESS THE PIT LANE" to "TODOS LOS ACREDITADOS PUEDEN ACCEDER AL PIT LANE",
        "AWNINGS MAY BE USED" to "SE PUEDEN USAR TOLDOS",
        "AWNINGS TO BE REMOVED" to "DEBEN RETIRARSE LOS TOLDOS",
        "RACE START" to "SALIDA",
        "SPRINT START" to "SALIDA DE LA SPRINT",
        "STANDING START" to "SALIDA PARADA",
        "STANDING START PROCEDURE" to "PROCEDIMIENTO DE SALIDA PARADA",
        "ROLLING START" to "SALIDA LANZADA",
        "ROLLING START PROCEDURE" to "PROCEDIMIENTO DE SALIDA LANZADA",
        "DELAYED START" to "SALIDA RETRASADA",
        "ABORTED START" to "SALIDA ABORTADA",
        "START ABORTED - EXTRA FORMATION LAP" to "SALIDA ABORTADA - VUELTA DE FORMACIÓN EXTRA",
        "EXTRA FORMATION LAP" to "VUELTA DE FORMACIÓN EXTRA",
        "STARTING PROCEDURE SUSPENDED" to "PROCEDIMIENTO DE SALIDA SUSPENDIDO",
        "FORMATION LAP WILL BE STARTED BEHIND THE SAFETY CAR" to "VUELTA DE FORMACIÓN DETRÁS DEL SAFETY CAR",
        "FORMATION LAP(S) BEHIND SAFETY CAR" to "VUELTA(S) DE FORMACIÓN DETRÁS DEL SAFETY CAR",
        "RACE WILL START BEHIND THE SAFETY CAR" to "LA CARRERA SALDRÁ DETRÁS DEL SAFETY CAR",
        "START ORDER: ORIGINAL GRID" to "ORDEN DE SALIDA: PARRILLA ORIGINAL",
        "TRACK TEST COMPLETED" to "PRUEBA DE PISTA COMPLETADA",
        "TRACK LIMIT INFRINGEMENTS ARE STILL UNDER INVESTIGATION" to
            "LAS INFRACCIONES DE LÍMITES DE PISTA SIGUEN BAJO INVESTIGACIÓN",
        "ALL CARS ARE RELEASED FROM POST-SPRINT PARC FERME" to "TODOS LOS COCHES QUEDAN LIBERADOS DEL PARQUE CERRADO POST-SPRINT",
    )

    override val reasons = mapOf(
        "CAUSING A COLLISION" to "PROVOCAR UNA COLISIÓN",
        "LEAVING THE TRACK AND GAINING AN ADVANTAGE" to "SALIR DE LA PISTA Y OBTENER VENTAJA",
        "LEAVING THE TRACK MULTIPLE TIMES AND GAINING AN ADVANTAGE" to "SALIR DE LA PISTA VARIAS VECES Y OBTENER VENTAJA",
        "FORCING ANOTHER DRIVER OFF THE TRACK" to "FORZAR A OTRO PILOTO FUERA DE LA PISTA",
        "FORCING ANOTHER CAR OFF THE TRACK" to "FORZAR A OTRO COCHE FUERA DE LA PISTA",
        "FAILING TO FOLLOW RACE DIRECTORS INSTRUCTIONS" to "NO SEGUIR LAS INSTRUCCIONES DEL DIRECTOR DE CARRERA",
        "MAXIMUM DELTA TIME" to "TIEMPO DELTA MÁXIMO",
        "PRACTICE START INFRINGEMENT" to "INFRACCIÓN EN LA PRÁCTICA DE SALIDA",
        "ESCAPE ROAD INSTRUCTIONS" to "INSTRUCCIONES DE LA ESCAPATORIA",
        "CROSSING THE LINE AT PIT EXIT" to "CRUZAR LA LÍNEA EN LA SALIDA DE BOXES",
        "CROSSING THE LINE AT PIT ENTRY" to "CRUZAR LA LÍNEA EN LA ENTRADA DE BOXES",
        "LEAVING GARAGE BEFORE RESUMPTION TIME ANNOUNCED" to "SALIR DEL GARAJE ANTES DE LA HORA DE REANUDACIÓN",
        "SPEEDING IN THE PIT LANE" to "EXCESO DE VELOCIDAD EN EL PIT LANE",
        "TRACK LIMITS" to "LÍMITES DE PISTA",
        "MULTIPLE TRACK LIMITS" to "MÚLTIPLES INFRACCIONES DE LÍMITES DE PISTA",
        "MULTIPLE TRACK LIMIT INFRINGEMENTS" to "MÚLTIPLES INFRACCIONES DE LÍMITES DE PISTA",
        "DOUBLE YELLOW" to "DOBLE BANDERA AMARILLA",
        "YELLOW FLAG INFRINGEMENT" to "INFRACCIÓN DE BANDERA AMARILLA",
        "FAILING TO SLOW UNDER YELLOW FLAGS" to "NO REDUCIR BAJO BANDERA AMARILLA",
        "RED FLAG INFRINGEMENT" to "INFRACCIÓN DE BANDERA ROJA",
        "IGNORING BLUE FLAGS" to "IGNORAR BANDERAS AZULES",
        "UNSAFE RELEASE" to "LIBERACIÓN INSEGURA",
        "RELEASING IN UNSAFE CONDITION" to "LIBERACIÓN EN CONDICIÓN INSEGURA",
        "RELEASED IN UNSAFE CONDITION" to "LIBERADO EN CONDICIÓN INSEGURA",
        "UNSAFE CONDITION" to "CONDICIÓN INSEGURA",
        "DRIVING IN AN UNSAFE CONDITION" to "CONDUCIR EN CONDICIÓN INSEGURA",
        "SAFETY CAR INFRINGEMENT" to "INFRACCIÓN BAJO SAFETY CAR",
        "SAFETY CAR PROCEDURE INFRINGEMENT" to "INFRACCIÓN EN EL PROCEDIMIENTO DE SAFETY CAR",
        "VIRTUAL SAFETY CAR INFRINGEMENT" to "INFRACCIÓN BAJO VIRTUAL SAFETY CAR",
        "VSC INFRINGEMENT" to "INFRACCIÓN BAJO VSC",
        "OVERTAKING UNDER SAFETY CAR" to "ADELANTAR BAJO SAFETY CAR",
        "ENTERING PIT LANE UNDER SAFETY CAR CONDITIONS" to "ENTRAR AL PIT LANE BAJO SAFETY CAR",
        "IMPEDING" to "OBSTACULIZAR A OTRO PILOTO",
        "STARTING PROCEDURE INFRINGEMENT" to "INFRACCIÓN EN EL PROCEDIMIENTO DE SALIDA",
        "OUT OF POSITION AT SAFETY CAR LINE" to "FUERA DE POSICIÓN EN LA LÍNEA DEL SAFETY CAR",
        "RESTART INFRINGEMENT" to "INFRACCIÓN EN LA RELANZADA",
        "FALSE START" to "SALIDA EN FALSO",
        "JUMP START" to "SALIDA ANTICIPADA",
        "MOVING BEFORE SIGNAL" to "MOVERSE ANTES DE LA SEÑAL",
        "OUT OF POSITION" to "FUERA DE POSICIÓN",
        "INCORRECT STARTING LOCATION" to "POSICIÓN DE SALIDA INCORRECTA",
        "INACCURATE GRID POSITION" to "POSICIÓN EN PARRILLA INCORRECTA",
        "DRIVING ERRATICALLY" to "CONDUCCIÓN ERRÁTICA",
        "DANGEROUS DRIVING" to "CONDUCCIÓN PELIGROSA",
        "DRIVING UNNECESSARILY SLOWLY" to "CONDUCIR INNECESARIAMENTE LENTO",
        "MOVING UNDER BRAKING" to "MOVERSE EN FRENADA",
        "MORE THAN ONE CHANGE OF DIRECTION" to "MÁS DE UN CAMBIO DE DIRECCIÓN",
        "WEAVING ON STRAIGHT" to "ZIGZAGUEAR EN LA RECTA",
        "REJOINING UNSAFELY" to "REINCORPORARSE DE FORMA INSEGURA",
        "UNSAFE REJOIN" to "REINCORPORACIÓN INSEGURA",
        "FAILING TO SERVE TIME PENALTY CORRECTLY" to "NO CUMPLIR CORRECTAMENTE LA PENALIZACIÓN DE TIEMPO",
        "FAILING TO SERVE PENALTY CORRECTLY" to "NO CUMPLIR CORRECTAMENTE LA PENALIZACIÓN",
        "SERVING PENALTY INCORRECTLY" to "CUMPLIR LA PENALIZACIÓN INCORRECTAMENTE",
        "SERVING TIME PENALTY INCORRECTLY" to "CUMPLIR LA PENALIZACIÓN DE TIEMPO INCORRECTAMENTE",
        "PIT LANE INFRINGEMENT" to "INFRACCIÓN EN EL PIT LANE",
        "PIT STOP INFRINGEMENT" to "INFRACCIÓN EN LA PARADA EN BOXES",
        "LEAVING PIT EXIT ON RED LIGHT" to "SALIR DE BOXES CON LUZ ROJA",
        "NOT ENTERING PIT LANE" to "NO ENTRAR AL PIT LANE",
        "CROSSING THE TRACK" to "CRUZAR LA PISTA",
        "ENTERING THE TRACK ON FOOT" to "ENTRAR A PIE EN LA PISTA",
        "CAR SAFETY LIGHTS" to "LUCES DE SEGURIDAD DEL COCHE",
        "LOOSE REAR WING" to "ALERÓN TRASERO SUELTO",
        "CONTINUING ON TRACK AFTER RECEIVING ASSISTANCE" to "CONTINUAR EN PISTA TRAS RECIBIR AYUDA",
        "UNSPORTSMANLIKE BEHAVIOUR" to "CONDUCTA ANTIDEPORTIVA",
        "PLACE WAS GIVEN BACK" to "POSICIÓN DEVUELTA",
        "POSITIONS GIVEN BACK" to "POSICIONES DEVUELTAS",
    )

    override val sessions = mapOf(
        "RACE" to "LA CARRERA",
        "SPRINT" to "LA SPRINT",
        "SPRINT RACE" to "LA SPRINT",
        "QUALIFYING" to "LA CLASIFICACIÓN",
        "QUALIFYING SESSION" to "LA CLASIFICACIÓN",
        "SPRINT QUALIFYING" to "LA CLASIFICACIÓN SPRINT",
        "SPRINT QUALIFYING SESSION" to "LA CLASIFICACIÓN SPRINT",
        "FIRST PRACTICE SESSION" to "LOS LIBRES 1",
        "SECOND PRACTICE SESSION" to "LOS LIBRES 2",
        "THIRD PRACTICE SESSION" to "LOS LIBRES 3",
        "SESSION" to "LA SESIÓN",
        "FORMATION LAP" to "LA VUELTA DE FORMACIÓN",
    )

    override fun car(number: String) = "COCHE $number"
    override fun cars(numbers: String) = "COCHES $numbers"
    override val and = "Y"
    override fun offence(n: String) = "(${n}ª INFRACCIÓN)"

    override fun atTurn(turn: String) = "EN LA CURVA $turn"
    override fun betweenTurns(first: String, second: String) = "ENTRE LAS CURVAS $first Y $second"
    override fun beforeTurn(turn: String) = "ANTES DE LA CURVA $turn"
    override val atPitEntry = "EN LA ENTRADA DE BOXES"
    override val inPitLane = "EN EL PIT LANE"
    override val onStartFinishStraight = "EN LA RECTA DE SALIDA/META"
    override fun inPhase(phase: String) = "EN LA $phase"
    override fun onLap(lap: String) = "VUELTA $lap"

    override fun sectorFlag(flag: String, sector: String) = when (flag) {
        "CLEAR" -> "PISTA DESPEJADA EN EL SECTOR $sector"
        "YELLOW" -> "BANDERA AMARILLA EN EL SECTOR $sector"
        "DOUBLE YELLOW" -> "DOBLE BANDERA AMARILLA EN EL SECTOR $sector"
        "TRACK SURFACE SLIPPERY" -> "PISTA RESBALADIZA EN EL SECTOR $sector"
        else -> null
    }
    override fun blueFlag(car: String, time: String) = "BANDERA AZUL PARA EL $car A LAS $time"
    override fun blackAndWhiteFlag(car: String, reason: String?) =
        "BANDERA BLANCA Y NEGRA PARA EL $car" + (reason?.let { " - $it" } ?: "")
    override fun blackAndOrangeFlag(car: String) = "BANDERA NEGRA Y NARANJA PARA EL $car"
    override fun firstCarToTakeTheFlag(car: String) = "PRIMER COCHE EN RECIBIR LA BANDERA - $car"

    override fun timeDeleted(car: String, time: String, detail: String) = "$car: TIEMPO $time ELIMINADO - $detail"
    override fun lapDeleted(car: String, detail: String) = "$car: VUELTA ELIMINADA - $detail"
    override fun timeReinstated(car: String, time: String) = "$car: EL TIEMPO $time SERÁ RESTITUIDO"
    override fun lapReinstated(car: String, lap: String) = "$car: LA VUELTA $lap SERÁ RESTITUIDA"
    override val notes = mapOf("PIT" to "(BOXES)", "NEXT LAP" to "(VUELTA SIGUIENTE)", "NEXT LAP PIT" to "(VUELTA SIGUIENTE, BOXES)")

    override fun incident(location: String?, involving: String?, status: IncidentStatus, reason: String?): String {
        val statusText = when (status) {
            IncidentStatus.NOTED -> "ANOTADO"
            IncidentStatus.UNDER_INVESTIGATION -> "BAJO INVESTIGACIÓN"
            IncidentStatus.REVIEWED -> "REVISADO, SIN MÁS INVESTIGACIÓN"
            IncidentStatus.NO_FURTHER_ACTION -> "SIN SANCIÓN"
            IncidentStatus.AFTER_RACE -> "SERÁ INVESTIGADO DESPUÉS DE LA CARRERA"
            IncidentStatus.AFTER_SESSION -> "SERÁ INVESTIGADO DESPUÉS DE LA SESIÓN"
            IncidentStatus.AFTER_SPRINT -> "SERÁ INVESTIGADO DESPUÉS DE LA SPRINT"
        }
        return listOfNotNull("INCIDENTE", location, involving?.let { "CON EL $it".replace("EL COCHES", "LOS COCHES") }, statusText)
            .joinToString(" ") + (reason?.let { " - $it" } ?: "")
    }
    override fun sanction(kind: Sanction, seconds: String?, car: String, reason: String?): String {
        val text = when (kind) {
            Sanction.TIME_PENALTY -> "PENALIZACIÓN DE $seconds SEGUNDOS"
            Sanction.STOP_GO_PENALTY -> "STOP AND GO" + (seconds?.let { " DE $it SEGUNDOS" } ?: "")
            Sanction.DRIVE_THROUGH_PENALTY -> "DRIVE-THROUGH"
            Sanction.REPRIMAND -> "AMONESTACIÓN"
            Sanction.WARNING -> "ADVERTENCIA"
            Sanction.BLACK_FLAG -> "BANDERA NEGRA"
        }
        return "$text PARA EL $car" + (reason?.let { " - $it" } ?: "")
    }
    override fun restartProcedure(status: IncidentStatus) =
        incident(null, null, status, null).replace("INCIDENTE", "PROCEDIMIENTO DE RELANZAMIENTO")
    override fun trackLimitsCount(count: String) = "$count INFRACCIONES DE LÍMITES DE PISTA"
    override val atPitExit = "EN LA SALIDA DE BOXES"
    override val onStartStraight = "EN LA RECTA DE SALIDA"
    override fun penaltyServed(penalty: String) = "PENALIZACIÓN CUMPLIDA - $penalty"

    override fun drs(enabled: Boolean, zone: String?) =
        (if (enabled) "DRS HABILITADO" else "DRS DESHABILITADO") + (zone?.let { " EN LA ZONA $it" } ?: "")
    override fun marshals(location: String) = "COMISARIOS DE PISTA EN LA PISTA $location"
    override fun recoveryVehicle(location: String) = "VEHÍCULO DE RECUPERACIÓN EN PISTA $location"
    override fun carOnTrack(car: String, event: CarEvent, location: String) = when (event) {
        CarEvent.OFF_TRACK -> "$car SE SALIÓ DE LA PISTA Y CONTINUÓ $location"
        CarEvent.SPUN -> "$car TROMPEÓ Y CONTINUÓ $location"
        CarEvent.STOPPED -> "$car DETENIDO $location"
        CarEvent.MISSED_APEX -> "$car NO TOCÓ EL VÉRTICE DE LA CURVA ${location.removePrefix("EN LA CURVA ")}"
    }
    override fun headPadding(color: String) = when (color) {
        "PINK" -> "OBLIGATORIO EL PROTECTOR DE CABEZA ROSA"
        "BLUE" -> "OBLIGATORIO EL PROTECTOR DE CABEZA AZUL"
        "LIGHT BLUE" -> "OBLIGATORIO EL PROTECTOR DE CABEZA AZUL CLARO"
        else -> null
    }
    override fun riskOfRain(session: String?, series: String?, percent: String) =
        "RIESGO DE LLUVIA PARA ${session ?: "ESTA SESIÓN"}${series?.let { " DE LA $it" } ?: ""}: $percent%"
    override fun willStart(subject: String, time: String) = "INICIO ${of(subject)} A LAS $time"
    override fun willResume(subject: String, time: String) = "REANUDACIÓN ${of(subject)} A LAS $time"
    override fun startDelayed(subject: String) = "INICIO ${of(subject)} RETRASADO"
    override fun estimatedStart(subject: String, time: String) = "HORA ESTIMADA DE INICIO ${of(subject)} - $time"
    override fun estimatedResumption(subject: String, time: String) = "HORA ESTIMADA DE REANUDACIÓN ${of(subject)} - $time"
    override fun order(kind: OrderKind, list: String) = when (kind) {
        OrderKind.RESTART -> "ORDEN PARA EL RELANZAMIENTO: $list"
        OrderKind.PUSH_TO_FRONT -> "EMPUJAR LOS COCHES HACIA DELANTE EN EL ORDEN: $list"
        OrderKind.LAPPED_CARS -> "LOS DOBLADOS PUEDEN ADELANTAR AL SAFETY CAR: $list"
    }

    /** "LA CARRERA" -> "DE LA CARRERA", "LOS LIBRES 1" -> "DE LOS LIBRES 1", "Q2" -> "DE LA Q2". */
    private fun of(subject: String) = if (PHASE.matches(subject)) "DE LA $subject" else "DE $subject"

    private val PHASE = Regex("""S?Q\d""")
}
