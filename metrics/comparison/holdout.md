# Comparação FastPilot × FastF1

Execução: **validação (Japão 2023 e Las Vegas 2024, fora dos ajustes)**. Gerado por `scripts/compare_metrics.py` a partir de
`metrics/comparison/fastpilot/` (`./gradlew :shared:exportMetrics`) e `metrics/comparison/fastf1/`
(`scripts/fastf1_reference.py`). Detalhe por item em `errors_holdout.csv`.

## Métricas em segundos de volta (MAPE, meta ≤ 5%)

| Métrica | 2023_japan | 2024_las_vegas | Geral | MAE geral (s) | n |
|---|---:|---:|---:|---:|---:|
| Ritmo limpo (média) | 0.00% | 0.00% | **0.00%** | 0.000 | 40 |
| Ritmo limpo (mediana) | 0.00% | 0.00% | **0.00%** | 0.000 | 40 |
| Volta teórica | 0.00% | 0.01% | **0.00%** | 0.004 | 40 |
| Melhor volta | 0.00% | 0.00% | **0.00%** | 0.000 | 40 |

## Degradação de pneu (inclinação em s/volta por stint)

| Corrida | Stints comparados | MAE (s/volta) | Mediana do erro (s/volta) | MAPE | Mesmo sinal |
|---|---:|---:|---:|---:|---:|
| 2023_japan | 50 | 0.000 | 0.000 | 0.0% | 50/50 |
| 2024_las_vegas | 56 | 0.000 | 0.000 | 0.0% | 56/56 |
| **Geral** | 106 | **0.000** | 0.000 | 0.0% | 106/106 |

## Delta na volta (§4.7)

Erro médio absoluto entre as curvas em 201 pontos da distância normalizada, e diferença no valor final.

| Corrida | Par | MAE da curva (s) | Gap final FastPilot (s) | Gap final FastF1 (s) |
|---|---|---:|---:|---:|
| 2023_japan | #1 L39 vs #4 L40 | 0.231 | 1.064 | 1.064 |
| 2023_japan | #1 L39 vs #81 L38 | 0.059 | 2.145 | 2.145 |
| 2023_japan | #4 L40 vs #81 L38 | 0.223 | 1.081 | 1.081 |
| 2024_las_vegas | #63 L46 vs #44 L41 | 0.065 | -0.237 | -0.237 |
| 2024_las_vegas | #63 L46 vs #55 L44 | 0.111 | 0.158 | 0.158 |
| 2024_las_vegas | #44 L41 vs #55 L44 | 0.110 | 0.395 | 0.395 |
| **Geral** | 6 pares | **0.133** | | |
