# Degradação de pneu: meio do stint × bordas

Gerado por `python3 scripts/degradation_edges.py` a partir de `metrics/comparison/fastpilot/` (`./gradlew :shared:exportMetrics`). Verifica a afirmação do TCC I (§4.7) de que a regressão linear do tempo de volta pela idade do pneu funciona melhor no meio do stint que nas primeiras e últimas voltas.

Método: stints com pelo menos 8 voltas válidas (as mesmas que a regressão do FastPilot usa: sem volta de saída/entrada do box, safety car, VSC ou bandeira vermelha). Resíduo de cada volta = tempo real − reta ajustada. Grupos pela posição da volta no stint: início = 2 primeiras, fim = 2 últimas, meio = as demais. Erro = média do resíduo absoluto (s); viés = média do resíduo com sinal (positivo = mais lento que a reta).

Stints analisados: **199**; em **143** (72%) o erro médio das bordas é maior que o do meio.

## Todas as corridas

| Grupo | Voltas | Erro médio (s) | Erro mediano (s) | Viés (s) |
|---|---:|---:|---:|---:|
| Início (2 primeiras) | 164 | 1,330 | 0,553 | 0,915 |
| Meio | 4133 | 0,671 | 0,405 | -0,059 |
| Fim (2 últimas) | 270 | 0,816 | 0,543 | 0,345 |

## Por corrida (erro médio, s)

| Corrida | Início | Meio | Fim | Bordas ÷ meio |
|---|---:|---:|---:|---:|
| 2023_bahrain | 0,321 | 0,212 | 0,250 | 1,33× |
| 2023_belgium | 0,766 | 0,515 | 0,672 | 1,38× |
| 2023_singapore | 1,123 | 0,621 | 1,031 | 1,71× |
| 2024_monaco | — | 0,749 | 0,854 | 1,14× |
| 2024_brazil | 4,118 | 1,114 | 1,745 | 2,50× |
