# Código compartilhado — FastPilot

Gerado por `python3 scripts/measure_shared_code.py` em 2026-10-07 com cloc 2.10.
Unidade: linhas de código (sem linhas em branco e comentários). Metodologia no cabeçalho do script.

## Resultado

| Medida | Compartilhado | Específico | % compartilhado |
|---|---:|---:|---:|
| Android + iOS + Web (plataformas do TCC) | 5142 | 276 | **94.9%** |
| Todos os alvos (inclui Desktop) | 5142 | 347 | 93.7% |
| Só Kotlin, todos os alvos | 4962 | 309 | 94.1% |

## Por plataforma

Quanto do código de cada app vem do código comum: compartilhado ÷ (compartilhado + específico daquele alvo).

| Plataforma | Código específico | Linguagens | % do app que é compartilhado |
|---|---:|---|---:|
| Android | 114 | Kotlin 114 | 97.8% |
| iOS | 97 | Kotlin 73, Swift 24 | 98.1% |
| Web (Wasm) | 65 | Kotlin 51, HTML 14 | 98.8% |
| Desktop (JVM) | 71 | Kotlin 71 | 98.6% |

## Composição do código compartilhado

| Módulo | Linhas | Linguagens |
|---|---:|---|
| `shared/src/commonMain` | 1941 | Kotlin 1761, SQL 180 |
| `composeApp/src/commonMain` | 3201 | Kotlin 3201 |

## Fora da conta

- Testes (`shared/src/commonTest`, `shared/src/desktopTest`, `composeApp/src/commonTest`): 1466 linhas (Kotlin 1466).
- Código gerado (SQLDelight em `build/`), recursos Android, Info.plist, catálogos de assets,
  scripts Gradle e o `.xcodeproj` gerado pelo XcodeGen.
