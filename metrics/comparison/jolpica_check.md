# Verificação com a Jolpica (tempos oficiais de volta)

Gerado por `python3 scripts/jolpica_check.py`. Fonte independente: Jolpica (sucessora da Ergast), respostas gravadas em `fixtures/<corrida>/jolpica/`. Detalhe por volta e por piloto em `errors_jolpica.csv`.

Corridas dos ajustes dos filtros: 2023_bahrain, 2023_belgium, 2023_singapore, 2024_monaco, 2024_brazil. Corridas de validação (fora dos ajustes, sem referência FastF1 por causa da restrição de acesso de 15/10/2026): 2023_japan, 2024_las_vegas.

Iguais = diferença ≤ 0,0005 s (as duas fontes dão o tempo em milésimos). Ritmo limpo, degradação e delta não são verificados aqui: dependem de setores, períodos de safety car e telemetria, que a Jolpica não tem.

## Tempo de cada volta (OpenF1 × Jolpica), sem a volta 1

| Corrida | Grupo | Voltas na Jolpica | Com tempo na OpenF1 | Iguais | MAE (s) | Maior diferença (s) |
|---|---|---:|---:|---:|---:|---:|
| 2023_bahrain | ajuste | 1035 | 1035 | 1035 | 0,0000 | 0,000 |
| 2023_belgium | ajuste | 796 | 796 | 796 | 0,0000 | 0,000 |
| 2023_singapore | ajuste | 1066 | 1066 | 1066 | 0,0000 | 0,000 |
| 2024_monaco | ajuste | 1217 | 1217 | 1217 | 0,0000 | 0,000 |
| 2024_brazil | ajuste | 1115 | 1115 | 1115 | 0,0000 | 0,000 |
| 2023_japan | **validação** | 860 | 860 | 860 | 0,0000 | 0,000 |
| 2024_las_vegas | **validação** | 918 | 918 | 918 | 0,0000 | 0,000 |
| **Ajuste (5)** | | 5229 | 5229 | 5229 | 0,0000 | 0,000 |
| **Validação (2)** | | 1778 | 1778 | 1778 | 0,0000 | 0,000 |

Volta 1 (definições diferentes: a Jolpica mede desde a largada):

| Grupo | Voltas 1 na Jolpica | Com tempo na OpenF1 | Iguais |
|---|---:|---:|---:|
| Ajuste (5) | 91 | 72 | 0 |
| Validação (2) | 40 | 38 | 0 |

## Melhor volta (FastPilot × menor tempo na Jolpica)

| Corrida | Grupo | Pilotos | Iguais | MAE (s) | Maior diferença (s) |
|---|---|---:|---:|---:|---:|
| 2023_bahrain | ajuste | 20 | 20 | 0,0000 | 0,000 |
| 2023_belgium | ajuste | 19 | 19 | 0,0000 | 0,000 |
| 2023_singapore | ajuste | 18 | 18 | 0,0000 | 0,000 |
| 2024_monaco | ajuste | 16 | 16 | 0,0000 | 0,000 |
| 2024_brazil | ajuste | 18 | 18 | 0,0000 | 0,000 |
| 2023_japan | **validação** | 20 | 20 | 0,0000 | 0,000 |
| 2024_las_vegas | **validação** | 20 | 20 | 0,0000 | 0,000 |
