# Comparação FastPilot × FastF1

Execução: **após correções (filtros do §4.7)**. Gerado por `scripts/compare_metrics.py` a partir de
`metrics/comparison/fastpilot/` (`./gradlew :shared:exportMetrics`) e `metrics/comparison/fastf1/`
(`scripts/fastf1_reference.py`). Detalhe por item em `errors.csv`.

## Métricas em segundos de volta (MAPE, meta ≤ 5%)

| Métrica | 2023_bahrain | 2023_belgium | 2023_singapore | 2024_monaco | 2024_brazil | Geral | MAE geral (s) | n |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Ritmo limpo (média) | 0.00% | 0.00% | 0.01% | 0.00% | 0.00% | **0.00%** | 0.002 | 91 |
| Ritmo limpo (mediana) | 0.00% | 0.00% | 0.00% | 0.00% | 0.00% | **0.00%** | 0.001 | 91 |
| Volta teórica | 0.00% | 0.01% | 0.00% | 0.00% | 0.00% | **0.00%** | 0.002 | 91 |
| Melhor volta | 0.00% | 0.00% | 0.00% | 0.00% | 0.00% | **0.00%** | 0.000 | 91 |

## Degradação de pneu (inclinação em s/volta por stint)

| Corrida | Stints comparados | MAE (s/volta) | Mediana do erro (s/volta) | MAPE | Mesmo sinal |
|---|---:|---:|---:|---:|---:|
| 2023_bahrain | 67 | 0.002 | 0.000 | 1.0% | 67/67 |
| 2023_belgium | 55 | 0.000 | 0.000 | 0.0% | 55/55 |
| 2023_singapore | 42 | 0.001 | 0.000 | 0.9% | 42/42 |
| 2024_monaco | 23 | 0.000 | 0.000 | 0.0% | 23/23 |
| 2024_brazil | 34 | 0.032 | 0.000 | 1.0% | 34/34 |
| **Geral** | 221 | **0.006** | 0.000 | 0.6% | 221/221 |

## Delta na volta (§4.7)

Erro médio absoluto entre as curvas em 201 pontos da distância normalizada, e diferença no valor final.

| Corrida | Par | MAE da curva (s) | Gap final FastPilot (s) | Gap final FastF1 (s) |
|---|---|---:|---:|---:|
| 2023_bahrain | #1 L44 vs #11 L37 | 0.105 | 0.108 | 0.108 |
| 2023_bahrain | #1 L44 vs #14 L36 | 0.112 | -0.080 | -0.080 |
| 2023_bahrain | #11 L37 vs #14 L36 | 0.133 | -0.188 | -0.188 |
| 2023_belgium | #1 L32 vs #11 L31 | 0.104 | 1.386 | 1.386 |
| 2023_belgium | #1 L32 vs #16 L30 | 0.077 | 1.514 | 1.514 |
| 2023_belgium | #11 L31 vs #16 L30 | 0.064 | 0.128 | 0.128 |
| 2023_singapore | #55 L47 vs #4 L46 | 0.128 | 0.380 | 0.380 |
| 2023_singapore | #55 L47 vs #44 L47 | 0.116 | -1.799 | -1.799 |
| 2023_singapore | #4 L46 vs #44 L47 | 0.060 | -2.179 | -2.179 |
| 2024_monaco | #16 L71 vs #81 L73 | 0.086 | 1.119 | 1.119 |
| 2024_monaco | #16 L71 vs #55 L62 | 0.074 | -0.436 | -0.436 |
| 2024_monaco | #81 L73 vs #55 L62 | 0.069 | -1.555 | -1.555 |
| 2024_brazil | #1 L67 vs #31 L64 | 0.201 | 1.299 | 1.299 |
| 2024_brazil | #1 L67 vs #10 L66 | 0.139 | 1.173 | 1.173 |
| 2024_brazil | #31 L64 vs #10 L66 | 0.082 | -0.126 | -0.126 |
| **Geral** | 15 pares | **0.103** | | |
