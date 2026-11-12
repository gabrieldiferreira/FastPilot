# Funcionamento sem internet (objetivo específico b do TCC I)

Teste refeito em 06/11/2026 no emulador Android (Android 17, `composeApp-debug.apk` do commit `25c2102`, tag
`tcc2-avaliacao`). O primeiro teste, de 14/10/2026 (GP de Singapura 2026), está no histórico do git.

1. App instalado do zero (`pm clear`), com internet: idioma Português, entrar como convidado e abrir as 7 telas na
   corrida padrão (GP do México 2026, a mais recente), escolhendo VER na Telemetria e VER × ANT no Duelo, e
   esperando cada tela terminar de carregar.
2. Modo avião ligado, Wi-Fi e dados desligados (`ping` → *Network is unreachable*), app fechado (`force-stop`) e
   aberto de novo.
3. As mesmas telas e escolhas, sem internet. Depois, uma sessão que nunca foi aberta com internet (a
   Classificação do mesmo GP).
4. Rede do emulador religada.

| Tela | Sem internet | Captura |
|---|---|---|
| Seletor de sessões (todas as telas) | ✅ funciona | — |
| Dashboard | ✅ posições, intervalos e gaps, direção de prova e eventos com a volta | `2_offline_dashboard.png`, `2b_offline_dashboard_eventos.png` |
| Telemetria | ✅ voltas, volta teórica e gráficos | `3_offline_telemetria.png` |
| Estratégia | ✅ stints, degradação e ritmo limpo | `4_offline_estrategia.png`, `4b_offline_estrategia_ritmo.png` |
| Duelo | ✅ delta acumulado e delta na volta mais rápida | `5_offline_duelo.png` |
| Rádio | ✅ lista das 27 mensagens; o áudio não toca (fica nos servidores da F1) e o app avisa "Não foi possível tocar o áudio. Verifique sua conexão." | `6_offline_radio.png`, `6b_offline_radio_play.png` |
| Clima | ✅ | `7_offline_clima.png` |
| Classificação | ✅ | `8_offline_classificacao.png` |
| Sessão nunca aberta com internet | ✅ mostra o erro em poucos segundos | `9_offline_sessao_nao_aberta.png` |

`1_online_dashboard.png` é o Dashboard com internet, antes do modo avião.

**Comparação com o teste anterior a 14/10/2026 (commit `11e4d27`):** antes só Telemetria, Duelo e Classificação
funcionavam sem internet; Dashboard ficava em "Carregando dados…", Estratégia mostrava "Nenhum stint encontrado" e
Rádio e Clima davam erro de rede. Agora as 7 telas funcionam, porque o cache guarda, para sessões encerradas, as
posições e intervalos finais, pit stops, ultrapassagens, direção de prova, clima, lista de rádios e as voltas e
stints de todos os pilotos (`SamplesCache.getOrFetchSession`).

**Problemas encontrados e corrigidos a partir deste teste:**

1. **Rádio dava "Request timeout has expired" mesmo com internet** ao abrir as telas em sequência. Abrir as 7
   telas passa da cota grátis da OpenF1 (30 requisições por minuto); as requisições esperam na fila do limitador,
   e essa espera contava no limite de 60 s da requisição. O limitador passou a ficar antes do `HttpTimeout`, que
   agora mede só a rede (teste `waiting for the OpenF1 quota does not count against the request timeout`).
2. **Sem internet, uma sessão fora do cache ficava em "Carregando dados…" para sempre.** O cliente HTTP repetia
   requisições sem resposta 5 vezes (2 + 4 + 8 + 16 + 16 s = 46 s por requisição), e o Dashboard engolia o erro
   da carga única esperando uma nova tentativa que nunca vinha. Agora uma falha sem resposta é repetida uma vez
   só (teste `without network a request fails after one quick retry`) e o Dashboard mostra o erro
   (`GetLivePositionsUseCase.once`). As respostas `429`/`5xx` continuam com as 5 tentativas.

3. **O áudio do rádio sem internet aparecia como "Reproduzindo"** sem tocar nada (e, com internet, continuava
   "Reproduzindo" depois do fim da mensagem). O `AudioPlayer` de cada plataforma agora avisa quando a gravação
   termina ou falha (Android: `MediaPlayer`, com checagem de rede antes de tentar — sem ela o `MediaPlayer`
   insiste ~25 s; iOS: estado do `AVPlayerItem`, com limite de 15 s; Web: evento `error` do `<audio>`), e a tela
   volta para "Ouvir" e mostra o aviso no cartão da mensagem (`RadioPlayback`, testes em `RadioPlaybackTest`).
   Conferido no emulador Android e no Chrome (modo offline); no iOS só compilado e testado em unidade.

**Limites:** os áudios de rádio sempre precisam de internet; uma sessão só funciona sem internet depois de aberta
uma vez com internet, e só se já tiver terminado (sessões ao vivo não são guardadas). A versão Web não tem banco
local e não funciona sem internet. O teste no iOS está na seção "iOS (simulador)", abaixo.

## iOS (simulador)

Teste feito em 12/11/2026 no simulador iPhone 17 com iOS 26.5 (Xcode 27.2), com o app em build Release compilado
do código do commit `25c2102` (o mesmo da tag `tcc2-avaliacao`; os commits seguintes só mudam README e scripts).
O simulador não tem modo avião: ele usa a rede do Mac, então a internet foi cortada desligando o Wi-Fi do Mac.

1. App instalado do zero (cache vazio), com internet: idioma Português, entrar como convidado e abrir as telas.
   A corrida padrão agora é o GP do Brasil 2026 (Interlagos, a mais recente); Dashboard, Telemetria e Estratégia
   foram abertos no GP do México 2026, nas cinco sessões, e Duelo, Rádio e Clima no GP do Brasil 2026.
2. App fechado, Wi-Fi do Mac desligado e app aberto de novo (volta à tela de login, `1_offline_reaberto_login.png`).
3. As mesmas telas e sessões, sem internet.
4. Wi-Fi religado.

| Tela | Sem internet | Captura |
|---|---|---|
| Dashboard (México, 5 sessões) | ✅ posições, gaps, direção de prova, Q1/Q2/Q3 | `2_offline_dashboard_corrida.png`, `2b_offline_dashboard_q3.png` |
| Telemetria (México, 5 sessões) | ✅ voltas, volta teórica e setores | `3_offline_telemetria.png` |
| Estratégia (México, 5 sessões) | ✅ stints e degradação | `4_offline_estrategia.png` |
| Duelo (Brasil) | ✅ delta acumulado NOR × ANT | `5_offline_duelo.png` |
| Rádio (Brasil) | ✅ lista das 30 mensagens; o áudio não toca e o app avisa "Não foi possível tocar o áudio. Verifique sua conexão." | `6_offline_radio_play.png` |
| Clima (Brasil, 5 sessões) | ✅ | `7_offline_clima.png` |
| Classificação | ✅ pilotos e equipes | `8_offline_classificacao.png` |
| Dado nunca aberto com internet (telemetria de uma volta no Brasil) | ✅ mostra o erro do iOS `NSURLErrorDomain Code=-1009 "The Internet connection appears to be offline"`, que também confirma que o simulador estava sem rede | `9_offline_volta_nao_aberta.png` |

Todas as telas abertas com internet mostraram os mesmos dados sem internet. O aviso do áudio do rádio no iOS, antes
só testado em unidade, foi visto funcionando aqui.
