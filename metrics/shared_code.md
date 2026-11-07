# Código compartilhado — FastPilot

Gerado por `python3 scripts/measure_shared_code.py` em 2026-11-06 com cloc 2.10.
Unidade: linhas de código (sem linhas em branco e comentários). Metodologia no cabeçalho do script.

## Resultado

| Medida | Compartilhado | Específico | % compartilhado |
|---|---:|---:|---:|
| Android + iOS + Web (plataformas do TCC) | 8520 | 852 | **90.9%** |
| Todos os alvos (inclui Desktop) | 8520 | 939 | 90.1% |
| Só Kotlin, todos os alvos | 8302 | 600 | 93.3% |

## Por plataforma

Quanto do código de cada app vem do código comum: compartilhado ÷ (compartilhado + específico daquele alvo).

| Plataforma | Código específico | Linguagens | % do app que é compartilhado |
|---|---:|---|---:|
| Android | 212 | Kotlin 212 | 97.6% |
| iOS | 278 | Kotlin 159, Swift 119 | 96.8% |
| Web (Wasm) | 362 | HTML 220, Kotlin 142 | 95.9% |
| Desktop (JVM) | 87 | Kotlin 87 | 99.0% |

## Composição do código compartilhado

| Módulo | Linhas | Linguagens |
|---|---:|---|
| `shared/src/commonMain` | 2550 | Kotlin 2332, SQL 218 |
| `composeApp/src/commonMain` | 5970 | Kotlin 5970 |

## Fora da conta

- Testes (`shared/src/commonTest`, `shared/src/desktopTest`, `composeApp/src/commonTest`): 2541 linhas (Kotlin 2541).
- Código gerado (SQLDelight em `build/`), recursos Android, Info.plist, catálogos de assets,
  scripts Gradle e o `.xcodeproj` gerado pelo XcodeGen.
