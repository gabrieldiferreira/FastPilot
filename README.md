<div align="center">

  # 🏎️ FastPilot

  **Aplicativo em Kotlin Multiplatform para dados de Fórmula 1 ao vivo e históricos: Android, iOS e Web com um só código.**

  [![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/) 
  [![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-000000?style=for-the-badge&logo=android&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
  [![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Web%20%7C%20Desktop-lightgrey?style=for-the-badge)](#)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](#)
</div>

<br/>

## 📖 Visão geral

O **FastPilot** é um aplicativo multiplataforma feito com **Kotlin Multiplatform (KMP)** e **Compose Multiplatform**, desenvolvido como Trabalho de Conclusão de Curso (TCC) em Sistemas de Informação na UNISINOS. Ele consome as APIs públicas [OpenF1](https://openf1.org) e [Jolpica (Ergast)](https://github.com/jolpica/jolpica-f1) e calcula as métricas de corrida no próprio aparelho (ritmo limpo, degradação de pneu, volta teórica, duelo entre pilotos e delta na volta) em Kotlin puro, com testes de unidade.

A lógica de negócio **e as telas** são compartilhadas: cada tela é escrita uma vez em Compose Multiplatform e roda no Android, no iOS e no navegador (Kotlin/Wasm). O código específico de cada plataforma se limita ao ponto de entrada do app, ao motor HTTP, ao driver do SQLite, ao player de áudio, à conexão com o login do Google, ao lugar onde o idioma escolhido é salvo e à leitura do fuso horário. Medido com o `cloc`, **90,9% do código dos apps Android + iOS + Web é compartilhado** (ver [`metrics/shared_code.md`](metrics/shared_code.md)).

O app fala **português do Brasil, inglês americano e espanhol** (escolhidos pelas bandeiras na tela de login) e mostra todos os horários no fuso do usuário. O login é com a conta Google (Firebase Authentication) ou como convidado.

- **Web:** https://fastpilot.vercel.app
- **Android:** publicado na Google Play.
- **Versão avaliada no TCC II:** tag `tcc2-avaliacao` (commit `980e90d`).

---

## ✨ Funcionalidades

Sete abas, todas compartilhadas entre as plataformas:

| Aba | O que mostra |
|---|---|
| **Painel** | Posições com intervalo e diferença para o líder (corridas e sprints) ou melhor volta e diferença para o mais rápido (treinos e classificação, separada em **Q1/Q2/Q3**, sem as voltas apagadas pela direção de prova); pit stops e ultrapassagens com a volta; mensagens da direção de prova; abre a sessão ao vivo (ou a corrida mais recente) e consulta a OpenF1 periodicamente enquanto ela está ao vivo. |
| **Telemetria** | Gráficos de velocidade, rotação, marcha e acelerador de um piloto, mais o card de **volta teórica** (melhor S1 + S2 + S3, comparada com a melhor volta real). |
| **Estratégia** | Linha do tempo dos stints de cada piloto com as cores dos compostos, **degradação de pneu** (s/volta por stint) e ranking de **ritmo limpo** (mediana das voltas limpas, diferença para o líder, σ). |
| **Duelo** | Comparação entre dois pilotos e o **delta na volta** das voltas mais rápidas, alinhado pela posição na pista. |
| **Rádio** | Gravações do rádio das equipes na sessão; mensagens novas a cada 15 s enquanto está ao vivo. |
| **Clima** | Temperatura do ar e da pista, vento, umidade e chuva; atualizado a cada 60 s ao vivo. |
| **Classificação** | Campeonato de pilotos e de construtores (Jolpica), em cache por 7 dias (1 hora para a temporada atual). |

Todas as abas usam o mesmo **seletor de sessão**: ano, Grande Prêmio (com o número da etapa) e um botão por evento do fim de semana (TL1–TL3, Classificação Sprint, Sprint, Classificação, Corrida).

O ritmo limpo e a degradação de pneu descartam as voltas que não são representativas (volta 1, voltas de entrada e saída do box, voltas sob SC/VSC/bandeira vermelha, a volta do abandono) com o `LapFilters`, cruzando com o `/race_control` pelo horário.

---

## 📸 Galeria

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Abertura</b></td>
      <td align="center"><b>Login</b></td>
      <td align="center"><b>Painel</b></td>
      <td align="center"><b>Telemetria</b></td>
    </tr>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/b5691e0c-40c7-418e-afec-c03395807ff9" alt="Abertura" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/2d51373a-af64-4718-94ef-4eb9df1199bc" alt="Login" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/e3e9f1ba-11f5-4e2b-be1a-128b03d8e8a6" alt="Painel" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/3ca2053c-bdc3-4589-bcde-e7c64e60d736" alt="Telemetria" width="250"/></td>
    </tr>
    <tr>
      <td align="center"><b>Duelo</b></td>
      <td align="center"><b>Estratégia</b></td>
      <td align="center"><b>Rádio</b></td>
      <td align="center"><b>Clima</b></td>
    </tr>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/1e0a3075-8b02-4026-ae96-e6403c986724" alt="Duelo" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/7d3c8e46-8319-4bc1-a8c8-5fb720001ff0" alt="Estratégia" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/a50e3587-8fcc-4d5d-87b3-79e408b607df" alt="Rádio" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/1ebe2c89-da0b-4db6-ab51-ea6bf11aad51" alt="Clima" width="250"/></td>
    </tr>
  </table>
</div>

---

## 🛠️ Arquitetura

```
FastPilot/
├── shared/          # dados + domínio (biblioteca Kotlin Multiplatform)
│   └── src/
│       ├── commonMain/      # clientes das APIs, repositórios, esquema SQLDelight, casos de uso (métricas)
│       ├── androidMain/     # motor OkHttp, driver SQLite do Android
│       ├── iosMain/         # motor Darwin, driver SQLite nativo
│       ├── wasmJsMain/      # motor fetch do JS (sem banco local na Web)
│       ├── desktopMain/     # motor CIO, driver SQLite JDBC
│       ├── commonTest/      # testes de casos de uso, repositórios e rede
│       └── desktopTest/     # migração do esquema, corridas gravadas, exportação das métricas
├── composeApp/      # apresentação: telas em Compose Multiplatform, ViewModels, navegação, injeção de dependências
│   └── src/
│       ├── commonMain/      # as 7 telas + login/abertura, abas do Voyager, módulos do Koin
│       └── androidMain | iosMain | wasmJsMain | desktopMain   # pontos de entrada e login de cada plataforma
├── iosApp/          # projeto Xcode (gerado pelo XcodeGen) que hospeda o ComposeUIViewController
├── api/             # funções da Vercel (proxy da OpenF1 ao vivo e da Jolpica)
├── fixtures/        # respostas gravadas da OpenF1/Jolpica de 7 corridas
├── scripts/         # scripts de avaliação (código compartilhado, comparação com a FastF1, desempenho, Vercel)
└── metrics/         # resultados da avaliação gerados pelos scripts
```

O projeto segue a Clean Architecture: o `shared` tem a camada de dados (clientes Ktor da OpenF1 e da Jolpica, repositórios que leem primeiro o cache do SQLDelight) e a camada de domínio (casos de uso em Kotlin puro); o `composeApp` tem a camada de apresentação (um ViewModel por tela, que expõe o estado da tela em `StateFlow`).

**Cache para uso sem internet.** No Android, no iOS e no Desktop, os repositórios leem primeiro o SQLite e só vão à rede quando o dado não está lá. Os dados baixados depois que a sessão terminou (pilotos, voltas, stints, posições e intervalos finais, direção de prova, clima, lista do rádio, pit stops, ultrapassagens e a telemetria e as posições na pista de cada volta) não expiram e não são baixados de novo, então uma corrida aberta uma vez funciona sem internet em todas as telas; os dados de corrida são apagados 30 dias depois de baixados. Sessões ao vivo nunca vão para o cache. O SQLDelight não tem driver síncrono para o navegador, então **na Web o app roda sem cache local** e todo carregamento vai à rede.

**Rede.** O plano grátis da OpenF1 permite 3 requisições por segundo **e** 30 em qualquer janela de 60 s (medido). O `HttpClient` compartilhado espaça as requisições (uma a cada 400 ms, no máximo 29 em 61 s), para o app esperar a sua vez em vez de receber erro 429; as sessões ao vivo passam pelo proxy do plano patrocinador e não são limitadas. Respostas 429/5xx são repetidas com espera crescente (2, 4, 8, 16 e 16 s), e as requisições são contadas para o benchmark de desempenho. O calendário de todas as temporadas vem em uma só requisição (`year>=2023`).

**Idiomas.** Pacotes de textos tipados (`composeApp/.../ui/i18n`: `PtBrStrings`, `EnUsStrings`, `EsStrings`) implementam a mesma interface `Strings`, então um texto que falte em algum idioma não compila; trocar o idioma recompõe o app na hora, e a escolha fica salva em cada plataforma. Os textos das APIs também são traduzidos: nomes de sessões, países e circuitos e as **mensagens da direção de prova** da FIA, por regras sobre o conjunto fechado de formatos (99,9% de 12.372 mensagens reais; uma mensagem com alguma parte desconhecida fica em inglês).

**Horários.** Os horários das APIs vêm em UTC, e os horários escritos dentro das mensagens da direção de prova estão no horário local do circuito; os dois são mostrados no fuso do usuário (fuso do sistema; na Web, do navegador), usando o `gmt_offset` da sessão para o segundo caso.

**Endereços na Web.** Cada tela tem o seu endereço no idioma do app: `/painel`, `/telemetria`, `/estrategia`, `/duelo`, `/radio`, `/clima`, `/classificacao`, `/entrar` em português; `/dashboard`, `/telemetry`, `/strategy`, `/duel`, `/radio`, `/weather`, `/standings`, `/login` em inglês; `/panel`, `/telemetria`, `/estrategia`, `/duelo`, `/radio`, `/clima`, `/clasificacion`, `/acceder` em espanhol. Eles usam a History API do navegador: um link abre a tela direto (um endereço em qualquer idioma funciona), recarregar mantém a tela, voltar/avançar troca de tela, e trocar o idioma reescreve o endereço sem criar uma nova entrada no histórico. O `vercel.json` e o `scripts/serve_web.py` respondem o `index.html` para esses endereços (um teste confere que os dois listam todos).

### Tecnologias principais

| Biblioteca | Versão | Uso |
|---|---|---|
| [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) | 2.1.0 | Alvos: `androidTarget`, `iosX64`/`iosArm64`/`iosSimulatorArm64`, `wasmJs`, `jvm("desktop")` |
| [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) | 1.7.3 | Telas compartilhadas em todas as plataformas |
| [Ktor Client](https://ktor.io/) | 3.0.3 | HTTP (motores OkHttp / Darwin / JS / CIO) |
| [SQLDelight](https://cashapp.github.io/sqldelight/) | 2.1.0 | Cache local (esquema v4 com migrações) |
| [Koin](https://insert-koin.io/) | 4.0.1 | Injeção de dependências |
| [Voyager](https://voyager.adriel.cafe/) | 1.1.0-beta03 | Navegação por abas |
| [Firebase Authentication](https://firebase.google.com/docs/auth) | BoM 33.7.0 | Login com Google (Android, iOS e Web) |

### Fontes de dados
* **[OpenF1 API](https://api.openf1.org/v1)**: sessões, voltas, stints, dados do carro, posição na pista, posições, intervalos, direção de prova, rádio das equipes, clima.
* **[Jolpica Ergast API](https://api.jolpi.ca/ergast/f1/)**: classificação do campeonato.

---

## 🚀 Como rodar

### Pré-requisitos
* JDK 17
* Android SDK (compileSdk 36, minSdk 26): só para o Android
* Xcode e [XcodeGen](https://github.com/yonaskolb/XcodeGen): só para o iOS (macOS)
* Google Chrome: para os testes da Web

```bash
git clone https://github.com/gabrieldiferreira/FastPilot.git
cd FastPilot
```

### Android
Rode a configuração `composeApp` no Android Studio, ou:
```bash
./gradlew :composeApp:installDebug
```

### iOS
O projeto Xcode é gerado a partir do `iosApp/project.yml`; uma etapa do build roda `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, então não é preciso CocoaPods.
```bash
cd iosApp && xcodegen generate   # só depois de editar o project.yml
open iosApp.xcodeproj            # escolha um simulador de iPhone e rode
```

### Web (Kotlin/Wasm)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun        # servidor de desenvolvimento com recarga
./gradlew :composeApp:wasmJsBrowserDistribution          # build de produção
python3 scripts/serve_web.py 8080                       # serve o build; os endereços das telas (/telemetria...) respondem o index.html
```
Requer um navegador com WasmGC (Chrome e Firefox atuais, Safari 18.2+).

**Publicação na Vercel:** o `vercel.json` roda o `scripts/vercel_build.sh`, que baixa um JDK 17 (a imagem da Vercel não tem), compila a versão Wasm e publica `composeApp/build/dist/wasmJs/productionExecutable`. As opções de build ficam no `.env` (sem segredos).

**Dados ao vivo:** a OpenF1 só entrega dados em tempo real para contas pagas (*Sponsor*). O `api/openf1.mjs` é uma função da Vercel que guarda a conta (`OPENF1_USERNAME` / `OPENF1_PASSWORD`, cadastradas como variáveis de ambiente sensíveis no painel da Vercel), acrescenta o token de acesso e repassa as requisições em `/openf1/v1/<endpoint>`, com cache de 4 s na CDN. Para ativar, preencha `OPENF1_PROXY_URL` em `shared/.../network/OpenF1Proxy.kt` com `https://<seu-deploy>/openf1/v1`: o app passa a mandar as requisições das sessões ao vivo pelo proxy, e os dados históricos continuam indo direto à OpenF1. **O modo ao vivo foi implementado, mas não foi testado durante uma sessão ao vivo**, porque depende da assinatura do plano pago.

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

---

## 🧪 Testes

```bash
./gradlew :shared:desktopTest :composeApp:desktopTest     # JVM
./gradlew :shared:iosSimulatorArm64Test                    # simulador iOS
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :shared:wasmJsBrowserTest :composeApp:wasmJsBrowserTest   # Web
```

Os testes cobrem os casos de uso das métricas, o `LapFilters`, os repositórios, a política de repetição do HTTP, a migração do SQLDelight, as corridas gravadas e os ViewModels (`MockEngine` do Ktor rodando no dispatcher de teste).

---

## 📊 Avaliação (TCC II)

Todos os resultados são gerados pelos scripts em `scripts/` e gravados em `metrics/`. Medição de 06/11/2026, na versão da tag `tcc2-avaliacao`.

> **Sobre os scripts de avaliação:** os scripts em `scripts/` (medição do código compartilhado, comparação com a FastF1 e com a Jolpica, benchmark de desempenho e build da Vercel) foram criados com o uso de inteligência artificial (Claude, da Anthropic): o autor pediu os scripts à IA, rodou-os e conferiu os resultados abaixo.

### Código compartilhado
```bash
python3 scripts/measure_shared_code.py      # requer cloc → metrics/shared_code.md
```
| Escopo | % compartilhado |
|---|---:|
| Apps Android + iOS + Web | **90,9%** |
| App Android | 97,6% |
| App iOS | 96,8% |
| App Web | 95,9% |

Em 14/10/2026 o compartilhado era 96,1%; caiu porque a conexão com o login do Google precisou de código próprio no iOS (Swift) e na Web (HTML). A meta do TCC era 75%. Testes, código gerado e scripts de build ficam fora da conta; a metodologia está no cabeçalho do script.

### Concordância com a FastF1
As métricas do FastPilot são comparadas com as mesmas métricas calculadas pela biblioteca Python [FastF1](https://docs.fastf1.dev/) em cinco corridas (Bahrein 2023, Bélgica 2023, Singapura 2023, Mônaco 2024, São Paulo 2024), usando as respostas gravadas em `fixtures/`.
```bash
python3 scripts/fetch_fixtures.py                     # grava as respostas das APIs (já estão no repositório)
./gradlew :shared:exportMetrics                       # resultados do FastPilot → metrics/comparison/fastpilot/
python3 scripts/fastf1_reference.py                   # resultados da FastF1    → metrics/comparison/fastf1/
python3 scripts/compare_metrics.py                    # → metrics/comparison/README.md
```
| Métrica | Erro (meta: MAPE ≤ 5%) |
|---|---|
| Ritmo limpo (média / mediana) | MAPE 0,00% (n = 91) |
| Volta teórica | MAPE 0,00% (n = 91) |
| Melhor volta | MAPE 0,00% (n = 91) |
| Degradação de pneu | MAE 0,006 s/volta, mesmo sinal em 221 de 221 stints |
| Delta na volta | MAE da curva 0,103 s, diferença final idêntica (15 pares) |

Em duas corridas de validação que não participaram dos ajustes dos filtros (Japão 2023 e Las Vegas 2024), os resultados se mantiveram: MAPE 0,00% e degradação com o mesmo sinal em 106 de 106 stints ([`metrics/comparison/holdout.md`](metrics/comparison/holdout.md)). Os tempos de volta também foram conferidos com a Jolpica: 7.007 voltas iguais nas sete corridas, sem contar a volta 1 ([`metrics/comparison/jolpica_check.md`](metrics/comparison/jolpica_check.md)).

A concordância mostra que as contas estão certas, mas não que a inclinação dos tempos de volta mede só o desgaste do pneu. Detalhes, os erros que a comparação revelou e as limitações: [`metrics/comparison/README.md`](metrics/comparison/README.md) e [`metrics/comparison/NOTAS.md`](metrics/comparison/NOTAS.md).

### Desempenho
Um benchmark embutido no app (`composeApp/.../ui/perf/`) abre cada tela várias vezes e mede o tempo desde a criação do ViewModel até o conteúdo estar pronto, com e sem o cache local, enquanto um script mede a CPU e a memória do processo do app.
```bash
./gradlew :composeApp:assembleBenchmark                        # build de release do Android para medição
python3 scripts/perf_run.py android --serial <serial do adb>
python3 scripts/perf_run.py ios --device <UDID do simulador>   # build Release em build/ios-perf
python3 scripts/perf_run.py web                                # Chrome sem interface
python3 scripts/perf_report.py                                 # → metrics/performance/README.md
```
O benchmark também pode ser iniciado à mão: Android `adb shell am start -n com.fastpilot.app/.MainActivity -e perf "off:5,on:3"`, iOS argumento de inicialização `-perf off:5,on:3`, Web `index.html?perf=off:5,on:3`.

Rodada mais recente (06/11/2026, GP do México 2026, 5 repetições por cenário, 250 carregamentos, todos bem-sucedidos, nenhuma resposta 429). Ambientes: emulador Android 17, simulador iPhone 17 com iOS 26.5 e Chrome 157 sem interface. Mediana sem cache → com cache:

| Tela | Emulador Android | Simulador iOS | Web (Chrome) |
|---|---|---|---|
| Painel | 3,77 s → 0,02 s | 1,54 s → 0,02 s | 1,08 s |
| Estratégia (stints) | 2,91 s → 0,01 s | 1,94 s → 0,02 s | 1,47 s |
| Duelo (abertura) | 2,17 s → 0,01 s | 1,13 s → 0,02 s | 0,72 s |
| Classificação | 1,23 s → 0,00 s | 0,21 s → 0,00 s | 0,08 s |

Com o cache local, a mediana das telas abre em cerca de 1% do tempo sem cache (a Web não tem cache local). Esta rodada foi medida sem o polling ao vivo: no plano grátis da OpenF1 (30 requisições por minuto), o polling sozinho (~46/min) esgota a cota. Tabelas completas, memória e CPU: [`metrics/performance/README.md`](metrics/performance/README.md).

### Funcionamento sem internet
Teste no emulador Android (06/11/2026): com o celular em modo avião, as sete telas funcionaram com os dados salvos em cache. O iOS não foi testado sem internet, e a Web não funciona sem internet. Capturas: [`metrics/offline/README.md`](metrics/offline/README.md).

---

## 📚 Documentação

* [`FastPilot_Documentation.md`](FastPilot_Documentation.md): documentação funcional.
* No TCC I (proposta), as telas foram planejadas em Jetpack Compose + SwiftUI; no fim, ficaram inteiras em Compose Multiplatform, o que também trouxe a versão Web quase sem código a mais.

## 📝 Licença

Este projeto usa a licença [MIT](https://choosealicense.com/licenses/mit/).
