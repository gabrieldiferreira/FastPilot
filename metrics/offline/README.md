# Funcionamento sem internet (objetivo específico b do TCC I)

Teste manual em 14/10/2026 no emulador Android (Android 17, `composeApp-debug.apk` do commit `11e4d27`):

1. App instalado do zero (`pm clear`), com internet: entrar como convidado e abrir as 7 telas na corrida
   padrão (GP de Singapura 2026, a mais recente), escolhendo VER na Telemetria e VER × ANT no Duelo.
2. Modo avião ligado (Wi-Fi e dados desligados; `ping` → *Network is unreachable*), app fechado (`force-stop`)
   e aberto de novo.
3. As mesmas telas e escolhas, sem internet.

| Tela | Sem internet | Por quê |
|---|---|---|
| Seletor de sessões (todas as telas) | ✅ funciona | calendário no cache (`cached_session`) |
| Dashboard | ❌ fica em "Carregando dados…" | posições, intervalos e eventos não têm cache |
| Telemetria | ✅ funciona: voltas, volta teórica e gráficos | voltas (`cached_lap`) e telemetria bruta da volta (`cached_samples`) |
| Estratégia | ❌ "Nenhum stint encontrado" | stints e voltas de **todos** os pilotos vêm sempre da rede |
| Duelo | ✅ funciona: delta acumulado e delta na volta mais rápida | voltas e posições na pista da volta (`cached_samples`) |
| Rádio | ❌ erro de rede | sem cache (os áudios também são da rede) |
| Clima | ❌ erro de rede | sem cache |
| Classificação | ✅ funciona (2023–2026) | `cached_driver_standing` / `cached_constructor_standing` |

**Depois deste teste** o cache passou a guardar também, para sessões encerradas, as posições e intervalos
finais, pit stops, ultrapassagens, direção de prova, clima, lista de rádios e as voltas e stints de todos os
pilotos (`SamplesCache.getOrFetchSession`), o que deve fazer Dashboard, Estratégia, Rádio e Clima funcionarem
sem internet. **Teste a refazer** com a versão atual.

As capturas estão nesta pasta (`1_online_dashboard.png` com internet; as demais sem). A versão Web não tem
banco local e não funciona sem internet.
