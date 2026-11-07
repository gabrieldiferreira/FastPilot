#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: calcula, com a biblioteca FastF1 (cronometragem oficial da F1), as mesmas métricas do FastPilot nas 5 corridas, para servir de referência.
"""
Reference values for the FastPilot metrics computed with FastF1 (TCC II, Fase 4).

FastF1 uses the official F1 live timing feed (not OpenF1), so the comparison checks
both the data FastPilot relies on and its calculations. The definitions follow
TCC I §4.7, implemented with FastF1's own data and filters:

- clean pace: laps without pit in/out, without safety car / VSC / red flag (TrackStatus
  4, 5, 6, 7) and excluding lap 1 (standing start); 5% trimmed on each side; mean and median.
- tyre degradation: per stint, same lap filter, linear regression LapTime ~ TyreLife (>= 3 laps).
- theoretical best: min(S1) + min(S2) + min(S3) over laps that are not pit-out laps.
- best lap: fastest lap time that is not a pit-out lap.
- lap delta: fastf1.utils.delta_time between the fastest laps of the top-3 finishers.

Usage:
  python3 -m venv ~/.venvs/fastpilot-fastf1 && ~/.venvs/fastpilot-fastf1/bin/pip install fastf1
  ~/.venvs/fastpilot-fastf1/bin/python scripts/fastf1_reference.py [--holdout]
Since 15 Oct 2026 the F1 live timing archive requires an F1 TV account: FastF1 >= 3.9 with
F1_USERNAME/F1_PASSWORD (the five comparison races were fetched on 6 Oct 2026 with FastF1 3.8.3).
Writes metrics/comparison/fastf1/<race>.json (FastF1 cache in ~/.cache/fastf1).
"""
# Importa o módulo que escreve o resultado em JSON.
import json
import sys
# Importa o módulo de avisos, usado para silenciar avisos da FastF1 no cálculo do delta.
import warnings
# Importa a classe Path, para montar caminhos de pastas e arquivos.
from pathlib import Path

# Importa a biblioteca FastF1, que baixa e organiza os dados oficiais de cronometragem.
import fastf1
# Importa as funções utilitárias da FastF1 (entre elas, delta_time).
import fastf1.utils
# Importa o NumPy, usado na regressão linear, na média e na mediana.
import numpy as np
# Importa o pandas, a biblioteca de tabelas em que a FastF1 entrega as voltas.
import pandas as pd

# Pasta raiz do repositório (duas pastas acima deste arquivo).
ROOT = Path(__file__).resolve().parent.parent
# Pasta onde os JSON de referência são gravados.
OUT = ROOT / "metrics" / "comparison" / "fastf1"

# Same races and folders as scripts/fetch_fixtures.py
# Lista das corridas: (nome da pasta, ano, nome do evento como a FastF1 entende).
RACES = [
    # GP do Bahrein 2023.
    ("2023_bahrain", 2023, "Bahrain"),
    # GP da Bélgica 2023.
    ("2023_belgium", 2023, "Belgium"),
    # GP de Singapura 2023.
    ("2023_singapore", 2023, "Singapore"),
    # GP de Mônaco 2024.
    ("2024_monaco", 2024, "Monaco"),
    # GP de São Paulo 2024.
    ("2024_brazil", 2024, "São Paulo"),
# Fim da lista.
]

# Hold-out races (HOLDOUT_RACES in scripts/fetch_fixtures.py), only with --holdout
HOLDOUT_RACES = [
    ("2023_japan", 2023, "Japan"),
    ("2024_las_vegas", 2024, "Las Vegas"),
]

# Códigos de situação da pista que indicam neutralização: 4 safety car, 5 bandeira vermelha, 6 VSC, 7 fim do VSC.
NEUTRALISED = set("4567")  # SC, red flag, VSC, VSC ending
# Fração descartada em cada ponta no ritmo limpo (5%).
TRIM = 0.05


# Converte um intervalo de tempo do pandas em segundos.
def seconds(td):
    # Devolve None se o valor está vazio; senão, o total em segundos.
    return None if pd.isna(td) else td.total_seconds()


# Aplica o filtro de voltas do TCC I e devolve só as voltas válidas.
def clean_laps(laps: pd.DataFrame) -> pd.DataFrame:
    """Lap filter from TCC I §4.7: no pit in/out laps, no neutralised laps, no standing-start lap."""
    # Situação da pista de cada volta como texto (pode ter vários códigos, ex.: "14").
    status = laps["TrackStatus"].astype(str)
    # Devolve as voltas que cumprem todas as condições abaixo ao mesmo tempo.
    return laps[
        # Sem horário de entrada no box (não é volta de entrada).
        laps["PitInTime"].isna()
        # Sem horário de saída do box (não é volta de saída).
        & laps["PitOutTime"].isna()
        # Com tempo de volta registrado.
        & laps["LapTime"].notna()
        # Não é a volta 1 (largada parada).
        & (laps["LapNumber"] > 1)
        # Nenhum código de neutralização apareceu durante a volta.
        & ~status.apply(lambda s: bool(NEUTRALISED & set(s)))
    # Fim da seleção.
    ]


# Ordena os valores e descarta 5% de cada ponta.
def trimmed(values):
    # Ordena do menor para o maior.
    values = sorted(values)
    # Quantos valores formam 5% da lista (arredondado para baixo).
    k = int(len(values) * TRIM)
    # Devolve a lista sem os k primeiros e os k últimos; se isso esvaziaria a lista, devolve a lista inteira.
    return values[k:len(values) - k] if len(values) > 2 * k else values


# Calcula as métricas de um piloto a partir das voltas dele.
def driver_metrics(laps: pd.DataFrame) -> dict:
    # Voltas que não são de saída do box.
    not_pit_out = laps[laps["PitOutTime"].isna()]
    # Tempos (em segundos) das voltas limpas, já sem 5% de cada ponta.
    clean = trimmed([seconds(t) for t in clean_laps(laps)["LapTime"]])
    # Voltas que têm os três setores registrados (para a volta teórica).
    with_sectors = not_pit_out.dropna(subset=["Sector1Time", "Sector2Time", "Sector3Time"])
    # Melhor volta: o menor tempo entre as voltas que não são de saída do box.
    best_lap = not_pit_out["LapTime"].min()

    # Lista dos stints com a degradação calculada.
    stints = []
    # Primeira e última volta de cada stint (antes do filtro), para comparar com os stints do FastPilot.
    stint_ranges = laps.groupby("Stint")["LapNumber"].agg(["min", "max"])
    # Percorre os stints, usando só as voltas limpas.
    for stint_number, stint in clean_laps(laps).groupby("Stint"):
        # Tira as voltas sem idade do pneu registrada.
        stint = stint.dropna(subset=["TyreLife"])
        # Pula stints com menos de 3 voltas (pouco para uma reta).
        if len(stint) < 3:
            # Vai para o próximo stint.
            continue
        # x = idade do pneu em voltas, como números decimais.
        x = stint["TyreLife"].to_numpy(dtype=float)
        # y = tempo de cada volta em segundos.
        y = stint["LapTime"].dt.total_seconds().to_numpy()
        # Ajusta a reta de grau 1 (mínimos quadrados): devolve a inclinação (degradação) e o intercepto.
        slope, intercept = np.polyfit(x, y, 1)
        # Guarda os dados do stint.
        stints.append({
            # Número do stint.
            "stint": int(stint_number),
            # Composto do pneu (macio, médio, duro...).
            "compound": str(stint["Compound"].iloc[0]),
            # Primeira volta do stint.
            "lap_start": int(stint_ranges.loc[stint_number, "min"]),
            # Última volta do stint.
            "lap_end": int(stint_ranges.loc[stint_number, "max"]),
            # Quantas voltas entraram na regressão.
            "laps": len(stint),
            # Degradação em segundos por volta.
            "slope": float(slope),
            # Intercepto da reta.
            "intercept": float(intercept),
        # Fim do dicionário do stint.
        })

    # Devolve todas as métricas do piloto.
    return {
        # Ritmo limpo pela média (None se não há voltas limpas).
        "clean_pace_mean": float(np.mean(clean)) if clean else None,
        # Ritmo limpo pela mediana (None se não há voltas limpas).
        "clean_pace_median": float(np.median(clean)) if clean else None,
        # Quantas voltas limpas sobraram.
        "clean_lap_count": len(clean),
        # Volta teórica: melhor setor 1 + melhor setor 2 + melhor setor 3...
        "theoretical_best": (
            # ...soma dos menores tempos de setor 1 e setor 2...
            seconds(with_sectors["Sector1Time"].min()) + seconds(with_sectors["Sector2Time"].min())
            # ...mais o menor tempo de setor 3...
            + seconds(with_sectors["Sector3Time"].min())
        # ...ou None se nenhuma volta tem os três setores.
        ) if len(with_sectors) else None,
        # Melhor volta em segundos.
        "best_lap": seconds(best_lap),
        # Lista de stints calculada acima.
        "stints": stints,
    # Fim do dicionário.
    }


def _delta_time_383(reference_lap, compare_lap):
    """fastf1.utils.delta_time as in FastF1 3.8.3 (MIT licence), removed in FastF1 3.9."""
    ref = reference_lap.get_car_data(interpolate_edges=True).add_distance()
    comp = compare_lap.get_car_data(interpolate_edges=True).add_distance()

    def mini_pro(stream):
        dstream_start = stream[1] - stream[0]
        dstream_end = stream[-1] - stream[-2]
        return np.concatenate([[stream[0] - dstream_start], stream, [stream[-1] + dstream_end]])

    ltime = mini_pro(comp["Time"].dt.total_seconds().to_numpy())
    multiplier = ref.Distance.iat[-1] / comp.Distance.iat[-1]
    ldistance = mini_pro(comp["Distance"].to_numpy()) * multiplier
    lap_time = np.interp(ref["Distance"], ldistance, ltime)
    return lap_time - ref["Time"].dt.total_seconds(), ref, comp


# FastF1 >= 3.9 needs an F1 TV account for the timing archive and no longer has utils.delta_time
delta_time = getattr(fastf1.utils, "delta_time", _delta_time_383)


# Calcula o delta na volta entre os três primeiros colocados, dois a dois.
def lap_deltas(session, top3: list) -> list:
    """delta_time between the fastest laps of each pair of the top-3 finishers, on a normalised distance grid."""
    # Volta mais rápida de cada um dos três primeiros.
    fastest = {n: session.laps.pick_drivers(n).pick_fastest() for n in top3}
    # Lista de resultados.
    out = []
    # Percorre os três pares: 1º×2º, 1º×3º e 2º×3º.
    for ref, cmp_ in [(top3[0], top3[1]), (top3[0], top3[2]), (top3[1], top3[2])]:
        # Abre um bloco em que os avisos ficam guardados (e não aparecem)...
        with warnings.catch_warnings():
            # ...e manda ignorar todos os avisos dentro dele.
            warnings.simplefilter("ignore")
            # Calcula o delta com a função da FastF1: devolve o delta, a telemetria do piloto de referência e outro valor não usado.
            delta, ref_tel, _ = delta_time(fastest[ref], fastest[cmp_])
        # Distância percorrida em cada ponto da volta de referência.
        distance = ref_tel["Distance"].to_numpy()
        # Guarda o resultado do par.
        out.append({
            # Número do piloto de referência.
            "reference": int(ref),
            # Número do piloto comparado.
            "compared": int(cmp_),
            # Volta usada do piloto de referência.
            "reference_lap": int(fastest[ref]["LapNumber"]),
            # Volta usada do piloto comparado.
            "compared_lap": int(fastest[cmp_]["LapNumber"]),
            # Comprimento da volta (última distância).
            "lap_length": float(distance[-1]),
            # Posição de cada ponto como fração da volta (0 a 1), com 5 casas.
            "fraction": (distance / distance[-1]).round(5).tolist(),
            # Valor do delta em cada ponto, em segundos, com 4 casas.
            "delta": np.asarray(delta, dtype=float).round(4).tolist(),
            # Diferença oficial entre os tempos das duas voltas.
            "official_gap": seconds(fastest[cmp_]["LapTime"] - fastest[ref]["LapTime"]),
        # Fim do dicionário do par.
        })
    # Devolve os três pares.
    return out


# Função principal do script.
def main():
    # Pasta do cache local da FastF1 (evita baixar os dados de novo).
    cache = Path.home() / ".cache" / "fastf1"
    # Cria a pasta do cache, se ainda não existir.
    cache.mkdir(parents=True, exist_ok=True)
    # Liga o cache da FastF1 nessa pasta.
    fastf1.Cache.enable_cache(str(cache))
    # Mostra só avisos e erros da FastF1, sem as mensagens de progresso.
    fastf1.set_log_level("WARNING")
    # Cria a pasta de saída, se ainda não existir.
    OUT.mkdir(parents=True, exist_ok=True)

    # Percorre as cinco corridas.
    for folder, year, event in (HOLDOUT_RACES if "--holdout" in sys.argv else RACES):
        # Mostra qual corrida está sendo carregada.
        print(f"{folder}: loading {year} {event}")
        # Abre a sessão de corrida ("R" = Race) do evento.
        session = fastf1.get_session(year, event, "R")
        # Baixa (ou lê do cache) as voltas e a telemetria; clima e mensagens não são necessários.
        session.load(laps=True, telemetry=True, weather=False, messages=False)
        # Resultado final ordenado pela posição de chegada.
        results = session.results.sort_values("Position")
        # Números dos três primeiros colocados, como texto.
        top3 = [str(n) for n in results["DriverNumber"].head(3)]

        # Métricas por piloto.
        drivers = {}
        # Percorre cada piloto que aparece nas voltas.
        for number in session.laps["DriverNumber"].unique():
            # Calcula as métricas desse piloto e guarda pelo número dele.
            drivers[str(int(number))] = driver_metrics(session.laps.pick_drivers(number))

        # Monta o conteúdo do arquivo de saída.
        report = {
            # Nome da corrida.
            "race": folder,
            # Versão da FastF1 usada (registro para reprodução).
            "fastf1_version": fastf1.__version__,
            # Métricas de todos os pilotos.
            "drivers": drivers,
            # Deltas entre os três primeiros.
            "lap_deltas": lap_deltas(session, top3),
        # Fim do dicionário.
        }
        # Grava o JSON da corrida, com recuo de 1 espaço e uma quebra de linha no fim.
        (OUT / f"{folder}.json").write_text(json.dumps(report, indent=1) + "\n", encoding="utf-8")
        # Mostra quantos pilotos foram processados e quem são os três primeiros.
        print(f"  {len(drivers)} drivers, top3 {top3}")


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
