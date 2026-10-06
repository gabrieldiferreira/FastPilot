# Código compartilhado — FastPilot

Gerado por `python3 scripts/measure_shared_code.py` em 2026-10-06 com cloc 2.10.
Unidade: linhas de código (sem linhas em branco e comentários). Metodologia no cabeçalho do script.

## Resultado

| Medida | Compartilhado | Específico | % compartilhado |
|---|---:|---:|---:|
| Android + iOS + Web (plataformas do TCC) | 4801 | 276 | **94.6%** |
| Todos os alvos (inclui Desktop) | 4801 | 347 | 93.3% |
| Só Kotlin, todos os alvos | 4621 | 309 | 93.7% |

## Por plataforma

Quanto do código de cada app vem do código comum: compartilhado ÷ (compartilhado + específico daquele alvo).

| Plataforma | Código específico | Linguagens | % do app que é compartilhado |
|---|---:|---|---:|
| Android | 114 | Kotlin 114 | 97.7% |
| iOS | 97 | Kotlin 73, Swift 24 | 98.0% |
| Web (Wasm) | 65 | Kotlin 51, HTML 14 | 98.7% |
| Desktop (JVM) | 71 | Kotlin 71 | 98.5% |

## Composição do código compartilhado

| Módulo | Linhas | Linguagens |
|---|---:|---|
| `shared/src/commonMain` | 1877 | Kotlin 1697, SQL 180 |
| `composeApp/src/commonMain` | 2924 | Kotlin 2924 |

## Fora da conta

- Testes (`shared/src/commonTest`, `shared/src/desktopTest`, `composeApp/src/commonTest`): 1033 linhas (Kotlin 1033).
- Código gerado (SQLDelight em `build/`), recursos Android, Info.plist, catálogos de assets,
  scripts Gradle e o `.xcodeproj` gerado pelo XcodeGen.
