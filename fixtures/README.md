# Fixtures — respostas reais das APIs

Respostas brutas da OpenF1 e da Jolpica para as cinco corridas usadas na comparação com a FastF1 (TCC II, Fase 4). Gravá-las torna a avaliação reprodutível e independente da disponibilidade das APIs (risco #2 do TCC I).

Gerado por `python3 scripts/fetch_fixtures.py` em 06/10/2026 (`--force` regrava tudo).

| Pasta | Corrida | OpenF1 `session_key` | Jolpica | Por que foi escolhida |
|---|---|---|---|---|
| `2023_bahrain` | GP do Bahrein 2023 | 7953 | 2023, etapa 1 | seco, baseline |
| `2023_belgium` | GP da Bélgica 2023 | 9141 | 2023, etapa 12 | chuva |
| `2023_singapore` | GP de Singapura 2023 | 9165 | 2023, etapa 15 | safety car |
| `2024_monaco` | GP de Mônaco 2024 | 9523 | 2024, etapa 8 | poucas ultrapassagens |
| `2024_brazil` | GP de São Paulo 2024 | 9636 | 2024, etapa 21 | chuva + estratégias variadas |

Cada pasta contém:

- `openf1/{sessions,drivers,laps,stints,pit,race_control,weather,intervals}.json` — resposta de `https://api.openf1.org/v1/<endpoint>?session_key=<key>`, sem alterações.
- `jolpica/results.json` — resultado oficial da corrida.
- `jolpica/laps.json` — tempos de volta de todos os pilotos. A Jolpica pagina em 100 registros; o script junta todas as páginas em um único arquivo (`limit` = `total`).

`car_data` e `location` ficaram de fora: dezenas de MB por corrida e não são usados pelas quatro métricas.

## Observações sobre os dados

- **Bahrein 2023 não tem dados de pit stop na OpenF1**: o endpoint `/pit` responde `404 {"detail": "No results found."}` — a OpenF1 usa 404 para resultado vazio. O arquivo é gravado como `[]`.
- **Singapura 2023 tem 19 pilotos na OpenF1**: Lance Stroll se retirou antes da largada. A Jolpica o lista com status `Withdrew` (0 voltas); a OpenF1 simplesmente o omite.
- A validação automática está em `shared/src/desktopTest/kotlin/com/fastpilot/fixtures/RaceFixturesTest.kt` (decodifica tudo com os modelos do app e roda as métricas).
