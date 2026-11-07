# Desempenho — tempos de carregamento, memória e CPU

Gerado por `scripts/perf_report.py` a partir das execuções de `scripts/perf_run.py` (benchmark embutido no app: `composeApp/src/commonMain/kotlin/com/fastpilot/ui/perf/LoadBenchmark.kt`).

**Metodologia.** Cada tela é aberta criando o seu ViewModel, como faz a navegação, e o tempo vai até o estado que exibe o conteúdo (renderização do quadro não incluída). *Sem cache*: banco local (SQLDelight), cache HTTP da plataforma e, desde a rodada 4, o calendário e a classificação guardados em memória limpos antes da medição; *com cache*: a mesma tela logo em seguida, com o banco preenchido (a sessão medida já terminou: o app guarda pilotos, voltas, stints, posições finais, intervalos, telemetria e posições na pista da volta, rádio, clima, direção de prova, pit stops, ultrapassagens e a classificação; só os áudios de rádio não entram). *Polling*: o modo ao vivo do Dashboard simulado em segundo plano, direto na API (posições e intervalos a cada 4 s, direção de prova a cada 10 s, pit stops e ultrapassagens a cada 15 s, clima a cada 60 s). Pausa antes de cada medição (15 s nas rodadas 1–2, 20 s nas rodadas 3 e 4) para respeitar a cota grátis da OpenF1 (3 requisições/s e 30/min). Desde a rodada 3 o cliente espaça as requisições (uma a cada 400 ms, no máximo 29 em 61 s) em vez de receber 429; respostas 429 que ainda ocorram são repetidas após 2, 4, 8, 16 e 16 s. Sessão: a corrida mais recente no dia da medição (rodada 4, 06/11/2026: GP do México 2026), a mesma que as telas escolhem por padrão. Valores em segundos: mediana (média ± desvio padrão), apenas medições bem-sucedidas; n = sucessos/tentativas.

¹ Tempo a partir da ação do usuário (selecionar piloto / os dois pilotos). ² Tempo a partir da abertura da tela (as métricas continuam carregando depois dos stints).

## Ambientes

| Pasta | Ambiente | Build |
|---|---|---|
| `android_emulator` | Android 17 — sdk_gphone16k_arm64 (emulador) | benchmark (release, not debuggable) |
| `ios_simulator` | iOS 26.5 — iPhone 17 (simulador) | Release |
| `web_chrome` | Web — Chrome/157.0.8103.38 headless | wasmJs production |

## Android 17 — sdk_gphone16k_arm64 (emulador)

Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo sozinho (~46/min) esgota a cota.

| Tela | Sem cache | Com cache |
|---|---|---|
| Dashboard | 3,77 (3,78 ± 0,10) n=5/5 | 0,02 (0,02 ± 0,01) n=5/5 |
| Telemetria — abertura | 2,12 (2,14 ± 0,09) n=5/5 | 0,01 (0,02 ± 0,00) n=5/5 |
| Telemetria — volta do piloto¹ | 0,83 (0,84 ± 0,06) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 |
| Estratégia — stints | 2,91 (2,96 ± 0,16) n=5/5 | 0,01 (0,01 ± 0,01) n=5/5 |
| Estratégia — ritmo/degradação² | 4,59 (4,72 ± 0,42) n=5/5 | 0,35 (0,36 ± 0,04) n=5/5 |
| Duelo — abertura | 2,17 (2,15 ± 0,07) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 |
| Duelo — comparação¹ | 1,69 (1,87 ± 0,43) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 |
| Rádio | 2,47 (3,05 ± 1,33) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 |
| Clima | 2,10 (2,26 ± 0,42) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Classificação | 1,23 (1,11 ± 0,49) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 |

Mediana das razões por tela: com cache, a mediana das telas cai para **1%** do tempo sem cache.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 150 | 0 | 0 de 50 |
| Com cache | 0 | 0 | 0 de 50 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | PSS (MB) média | PSS (MB) pico | RSS (MB) média | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|---|
| Ocioso | 42,5 | 42,5 | 169,6 | 1,1 | 10,7 |
| Carregando telas | 55,8 | 66,6 | 185,8 | 0,9 | 4,0 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## iOS 26.5 — iPhone 17 (simulador)

Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo sozinho (~46/min) esgota a cota.

| Tela | Sem cache | Com cache |
|---|---|---|
| Dashboard | 1,54 (1,67 ± 0,29) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Telemetria — abertura | 1,09 (1,09 ± 0,02) n=5/5 | 0,02 (0,02 ± 0,01) n=5/5 |
| Telemetria — volta do piloto¹ | 0,86 (0,87 ± 0,02) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 |
| Estratégia — stints | 1,94 (1,93 ± 0,04) n=5/5 | 0,02 (0,01 ± 0,01) n=5/5 |
| Estratégia — ritmo/degradação² | 3,57 (3,65 ± 0,18) n=5/5 | 0,49 (0,49 ± 0,01) n=5/5 |
| Duelo — abertura | 1,13 (1,14 ± 0,05) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Duelo — comparação¹ | 1,67 (1,67 ± 0,04) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 |
| Rádio | 1,51 (1,51 ± 0,04) n=5/5 | 0,02 (0,01 ± 0,01) n=5/5 |
| Clima | 1,15 (1,16 ± 0,07) n=5/5 | 0,02 (0,02 ± 0,00) n=5/5 |
| Classificação | 0,21 (0,27 ± 0,11) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 |

Mediana das razões por tela: com cache, a mediana das telas cai para **1%** do tempo sem cache.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 150 | 0 | 0 de 50 |
| Com cache | 0 | 0 | 0 de 50 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | RSS (MB) média | RSS (MB) pico | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|
| Ocioso | 292,5 | 306,2 | 0,1 | 0,4 |
| Carregando telas | 178,1 | 302,7 | 1,0 | 3,6 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## Web — Chrome/157.0.8103.38 headless

Sem banco local nesta plataforma: todas as medições são *sem cache*.

Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo sozinho (~46/min) esgota a cota.

| Tela | Sem cache |
|---|---|
| Dashboard | 1,08 (1,19 ± 0,27) n=5/5 |
| Telemetria — abertura | 0,69 (0,71 ± 0,05) n=5/5 |
| Telemetria — volta do piloto¹ | 0,84 (0,88 ± 0,08) n=5/5 |
| Estratégia — stints | 1,47 (1,54 ± 0,16) n=5/5 |
| Estratégia — ritmo/degradação² | 3,47 (3,55 ± 0,22) n=5/5 |
| Duelo — abertura | 0,72 (0,71 ± 0,05) n=5/5 |
| Duelo — comparação¹ | 1,68 (1,70 ± 0,04) n=5/5 |
| Rádio | 1,07 (1,09 ± 0,03) n=5/5 |
| Clima | 0,67 (0,67 ± 0,01) n=5/5 |
| Classificação | 0,08 (0,10 ± 0,06) n=5/5 |

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 150 | 0 | 0 de 50 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | RSS (MB) média | RSS (MB) pico | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|
| Ocioso | 394,5 | 395,4 | 0,2 | 1,7 |
| Carregando telas | 449,8 | 555,3 | 0,9 | 4,2 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## Comparação entre plataformas (sem cache, sem polling — mediana em s)

| Tela | `android_emulator` | `ios_simulator` | `web_chrome` |
|---|---|---|---|
| Dashboard | 3,77 | 1,54 | 1,08 |
| Telemetria — abertura | 2,12 | 1,09 | 0,69 |
| Telemetria — volta do piloto¹ | 0,83 | 0,86 | 0,84 |
| Estratégia — stints | 2,91 | 1,94 | 1,47 |
| Estratégia — ritmo/degradação² | 4,59 | 3,57 | 3,47 |
| Duelo — abertura | 2,17 | 1,13 | 0,72 |
| Duelo — comparação¹ | 1,69 | 1,67 | 1,68 |
| Rádio | 2,47 | 1,51 | 1,07 |
| Clima | 2,10 | 1,15 | 0,67 |
| Classificação | 1,23 | 0,21 | 0,08 |
