# Consistência dos setores da OpenF1

Gerado por `python3 scripts/sector_consistency.py` a partir de `fixtures/<corrida>/openf1/laps.json`. Voltas consideradas: com tempo de volta e que não são de saída do box (as que a volta teórica pode usar). Soma ≠ tempo da volta = |S1 + S2 + S3 − tempo da volta| > 0,01 s. Detalhe por volta em `sector_consistency.csv`.

## Voltas com problema nos setores

| Corrida | Grupo | Voltas verificadas | Setor ausente | Soma ≠ tempo da volta |
|---|---|---:|---:|---:|
| 2023_bahrain | ajuste | 1005 | 4 | 0 |
| 2023_belgium | ajuste | 777 | 0 | 0 |
| 2023_singapore | ajuste | 1058 | 0 | 0 |
| 2024_monaco | ajuste | 1194 | 1 | 0 |
| 2024_brazil | ajuste | 1098 | 3 | 0 |
| 2023_japan | validação | 836 | 3 | 0 |
| 2024_las_vegas | validação | 898 | 3 | 16 |

## Diferenças da volta teórica em relação à FastF1

Pilotos com diferença acima de 0,0005 s em `errors.csv` ou `errors_holdout.csv`, com as voltas problemáticas do piloto. Marcadas com **melhor** as que fornecem um dos melhores setores do piloto no FastPilot.

### 2023_bahrain, #16: FastPilot 96,923 s × FastF1 96,864 s (diferença 0,059 s)

Melhores setores no FastPilot: S1 30,859 s (volta 16); S2 42,444 s (volta 38); S3 23,620 s (volta 39).

| Volta | Problema | S1 | S2 | S3 | Tempo da volta | Soma − tempo |
|---:|---|---:|---:|---:|---:|---:|
| 37 | setor ausente | — | 42,385 | 23,715 | 97,170 | — |

### 2023_belgium, #16: FastPilot 110,313 s × FastF1 110,436 s (diferença -0,123 s)

Melhores setores no FastPilot: S1 32,085 s (volta 30); S2 48,292 s (volta 30); S3 29,936 s (volta 1).

Um dos melhores setores vem da volta 1. A cronometragem oficial não tem o S1 da volta 1 (largada do grid), por isso a FastF1 não usa essa volta; a OpenF1 tem os três setores.

Nenhuma volta com setor ausente ou soma inconsistente.

### 2024_las_vegas, #22: FastPilot 96,714 s × FastF1 96,628 s (diferença 0,086 s)

Melhores setores no FastPilot: S1 27,833 s (volta 48); S2 32,377 s (volta 47); S3 36,504 s (volta 35).

| Volta | Problema | S1 | S2 | S3 | Tempo da volta | Soma − tempo |
|---:|---|---:|---:|---:|---:|---:|
| 34 | setor ausente | — | 33,401 | 36,418 | 98,088 | — |
| 44 | setor ausente | — | 32,485 | 37,377 | 97,976 | — |

### 2024_las_vegas, #77: FastPilot 95,880 s × FastF1 95,963 s (diferença -0,083 s)

Melhores setores no FastPilot: S1 27,422 s (volta 44); S2 31,876 s (volta 38); S3 36,582 s (volta 44).

| Volta | Problema | S1 | S2 | S3 | Tempo da volta | Soma − tempo |
|---:|---|---:|---:|---:|---:|---:|
| 38 (**melhor**) | soma ≠ tempo da volta | 27,653 | 31,876 | 37,055 | 97,092 | -0,508 |
| 40 | soma ≠ tempo da volta | 29,896 | 32,362 | 36,841 | 99,608 | -0,509 |
| 45 | soma ≠ tempo da volta | 27,660 | 31,959 | 36,625 | 96,762 | -0,518 |
