package com.fastpilot.ui.i18n

/** Race control messages in Brazilian Portuguese, upper case like the FIA originals. */
internal object RaceControlPtBr : RaceControlVocabulary {
    override val prefixes = mapOf(
        "FIA STEWARDS" to "COMISSÁRIOS DA FIA",
        "CORRECTION" to "CORREÇÃO",
        "UPDATE" to "ATUALIZAÇÃO",
    )

    override val phrases = mapOf(
        "GREEN LIGHT - PIT EXIT OPEN" to "LUZ VERDE - SAÍDA DOS BOXES ABERTA",
        "SESSION STARTED" to "SESSÃO INICIADA",
        "SESSION FINISHED" to "SESSÃO ENCERRADA",
        "SESSION ABORTED" to "SESSÃO INTERROMPIDA",
        "SESSION WILL NOT BE RESUMED" to "A SESSÃO NÃO SERÁ RETOMADA",
        "CHEQUERED FLAG" to "BANDEIRA QUADRICULADA",
        "TRACK CLEAR" to "PISTA LIVRE",
        "PIT EXIT OPEN" to "SAÍDA DOS BOXES ABERTA",
        "PIT EXIT CLOSED" to "SAÍDA DOS BOXES FECHADA",
        "PIT LANE ENTRY OPEN" to "ENTRADA DOS BOXES ABERTA",
        "PIT LANE ENTRY CLOSED" to "ENTRADA DOS BOXES FECHADA",
        "PIT LANE CLEAR" to "PIT LANE LIVRE",
        "YELLOW IN PIT LANE" to "BANDEIRA AMARELA NO PIT LANE",
        "RED FLAG" to "BANDEIRA VERMELHA",
        "RED FLAG - RACE SUSPENDED" to "BANDEIRA VERMELHA - CORRIDA SUSPENSA",
        "SAFETY CAR DEPLOYED" to "SAFETY CAR NA PISTA",
        "SAFETY CAR IN THIS LAP" to "SAFETY CAR ENTRA NOS BOXES NESTA VOLTA",
        "SAFETY CAR LIGHTS ON" to "LUZES DO SAFETY CAR ACESAS",
        "SAFETY CAR LIGHTS OFF" to "LUZES DO SAFETY CAR APAGADAS",
        "SAFETY CAR THROUGH THE PIT LANE" to "SAFETY CAR PASSA PELO PIT LANE",
        "SAFETY CAR WILL USE START/FINISH STRAIGHT" to "O SAFETY CAR USARÁ A RETA DE LARGADA/CHEGADA",
        "SAFETY CAR WILL ENTER PITS: ROLLING START PROCEDURE" to
            "O SAFETY CAR ENTRARÁ NOS BOXES: PROCEDIMENTO DE LARGADA LANÇADA",
        "ALL CARS MAY OVERTAKE THE SAFETY CAR" to "TODOS OS CARROS PODEM ULTRAPASSAR O SAFETY CAR",
        "ALL CARS THROUGH THE PIT LANE" to "TODOS OS CARROS PASSAM PELO PIT LANE",
        "ALL CARS TO FOLLOW THE SAFETY CAR THROUGH THE PIT LANE" to "TODOS OS CARROS SEGUEM O SAFETY CAR PELO PIT LANE",
        "ALL CARS USE START/FINISH STRAIGHT" to "TODOS OS CARROS USAM A RETA DE LARGADA/CHEGADA",
        "VIRTUAL SAFETY CAR DEPLOYED" to "VIRTUAL SAFETY CAR ACIONADO",
        "VIRTUAL SAFETY CAR ENDING" to "FIM DO VIRTUAL SAFETY CAR",
        "VSC DEPLOYED" to "VSC ACIONADO",
        "VSC ENDING" to "FIM DO VSC",
        "MEDICAL CAR DEPLOYED" to "CARRO MÉDICO NA PISTA",
        "DRS ENABLED" to "DRS HABILITADO",
        "DRS DISABLED" to "DRS DESABILITADO",
        "OVERTAKE ENABLED" to "MODO ULTRAPASSAGEM HABILITADO",
        "OVERTAKE DISABLED" to "MODO ULTRAPASSAGEM DESABILITADO",
        "STRAIGHT MODE - LOW GRIP" to "MODO RETA - BAIXA ADERÊNCIA",
        "STRAIGHT MODE - NORMAL GRIP" to "MODO RETA - ADERÊNCIA NORMAL",
        "STRAIGHT MODE - DISABLED" to "MODO RETA - DESABILITADO",
        "LOW GRIP CONDITIONS" to "CONDIÇÕES DE BAIXA ADERÊNCIA",
        "NORMAL GRIP CONDITIONS" to "CONDIÇÕES NORMAIS DE ADERÊNCIA",
        "LOW GRIP DELTA ACTIVE" to "DELTA DE BAIXA ADERÊNCIA ATIVO",
        "NORMAL GRIP DELTA ACTIVE" to "DELTA DE ADERÊNCIA NORMAL ATIVO",
        "WET TRACK" to "PISTA MOLHADA",
        "CHANGE IN CLIMATIC CONDITIONS" to "MUDANÇA NAS CONDIÇÕES CLIMÁTICAS",
        "USE OF INTERMEDIATE TYRES IS MANDATORY" to "USO OBRIGATÓRIO DE PNEUS INTERMEDIÁRIOS",
        "WEATHER RADAR SYSTEM NOT AVAILABLE" to "RADAR METEOROLÓGICO INDISPONÍVEL",
        "WEATHER RADAR SYSTEM NOW OPERATIONAL" to "RADAR METEOROLÓGICO OPERACIONAL",
        "ALL PASS HOLDERS MAY ACCESS THE PIT LANE" to "TODOS OS CREDENCIADOS PODEM ACESSAR O PIT LANE",
        "AWNINGS MAY BE USED" to "TOLDOS PODEM SER USADOS",
        "AWNINGS TO BE REMOVED" to "OS TOLDOS DEVEM SER REMOVIDOS",
        "RACE START" to "LARGADA",
        "SPRINT START" to "LARGADA DA SPRINT",
        "STANDING START" to "LARGADA PARADA",
        "STANDING START PROCEDURE" to "PROCEDIMENTO DE LARGADA PARADA",
        "ROLLING START" to "LARGADA LANÇADA",
        "ROLLING START PROCEDURE" to "PROCEDIMENTO DE LARGADA LANÇADA",
        "DELAYED START" to "LARGADA ADIADA",
        "ABORTED START" to "LARGADA ABORTADA",
        "START ABORTED - EXTRA FORMATION LAP" to "LARGADA ABORTADA - VOLTA DE APRESENTAÇÃO EXTRA",
        "EXTRA FORMATION LAP" to "VOLTA DE APRESENTAÇÃO EXTRA",
        "STARTING PROCEDURE SUSPENDED" to "PROCEDIMENTO DE LARGADA SUSPENSO",
        "FORMATION LAP WILL BE STARTED BEHIND THE SAFETY CAR" to "VOLTA DE APRESENTAÇÃO ATRÁS DO SAFETY CAR",
        "FORMATION LAP(S) BEHIND SAFETY CAR" to "VOLTA(S) DE APRESENTAÇÃO ATRÁS DO SAFETY CAR",
        "RACE WILL START BEHIND THE SAFETY CAR" to "A CORRIDA LARGARÁ ATRÁS DO SAFETY CAR",
        "START ORDER: ORIGINAL GRID" to "ORDEM DE LARGADA: GRID ORIGINAL",
        "TRACK TEST COMPLETED" to "TESTE DE PISTA CONCLUÍDO",
        "TRACK LIMIT INFRINGEMENTS ARE STILL UNDER INVESTIGATION" to
            "AS INFRAÇÕES DE LIMITES DE PISTA AINDA ESTÃO SOB INVESTIGAÇÃO",
        "ALL CARS ARE RELEASED FROM POST-SPRINT PARC FERME" to "TODOS OS CARROS ESTÃO LIBERADOS DO PARQUE FECHADO PÓS-SPRINT",
    )

    override val reasons = mapOf(
        "CAUSING A COLLISION" to "CAUSAR UMA COLISÃO",
        "LEAVING THE TRACK AND GAINING AN ADVANTAGE" to "SAIR DA PISTA E OBTER VANTAGEM",
        "LEAVING THE TRACK MULTIPLE TIMES AND GAINING AN ADVANTAGE" to "SAIR DA PISTA VÁRIAS VEZES E OBTER VANTAGEM",
        "FORCING ANOTHER DRIVER OFF THE TRACK" to "FORÇAR OUTRO PILOTO PARA FORA DA PISTA",
        "FORCING ANOTHER CAR OFF THE TRACK" to "FORÇAR OUTRO CARRO PARA FORA DA PISTA",
        "FAILING TO FOLLOW RACE DIRECTORS INSTRUCTIONS" to "NÃO SEGUIR AS INSTRUÇÕES DO DIRETOR DE PROVA",
        "MAXIMUM DELTA TIME" to "TEMPO DELTA MÁXIMO",
        "PRACTICE START INFRINGEMENT" to "INFRAÇÃO NO TREINO DE LARGADA",
        "ESCAPE ROAD INSTRUCTIONS" to "INSTRUÇÕES DA ÁREA DE ESCAPE",
        "CROSSING THE LINE AT PIT EXIT" to "CRUZAR A LINHA NA SAÍDA DOS BOXES",
        "CROSSING THE LINE AT PIT ENTRY" to "CRUZAR A LINHA NA ENTRADA DOS BOXES",
        "LEAVING GARAGE BEFORE RESUMPTION TIME ANNOUNCED" to "SAIR DA GARAGEM ANTES DO HORÁRIO DE RETOMADA",
        "SPEEDING IN THE PIT LANE" to "EXCESSO DE VELOCIDADE NO PIT LANE",
        "TRACK LIMITS" to "LIMITES DE PISTA",
        "MULTIPLE TRACK LIMITS" to "MÚLTIPLAS INFRAÇÕES DE LIMITES DE PISTA",
        "MULTIPLE TRACK LIMIT INFRINGEMENTS" to "MÚLTIPLAS INFRAÇÕES DE LIMITES DE PISTA",
        "DOUBLE YELLOW" to "BANDEIRA AMARELA DUPLA",
        "YELLOW FLAG INFRINGEMENT" to "INFRAÇÃO DE BANDEIRA AMARELA",
        "FAILING TO SLOW UNDER YELLOW FLAGS" to "NÃO REDUZIR SOB BANDEIRA AMARELA",
        "RED FLAG INFRINGEMENT" to "INFRAÇÃO DE BANDEIRA VERMELHA",
        "IGNORING BLUE FLAGS" to "IGNORAR BANDEIRAS AZUIS",
        "UNSAFE RELEASE" to "LIBERAÇÃO INSEGURA",
        "RELEASING IN UNSAFE CONDITION" to "LIBERAÇÃO EM CONDIÇÃO INSEGURA",
        "RELEASED IN UNSAFE CONDITION" to "LIBERADO EM CONDIÇÃO INSEGURA",
        "UNSAFE CONDITION" to "CONDIÇÃO INSEGURA",
        "DRIVING IN AN UNSAFE CONDITION" to "PILOTAR EM CONDIÇÃO INSEGURA",
        "SAFETY CAR INFRINGEMENT" to "INFRAÇÃO SOB SAFETY CAR",
        "SAFETY CAR PROCEDURE INFRINGEMENT" to "INFRAÇÃO NO PROCEDIMENTO DE SAFETY CAR",
        "VIRTUAL SAFETY CAR INFRINGEMENT" to "INFRAÇÃO SOB VIRTUAL SAFETY CAR",
        "VSC INFRINGEMENT" to "INFRAÇÃO SOB VSC",
        "OVERTAKING UNDER SAFETY CAR" to "ULTRAPASSAR SOB SAFETY CAR",
        "ENTERING PIT LANE UNDER SAFETY CAR CONDITIONS" to "ENTRAR NO PIT LANE SOB SAFETY CAR",
        "IMPEDING" to "ATRAPALHAR OUTRO PILOTO",
        "STARTING PROCEDURE INFRINGEMENT" to "INFRAÇÃO NO PROCEDIMENTO DE LARGADA",
        "OUT OF POSITION AT SAFETY CAR LINE" to "FORA DE POSIÇÃO NA LINHA DO SAFETY CAR",
        "RESTART INFRINGEMENT" to "INFRAÇÃO NA RELARGADA",
        "FALSE START" to "LARGADA QUEIMADA",
        "JUMP START" to "LARGADA QUEIMADA",
        "MOVING BEFORE SIGNAL" to "MOVIMENTO ANTES DO SINAL",
        "OUT OF POSITION" to "FORA DE POSIÇÃO",
        "INCORRECT STARTING LOCATION" to "POSIÇÃO DE LARGADA INCORRETA",
        "INACCURATE GRID POSITION" to "POSIÇÃO NO GRID INCORRETA",
        "DRIVING ERRATICALLY" to "PILOTAGEM ERRÁTICA",
        "DANGEROUS DRIVING" to "PILOTAGEM PERIGOSA",
        "DRIVING UNNECESSARILY SLOWLY" to "PILOTAR DESNECESSARIAMENTE DEVAGAR",
        "MOVING UNDER BRAKING" to "MUDAR DE TRAJETÓRIA NA FRENAGEM",
        "MORE THAN ONE CHANGE OF DIRECTION" to "MAIS DE UMA MUDANÇA DE DIREÇÃO",
        "WEAVING ON STRAIGHT" to "ZIGUE-ZAGUE NA RETA",
        "REJOINING UNSAFELY" to "VOLTAR À PISTA DE FORMA INSEGURA",
        "UNSAFE REJOIN" to "RETORNO INSEGURO À PISTA",
        "FAILING TO SERVE TIME PENALTY CORRECTLY" to "NÃO CUMPRIR A PENALIDADE DE TEMPO CORRETAMENTE",
        "FAILING TO SERVE PENALTY CORRECTLY" to "NÃO CUMPRIR A PENALIDADE CORRETAMENTE",
        "SERVING PENALTY INCORRECTLY" to "CUMPRIR A PENALIDADE INCORRETAMENTE",
        "SERVING TIME PENALTY INCORRECTLY" to "CUMPRIR A PENALIDADE DE TEMPO INCORRETAMENTE",
        "PIT LANE INFRINGEMENT" to "INFRAÇÃO NO PIT LANE",
        "PIT STOP INFRINGEMENT" to "INFRAÇÃO NO PIT STOP",
        "LEAVING PIT EXIT ON RED LIGHT" to "SAIR DOS BOXES COM LUZ VERMELHA",
        "NOT ENTERING PIT LANE" to "NÃO ENTRAR NO PIT LANE",
        "CROSSING THE TRACK" to "ATRAVESSAR A PISTA",
        "ENTERING THE TRACK ON FOOT" to "ENTRAR NA PISTA A PÉ",
        "CAR SAFETY LIGHTS" to "LUZES DE SEGURANÇA DO CARRO",
        "LOOSE REAR WING" to "ASA TRASEIRA SOLTA",
        "CONTINUING ON TRACK AFTER RECEIVING ASSISTANCE" to "CONTINUAR NA PISTA APÓS RECEBER AJUDA",
        "UNSPORTSMANLIKE BEHAVIOUR" to "COMPORTAMENTO ANTIDESPORTIVO",
        "PLACE WAS GIVEN BACK" to "POSIÇÃO DEVOLVIDA",
        "POSITIONS GIVEN BACK" to "POSIÇÕES DEVOLVIDAS",
    )

    override val sessions = mapOf(
        "RACE" to "A CORRIDA",
        "SPRINT" to "A SPRINT",
        "SPRINT RACE" to "A SPRINT",
        "QUALIFYING" to "A CLASSIFICAÇÃO",
        "QUALIFYING SESSION" to "A CLASSIFICAÇÃO",
        "SPRINT QUALIFYING" to "A CLASSIFICAÇÃO SPRINT",
        "SPRINT QUALIFYING SESSION" to "A CLASSIFICAÇÃO SPRINT",
        "FIRST PRACTICE SESSION" to "O TREINO LIVRE 1",
        "SECOND PRACTICE SESSION" to "O TREINO LIVRE 2",
        "THIRD PRACTICE SESSION" to "O TREINO LIVRE 3",
        "SESSION" to "A SESSÃO",
        "FORMATION LAP" to "A VOLTA DE APRESENTAÇÃO",
    )

    override fun car(number: String) = "CARRO $number"
    override fun cars(numbers: String) = "CARROS $numbers"
    override val and = "E"
    override fun offence(n: String) = "(${n}ª INFRAÇÃO)"

    override fun atTurn(turn: String) = "NA CURVA $turn"
    override fun betweenTurns(first: String, second: String) = "ENTRE AS CURVAS $first E $second"
    override fun beforeTurn(turn: String) = "ANTES DA CURVA $turn"
    override val atPitEntry = "NA ENTRADA DOS BOXES"
    override val inPitLane = "NO PIT LANE"
    override val onStartFinishStraight = "NA RETA DE LARGADA/CHEGADA"
    override fun inPhase(phase: String) = "NO $phase"
    override fun onLap(lap: String) = "VOLTA $lap"

    override fun sectorFlag(flag: String, sector: String) = when (flag) {
        "CLEAR" -> "PISTA LIVRE NO SETOR $sector"
        "YELLOW" -> "BANDEIRA AMARELA NO SETOR $sector"
        "DOUBLE YELLOW" -> "BANDEIRA AMARELA DUPLA NO SETOR $sector"
        "TRACK SURFACE SLIPPERY" -> "PISTA ESCORREGADIA NO SETOR $sector"
        else -> null
    }
    override fun blueFlag(car: String, time: String) = "BANDEIRA AZUL PARA O $car ÀS $time"
    override fun blackAndWhiteFlag(car: String, reason: String?) =
        "BANDEIRA PRETA E BRANCA PARA O $car" + (reason?.let { " - $it" } ?: "")
    override fun blackAndOrangeFlag(car: String) = "BANDEIRA PRETA E LARANJA PARA O $car"
    override fun firstCarToTakeTheFlag(car: String) = "PRIMEIRO CARRO A RECEBER A BANDEIRADA - $car"

    override fun timeDeleted(car: String, time: String, detail: String) = "$car: TEMPO $time APAGADO - $detail"
    override fun lapDeleted(car: String, detail: String) = "$car: VOLTA APAGADA - $detail"
    override fun timeReinstated(car: String, time: String) = "$car: TEMPO $time SERÁ RESTABELECIDO"
    override fun lapReinstated(car: String, lap: String) = "$car: VOLTA $lap SERÁ RESTABELECIDA"
    override val notes = mapOf("PIT" to "(BOX)", "NEXT LAP" to "(PRÓXIMA VOLTA)", "NEXT LAP PIT" to "(PRÓXIMA VOLTA, BOX)")

    override fun incident(location: String?, involving: String?, status: IncidentStatus, reason: String?): String {
        val statusText = when (status) {
            IncidentStatus.NOTED -> "REGISTRADO"
            IncidentStatus.UNDER_INVESTIGATION -> "SOB INVESTIGAÇÃO"
            IncidentStatus.REVIEWED -> "ANALISADO, SEM NECESSIDADE DE INVESTIGAÇÃO"
            IncidentStatus.NO_FURTHER_ACTION -> "SEM PUNIÇÃO"
            IncidentStatus.AFTER_RACE -> "SERÁ INVESTIGADO APÓS A CORRIDA"
            IncidentStatus.AFTER_SESSION -> "SERÁ INVESTIGADO APÓS A SESSÃO"
            IncidentStatus.AFTER_SPRINT -> "SERÁ INVESTIGADO APÓS A SPRINT"
        }
        return listOfNotNull("INCIDENTE", location, involving?.let { "ENVOLVENDO O $it".replace("O CARROS", "OS CARROS") }, statusText)
            .joinToString(" ") + (reason?.let { " - $it" } ?: "")
    }
    override fun sanction(kind: Sanction, seconds: String?, car: String, reason: String?): String {
        val text = when (kind) {
            Sanction.TIME_PENALTY -> "PENALIDADE DE $seconds SEGUNDOS"
            Sanction.STOP_GO_PENALTY -> "STOP AND GO" + (seconds?.let { " DE $it SEGUNDOS" } ?: "")
            Sanction.DRIVE_THROUGH_PENALTY -> "DRIVE-THROUGH"
            Sanction.REPRIMAND -> "REPREENSÃO"
            Sanction.WARNING -> "ADVERTÊNCIA"
            Sanction.BLACK_FLAG -> "BANDEIRA PRETA"
        }
        return "$text PARA O $car" + (reason?.let { " - $it" } ?: "")
    }
    override fun restartProcedure(status: IncidentStatus) =
        incident(null, null, status, null).replace("INCIDENTE", "PROCEDIMENTO DE RELARGADA")
    override fun trackLimitsCount(count: String) = "$count INFRAÇÕES DE LIMITES DE PISTA"
    override val atPitExit = "NA SAÍDA DOS BOXES"
    override val onStartStraight = "NA RETA DE LARGADA"
    override fun penaltyServed(penalty: String) = "PENALIDADE CUMPRIDA - $penalty"

    override fun drs(enabled: Boolean, zone: String?) =
        (if (enabled) "DRS HABILITADO" else "DRS DESABILITADO") + (zone?.let { " NA ZONA $it" } ?: "")
    override fun marshals(location: String) = "FISCAIS NA PISTA $location"
    override fun recoveryVehicle(location: String) = "VEÍCULO DE RESGATE NA PISTA $location"
    override fun carOnTrack(car: String, event: CarEvent, location: String) = when (event) {
        CarEvent.OFF_TRACK -> "$car SAIU DA PISTA E CONTINUOU $location"
        CarEvent.SPUN -> "$car RODOU E CONTINUOU $location"
        CarEvent.STOPPED -> "$car PAROU $location"
        CarEvent.MISSED_APEX -> "$car NÃO TANGENCIOU O ÁPICE DA CURVA ${location.removePrefix("NA CURVA ")}"
    }
    override fun headPadding(color: String) = when (color) {
        "PINK" -> "OBRIGATÓRIO O PROTETOR DE CABEÇA ROSA"
        "BLUE" -> "OBRIGATÓRIO O PROTETOR DE CABEÇA AZUL"
        "LIGHT BLUE" -> "OBRIGATÓRIO O PROTETOR DE CABEÇA AZUL-CLARO"
        else -> null
    }
    override fun riskOfRain(session: String?, series: String?, percent: String) =
        "RISCO DE CHUVA PARA ${session ?: "ESTA SESSÃO"}${series?.let { " DA $it" } ?: ""}: $percent%"
    override fun willStart(subject: String, time: String) = "INÍCIO ${of(subject)} ÀS $time"
    override fun willResume(subject: String, time: String) = "RETOMADA ${of(subject)} ÀS $time"
    override fun startDelayed(subject: String) = "INÍCIO ${of(subject)} ADIADO"
    override fun estimatedStart(subject: String, time: String) = "HORÁRIO ESTIMADO DE INÍCIO ${of(subject)} - $time"
    override fun estimatedResumption(subject: String, time: String) = "HORÁRIO ESTIMADO DE RETOMADA ${of(subject)} - $time"
    override fun order(kind: OrderKind, list: String) = when (kind) {
        OrderKind.RESTART -> "ORDEM PARA A RELARGADA: $list"
        OrderKind.PUSH_TO_FRONT -> "EMPURRAR OS CARROS PARA A FRENTE NA ORDEM: $list"
        OrderKind.LAPPED_CARS -> "RETARDATÁRIOS PODEM ULTRAPASSAR O SAFETY CAR: $list"
    }

    /** "A CORRIDA" -> "DA CORRIDA", "O TREINO LIVRE 1" -> "DO TREINO LIVRE 1", "Q2" -> "DO Q2". */
    private fun of(subject: String) = when {
        subject.startsWith("A ") -> "DA " + subject.removePrefix("A ")
        subject.startsWith("O ") -> "DO " + subject.removePrefix("O ")
        else -> "DO $subject"
    }
}
