#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: mede se a reta de degradação erra mais nas primeiras e últimas voltas do stint do que no meio (Seção 6.3 do TCC).
"""TCC II: does the tyre degradation regression fit the middle of a stint better than its edges?

TCC I §4.7 states that the linear regression of lap time on tyre age "works better in the
middle of the stint than in the first and last laps". This script measures it on the five
races of the FastF1 comparison, using the laps the FastPilot regression actually used
(exported by `./gradlew :shared:exportMetrics` in metrics/comparison/fastpilot/<race>.json).

For every stint with at least MIN_POINTS laps, each lap gets the residual
    lap_time - (slope * tyre_age + intercept)
and is classified by its place in the stint (lap_start..lap_end):
    start  = first EDGE_LAPS laps of the stint,
    end    = last EDGE_LAPS laps,
    middle = the others.
The error of a group is the mean absolute residual (MAE, seconds); the bias is the mean
signed residual (positive = slower than the line).

Usage: python3 scripts/degradation_edges.py [--out metrics/comparison/degradation_edges.md]
"""

# Importa o módulo que lê opções da linha de comando (--data, --out).
import argparse
# Importa o módulo que lê e escreve JSON.
import json
# Importa a classe Path, para montar caminhos de arquivos.
from pathlib import Path
# Importa as funções de média e mediana.
from statistics import mean, median

# As cinco corridas da comparação com a FastF1 (nomes das pastas e arquivos).
RACES = ["2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil"]
# Quantas voltas contam como "borda" no início e no fim do stint.
EDGE_LAPS = 2
# Mínimo de voltas para analisar um stint: 2 no início, 2 no fim e pelo menos 4 no meio.
MIN_POINTS = 8  # 2 + 2 edge laps and at least 4 in the middle
# Nomes internos dos três grupos de voltas.
GROUPS = ["start", "middle", "end"]
# Nomes dos grupos em português, usados no relatório.
GROUP_NAMES = {"start": "Início (2 primeiras)", "middle": "Meio", "end": "Fim (2 últimas)"}


# Função que calcula o resíduo de cada volta de um stint e diz a qual grupo ela pertence.
def residuals(stint):
    # Primeira e última volta do stint.
    first, last = stint["lap_start"], stint["lap_end"]
    # Percorre cada volta usada na regressão.
    for point in stint["points"]:
        # Tempo previsto pela reta: inclinação × idade do pneu + intercepto.
        fitted = stint["slope"] * point["tyre_age"] + stint["intercept"]
        # Se a volta está entre as EDGE_LAPS primeiras do stint...
        if point["lap"] - first < EDGE_LAPS:
            # ...ela é do grupo "início".
            group = "start"
        # Senão, se está entre as EDGE_LAPS últimas...
        elif last - point["lap"] < EDGE_LAPS:
            # ...ela é do grupo "fim".
            group = "end"
        # Caso contrário...
        else:
            # ...ela é do grupo "meio".
            group = "middle"
        # Devolve o grupo e o resíduo (tempo real − tempo previsto; positivo = mais lento que a reta).
        yield group, point["lap_time"] - fitted


# Formata um número com casas decimais e vírgula no lugar do ponto (padrão brasileiro).
def fmt(value, decimals=3):
    # Monta o texto com o número de casas pedido e troca "." por ",".
    return f"{value:.{decimals}f}".replace(".", ",")


# Função principal do script.
def main():
    # Cria o leitor de opções da linha de comando.
    parser = argparse.ArgumentParser()
    # Opção --data: pasta com os JSON exportados pelo FastPilot.
    parser.add_argument("--data", default="metrics/comparison/fastpilot")
    # Opção --out: arquivo Markdown onde o relatório é gravado.
    parser.add_argument("--out", default="metrics/comparison/degradation_edges.md")
    # Lê as opções passadas.
    args = parser.parse_args()

    # Resíduos separados por corrida.
    per_race = {}
    # Resíduos de todas as corridas, separados por grupo.
    all_res = {g: [] for g in GROUPS}
    # Quantos stints foram analisados.
    stint_count = 0
    # Quantos stints têm erro maior nas bordas do que no meio.
    worse_edges = 0  # stints whose edge MAE is above their middle MAE
    # Percorre as cinco corridas.
    for race in RACES:
        # Lê o JSON da corrida exportado pelo FastPilot.
        data = json.loads((Path(args.data) / f"{race}.json").read_text())
        # Resíduos desta corrida, por grupo.
        race_res = {g: [] for g in GROUPS}
        # Percorre os dados de cada piloto.
        for driver in data["drivers"].values():
            # Percorre cada stint do piloto (lista vazia se não houver).
            for stint in driver.get("stints", []):
                # Pula o stint se não tem voltas exportadas ou se tem menos que o mínimo.
                if "points" not in stint or len(stint["points"]) < MIN_POINTS:
                    # Vai para o próximo stint.
                    continue
                # Resíduos deste stint, por grupo.
                stint_res = {g: [] for g in GROUPS}
                # Para cada volta, pega o grupo e o resíduo...
                for group, r in residuals(stint):
                    # ...e guarda o resíduo no grupo certo.
                    stint_res[group].append(r)
                # Pula o stint se ficou sem voltas no meio ou sem voltas nas bordas.
                if not stint_res["middle"] or not (stint_res["start"] or stint_res["end"]):
                    # Vai para o próximo stint.
                    continue
                # Conta mais um stint analisado.
                stint_count += 1
                # Lista dos erros absolutos das voltas de borda (início + fim).
                edge = [abs(r) for r in stint_res["start"] + stint_res["end"]]
                # Se o erro médio das bordas é maior que o erro médio do meio...
                if mean(edge) > mean(abs(r) for r in stint_res["middle"]):
                    # ...conta mais um stint com bordas piores.
                    worse_edges += 1
                # Para cada grupo...
                for g in GROUPS:
                    # ...junta os resíduos do stint aos da corrida...
                    race_res[g] += stint_res[g]
                    # ...e aos de todas as corridas.
                    all_res[g] += stint_res[g]
        # Guarda os resíduos desta corrida.
        per_race[race] = race_res

    # Linhas do relatório em Markdown.
    lines = [
        # Título do relatório.
        "# Degradação de pneu: meio do stint × bordas",
        # Linha em branco.
        "",
        # Texto que explica de onde vêm os dados e o que o relatório verifica.
        "Gerado por `python3 scripts/degradation_edges.py` a partir de `metrics/comparison/fastpilot/` "
        "(`./gradlew :shared:exportMetrics`). Verifica a afirmação do TCC I (§4.7) de que a regressão linear "
        "do tempo de volta pela idade do pneu funciona melhor no meio do stint que nas primeiras e últimas voltas.",
        # Linha em branco.
        "",
        # Texto que descreve o método, com os valores de MIN_POINTS e EDGE_LAPS.
        f"Método: stints com pelo menos {MIN_POINTS} voltas válidas (as mesmas que a regressão do FastPilot usa: "
        "sem volta de saída/entrada do box, safety car, VSC ou bandeira vermelha). Resíduo de cada volta = "
        "tempo real − reta ajustada. Grupos pela posição da volta no stint: início = "
        f"{EDGE_LAPS} primeiras, fim = {EDGE_LAPS} últimas, meio = as demais. "
        "Erro = média do resíduo absoluto (s); viés = média do resíduo com sinal (positivo = mais lento que a reta).",
        # Linha em branco.
        "",
        # Resumo: quantos stints e em quantos (e que %) as bordas erram mais; "Nenhum stint." se não houver dados.
        f"Stints analisados: **{stint_count}**; em **{worse_edges}** ({100 * worse_edges / stint_count:.0f}%) "
        "o erro médio das bordas é maior que o do meio." if stint_count else "Nenhum stint.",
        # Linha em branco.
        "",
        # Subtítulo da tabela geral.
        "## Todas as corridas",
        # Linha em branco.
        "",
        # Cabeçalho da tabela geral.
        "| Grupo | Voltas | Erro médio (s) | Erro mediano (s) | Viés (s) |",
        # Linha de alinhamento da tabela (números à direita).
        "|---|---:|---:|---:|---:|",
    # Fim da lista inicial.
    ]
    # Para cada grupo (início, meio, fim)...
    for g in GROUPS:
        # ...pega todos os resíduos do grupo...
        res = all_res[g]
        # ...e acrescenta a linha com: nome, nº de voltas, erro médio, erro mediano e viés (média com sinal).
        lines.append(f"| {GROUP_NAMES[g]} | {len(res)} | {fmt(mean(abs(r) for r in res))} | "
                     f"{fmt(median(abs(r) for r in res))} | {fmt(mean(res))} |")
    # Acrescenta o subtítulo e o cabeçalho da tabela por corrida.
    lines += ["", "## Por corrida (erro médio, s)", "",
              "| Corrida | Início | Meio | Fim | Bordas ÷ meio |", "|---|---:|---:|---:|---:|"]
    # Para cada corrida e seus resíduos...
    for race, res in per_race.items():
        # ...pula a corrida se não teve nenhuma volta no meio.
        if not res["middle"]:
            # Vai para a próxima corrida.
            continue
        # Erro médio de cada grupo que tem voltas.
        mae = {g: mean(abs(r) for r in res[g]) for g in GROUPS if res[g]}
        # Erro médio das bordas (início + fim juntos).
        edges = mean(abs(r) for r in res["start"] + res["end"])
        # Texto de cada célula: o erro formatado, ou "—" quando o grupo não tem volta.
        cell = {g: fmt(mae[g]) if g in mae else "—" for g in GROUPS}  # — = no lap in that group
        # Acrescenta a linha da corrida, com a razão bordas ÷ meio no fim.
        lines.append(f"| {race} | {cell['start']} | {cell['middle']} | {cell['end']} | "
                     f"{fmt(edges / mae['middle'], 2)}× |")
    # Linha em branco no fim do arquivo.
    lines.append("")

    # Grava o relatório no arquivo de saída.
    Path(args.out).write_text("\n".join(lines), encoding="utf-8")
    # Mostra o relatório no terminal.
    print("\n".join(lines))


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
