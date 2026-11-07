#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: compara as métricas do FastPilot com as da FastF1, calcula o MAPE e o erro médio e gera o README.md e o errors.csv da comparação.
"""
Compares FastPilot's metrics with the FastF1 reference (TCC II, Fase 4).

Inputs:  metrics/comparison/fastpilot/<race>.json   (./gradlew :shared:exportMetrics)
         metrics/comparison/fastf1/<race>.json      (scripts/fastf1_reference.py)
Outputs: metrics/comparison/README.md (tables) and metrics/comparison/errors.csv (one row per compared value)

Error measures:
- MAPE (mean absolute percentage error, target <= 5%, TCC I §4.8) for values measured in seconds
  of lap time: clean pace (mean and median), theoretical best, best lap.
- Degradation slope and lap delta oscillate around zero, where percentage errors are unstable
  (division by ~0), so they are also reported as absolute errors in seconds (MAE).
- Lap delta curves are compared on a grid of 201 points of normalised lap distance (0..1).

Stints are paired by driver, compound and the largest overlap of their lap ranges (OpenF1 and
FastF1 number stints differently in some races).

Usage: python3 scripts/compare_metrics.py [--label NAME] [--fastpilot DIR] [--out NAME] [--holdout]
  e.g. --fastpilot metrics/comparison/fastpilot_baseline --out baseline
"""
# Importa o módulo que escreve arquivos CSV (planilha em texto).
import csv
# Importa o módulo que lê JSON.
import json
# Importa o módulo sys, usado para ler os argumentos da linha de comando.
import sys
# Importa a classe Path, para montar caminhos de arquivos.
from pathlib import Path
# Importa as funções de média e mediana.
from statistics import mean, median

# Pasta raiz do repositório (duas pastas acima deste arquivo).
ROOT = Path(__file__).resolve().parent.parent
# Pasta da comparação (entradas e saídas).
BASE = ROOT / "metrics" / "comparison"
# As cinco corridas comparadas.
RACES = ["2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil"]
# Hold-out races, not used to tune the lap filters (see scripts/fetch_fixtures.py)
HOLDOUT_RACES = ["2023_japan", "2024_las_vegas"]
if "--holdout" in sys.argv:
    RACES = HOLDOUT_RACES
# Métricas de um único valor por piloto: (nome no JSON, nome em português no relatório).
SCALAR_METRICS = [
    # Ritmo limpo calculado pela média.
    ("clean_pace_mean", "Ritmo limpo (média)"),
    # Ritmo limpo calculado pela mediana.
    ("clean_pace_median", "Ritmo limpo (mediana)"),
    # Volta teórica (soma dos melhores setores).
    ("theoretical_best", "Volta teórica"),
    # Melhor volta real.
    ("best_lap", "Melhor volta"),
# Fim da lista.
]
# Grade de 201 pontos de 0 a 1 (0, 0,005, 0,01... 1), usada para comparar as curvas de delta no mesmo ponto da volta.
GRID = [i / 200 for i in range(201)]


# Erro percentual absoluto de um item (a parte de dentro do MAPE).
def ape(fp, ref):
    # |FastPilot − FastF1| ÷ |FastF1| × 100.
    return abs(fp - ref) / abs(ref) * 100


# Interpolação linear: estima y no ponto x a partir dos pontos (xs, ys), com xs em ordem crescente.
def interp(x, xs, ys):
    """Linear interpolation, xs ascending."""
    # Se x está antes do primeiro ponto...
    if x <= xs[0]:
        # ...usa o primeiro valor.
        return ys[0]
    # Se x está depois do último ponto...
    if x >= xs[-1]:
        # ...usa o último valor.
        return ys[-1]
    # Começa a busca binária com os índices do primeiro e do último ponto.
    lo, hi = 0, len(xs) - 1
    # Repete até sobrar só o par de pontos vizinhos que cerca x.
    while hi - lo > 1:
        # Índice do meio do intervalo atual.
        mid = (lo + hi) // 2
        # Se o ponto do meio ainda está à esquerda de x (ou em cima)...
        if xs[mid] <= x:
            # ...o limite de baixo sobe para o meio.
            lo = mid
        # Senão...
        else:
            # ...o limite de cima desce para o meio.
            hi = mid
    # Distância entre os dois pontos vizinhos.
    span = xs[hi] - xs[lo]
    # Se os dois pontos coincidem, usa o de baixo; senão, liga os dois por uma reta e lê o valor em x.
    return ys[lo] if span == 0 else ys[lo] + (ys[hi] - ys[lo]) * (x - xs[lo]) / span


# Formata um número para o relatório.
def fmt(value, digits=2, suffix=""):
    # Devolve "—" quando não há valor; senão, o número com as casas pedidas e o sufixo (ex.: "%").
    return "—" if value is None else f"{value:.{digits}f}{suffix}"


# Lê o valor de uma opção da linha de comando (ex.: --out baseline).
def arg(name, default):
    # Se a opção foi passada, devolve o argumento seguinte a ela; senão, o valor padrão.
    return sys.argv[sys.argv.index(name) + 1] if name in sys.argv else default


# Acha, na FastF1, o stint que corresponde a um stint do FastPilot.
def matching_stint(stint, candidates):
    """Reference stint with the same compound and the largest lap overlap (at least half of the stint)."""
    # Melhor candidato até agora e quantas voltas ele tem em comum.
    best, best_overlap = None, 0
    # Percorre os stints da FastF1 do mesmo piloto.
    for c in candidates:
        # Se o composto do pneu é diferente...
        if c["compound"] != stint["compound"]:
            # ...não pode ser o mesmo stint; vai para o próximo.
            continue
        # Quantas voltas os dois stints têm em comum (sobreposição dos intervalos de voltas).
        overlap = min(stint["lap_end"], c["lap_end"]) - max(stint["lap_start"], c["lap_start"]) + 1
        # Se essa sobreposição é a maior até agora...
        if overlap > best_overlap:
            # ...esse passa a ser o melhor candidato.
            best, best_overlap = c, overlap
    # Número de voltas do stint do FastPilot.
    length = stint["lap_end"] - stint["lap_start"] + 1
    # Só aceita o par se as voltas em comum cobrem pelo menos metade do stint; senão, não há par.
    return best if best_overlap * 2 >= length else None


# Função principal do script.
def main():
    # Nome da execução que aparece no relatório (padrão: "atual").
    label = arg("--label", "atual")
    # Pasta com os JSON do FastPilot (padrão: metrics/comparison/fastpilot).
    fastpilot_dir = ROOT / arg("--fastpilot", "metrics/comparison/fastpilot")
    # Nome alternativo para os arquivos de saída (vazio = README.md e errors.csv).
    out_name = arg("--out", "")
    # Linhas comparadas: (corrida, métrica, item, valor FastPilot, valor FastF1, erro absoluto, erro %).
    rows = []  # (race, metric, item, fastpilot, fastf1, abs_error, ape)

    # Percorre as cinco corridas.
    for race in RACES:
        # Lê o JSON do FastPilot da corrida.
        fp = json.loads((fastpilot_dir / f"{race}.json").read_text())
        # Lê o JSON da FastF1 da corrida.
        ff = json.loads((BASE / "fastf1" / f"{race}.json").read_text())

        # Percorre os pilotos que aparecem nos dois lados, em ordem numérica.
        for driver in sorted(set(fp["drivers"]) & set(ff["drivers"]), key=int):
            # a = métricas do FastPilot, b = métricas da FastF1, para esse piloto.
            a, b = fp["drivers"][driver], ff["drivers"][driver]
            # Para cada métrica de valor único...
            for key, _ in SCALAR_METRICS:
                # ...se os dois lados têm o valor...
                if a.get(key) is not None and b.get(key) is not None:
                    # ...guarda a linha com os dois valores, o erro absoluto e o erro percentual.
                    rows.append((race, key, f"#{driver}", a[key], b[key], abs(a[key] - b[key]), ape(a[key], b[key])))
            # Para cada stint do FastPilot...
            for s in a["stints"]:
                # ...procura o stint correspondente na FastF1.
                r = matching_stint(s, b["stints"])
                # Se encontrou o par...
                if r:
                    # ...guarda a linha da degradação: inclinações dos dois lados, erro absoluto e erro %
                    rows.append((race, "degradation_slope", f"#{driver} stint {s['stint']} {s['compound']}",
                                 s["slope"], r["slope"], abs(s["slope"] - r["slope"]),
                                 # (o erro % fica vazio quando a inclinação da FastF1 é exatamente zero, para não dividir por zero).
                                 ape(s["slope"], r["slope"]) if r["slope"] != 0 else None))

        # Deltas da FastF1 indexados pelo par (piloto de referência, piloto comparado).
        ref_deltas = {(d["reference"], d["compared"]): d for d in ff["lap_deltas"]}
        # Percorre os deltas do FastPilot.
        for d in fp["lap_deltas"]:
            # Procura o mesmo par na FastF1.
            r = ref_deltas.get((d["reference"], d["compared"]))
            # Se a FastF1 não tem esse par...
            if not r:
                # ...pula para o próximo.
                continue
            # Nome do item, ex.: "#1 L44 vs #11 L37" (piloto e volta de cada lado).
            item = f"#{d['reference']} L{d['reference_lap']} vs #{d['compared']} L{d['compared_lap']}"
            # Se a FastF1 escolheu voltas diferentes como as mais rápidas...
            if (r["reference_lap"], r["compared_lap"]) != (d["reference_lap"], d["compared_lap"]):
                # ...anota no nome do item quais voltas ela usou.
                item += f" (FastF1: L{r['reference_lap']} vs L{r['compared_lap']})"
            # Em cada ponto da grade, a diferença absoluta entre as duas curvas de delta (cada uma interpolada no ponto).
            errors = [abs(interp(g, d["fraction"], d["delta"]) - interp(g, r["fraction"], r["delta"])) for g in GRID]
            # Guarda o erro médio entre as curvas.
            rows.append((race, "lap_delta_curve", item, None, None, mean(errors), None))
            # Guarda a comparação do valor final: diferença oficial do FastPilot × último valor da curva da FastF1.
            rows.append((race, "lap_delta_final", item, d["official_gap"], r["delta"][-1],
                         abs(d["official_gap"] - r["delta"][-1]), None))

    # Sufixo dos arquivos de saída (ex.: "_baseline"), ou vazio.
    suffix = f"_{out_name}" if out_name else ""
    # Abre o CSV de erros para escrita, em UTF-8.
    with open(BASE / f"errors{suffix}.csv", "w", newline="", encoding="utf-8") as f:
        # Cria o escritor de CSV.
        w = csv.writer(f)
        # Escreve o cabeçalho das colunas.
        w.writerow(["race", "metric", "item", "fastpilot", "fastf1", "abs_error", "ape_percent"])
        # Para cada linha comparada...
        for row in rows:
            # ...escreve a linha, arredondando os números decimais para 5 casas.
            w.writerow([x if not isinstance(x, float) else round(x, 5) for x in row])

    # Função auxiliar: filtra as linhas de uma métrica (e, se pedido, de uma corrida).
    def select(metric, race=None):
        # Devolve as linhas cuja métrica bate e cuja corrida bate (ou qualquer corrida, se race é None).
        return [r for r in rows if r[1] == metric and (race is None or r[0] == race)]

    # Linhas do relatório em Markdown.
    lines = [
        # Título.
        "# Comparação FastPilot × FastF1",
        # Linha em branco.
        "",
        # Nome da execução e origem dos dados.
        f"Execução: **{label}**. Gerado por `scripts/compare_metrics.py` a partir de",
        # Pasta do FastPilot usada e pasta da FastF1.
        f"`{fastpilot_dir.relative_to(ROOT)}/` (`./gradlew :shared:exportMetrics`) e `metrics/comparison/fastf1/`",
        # Onde estão os detalhes por item.
        f"(`scripts/fastf1_reference.py`). Detalhe por item em `errors{suffix}.csv`.",
        # Linha em branco.
        "",
        # Subtítulo da tabela de MAPE.
        "## Métricas em segundos de volta (MAPE, meta ≤ 5%)",
        # Linha em branco.
        "",
        # Cabeçalho: uma coluna por corrida, mais geral, erro médio e quantidade.
        "| Métrica | " + " | ".join(RACES) + " | Geral | MAE geral (s) | n |",
        # Linha de alinhamento (colunas de número à direita).
        "|---|" + "---:|" * (len(RACES) + 3),
    # Fim da lista inicial.
    ]
    # Para cada métrica de valor único...
    for key, name in SCALAR_METRICS:
        # ...as linhas dela em cada corrida...
        per_race = [select(key, race) for race in RACES]
        # ...e em todas as corridas.
        all_rows = select(key)
        # MAPE de cada corrida: média dos erros percentuais (coluna 6), ou "—" se não há linhas.
        cells = [fmt(mean(r[6] for r in pr), 2, "%") if pr else "—" for pr in per_race]
        # Acrescenta a linha: MAPE por corrida, MAPE geral em negrito, erro médio em segundos (coluna 5) e quantidade.
        lines.append(f"| {name} | " + " | ".join(cells) +
                     f" | **{fmt(mean(r[6] for r in all_rows), 2, '%')}** | {fmt(mean(r[5] for r in all_rows), 3)} | {len(all_rows)} |")

    # Todas as linhas de degradação.
    deg = select("degradation_slope")
    # Erros percentuais da degradação, sem os vazios (stints com inclinação zero na FastF1).
    deg_ape = [r[6] for r in deg if r[6] is not None]
    # Quantos stints têm o mesmo sinal nos dois lados (os dois positivos ou os dois não positivos).
    same_sign = sum(1 for r in deg if (r[3] > 0) == (r[4] > 0))
    # Acrescenta o subtítulo e o cabeçalho da tabela de degradação.
    lines += [
        # Linha em branco.
        "",
        # Subtítulo.
        "## Degradação de pneu (inclinação em s/volta por stint)",
        # Linha em branco.
        "",
        # Cabeçalho.
        "| Corrida | Stints comparados | MAE (s/volta) | Mediana do erro (s/volta) | MAPE | Mesmo sinal |",
        # Alinhamento.
        "|---|---:|---:|---:|---:|---:|",
    # Fim da lista.
    ]
    # Para cada corrida...
    for race in RACES:
        # ...as linhas de degradação dela.
        rr = select("degradation_slope", race)
        # Se há linhas...
        if rr:
            # ...acrescenta: nº de stints, erro médio, erro mediano,
            lines.append(f"| {race} | {len(rr)} | {fmt(mean(r[5] for r in rr), 3)} | {fmt(median(r[5] for r in rr), 3)} | "
                         # MAPE (sem os stints de inclinação zero)
                         f"{fmt(mean(r[6] for r in rr if r[6] is not None), 1, '%')} | "
                         # e quantos stints têm o mesmo sinal, do total.
                         f"{sum(1 for r in rr if (r[3] > 0) == (r[4] > 0))}/{len(rr)} |")
    # Acrescenta a linha geral da degradação, com o erro médio em negrito.
    lines.append(f"| **Geral** | {len(deg)} | **{fmt(mean(r[5] for r in deg), 3)}** | {fmt(median(r[5] for r in deg), 3)} | "
                 f"{fmt(mean(deg_ape), 1, '%')} | {same_sign}/{len(deg)} |")

    # Acrescenta o subtítulo e o cabeçalho da tabela de delta.
    lines += [
        # Linha em branco.
        "",
        # Subtítulo.
        "## Delta na volta (§4.7)",
        # Linha em branco.
        "",
        # Explicação da medida.
        "Erro médio absoluto entre as curvas em 201 pontos da distância normalizada, e diferença no valor final.",
        # Linha em branco.
        "",
        # Cabeçalho.
        "| Corrida | Par | MAE da curva (s) | Gap final FastPilot (s) | Gap final FastF1 (s) |",
        # Alinhamento.
        "|---|---|---:|---:|---:|",
    # Fim da lista.
    ]
    # Para cada corrida...
    for race in RACES:
        # ...as comparações de valor final, indexadas pelo nome do par.
        finals = {r[2]: r for r in select("lap_delta_final", race)}
        # Para cada comparação de curva...
        for r in select("lap_delta_curve", race):
            # ...pega a comparação de valor final do mesmo par...
            f = finals[r[2]]
            # ...e acrescenta a linha: par, erro da curva, gap final do FastPilot e da FastF1.
            lines.append(f"| {race} | {r[2]} | {fmt(r[5], 3)} | {fmt(f[3], 3)} | {fmt(f[4], 3)} |")
    # Todas as comparações de curva.
    curve = select("lap_delta_curve")
    # Acrescenta a linha geral, com o erro médio das curvas em negrito.
    lines.append(f"| **Geral** | {len(curve)} pares | **{fmt(mean(r[5] for r in curve), 3)}** | | |")
    # Linha em branco no fim.
    lines.append("")

    # Junta as linhas em um único texto.
    report = "\n".join(lines)
    # Grava o relatório (README.md, ou <out>.md se --out foi passado).
    (BASE / (f"{out_name}.md" if out_name else "README.md")).write_text(report, encoding="utf-8")
    # Mostra o relatório no terminal.
    print(report)


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
