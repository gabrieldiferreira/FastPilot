# Código compartilhado — FastPilot

Gerado por `python3 scripts/measure_shared_code.py` em 2026-10-14 com cloc 2.10.
Unidade: linhas de código (sem linhas em branco e comentários). Metodologia no cabeçalho do script.

## Resultado

| Medida | Compartilhado | Específico | % compartilhado |
|---|---:|---:|---:|
| Android + iOS + Web (plataformas do TCC) | 7992 | 328 | **96.1%** |
| Todos os alvos (inclui Desktop) | 7992 | 414 | 95.1% |
| Só Kotlin, todos os alvos | 7774 | 376 | 95.4% |

## Por plataforma

Quanto do código de cada app vem do código comum: compartilhado ÷ (compartilhado + específico daquele alvo).

| Plataforma | Código específico | Linguagens | % do app que é compartilhado |
|---|---:|---|---:|
| Android | 131 | Kotlin 131 | 98.4% |
| iOS | 115 | Kotlin 91, Swift 24 | 98.6% |
| Web (Wasm) | 82 | Kotlin 68, HTML 14 | 99.0% |
| Desktop (JVM) | 86 | Kotlin 86 | 98.9% |

## Composição do código compartilhado

| Módulo | Linhas | Linguagens |
|---|---:|---|
| `shared/src/commonMain` | 2443 | Kotlin 2225, SQL 218 |
| `composeApp/src/commonMain` | 5549 | Kotlin 5549 |

## Fora da conta

- Testes (`shared/src/commonTest`, `shared/src/desktopTest`, `composeApp/src/commonTest`): 2211 linhas (Kotlin 2211).
- Código gerado (SQLDelight em `build/`), recursos Android, Info.plist, catálogos de assets,
  scripts Gradle e o `.xcodeproj` gerado pelo XcodeGen.
