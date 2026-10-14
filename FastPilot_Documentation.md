# Documentação do Projeto: FastPilot

## 1. Visão Geral do Projeto
O **FastPilot** é um aplicativo multiplataforma para acompanhar dados da Fórmula 1 ao vivo e de corridas passadas, desenvolvido como Trabalho de Conclusão de Curso. Roda em **Android, iOS e Web** (navegador, via Kotlin/Wasm) a partir de uma única base de código Kotlin; um alvo **Desktop (JVM)** é mantido para desenvolvimento e para a avaliação.

O aplicativo consome duas APIs públicas (OpenF1 e Jolpica) e calcula localmente, em Kotlin puro e testado, as métricas de análise de corrida propostas no TCC I: ritmo limpo, degradação de pneu, volta teórica, duelo entre pilotos e delta na volta.

Tanto a lógica quanto a **interface** são compartilhadas: todas as telas são escritas uma vez em Compose Multiplatform. O código específico de cada plataforma se resume ao ponto de entrada do app, ao motor HTTP, ao driver SQLite e ao reprodutor de áudio. Medido com `cloc`, **94,9% do código dos apps Android + iOS + Web é compartilhado** (`metrics/shared_code.md`).

> **Mudança em relação ao TCC I.** O TCC I previa Jetpack Compose no Android e SwiftUI no iOS, com telas separadas. Na implementação a UI ficou inteira em Compose Multiplatform, o que eleva o reuso muito acima da meta de 75% e trouxe o alvo Web quase sem código adicional. O iOS usa SwiftUI apenas como hospedeiro (`ComposeUIViewController`).

---

## 2. Tecnologias e Bibliotecas (Stack)

| Biblioteca | Versão | Uso |
|---|---|---|
| Kotlin Multiplatform | 2.1.0 | Alvos `androidTarget`, `iosX64`/`iosArm64`/`iosSimulatorArm64`, `wasmJs`, `jvm("desktop")` |
| Compose Multiplatform | 1.7.3 | UI compartilhada em todas as plataformas; gráficos desenhados com `Canvas` |
| Voyager | 1.1.0-beta03 | Navegação por abas (`TabNavigator`) e entre telas |
| Koin | 4.0.1 | Injeção de dependência, incluindo ViewModels (`koin-compose-viewmodel`) |
| Ktor Client | 3.0.3 | HTTP: OkHttp (Android), Darwin (iOS), JS/fetch (Web), CIO (Desktop) |
| Kotlinx Serialization | 1.7.3 | JSON → data classes, ignorando chaves desconhecidas |
| SQLDelight | 2.1.0 | Cache local em SQLite (schema versão 2, com migração `1.sqm`) |
| Kotlinx Coroutines | 1.9.0 | ViewModels com `StateFlow`, polling ao vivo |
| Kotlinx Datetime | 0.6.1 | Datas das sessões, idade do cache, filtros por horário |
| Napier | 2.7.1 | Logs |

Bibliotecas declaradas no catálogo (`gradle/libs.versions.toml`) mas **não usadas** no código: Vico (gráficos) e Play Services Auth. Firebase não está integrado (ver §5).

---

## 3. APIs Externas Consumidas

### A. OpenF1 API (tempo real e telemetria)
* **Base:** `https://api.openf1.org/v1`
* **Endpoints usados:** `sessions`, `meetings`, `drivers`, `laps`, `stints`, `car_data` (velocidade, RPM, marcha, acelerador, freio), `location` (posição x/y/z na pista, em decímetros), `position`, `intervals`, `race_control`, `weather`, `team_radio`, `pit`.
* **Tempo real é pago:** desde 2025 a OpenF1 só entrega dados ao vivo (de 30 min antes até 30 min depois de cada sessão) para contas *Sponsor* (€9,90/mês, 60 requisições por minuto), autenticadas com token OAuth de 1 h (`POST /token`). O histórico desde 2023 continua grátis e sem autenticação.
* **Limites observados:** o plano gratuito aceita cerca de 30 requisições por minuto, além de um limite por segundo, e responde `429` acima disso (sempre com `Retry-After: 60`). Quando não há dados para o filtro pedido, responde `404 {"detail":"No results found."}` em vez de uma lista vazia.

### B. Jolpica API (sucessora do Ergast)
* **Base:** `https://api.jolpi.ca/ergast/f1/`
* **Uso:** classificação do campeonato de pilotos e de construtores (tela "Tabela").
* As respostas trazem `Cache-Control: max-age=600`; a API às vezes fica lenta (~5 s) ou responde `522`.

---

## 4. Arquitetura da Aplicação (MVVM + Clean Architecture)

O projeto tem dois módulos Gradle: `shared` (dados e domínio) e `composeApp` (apresentação). Cada um tem um conjunto de fontes comum (`commonMain`) e conjuntos por plataforma (`androidMain`, `iosMain`, `wasmJsMain`, `desktopMain`).

### Camada 1: Rede (`shared/.../network`)
* `OpenF1Api` e `JolpicaApi` encapsulam as chamadas HTTP (funções `suspend`).
* `HttpClientProvider` configura o cliente Ktor comum: Content Negotiation, `HttpRequestRetry` para `429`/`5xx` (até 5 tentativas, com espera de 2, 4, 8, 16 e 16 s, sem respeitar o `Retry-After: 60` da OpenF1, que estouraria o timeout), e `HttpTimeout` por tentativa. Também conta requisições e respostas `429` (`RequestStats`), usadas pelo benchmark de desempenho.
* `HttpClientFactory` (expect/actual) escolhe o motor HTTP de cada plataforma.

### Camada 2: Repositórios (`shared/.../data/repository`)
* **Com cache local (cache-first):** `SessionRepository`, `DriverRepository`, `LapRepository`, `StintRepository`, `StandingsRepository`. Leem o SQLite primeiro; se o dado estiver vencido, buscam na rede, gravam no banco e devolvem; em erro de rede, devolvem o cache vencido se houver.

  | Dado | Validade do cache |
  |---|---|
  | Sessões | 24 h (temporadas passadas: sem prazo) |
  | Pilotos | 6 h |
  | Voltas, stints | 1 h |
  | Dados brutos da sessão (`cached_samples`): telemetria e posições na pista de cada volta; voltas e stints de todos os pilotos; posições e intervalos finais; direção de prova, clima, rádio, pit stops, ultrapassagens | só sessões encerradas, sem prazo |
  | Classificação | 7 dias (temporada atual: 1 h, pois muda a cada corrida) |

* **Corrida encerrada não é baixada de novo (TCC I §4.5):** pilotos, voltas, stints e amostras baixados depois do fim da sessão (`dateEnd` + 1 h, `SessionRegistry.isFinal`) valem sem prazo; dados baixados durante a sessão seguem a validade da tabela. Os dados brutos ficam guardados como o JSON da API (`SamplesCache`, schema v3, migração `2.sqm`). Com isso, uma corrida já aberta funciona sem internet em todas as telas (teste em `metrics/offline/README.md`); só os áudios de rádio precisam de rede.
* **Limpeza de 30 dias:** ao abrir o app, `CacheMaintenance` apaga pilotos, voltas, stints e amostras baixados há mais de 30 dias. O calendário e a classificação são pequenos e ficam.
* **Sessão ao vivo:** tudo vem da rede, nada é guardado (os dados ainda mudam). A OpenF1 responde `404 {"detail":"No results found."}` quando não há resultado; `OpenF1Api` trata isso como lista vazia.
* **Web sem cache:** o SQLDelight não tem driver síncrono para o navegador, então no Web o banco não é registrado no Koin (`db = null`) e todo carregamento vai à rede.
* **Temporadas:** as telas listam de 2023 (início da OpenF1) até a temporada atual, calculada pela data do aparelho (`availableSeasons`), só com sessões que já começaram (a OpenF1 lista o calendário inteiro com antecedência), da mais nova para a mais antiga. Abrem na sessão ao vivo ou, sem ela, na corrida mais recente (`RecentSessions.kt`). A Classificação usa as mesmas temporadas.
* `LiveSessionDetector` considera ao vivo a sessão em que `dateStart ≤ agora ≤ dateEnd + 30 min`, a mesma janela de tempo real da OpenF1 (cobre atrasos e a classificação final). Com a tela aberta, o Dashboard confere a cada 30 s se uma sessão começou, abrindo-a ao vivo se o usuário não escolheu outra, ou terminou, voltando a uma carga única.
* Durante sessões ao vivo a API é reconsultada nos intervalos do TCC I (§4.5): posições e intervalos a cada 4 s (Dashboard), rádio a cada 15 s (Rádio), clima a cada 60 s (Clima); além disso race control a cada 10 s e pit stops e ultrapassagens a cada 15 s (Dashboard). Com todas as telas abertas são ≈49 requisições por minuto, abaixo das 60 do plano Sponsor. Voltas, stints e amostras de uma sessão ao vivo ignoram o cache local.
* **Proxy de tempo real** (`api/openf1.mjs`, função da Vercel): guarda a conta Sponsor nas variáveis `OPENF1_USERNAME`/`OPENF1_PASSWORD`, obtém e renova o token e repassa só os endpoints usados pelo app, com cache de 4 s na CDN para que vários usuários dividam a mesma cota. No app, `OpenF1Api` manda ao proxy (`OPENF1_PROXY_URL`) só as requisições de sessões ao vivo (`SessionRegistry`, alimentado pelo `SessionRepository`) e repete pelo proxy uma requisição recusada com `401`; o histórico vai direto à OpenF1, na cota grátis de cada usuário. Nenhuma senha vai no app.

### Camada 3: Domínio — métricas (`shared/.../domain/usecase`)
Todas em Kotlin puro, sem dependência de plataforma, com testes unitários:

| Use case | O que calcula |
|---|---|
| `CalculateCleanPaceUseCase` | Ritmo limpo: média e mediana das voltas representativas (após `LapFilters`, descarta ainda os 5% mais rápidos e os 5% mais lentos), desvio-padrão, número de voltas limpas. |
| `GetTyreDegradationUseCase` | Degradação por stint: inclinação (s/volta) da regressão linear do tempo de volta pela idade do pneu. |
| `CalculateTheoreticalBestUseCase` | Volta teórica = melhor S1 + melhor S2 + melhor S3, comparada com a melhor volta real. |
| `GetDriverDuelUseCase` | Duelo volta a volta entre dois pilotos, só nas voltas que ambos completaram (ignora saída de box). |
| `CalculateLapDeltaUseCase` | Delta na volta (TCC I §4.7): diferença de tempo ao longo da volta, projetando as amostras de `/location` (~3,7 Hz) de um piloto na linha do outro e interpolando no tempo. |
| `CalculateChampionshipGapsUseCase` | Diferença de pontos para o líder e para o anterior na classificação. |
| `LapFilters` | Filtros comuns ao ritmo limpo e à degradação (ver abaixo). |

**Voltas atípicas (`LapFilters`).** São descartadas a volta 1 (largada parada), as voltas de entrada e saída de box, a volta do abandono e as voltas sob safety car, VSC ou bandeira vermelha. Os períodos neutralizados são reconstruídos a partir de `/race_control` **por horário**, não por número de volta: a direção de prova numera as mensagens pela volta do líder, e um retardatário está em outra volta no mesmo instante. Bandeira amarela local não invalida a volta.

### Camada 4: Apresentação (`composeApp/.../ui`)
* Um ViewModel por tela, expondo um `UiState` imutável via `StateFlow`; as coletas rodam em `viewModelScope`, tratando `CancellationException` para não confundir troca de aba com erro.
* Injeção pelo Koin (`ui/di`), navegação pelo Voyager (`ui/navigation/Tabs.kt`).

**Telas:**
1. **Splash / Login** — botões "Continuar com o Google" e "Continuar como Convidado". Hoje os dois apenas entram no app (ver §5).
2. **Dashboard** — seletor de ano, corrida e sessão; posições, intervalos, **eventos** (pit stops com tempo parado e ultrapassagens, os 30 mais recentes; `GetRaceEventsUseCase`) e mensagens da direção de prova. Em sessão ao vivo, liga o polling. O status do DRS, citado no TCC I (§4.6), não é mostrado: o DRS foi abolido pelo regulamento de 2026, e só sessões de 2026 em diante podem ser acompanhadas ao vivo.
3. **Telemetria** — gráficos de velocidade, RPM, marcha e acelerador de um piloto e o card **Volta Teórica** (teórica, melhor volta real, diferença e setores com a volta de origem).
4. **Estratégia** — linha do tempo de stints com a cor do composto, **degradação** (s/volta) sob cada stint e a seção **Ritmo Limpo** (mediana, diferença para o 1º, σ, voltas limpas/total; mínimo de 5 voltas limpas; só em corridas).
5. **Duelo** — comparação volta a volta entre dois pilotos e o card **Delta na Volta Mais Rápida**.
6. **Rádio** — lista de mensagens de rádio com reprodução de áudio (`AudioPlayer` expect/actual em cada plataforma); em sessão ao vivo, busca mensagens novas a cada 15 s.
7. **Clima** — temperatura do ar e da pista, vento, umidade e chuva; em sessão ao vivo, atualiza a cada 60 s.
8. **Tabela** — classificação de pilotos e construtores com diferenças de pontos.

### Código específico por plataforma
| Plataforma | Ponto de entrada | Específico |
|---|---|---|
| Android | `MainActivity` | OkHttp, `AndroidSqliteDriver`, `MediaPlayer` |
| iOS | `MainViewController` + `iosApp/` (SwiftUI hospedeiro, projeto gerado por XcodeGen) | Darwin, `NativeSqliteDriver`, `AVPlayer` |
| Web | `main()` com `ComposeViewport` | fetch, sem banco, `HTMLAudioElement` |
| Desktop | `main()` com `Window` | CIO, `JdbcSqliteDriver`, áudio apenas simulado (log) |

---

## 5. Limitações Conhecidas
* **Login:** a autenticação com Google/Firebase não está implementada; os dois botões da tela de login apenas entram no app.
* **Web sem cache local:** todo carregamento vai à rede (ver §4, Camada 2).
* **Limite da OpenF1:** telas que disparam muitas requisições ao mesmo tempo recebem `429` e dependem das novas tentativas. O benchmark de desempenho (§6) mediu o polling com race control a cada 5 s (hoje 10 s).
* **Tempo real depende da assinatura:** sem o proxy configurado (ou com a assinatura vencida), uma sessão ao vivo só mostra dados quando a OpenF1 os libera, 30 min depois do fim. A cota Sponsor (60 req/min) é dividida entre todos os usuários; o cache de 4 s do proxy limita o consumo por endpoint, não por usuário.
* **Telemetria, Estratégia e Duelo** carregam uma sessão ao vivo uma vez (sem polling); para atualizar, basta selecioná-la de novo.
* **Precisão do delta na volta:** com amostragem de ~3,7 Hz, a linha traçada fica 1–3% mais curta que o traçado oficial, porque corta as curvas; o erro de fechamento contra a diferença oficial fica entre 3 e 113 ms.

---

## 6. Avaliação (TCC II)
Os resultados são gerados por scripts em `scripts/` e gravados em `metrics/`:

* **Código compartilhado** — `scripts/measure_shared_code.py` → `metrics/shared_code.md`: 94,9% (Android 97,8%, iOS 98,1%, Web 98,8%).
* **Precisão em relação à biblioteca FastF1** — cinco corridas (Bahrein 2023, Bélgica 2023, Singapura 2023, Mônaco 2024, São Paulo 2024), com respostas gravadas em `fixtures/`. MAPE de 0,00% para ritmo limpo, volta teórica e melhor volta; degradação com erro médio de 0,006 s/volta e mesmo sinal em 221/221 stints; delta na volta com erro médio de ~0,1 s e diferença final idêntica. Detalhes em `metrics/comparison/README.md` e `metrics/comparison/NOTAS.md`.
* **Desempenho** — benchmark embutido no app (`composeApp/.../ui/perf/`) que mede o tempo de carregamento de cada tela com e sem cache, com e sem polling ao vivo, enquanto `scripts/perf_run.py` amostra CPU e memória. Resultados em `metrics/performance/README.md`.

Instruções de build, testes e reprodução das medições estão no `README.md`.
