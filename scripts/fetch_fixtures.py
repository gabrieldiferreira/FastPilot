#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: baixa e grava em fixtures/ as respostas da OpenF1 e da Jolpica das 5 corridas da avaliação, para os testes e a comparação não dependerem da internet.
"""
Downloads raw OpenF1 and Jolpica responses for the five races used in the
FastF1 comparison (TCC II, Fase 4) and stores them under fixtures/.

Recorded responses make the evaluation reproducible and independent of API
availability (TCC I, risk #2). Re-run with --force to refresh existing files.

Usage: python3 scripts/fetch_fixtures.py [--force] [--holdout]
"""
# Importa o módulo que lê e escreve JSON.
import json
# Importa o módulo sys, usado para ler a opção --force.
import sys
# Importa as classes de data/hora e de intervalo de tempo.
from datetime import datetime, timedelta
# Importa o módulo time, usado para esperar entre requisições.
import time
# Importa as classes de erro de rede da biblioteca padrão.
import urllib.error
# Importa o módulo que faz requisições HTTP.
import urllib.request
# Importa a classe Path, para montar caminhos de pastas e arquivos.
from pathlib import Path

# Pasta fixtures/ na raiz do repositório.
ROOT = Path(__file__).resolve().parent.parent / "fixtures"
# Endereço base da API OpenF1.
OPENF1 = "https://api.openf1.org/v1"
# Endereço base da API Jolpica (compatível com a antiga Ergast).
JOLPICA = "https://api.jolpi.ca/ergast/f1"

# (folder, OpenF1 race session_key, season, Jolpica round)
# Corridas: (pasta, código da sessão de corrida na OpenF1, temporada, número da etapa na Jolpica).
RACES = [
    # Bahrein 2023: corrida seca, usada como base.
    ("2023_bahrain", 7953, 2023, 1),      # dry baseline
    # Bélgica 2023: com chuva.
    ("2023_belgium", 9141, 2023, 12),     # rain
    # Singapura 2023: com safety cars.
    ("2023_singapore", 9165, 2023, 15),   # safety cars
    # Mônaco 2024: poucas ultrapassagens.
    ("2024_monaco", 9523, 2024, 8),       # few overtakes
    # São Paulo 2024: chuva e estratégias variadas.
    ("2024_brazil", 9636, 2024, 21),      # rain + varied strategy
# Fim da lista.
]

# Hold-out races, not used to tune the lap filters: in each season, the round after the last race
# of RACES. Fetched only with --holdout.
HOLDOUT_RACES = [
    ("2023_japan", 9173, 2023, 16),       # round after Singapore 2023
    ("2024_las_vegas", 9644, 2024, 22),   # round after São Paulo 2024
]

# Everything the four metrics need; full car_data/location are left out (tens of MB per race)
# Rotas da OpenF1 baixadas (o necessário para as quatro métricas); car_data e location completos ficam de fora por serem enormes.
OPENF1_ENDPOINTS = ["sessions", "drivers", "laps", "stints", "pit", "race_control", "weather", "intervals"]

# Espera de 0,6 s entre requisições, para ficar abaixo do limite da OpenF1.
REQUEST_INTERVAL_S = 0.6  # stay below OpenF1's rate limit
# Número máximo de tentativas por requisição.
MAX_RETRIES = 8


# Baixa uma URL e devolve o JSON, tentando de novo em caso de falha temporária.
def fetch(url: str):
    # Repete até MAX_RETRIES vezes.
    for attempt in range(MAX_RETRIES):
        # Tenta fazer a requisição.
        try:
            # Monta a requisição com um User-Agent que identifica o projeto.
            req = urllib.request.Request(url, headers={"User-Agent": "FastPilot-TCC-fixtures"})
            # Abre a conexão com limite de 120 s...
            with urllib.request.urlopen(req, timeout=120) as resp:
                # ...e devolve a resposta convertida de JSON.
                return json.load(resp)
        # Se o servidor respondeu com um código de erro HTTP...
        except urllib.error.HTTPError as e:
            # ...e é um 404 da OpenF1...
            if e.code == 404 and url.startswith(OPENF1):
                # OpenF1 answers an empty result set with 404 {"detail": "No results found."}
                # ...a OpenF1 usa 404 para "nenhum resultado"; avisa...
                print("    no data (404)")
                # ...e devolve uma lista vazia.
                return []
            # Se é 429 (muitas requisições) ou erro do servidor (5xx) e ainda há tentativas...
            if (e.code == 429 or e.code >= 500) and attempt < MAX_RETRIES - 1:
                # ...espera 1, 2, 4, 8... segundos (dobra a cada tentativa)...
                wait = 2 ** attempt
                # ...avisa quanto vai esperar...
                print(f"    HTTP {e.code}, retrying in {wait}s")
                # ...espera...
                time.sleep(wait)
                # ...e tenta de novo.
                continue
            # Qualquer outro erro HTTP é repassado (o script para).
            raise
        # Se houve erro de rede ou de tempo esgotado...
        except (urllib.error.URLError, OSError):  # includes socket timeouts
            # ...e ainda há tentativas...
            if attempt < MAX_RETRIES - 1:
                # ...espera 1, 2, 4, 8... segundos...
                time.sleep(2 ** attempt)
                # ...e tenta de novo.
                continue
            # Na última tentativa, repassa o erro.
            raise


# Baixa as voltas da Jolpica, que vêm em páginas, e junta tudo em uma resposta só.
def fetch_jolpica_laps(url: str):
    """Jolpica caps pages at 100 lap timings; fetch every page and merge them per lap."""
    # Resposta montada, voltas agrupadas pelo número e posição da próxima página.
    merged, laps_by_number, offset = None, {}, 0
    # Repete até baixar todas as páginas.
    while True:
        # Baixa a página atual, com 100 tempos por página.
        page = fetch(f"{url}?limit=100&offset={offset}")
        # Lista de corridas da página (uma corrida, ou vazia).
        races = page["MRData"]["RaceTable"]["Races"]
        # Na primeira página...
        if merged is None:
            # ...usa a própria página como base da resposta final.
            merged = page
        # Para cada volta da página (nenhuma, se a lista de corridas está vazia)...
        for lap in (races[0]["Laps"] if races else []):
            # ...se essa volta já apareceu numa página anterior...
            if lap["number"] in laps_by_number:
                # ...junta os tempos desta página aos que já estavam guardados...
                laps_by_number[lap["number"]]["Timings"].extend(lap["Timings"])
            # ...senão...
            else:
                # ...guarda a volta pela primeira vez.
                laps_by_number[lap["number"]] = lap
        # Avança para a próxima página.
        offset += 100
        # Se já passou do total de registros...
        if offset >= int(page["MRData"]["total"]):
            # ...termina o laço.
            break
        # Espera um pouco antes da próxima página, para respeitar o limite da API.
        time.sleep(REQUEST_INTERVAL_S)
    # Lista de corridas da resposta final.
    races = merged["MRData"]["RaceTable"]["Races"]
    # Se há corrida...
    if races:
        # ...substitui as voltas pela lista completa, ordenada pelo número da volta.
        races[0]["Laps"] = sorted(laps_by_number.values(), key=lambda l: int(l["number"]))
    # Ajusta os campos de paginação para dizer que tudo veio numa página só.
    merged["MRData"].update(limit=merged["MRData"]["total"], offset="0")
    # Devolve a resposta completa.
    return merged


# Baixa uma URL e grava o resultado em um arquivo JSON.
def save(path: Path, url: str, force: bool, fetcher=fetch):
    # Se o arquivo já existe e --force não foi usado...
    if path.exists() and not force:
        # ...avisa que vai pular...
        print(f"  skip {path.relative_to(ROOT)} (exists)")
        # ...e sai sem baixar.
        return
    # Baixa os dados (com fetch, ou outra função passada em fetcher).
    data = fetcher(url)
    # Cria a pasta do arquivo, se não existir.
    path.parent.mkdir(parents=True, exist_ok=True)
    # Grava o JSON com acentos legíveis, recuo de 1 espaço e quebra de linha no fim.
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    # Quantidade de registros (tamanho da lista, ou 1 se a resposta é um objeto).
    count = len(data) if isinstance(data, list) else 1
    # Mostra o que foi gravado.
    print(f"  saved {path.relative_to(ROOT)} ({count} records)")
    # Espera antes da próxima requisição.
    time.sleep(REQUEST_INTERVAL_S)


# Baixa as posições na pista (/location) da volta mais rápida dos três primeiros colocados (usadas no delta).
def save_fastest_lap_locations(base: Path, session_key: int, force: bool, top: int = 3):
    """/location samples of the fastest lap of each of the top finishers (lap delta, TCC I §4.7)."""
    # Lê o resultado da corrida já gravado da Jolpica.
    results = json.loads((base / "jolpica" / "results.json").read_text())
    # Pega os três primeiros colocados.
    finishers = results["MRData"]["RaceTable"]["Races"][0]["Results"][:top]
    # Lê as voltas já gravadas da OpenF1.
    laps = json.loads((base / "openf1" / "laps.json").read_text())
    # Para cada um dos três primeiros...
    for result in finishers:
        # ...número do carro...
        driver = int(result["number"])
        # ...voltas dele que têm tempo e não são de saída do box...
        valid = [l for l in laps if l["driver_number"] == driver and l["lap_duration"] and not l["is_pit_out_lap"]]
        # ...a mais rápida delas...
        fastest = min(valid, key=lambda l: l["lap_duration"])
        # ...horário de início da volta...
        start = datetime.fromisoformat(fastest["date_start"])
        # ...horário de fim (início + duração + 1 s de folga)...
        end = start + timedelta(seconds=fastest["lap_duration"] + 1)
        # ...monta a URL da rota /location filtrando sessão, piloto e o intervalo de horário
        url = (f"{OPENF1}/location?session_key={session_key}&driver_number={driver}"
               # (com ">" e "<" codificados, e o "+" do fuso codificado como %2B)...
               f"&date%3E={start.isoformat()}&date%3C={end.isoformat()}").replace("+", "%2B")
        # ...e grava o arquivo location_<piloto>_lap<volta>.json.
        save(base / "openf1" / f"location_{driver}_lap{fastest['lap_number']}.json", url, force)


# Função principal do script.
def main():
    # Verifica se a opção --force foi passada (baixar de novo mesmo se o arquivo existe).
    force = "--force" in sys.argv
    # Percorre as cinco corridas.
    for folder, session_key, season, rnd in (HOLDOUT_RACES if "--holdout" in sys.argv else RACES):
        # Mostra qual corrida está sendo baixada.
        print(f"{folder} (session_key={session_key}, round={rnd})")
        # Pasta da corrida dentro de fixtures/.
        base = ROOT / folder
        # Para cada rota da OpenF1...
        for endpoint in OPENF1_ENDPOINTS:
            # ...baixa os dados da sessão e grava em openf1/<rota>.json.
            save(base / "openf1" / f"{endpoint}.json", f"{OPENF1}/{endpoint}?session_key={session_key}", force)
        # Baixa o resultado da corrida na Jolpica.
        save(base / "jolpica" / "results.json", f"{JOLPICA}/{season}/{rnd}/results.json", force)
        # Baixa as voltas na Jolpica, juntando as páginas.
        save(base / "jolpica" / "laps.json", f"{JOLPICA}/{season}/{rnd}/laps.json", force, fetch_jolpica_laps)
        # Baixa as posições da volta mais rápida dos três primeiros.
        save_fastest_lap_locations(base, session_key, force)


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
