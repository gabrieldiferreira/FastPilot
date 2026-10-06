# Comparação FastPilot × FastF1 — notas para o TCC II

Resultados: `README.md` (código atual) e `baseline.md` (código do commit `a40c44d`, antes das correções).
Detalhe por item: `errors.csv` e `errors_baseline.csv`.

## Como reproduzir

```sh
python3 -m venv ~/.venvs/fastpilot-fastf1 && ~/.venvs/fastpilot-fastf1/bin/pip install fastf1   # FastF1 3.8.3
~/.venvs/fastpilot-fastf1/bin/python scripts/fastf1_reference.py   # referência → metrics/comparison/fastf1/
./gradlew :shared:exportMetrics                                    # FastPilot sobre fixtures/ → metrics/comparison/fastpilot/
python3 scripts/compare_metrics.py                                 # tabelas → metrics/comparison/README.md
```

## Metodologia

- **Mesmas definições dos dois lados** (TCC I §4.7), implementadas com os dados e filtros de cada ferramenta:
  o FastPilot usa a OpenF1 (gravada em `fixtures/`), a FastF1 usa o feed oficial de cronometragem da F1.
- **Corridas**: Bahrein 2023, Bélgica 2023, Singapura 2023, Mônaco 2024, São Paulo 2024.
  91 pilotos×corrida, 221 stints, 15 pares de voltas para o delta.
- **Erro**: MAPE (meta ≤ 5%, §4.8) para valores em segundos de volta. A inclinação da degradação e o delta
  oscilam em torno de zero, onde o erro percentual explode (divisão por ~0); para eles o erro principal é o
  MAE em segundos, com o MAPE apenas informativo.
- **Pareamento de stints** por piloto, composto e maior sobreposição de voltas (OpenF1 e FastF1 numeram stints
  de forma diferente em algumas corridas).

## O que a comparação encontrou (baseline → corrigido)

| Métrica | Baseline | Corrigido |
|---|---:|---:|
| Ritmo limpo (média), MAPE | 0,29% | 0,00% |
| Ritmo limpo (mediana), MAPE | 0,07% | 0,00% |
| Volta teórica, MAPE | 0,00% | 0,00% |
| Melhor volta, MAPE | 0,00% | 0,00% |
| Degradação, MAE (s/volta) | 0,203 | 0,006 |
| Degradação, mesmo sinal | 158/221 | 221/221 |
| Delta na volta, MAE da curva | 0,103 s | 0,103 s |

Os tempos de volta e de setor da OpenF1 são idênticos aos da cronometragem oficial (melhor volta com erro zero
em 91 de 91 casos). As diferenças do baseline vinham dos **filtros de voltas atípicas**, que não seguiam o §4.7:

1. **Safety car**: só a volta em que a mensagem do `/race_control` foi emitida era removida; as voltas do meio
   do período (ex.: Singapura, voltas 20–22) ficavam. Agora o período vai de `DEPLOYED` até `IN THIS LAP` /
   `ENDING` (VSC) / bandeira vermelha.
2. **Carros retardatários**: o `/race_control` numera as mensagens pela volta do **líder**. Um carro uma volta
   atrás está em outra volta no mesmo instante (ex.: Ocon, Bahrein, VSC na "volta 41" pegou a volta 40
   dele, de 128 s). Os períodos agora são comparados por **horário**, não por número de volta.
3. **Volta de entrada nos boxes (in-lap)** não era removida (ex.: Singapura #77, volta 40 com 110 s contra
   ~102 s). Agora a volta anterior a cada volta de saída é excluída.
4. **Volta 1 (largada parada)** tem tempo na OpenF1 em 4 das 5 corridas e é 6–10 s mais lenta; distorcia a
   regressão do primeiro stint. Agora é excluída.
5. **Volta de abandono**: a última volta com tempo de um carro que abandona (ex.: Piastri, Bahrein, volta 13
   com 122 s). Detectada porque termina mais de 3 min antes da chegada do líder; retardatários que recebem a
   bandeira terminam depois dele.
6. Bandeiras amarelas locais (um setor) deixaram de invalidar a volta inteira — o §4.7 fala só de safety car.

## Diferenças residuais (dados, não algoritmo)

- **Volta teórica de Leclerc, Bélgica (FastPilot 0,12 s menor)**: na volta 1 a cronometragem oficial não tem
  S1 (largada do grid) e a referência descarta a volta; a OpenF1 tem os três setores e o S3 da volta 1
  (29,936 s) é o melhor dele. Diferença de tratamento da volta 1, não de dados faltando.
- **Volta teórica de Leclerc, Bahrein (FastPilot 0,06 s maior)**: a OpenF1 não tem o S1 da volta 37; como a
  volta precisa dos três setores, os S2/S3 dela (os melhores) ficam de fora. Falha de dado da OpenF1.
- **Degradação de Sainz em São Paulo, stint de intermediários**: stint curto em pista secando, inclinação
  muito sensível a uma volta; maior erro absoluto do conjunto (1,08 s/volta).
- **Atraso de numeração na OpenF1**: o VSC de São Paulo aparece nas mensagens na volta 28, enquanto o status
  oficial já o marca na 27. A comparação por horário absorve a maior parte disso.

## Delta na volta (§4.7) — métodos diferentes

O FastPilot calcula a distância a partir das **posições** (`/location`, ~3,9 Hz, decímetros) e projeta um
piloto sobre a linha do outro; a FastF1 (`utils.delta_time`) integra a **velocidade** da telemetria
(`car_data`). O MAE médio entre as curvas é de 0,103 s (0,060–0,201 s por par), e o valor final coincide com a
diferença oficial em todos os 15 pares. O comprimento traçado pelo FastPilot fica 1–3% abaixo do oficial
(segmentos retos entre amostras cortam as curvas) — o erro de posição previsto no §4.7, agora medido.

## Limitações da comparação

- Com as mesmas definições dos dois lados, erro ≈ 0 valida principalmente (a) que os dados da OpenF1 batem com
  a cronometragem oficial e (b) que a implementação está correta. A FastF1 não é uma "verdade" independente
  sobre degradação de pneu: ela aplica a mesma regressão linear.
- A regressão linear simples não modela combustível nem evolução de pista; o §4.7 já aponta que ela funciona
  melhor no meio do stint que nas bordas.
