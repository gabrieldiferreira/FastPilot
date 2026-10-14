package com.fastpilot.ui.i18n

/** Português brasileiro. */
object PtBrStrings : Strings {
    override val loginTagline = "Telemetria de elite nas suas mãos"
    override val loginWithGoogle = "Continuar com o Google"
    override val loginAsGuest = "Continuar como Convidado"
    override val language = "Idioma"

    override val routes = Routes(
        login = "/entrar",
        dashboard = "/painel",
        telemetry = "/telemetria",
        strategy = "/estrategia",
        duel = "/duelo",
        radio = "/radio",
        weather = "/clima",
        standings = "/classificacao",
    )

    override val tabDashboard = "Dashboard"
    override val tabTelemetry = "Telemetria"
    override val tabStrategy = "Estratégia"
    override val tabDuel = "Duelo"
    override val tabRadio = "Rádio"
    override val tabWeather = "Clima"
    override val tabStandings = "Tabela"

    override val decimalSeparator = ','
    override val loading = "Carregando..."
    override val select = "Selecione"
    override val selectAnotherSession = "Selecione outra sessão acima"
    override val selectYear = "Selecionar ano"
    override val selectRaceWeekend = "Selecionar etapa"
    override val selectDriver = "Selecionar piloto"
    override val selectADriver = "Selecione um piloto"
    override val preSeasonTesting = "Testes de pré-temporada"
    override fun round(number: Int) = "Etapa $number"
    override fun lap(number: Int) = "Volta $number"
    override fun gap(apiGap: String) = lapsDown(apiGap)?.let { "+$it " + if (it == 1) "VOLTA" else "VOLTAS" } ?: apiGap
    override fun localTimes(offset: String) = "Horários no seu fuso ($offset)"
    override fun lapShort(number: Int) = "V$number"
    override fun sessionShort(sessionName: String?): String {
        val (kind, n) = sessionName?.let { sessionKind(it) } ?: return sessionName ?: "?"
        return when (kind) {
            SessionKind.PRACTICE -> "TL$n"
            SessionKind.TESTING_DAY -> "Dia $n"
            SessionKind.SPRINT_QUALIFYING -> "Classif. Sprint"
            SessionKind.SPRINT -> "Sprint"
            SessionKind.QUALIFYING -> "Classificação"
            SessionKind.RACE -> "Corrida"
        }
    }
    override fun sessionLong(sessionName: String?): String {
        val (kind, n) = sessionName?.let { sessionKind(it) } ?: return sessionName ?: "?"
        return when (kind) {
            SessionKind.PRACTICE -> "Treino Livre $n"
            SessionKind.TESTING_DAY -> "Dia $n de testes"
            SessionKind.SPRINT_QUALIFYING -> "Classificação Sprint"
            SessionKind.SPRINT -> "Sprint"
            SessionKind.QUALIFYING -> "Classificação"
            SessionKind.RACE -> "Corrida"
        }
    }
    override fun error(error: UiError): String {
        val text = when (error.kind) {
            ErrorKind.NO_SESSIONS -> return "Nenhuma sessão encontrada. Verifique sua conexão."
            ErrorKind.SESSIONS -> "Erro ao carregar sessões"
            ErrorKind.POSITIONS -> "Erro ao carregar posições"
            ErrorKind.DRIVERS -> "Erro ao carregar pilotos"
            ErrorKind.LAPS -> "Erro ao carregar voltas"
            ErrorKind.TELEMETRY -> "Erro ao carregar telemetria"
            ErrorKind.RADIO -> "Erro ao carregar rádios"
            ErrorKind.WEATHER -> "Erro ao carregar clima"
            ErrorKind.STANDINGS -> "Erro ao carregar classificação"
            ErrorKind.DUEL -> "Erro ao comparar pilotos"
            ErrorKind.GENERIC -> "Erro"
        }
        return error.detail?.let { "$text: $it" } ?: text
    }
    override fun place(name: String?) = name?.let { PLACES_PT_BR[it] ?: it }.orEmpty()
    override fun raceControl(message: String) = translateRaceControl(message, RaceControlPtBr)
    override val retry = "Tentar novamente"
    override val refresh = "Atualizar"
    override val yes = "Sim"
    override val no = "Não"

    override val loadingData = "Carregando dados..."
    override val noPositions = "Nenhuma posição encontrada"
    override val live = "AO VIVO"
    override val historical = "HISTÓRICO"
    override val fastest = "Mais rápida"
    override val noTime = "Sem tempo"
    override val events = "Eventos"
    override val raceControl = "Direção de Prova"
    override fun pitStop(driver: String) = "$driver no box"
    override fun stationary(seconds: String) = "parado $seconds s"
    override fun pitLane(seconds: String) = "pit lane $seconds s"
    override fun overtake(driver: String, overtaken: String) = "$driver passa $overtaken"
    override val overtakeBadge = "ULTRAP"

    override val telemetryTitle = "Telemetria"
    override val driver = "Piloto"
    override val noDriversForSession = "Nenhum piloto disponível para esta sessão"
    override val laps = "Voltas"
    override fun noLapsFor(driver: String?) = "Nenhuma volta registrada para ${driver ?: "este piloto"}"
    override val loadingTelemetry = "Carregando telemetria..."
    override val speedChart = "Velocidade (km/h)"
    override val throttleBrakeChart = "Acelerador / Freio"
    override val throttle = "Acelerador"
    override val brake = "Freio"
    override val gear = "Marcha"
    override val selectDriverAbove = "Selecione um piloto acima"
    override val toSeeTelemetry = "para ver a telemetria"
    override val lapLabel = "Volta"
    override val timeLabel = "Tempo"
    override val theoreticalLap = "Volta Teórica"
    override val theoreticalLapHint = "Soma dos melhores setores da sessão"
    override val theoretical = "Teórica"
    override val bestActualLap = "Melhor volta real"
    override val difference = "Diferença"

    override val strategyTitle = "Estratégia de Pneus"
    override val loadingStrategy = "Carregando estratégia..."
    override val noStints = "Nenhum stint encontrado"
    override val selectARace = "Selecione uma sessão de corrida"
    override val degradationHint = "Degradação: aumento do tempo de volta por volta de uso do pneu " +
        "(regressão linear sobre as voltas limpas de cada stint)"
    override val pitStops = "Pit Stops"
    override val perLap = "s/volta"
    override val cleanPace = "Ritmo Limpo"
    override val cleanPaceHint = "Mediana das voltas representativas: sem volta 1, entrada/saída de box, " +
        "safety car, VSC e bandeira vermelha; 5% mais rápidas e mais lentas descartadas"
    override val raceControlUnavailable =
        "Mensagens da direção de prova indisponíveis: voltas sob safety car podem estar incluídas"
    override fun cleanLaps(clean: Int, total: Int) = "$clean/$total voltas limpas"
    override val reference = "referência"
    override val pitStopLabel = "pit stop"

    override val duelTitle = "Duelo de Pilotos"
    override fun driverN(n: Int) = "Piloto $n"
    override val selectTwoDrivers = "Selecione dois pilotos acima"
    override val toComparePerformance = "para comparar o desempenho"
    override val computingComparison = "Calculando comparação..."
    override val cumulativeDelta = "Delta Acumulado"
    override fun positiveMeansAhead(driver: String) = "Positivo = $driver à frente"
    override val averagePace = "Ritmo Médio"
    override val bestLap = "Melhor Volta"
    override val validLaps = "Voltas Válidas"
    override val noCommonLaps = "Sem dados de voltas comuns entre os pilotos selecionados"
    override val fastestLapDelta = "Delta na Volta Mais Rápida"
    override val positionDataUnavailable = "Dados de posição indisponíveis para estas voltas"
    override fun lapVsLap(lap1: Int, driver1: String, lap2: Int, driver2: String) =
        "Volta $lap1 de $driver1 vs volta $lap2 de $driver2 • acima de 0 = $driver1 à frente"
    override fun fasterBy(driver: String, gap: String) = "$driver mais rápido por $gap"
    override val sameLapTime = "Mesmo tempo de volta"

    override val radioTitle = "Rádio de Equipe"
    override val all = "Todos"
    override val loadingRadios = "Carregando rádios..."
    override val noRadios = "Nenhuma comunicação de rádio"
    override val noRadiosForSession = "encontrada para esta sessão"
    override fun radioCount(count: Int) = if (count == 1) "1 comunicação" else "$count comunicações"
    override val pause = "Pausar"
    override val listen = "Ouvir"
    override val playing = "Reproduzindo nativamente..."
    override val listenInApp = "Ouvir comunicação no App"
    override val audioUnavailable = "URL do áudio não disponível"

    override val weatherTitle = "Condições Climáticas"
    override val loadingWeather = "Carregando dados climáticos..."
    override val wetConditions = "Condições de chuva"
    override val dryTrack = "Pista seca"
    override val airTemp = "Temp. Ar"
    override val trackTemp = "Temp. Pista"
    override val humidity = "Umidade"
    override val rain = "Chuva"
    override val wind = "Vento"
    override val pressure = "Pressão"
    override val sessionHistory = "Histórico da sessão"
    override fun readings(count: Int) = if (count == 1) "1 medição" else "$count medições"
    override val trackPrefix = "Pista: "
    override val windPrefix = "Vento: "
    override val noWeather = "Sem dados de clima"
    override val windDirection = "Direção do Vento"

    override val standingsTitle = "Classificação"
    override fun seasonAfterRound(season: Int, round: Int) = "Temporada $season • após a etapa $round"
    override fun season(season: Int) = "Temporada $season"
    override val drivers = "Pilotos"
    override val constructors = "Equipes"
    override val loadingStandings = "Carregando classificação..."
    override val noStandings = "Sem classificação para esta temporada"
    override val wins = "Vitórias"
    override val leader = "líder"
}
