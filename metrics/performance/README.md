# Desempenho — tempos de carregamento, memória e CPU

Gerado por `scripts/perf_report.py` a partir das execuções de `scripts/perf_run.py` (benchmark embutido no app: `composeApp/src/commonMain/kotlin/com/fastpilot/ui/perf/LoadBenchmark.kt`).

**Metodologia.** Cada tela é aberta criando o seu ViewModel, como faz a navegação, e o tempo vai até o estado que exibe o conteúdo (renderização do quadro não incluída). *Sem cache*: banco local (SQLDelight) e cache HTTP da plataforma limpos antes da medição; *com cache*: a mesma tela logo em seguida, com o banco preenchido (sessões, pilotos, voltas, stints e classificação ficam em cache; posições, intervalos, telemetria, rádio, clima e direção de prova sempre vêm da rede). *Polling*: o modo ao vivo do Dashboard rodando em segundo plano (posições e intervalos a cada 4 s, direção de prova a cada 5 s, clima a cada 60 s). Pausa de 15 s antes de cada medição para respeitar o limite da OpenF1 (~30 requisições/min); respostas 429 são repetidas pelo cliente após 2, 4, 8, 16 e 16 s. Sessão: a corrida mais recente (GP de Abu Dhabi 2025), a mesma que as telas escolhem por padrão. Valores em segundos: mediana (média ± desvio padrão), apenas medições bem-sucedidas; n = sucessos/tentativas.

¹ Tempo a partir da ação do usuário (selecionar piloto / os dois pilotos). ² Tempo a partir da abertura da tela (as métricas continuam carregando depois dos stints).

## Ambientes

| Pasta | Ambiente | Build |
|---|---|---|
| `android_emulator` | Android 17 — sdk_gphone16k_arm64 (emulador) | benchmark (release, not debuggable) |
| `ios_simulator` | iOS 26.5 — iPhone 17 (simulador) | Release |
| `web_chrome` | Web — Chrome/155.0.8059.40 headless | wasmJs production |

## Android 17 — sdk_gphone16k_arm64 (emulador)

| Tela | Sem cache | Com cache | Sem cache + polling | Com cache + polling |
|---|---|---|---|---|
| Dashboard | 1,88 (2,28 ± 0,67) n=5/5 | 0,34 (0,34 ± 0,05) n=5/5 | 8,63 (11,48 ± 8,82) n=3/3 | 0,31 (0,29 ± 0,02) n=3/3 |
| Telemetria — abertura | 1,53 (1,58 ± 0,13) n=5/5 | 0,00 (0,01 ± 0,00) n=5/5 | 34,69 (23,79 ± 19,22) n=3/3 | 0,01 (0,01 ± 0,00) n=3/3 |
| Telemetria — volta do piloto¹ | 2,06 (2,22 ± 0,51) n=5/5 | 1,61 (1,62 ± 0,30) n=5/5 | 4,39 (13,96 ± 18,93) n=3/3 | 1,30 (1,36 ± 0,20) n=3/3 |
| Estratégia — stints | 2,06 (2,23 ± 0,37) n=5/5 | 0,50 (0,74 ± 0,47) n=5/5 | 18,71 (13,41 ± 9,73) n=3/3 | 0,64 (0,60 ± 0,10) n=3/3 |
| Estratégia — ritmo/degradação² | 3,47 (3,67 ± 0,44) n=5/5 | 1,87 (2,28 ± 0,90) n=5/5 | 19,95 (20,02 ± 0,12) n=3/3 | 1,99 (1,97 ± 0,09) n=3/3 |
| Duelo — abertura | 1,52 (1,63 ± 0,22) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 | 34,55 (23,77 ± 19,06) n=3/3 | 0,01 (0,01 ± 0,00) n=3/3 |
| Duelo — comparação¹ | 2,04 (2,10 ± 0,18) n=5/5 | 0,43 (0,66 ± 0,45) n=5/5 | 1,39 (12,68 ± 19,57) n=3/3 | 0,55 (0,78 ± 0,53) n=3/3 |
| Rádio | 1,78 (2,08 ± 0,70) n=5/5 | 0,25 (0,27 ± 0,05) n=5/5 | 18,57 (18,42 ± 0,58) n=3/3 | 0,28 (0,29 ± 0,05) n=3/3 |
| Clima | 1,44 (1,59 ± 0,31) n=5/5 | 0,27 (0,34 ± 0,11) n=5/5 | 1,56 (1,55 ± 0,04) n=3/3 | 16,22 (13,88 ± 5,15) n=3/3 |
| Classificação | 0,07 (0,58 ± 0,78) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 | 1,33 (1,77 ± 1,66) n=3/3 | 0,00 (0,00 ± 0,00) n=3/3 |

Mediana das razões por tela: com cache, a mediana das telas cai para **18%** do tempo sem cache; com polling ativo, o tempo sem cache fica em **742%** do tempo sem polling.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 187 | 0 | 0 de 50 |
| Com cache | 61 | 0 | 0 de 50 |
| Sem cache + polling | 369 | 194 | 0 de 30 |
| Com cache + polling | 69 | 20 | 0 de 30 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | PSS (MB) média | PSS (MB) pico | RSS (MB) média | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|---|
| Ocioso | 39,1 | 39,1 | 162,6 | 0,0 | 0,0 |
| Carregando telas | 53,4 | 64,0 | 177,0 | 0,6 | 4,0 |
| Ocioso + polling | 51,6 | 51,6 | 199,4 | 4,8 | 36,0 |
| Carregando telas + polling | 78,2 | 90,7 | 197,7 | 1,9 | 8,0 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## iOS 26.5 — iPhone 17 (simulador)

| Tela | Sem cache | Com cache | Sem cache + polling | Com cache + polling |
|---|---|---|---|---|
| Dashboard | 2,16 (2,62 ± 1,37) n=5/5 | 0,28 (0,32 ± 0,11) n=5/5 | 16,67 (18,10 ± 15,01) n=3/3 | 0,25 (0,29 ± 0,09) n=3/3 |
| Telemetria — abertura | 0,67 (0,71 ± 0,06) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 | 0,72 (11,52 ± 18,77) n=3/3 | 0,01 (0,01 ± 0,00) n=3/3 |
| Telemetria — volta do piloto¹ | 3,88 (3,88 ± 0,28) n=5/5 | 1,25 (1,29 ± 0,28) n=5/5 | 1,88 (13,74 ± 21,03) n=3/3 | 1,11 (1,22 ± 0,26) n=3/3 |
| Estratégia — stints | 3,82 (3,37 ± 1,27) n=5/5 | 0,43 (0,45 ± 0,04) n=5/5 | 3,69 (12,88 ± 18,22) n=3/3 | 0,45 (11,11 ± 18,48) n=3/3 |
| Estratégia — ritmo/degradação² | 4,93 (4,51 ± 1,31) n=5/5 | 1,62 (1,75 ± 0,31) n=5/5 | 17,57 (19,95 ± 14,01) n=3/3 | 1,63 (12,27 ± 18,65) n=3/3 |
| Duelo — abertura | 0,74 (0,78 ± 0,15) n=5/5 | 0,01 (0,01 ± 0,00) n=5/5 | 0,65 (5,74 ± 8,82) n=3/3 | 0,01 (0,01 ± 0,00) n=3/3 |
| Duelo — comparação¹ | 1,25 (1,86 ± 1,24) n=5/5 | 0,45 (0,43 ± 0,03) n=5/5 | 3,93 (13,83 ± 17,77) n=3/3 | 3,55 (7,12 ± 8,71) n=3/3 |
| Rádio | 0,89 (1,94 ± 1,44) n=5/5 | 0,22 (0,22 ± 0,01) n=5/5 | 33,16 (23,26 ± 17,32) n=3/3 | 0,20 (0,20 ± 0,01) n=3/3 |
| Clima | 0,74 (0,75 ± 0,06) n=5/5 | 0,23 (0,27 ± 0,05) n=5/5 | 8,24 (8,99 ± 8,62) n=3/3 | 0,34 (1,25 ± 1,67) n=3/3 |
| Classificação | 0,72 (1,17 ± 1,18) n=5/5 | 0,00 (0,00 ± 0,00) n=5/5 | 0,62 (0,59 ± 0,21) n=3/3 | 0,00 (0,00 ± 0,00) n=3/3 |

Mediana das razões por tela: com cache, a mediana das telas cai para **19%** do tempo sem cache; com polling ativo, o tempo sem cache fica em **210%** do tempo sem polling.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 208 | 22 | 0 de 50 |
| Com cache | 61 | 0 | 0 de 50 |
| Sem cache + polling | 372 | 192 | 0 de 30 |
| Com cache + polling | 91 | 39 | 0 de 30 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | RSS (MB) média | RSS (MB) pico | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|
| Ocioso | 170,1 | 174,5 | 1,6 | 16,0 |
| Carregando telas | 182,7 | 190,6 | 0,9 | 4,2 |
| Ocioso + polling | 209,7 | 219,4 | 1,8 | 9,3 |
| Carregando telas + polling | 214,3 | 225,9 | 2,5 | 12,7 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## Web — Chrome/155.0.8059.40 headless

Sem banco local nesta plataforma: todas as medições são *sem cache*.

| Tela | Sem cache | Sem cache + polling |
|---|---|---|
| Dashboard | 3,47 (2,69 ± 1,26) n=5/5 | 3,66 (2,81 ± 1,61) n=3/3 |
| Telemetria — abertura | 0,76 (0,77 ± 0,11) n=5/5 | 3,63 (7,02 ± 8,48) n=3/3 |
| Telemetria — volta do piloto¹ | 1,44 (1,95 ± 1,18) n=5/5 | 4,01 (4,14 ± 0,55) n=3/3 |
| Estratégia — stints | 3,58 (2,91 ± 1,51) n=5/5 | 3,45 (12,86 ± 18,27) n=3/3 |
| Estratégia — ritmo/degradação² | 5,37 (4,95 ± 1,30) n=5/5 | 5,39 (16,27 ± 19,25) n=3/3 |
| Duelo — abertura | 0,71 (0,77 ± 0,10) n=5/5 | 16,29 (17,77 ± 17,90) n=3/3 |
| Duelo — comparação¹ | 3,68 (3,97 ± 1,02) n=5/5 | 3,43 (13,57 ± 19,70) n=3/3 |
| Rádio | 0,98 (0,96 ± 0,09) n=5/5 | 1,00 (2,04 ± 1,82) n=3/3 |
| Clima | 0,85 (0,91 ± 0,26) n=5/5 | 18,46 (23,15 ± 9,00) n=3/3 |
| Classificação | 0,08 (0,18 ± 0,25) n=5/5 | 0,22 (0,33 ± 0,34) n=3/3 |

Mediana das razões por tela: com polling ativo, o tempo sem cache fica em **192%** do tempo sem polling.

| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |
|---|---|---|---|
| Sem cache | 211 | 19 | 0 de 50 |
| Sem cache + polling | 273 | 119 | 0 de 30 |

Requisições contadas durante as medições (as do polling entre medições não entram).

| Fase | RSS (MB) média | RSS (MB) pico | CPU média (%) | CPU p95 (%) |
|---|---|---|---|---|
| Ocioso | 532,8 | 534,2 | 0,2 | 1,4 |
| Carregando telas | 585,7 | 702,4 | 1,6 | 4,4 |
| Ocioso + polling | 667,1 | 695,0 | 0,9 | 2,0 |
| Carregando telas + polling | 771,2 | 939,2 | 2,3 | 8,3 |

CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app (Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; iOS simulador e Web: `ps` no processo do app / renderer do Chrome).

## Comparação entre plataformas (sem cache, sem polling — mediana em s)

| Tela | `android_emulator` | `ios_simulator` | `web_chrome` |
|---|---|---|---|
| Dashboard | 1,88 | 2,16 | 3,47 |
| Telemetria — abertura | 1,53 | 0,67 | 0,76 |
| Telemetria — volta do piloto¹ | 2,06 | 3,88 | 1,44 |
| Estratégia — stints | 2,06 | 3,82 | 3,58 |
| Estratégia — ritmo/degradação² | 3,47 | 4,93 | 5,37 |
| Duelo — abertura | 1,52 | 0,74 | 0,71 |
| Duelo — comparação¹ | 2,04 | 1,25 | 3,68 |
| Rádio | 1,78 | 0,89 | 0,98 |
| Clima | 1,44 | 0,74 | 0,85 |
| Classificação | 0,07 | 0,72 | 0,08 |
