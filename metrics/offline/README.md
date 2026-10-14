# Funcionamento sem internet (objetivo específico b do TCC I)

Teste manual em 14/10/2026 no emulador Android (Android 17, `composeApp-debug.apk` do código do commit
seguinte a `3d8ac08`, com as duas correções abaixo):

1. App instalado do zero (`pm clear`), com internet: idioma Português, entrar como convidado e abrir as 7 telas na
   corrida padrão (GP de Singapura 2026, a mais recente), escolhendo VER na Telemetria e VER × ANT no Duelo, e
   esperando cada tela terminar de carregar.
2. Modo avião ligado, Wi-Fi e dados desligados (`ping` → *Network is unreachable*), app fechado (`force-stop`) e
   aberto de novo.
3. As mesmas telas e escolhas, sem internet. Depois, uma sessão que nunca foi aberta com internet.
4. Rede do emulador religada.

| Tela | Sem internet | Captura |
|---|---|---|
| Seletor de sessões (todas as telas) | ✅ funciona | — |
| Dashboard | ✅ posições, intervalos e gaps (inclusive "+3 VOLTAS"), direção de prova e eventos com a volta | `2_offline_dashboard.png`, `2b_offline_dashboard_eventos.png` |
| Telemetria | ✅ voltas, volta teórica e gráficos | `3_offline_telemetria.png` |
| Estratégia | ✅ stints, degradação e ritmo limpo | `4_offline_estrategia.png`, `4b_offline_estrategia_ritmo.png` |
| Duelo | ✅ delta acumulado e delta na volta mais rápida | `5_offline_duelo.png` |
| Rádio | ✅ lista das 32 mensagens; ⚠️ o áudio não toca (fica nos servidores da F1) e o app não avisa | `6_offline_radio.png`, `6b_offline_radio_play.png` |
| Clima | ✅ | `7_offline_clima.png` |
| Classificação | ✅ | `8_offline_classificacao.png` |
| Sessão nunca aberta com internet | ✅ mostra o erro em poucos segundos | `9_offline_sessao_nao_aberta.png` |

`1_online_dashboard.png` é o Dashboard com internet, antes do modo avião.

**Comparação com o primeiro teste (mesmo dia, commit `11e4d27`):** antes só Telemetria, Duelo e Classificação
funcionavam sem internet; Dashboard ficava em "Carregando dados…", Estratégia mostrava "Nenhum stint encontrado" e
Rádio e Clima davam erro de rede. Agora as 7 telas funcionam, porque o cache guarda, para sessões encerradas, as
posições e intervalos finais, pit stops, ultrapassagens, direção de prova, clima, lista de rádios e as voltas e
stints de todos os pilotos (`SamplesCache.getOrFetchSession`).

**Problemas encontrados e corrigidos neste teste:**

1. **Rádio dava "Request timeout has expired" mesmo com internet** ao abrir as telas em sequência. Abrir as 7
   telas passa da cota grátis da OpenF1 (30 requisições por minuto); as requisições esperam na fila do limitador,
   e essa espera contava no limite de 60 s da requisição. O limitador passou a ficar antes do `HttpTimeout`, que
   agora mede só a rede (teste `waiting for the OpenF1 quota does not count against the request timeout`).
2. **Sem internet, uma sessão fora do cache ficava em "Carregando dados…" para sempre.** O cliente HTTP repetia
   requisições sem resposta 5 vezes (2 + 4 + 8 + 16 + 16 s = 46 s por requisição), e o Dashboard engolia o erro
   da carga única esperando uma nova tentativa que nunca vinha. Agora uma falha sem resposta é repetida uma vez
   só (teste `without network a request fails after one quick retry`) e o Dashboard mostra o erro
   (`GetLivePositionsUseCase.once`). As respostas `429`/`5xx` continuam com as 5 tentativas.

**Limites:** os áudios de rádio sempre precisam de internet; uma sessão só funciona sem internet depois de aberta
uma vez com internet, e só se já tiver terminado (sessões ao vivo não são guardadas). A versão Web não tem banco
local e não funciona sem internet.
