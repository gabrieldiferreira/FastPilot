# Desempenho — tempos de carregamento, memória e CPU

Gerado por `scripts/perf_report.py` a partir das execuções de `scripts/perf_run.py` (benchmark embutido no app: `composeApp/src/commonMain/kotlin/com/fastpilot/ui/perf/LoadBenchmark.kt`).

**Metodologia.** Cada tela é aberta criando o seu ViewModel, como faz a navegação, e o tempo vai até o estado que exibe o conteúdo (renderização do quadro não incluída). *Sem cache*: banco local (SQLDelight) e cache HTTP da plataforma limpos antes da medição; *com cache*: a mesma tela logo em seguida, com o banco preenchido (a sessão medida já terminou: o app guarda pilotos, voltas, stints, posições finais, intervalos, telemetria e posições na pista da volta, rádio, clima, direção de prova, pit stops, ultrapassagens e a classificação; só os áudios de rádio não entram). *Polling*: o modo ao vivo do Dashboard simulado em segundo plano, direto na API (posições e intervalos a cada 4 s, direção de prova a cada 10 s, pit stops e ultrapassagens a cada 15 s, clima a cada 60 s). Pausa antes de cada medição (15 s nas rodadas 1–2, 20 s na 3) para respeitar a cota grátis da OpenF1 (3 requisições/s e 30/min). Desde a rodada 3 o cliente espaça as requisições (uma a cada 400 ms, no máximo 29 em 61 s) em vez de receber 429; respostas 429 que ainda ocorram são repetidas após 2, 4, 8, 16 e 16 s. Sessão: a corrida mais recente no dia da medição (GP de Singapura 2026), a mesma que as telas escolhem por padrão. Valores em segundos: mediana (média ± desvio padrão), apenas medições bem-sucedidas; n = sucessos/tentativas.

¹ Tempo a partir da ação do usuário (selecionar piloto / os dois pilotos). ² Tempo a partir da abertura da tela (as métricas continuam carregando depois dos stints).

## Ambientes

| Pasta | Ambiente | Build |
|---|---|---|
| `android_emulator` | Android 17 — sdk_gphone16k_arm64 (emulador) | benchmark (release, not debuggable) |
| `ios_simulator` | iOS 26.5 — iPhone 17 (simulador) | Release |
| `web_chrome` | Web — Chrome/155.0.8059.40 headless | wasmJs production |

## Android 17 — sdk_gphone16k_arm64 (emulador)

Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo sozinho (~46/min) esgota a cota.

| Tela | Sem cache | Com cache |
|---|---|---|
| Dashboard | 1,70 (3,32 ± 3,93) n=5/5 | 0,04 (0,05 ± 0,03) n=5/5 |
| Telemetria — abertura | 1,77 (2,68 ± 2,55) n=5/5 | 0,03 (0,03 ± 0,02) n=5/5 |
| Telemetria — volta do piloto¹ | 1,59 (1,50 ± 0,43) n=5/5 | 0,02 (0,02 ± 0,02) n=5/5 |
| Estratégia — stints | 1,19 (1,16 ± 0,06) n=5/5 | 1,42 (1,42 ± 0,13) n=5/5 |
| Estratégia — ritmo/degradação² | 4,71 (4,52 ± 0,53) n=5/5 | 1,42 (1,42 ± 0,13) n=5/5 |
| Duelo — abertura | 0,55 (0,52 ± 0,10) n=5/5 | 0,01 (0,02 ± 0,01) n=5/5 |
| Duelo — comparação¹ | 3,39 (3,12 ± 0,52) n=5/5 | 0,04 (0,04 ± 0,02) n=5/5 |
| Rádio | 0,72 (0,73 ± 0,09) n=5/5 | 0,02 (0,02 ± 0,01) n=5/5 |
| Clima | 0,33 (0,36 ± 0,04) n=5/5 | 0,04 (0,04 ± 0,01) n=5/5 |
| Classificação | 0,71 (0,86 ± 0,59) n=5/5 | 0,01 (0,01 ± 0,01) n=5/5 |

Mediana das razões por tela: com cache, a mediana das telas cai para **3%** do tempo sem cache.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 116 | 0 | 0 de 50 |
| Com cache | 0 | 0 | 0 de 50 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | PSS (MB) média | PSS (MB) pico | RSS (MB) média | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|---|
| Ocioso | 48,1 | 48,1 | 170,0 | 0,0 | 0,0 |
| Carregando telas | 52,5 | 63,2 | 185,0 | 2,9 | 8,0 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## iOS 26.5 — iPhone 17 (simulador)

Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo sozinho (~46/min) esgota a cota.

| Tela | Sem cache | Com cache |
|---|---|---|
| Dashboard | 1,07 (1,19 ± 0,26) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Telemetria — abertura | 0,64 (0,65 ± 0,02) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Telemetria — volta do piloto¹ | 0,80 (0,82 ± 0,04) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 |
| Estratégia — stints | 1,27 (1,27 ± 0,04) n=5/5 | 0,46 (0,46 ± 0,01) n=5/5 |
| Estratégia — ritmo/degradação² | 2,91 (2,93 ± 0,09) n=5/5 | 0,46 (0,46 ± 0,01) n=5/5 |
| Duelo — abertura | 0,66 (0,66 ± 0,02) n=5/5 | 0,01 (0,01 ± 0,01) n=5/5 |
| Duelo — comparação¹ | 1,60 (1,65 ± 0,08) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 |
| Rádio | 0,85 (0,87 ± 0,04) n=5/5 | 0,02 (0,02 ± 0,01) n=5/5 |
| Clima | 0,66 (0,67 ± 0,04) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Classificação | 0,15 (0,36 ± 0,33) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 |

Mediana das razões por tela: com cache, a mediana das telas cai para **2%** do tempo sem cache.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 116 | 0 | 0 de 50 |
| Com cache | 0 | 0 | 0 de 50 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | RSS (MB) média | RSS (MB) pico | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|
| Ocioso | 169,8 | 175,3 | 4,1 | 44,2 |
| Carregando telas | 159,2 | 168,2 | 0,8 | 2,4 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## Web — Chrome/155.0.8059.40 headless

Sem banco local nesta plataforma: todas as medições são *sem cache*.

Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo sozinho (~46/min) esgota a cota.

| Tela | Sem cache |
|---|---|
| Dashboard | 0,65 (0,93 ± 0,59) n=5/5 |
| Telemetria — abertura | 0,24 (0,24 ± 0,02) n=5/5 |
| Telemetria — volta do piloto¹ | 0,96 (0,99 ± 0,06) n=5/5 |
| Estratégia — stints | 1,04 (1,10 ± 0,14) n=5/5 |
| Estratégia — ritmo/degradação² | 2,99 (3,17 ± 0,31) n=5/5 |
| Duelo — abertura | 0,24 (0,24 ± 0,01) n=5/5 |
| Duelo — comparação¹ | 1,80 (1,79 ± 0,02) n=5/5 |
| Rádio | 0,64 (0,64 ± 0,01) n=5/5 |
| Clima | 0,24 (0,24 ± 0,02) n=5/5 |
| Classificação | 0,18 (0,24 ± 0,21) n=5/5 |

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 116 | 0 | 0 de 50 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | RSS (MB) média | RSS (MB) pico | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|
| Ocioso | 539,1 | 540,5 | 0,4 | 2,7 |
| Carregando telas | 558,3 | 673,5 | 1,3 | 2,9 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## Comparação entre plataformas (sem cache, sem polling — mediana em s)

| Tela | `android_emulator` | `ios_simulator` | `web_chrome` |
|---|---|---|---|
| Dashboard | 1,70 | 1,07 | 0,65 |
| Telemetria — abertura | 1,77 | 0,64 | 0,24 |
| Telemetria — volta do piloto¹ | 1,59 | 0,80 | 0,96 |
| Estratégia — stints | 1,19 | 1,27 | 1,04 |
| Estratégia — ritmo/degradação² | 4,71 | 2,91 | 2,99 |
| Duelo — abertura | 0,55 | 0,66 | 0,24 |
| Duelo — comparação¹ | 3,39 | 1,60 | 1,80 |
| Rádio | 0,72 | 0,85 | 0,64 |
| Clima | 0,33 | 0,66 | 0,24 |
| Classificação | 0,71 | 0,15 | 0,18 |
