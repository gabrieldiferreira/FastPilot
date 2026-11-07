#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: lê as medições gravadas pelo perf_run.py e monta o relatório de desempenho (metrics/performance/README.md) e o summary.csv.
"""
Builds metrics/performance/README.md from the runs recorded by scripts/perf_run.py
(one folder per platform/device under metrics/performance/; folders starting with "_" are ignored).

    scripts/perf_report.py [--dir metrics/performance]

Tables: load time per screen without/with cache and without/with live polling, OpenF1 rate
limiting (429) per scenario, and memory/CPU per benchmark phase. Also writes summary.csv.
"""

# Importa o leitor de opções da linha de comando.
import argparse
# Importa o módulo que lê e escreve CSV.
import csv
# Importa o módulo que lê JSON.
import json
# Importa as funções estatísticas (mediana, média, desvio padrão).
import statistics
# Importa a classe Path, para montar caminhos.
from pathlib import Path

# Pasta raiz do repositório.
ROOT = Path(__file__).resolve().parent.parent

# Passos medidos: (tela, passo, nome em português no relatório).
STEPS = [
    # Abertura do Dashboard.
    ("dashboard", "open", "Dashboard"),
    # Abertura da Telemetria.
    ("telemetry", "open", "Telemetria — abertura"),
    # Telemetria depois de escolher o piloto (carrega a volta).
    ("telemetry", "lap", "Telemetria — volta do piloto¹"),
    # Abertura da Estratégia (stints).
    ("strategy", "open", "Estratégia — stints"),
    # Estratégia até calcular ritmo limpo e degradação.
    ("strategy", "metrics", "Estratégia — ritmo/degradação²"),
    # Abertura do Duelo.
    ("duel", "open", "Duelo — abertura"),
    # Duelo depois de escolher os dois pilotos.
    ("duel", "compare", "Duelo — comparação¹"),
    # Abertura do Rádio.
    ("radio", "open", "Rádio"),
    # Abertura do Clima.
    ("weather", "open", "Clima"),
    # Abertura da Classificação.
    ("standings", "open", "Classificação"),
# Fim da lista.
]
# Cenários: (cache frio = sem cache / quente = com cache, com ou sem busca ao vivo).
SCENARIOS = [("cold", False), ("warm", False), ("cold", True), ("warm", True)]
# Nome em português de cada cenário.
SCENARIO_NAMES = {
    # Sem cache, sem busca ao vivo.
    ("cold", False): "Sem cache",
    # Com cache, sem busca ao vivo.
    ("warm", False): "Com cache",
    # Sem cache, com busca ao vivo.
    ("cold", True): "Sem cache + polling",
    # Com cache, com busca ao vivo.
    ("warm", True): "Com cache + polling",
# Fim do dicionário.
}
# Fases do benchmark: (nome interno, nome em português) — usadas na tabela de memória e CPU.
PHASES = [
    # App parado.
    ("idle", "Ocioso"),
    # Abrindo telas sem busca ao vivo.
    ("loads_polling_off", "Carregando telas"),
    # Parado, com busca ao vivo.
    ("idle_polling_on", "Ocioso + polling"),
    # Abrindo telas com busca ao vivo.
    ("loads_polling_on", "Carregando telas + polling"),
# Fim da lista.
]


# Converte milissegundos em segundos com 2 casas e vírgula (ex.: 1700 -> "1,70").
def fmt_s(ms: float) -> str:
    # ms ÷ 1000, formatado, com "." trocado por ",".
    return f"{ms / 1000:.2f}".replace(".", ",")


# Formata um número com as casas pedidas e vírgula decimal.
def fmt(x: float, digits: int = 1) -> str:
    # Número formatado, com "." trocado por ",".
    return f"{x:.{digits}f}".replace(".", ",")


# Lê os arquivos de uma pasta de medição: eventos, dados do aparelho e amostras de recursos.
def load_platform(folder: Path):
    # Cada linha não vazia do loads.jsonl vira um evento (dicionário).
    events = [json.loads(line) for line in (folder / "loads.jsonl").read_text().splitlines() if line.strip()]
    # Dados do aparelho, se o device.json existe; senão, dicionário vazio.
    device = json.loads((folder / "device.json").read_text()) if (folder / "device.json").exists() else {}
    # Lista de amostras de memória e CPU.
    resources = []
    # Se existe o resources.csv...
    if (folder / "resources.csv").exists():
        # ...abre o arquivo...
        with (folder / "resources.csv").open() as f:
            # ...e, para cada linha...
            for row in csv.DictReader(f):
                # ...converte os valores preenchidos em números e guarda.
                resources.append({k: float(v) for k, v in row.items() if v != ""})
    # Devolve as três coisas.
    return events, device, resources


# Calcula quantidade, mediana, média e desvio padrão de uma lista de valores.
def stats(values):
    # Se a lista está vazia...
    if not values:
        # ...não há estatística.
        return None
    # Devolve as estatísticas.
    return {
        # Quantidade.
        "n": len(values),
        # Mediana.
        "median": statistics.median(values),
        # Média.
        "mean": statistics.fmean(values),
        # Desvio padrão (zero se só há um valor).
        "sd": statistics.stdev(values) if len(values) > 1 else 0.0,
    # Fim do dicionário.
    }


# Monta o nome do ambiente para o relatório (ex.: "Android 17 — sdk_gphone16k_arm64 (emulador)").
def device_label(name: str, device: dict) -> str:
    # Se é Android...
    if device.get("platform") == "android":
        # ...diz se é emulador ou aparelho físico...
        kind = "emulador" if device.get("emulator") else "dispositivo físico"
        # ...e devolve versão, modelo e tipo.
        return f"Android {device.get('android')} — {device.get('model')} ({kind})"
    # Se é iOS...
    if device.get("platform") == "ios":
        # ...devolve a versão do iOS (de "iOS-26-5" para "26.5") e o modelo do simulador.
        return f"iOS {device.get('ios', '').replace('iOS-', '').replace('-', '.')} — {device.get('model')} (simulador)"
    # Se é Web...
    if device.get("platform") == "web":
        # ...devolve o navegador.
        return f"Web — {device.get('browser')} headless"
    # Caso não reconheça, usa o nome da pasta.
    return name


# Função principal do script.
def main():
    # Cria o leitor de opções, usando o texto do topo do arquivo como ajuda.
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    # Opção --dir: pasta com as medições (padrão: metrics/performance).
    parser.add_argument("--dir", default=str(ROOT / "metrics/performance"))
    # Lê as opções.
    args = parser.parse_args()
    # Pasta das medições.
    base = Path(args.dir)

    # Lista das plataformas medidas.
    platforms = []
    # Percorre as subpastas em ordem alfabética, ignorando as que começam com "_" (medições antigas ou parciais).
    for folder in sorted(p for p in base.iterdir() if p.is_dir() and not p.name.startswith("_")):
        # Se a pasta tem o loads.jsonl...
        if (folder / "loads.jsonl").exists():
            # ...guarda (nome da pasta, eventos, aparelho, recursos).
            platforms.append((folder.name, *load_platform(folder)))

    # Linhas do relatório: começa pelo título e uma linha em branco.
    out = ["# Desempenho — tempos de carregamento, memória e CPU", ""]
    # Acrescenta a introdução, a metodologia e o cabeçalho da tabela de ambientes.
    out += [
        # Origem do relatório (duas partes de texto que formam uma linha).
        "Gerado por `scripts/perf_report.py` a partir das execuções de `scripts/perf_run.py` "
        "(benchmark embutido no app: `composeApp/src/commonMain/kotlin/com/fastpilot/ui/perf/LoadBenchmark.kt`).",
        # Linha em branco.
        "",
        # Parágrafo da metodologia (as próximas linhas de texto são pedaços de um único parágrafo).
        "**Metodologia.** Cada tela é aberta criando o seu ViewModel, como faz a navegação, e o tempo vai até o "
        "estado que exibe o conteúdo (renderização do quadro não incluída). *Sem cache*: banco local (SQLDelight), "
        "cache HTTP da plataforma e, desde a rodada 4, o calendário e a classificação guardados em memória limpos antes da medição; *com cache*: a mesma tela logo em seguida, com o banco "
        "preenchido (a sessão medida já terminou: o app guarda pilotos, voltas, stints, posições finais, intervalos, "
        "telemetria e posições na pista da volta, rádio, clima, direção de prova, pit stops, ultrapassagens e a "
        "classificação; só os áudios de rádio não entram). *Polling*: o modo ao vivo do Dashboard simulado em "
        "segundo plano, direto na API (posições e intervalos a cada 4 s, direção de prova a cada 10 s, pit stops e "
        "ultrapassagens a cada 15 s, clima a cada 60 s). "
        "Pausa antes de cada medição (15 s nas rodadas 1–2, 20 s nas rodadas 3 e 4) para respeitar a cota grátis da OpenF1 "
        "(3 requisições/s e 30/min). Desde a rodada 3 o cliente espaça as requisições (uma a cada 400 ms, no máximo "
        "29 em 61 s) em vez de receber 429; respostas 429 que ainda ocorram são repetidas após 2, 4, 8, 16 e 16 s. Sessão: a corrida mais recente no dia da medição "
        "(rodada 4, 06/11/2026: GP do México 2026), a mesma que as telas escolhem por padrão. Valores em segundos: mediana "
        "(média ± desvio padrão), apenas medições bem-sucedidas; n = sucessos/tentativas.",
        # Linha em branco.
        "",
        # Notas de rodapé ¹ e ² (duas partes de uma linha).
        "¹ Tempo a partir da ação do usuário (selecionar piloto / os dois pilotos). "
        "² Tempo a partir da abertura da tela (as métricas continuam carregando depois dos stints).",
        # Linha em branco.
        "",
        # Subtítulo.
        "## Ambientes",
        # Linha em branco.
        "",
        # Cabeçalho da tabela de ambientes.
        "| Pasta | Ambiente | Build |",
        # Alinhamento.
        "|---|---|---|",
    # Fim da lista.
    ]
    # Para cada plataforma...
    for name, events, device, _ in platforms:
        # ...acrescenta a pasta, o nome do ambiente e o tipo de build.
        out.append(f"| `{name}` | {device_label(name, device)} | {device.get('build', '')} |")
    # Linha em branco.
    out.append("")

    # Linhas do summary.csv.
    summary_rows = []
    # Medianas guardadas para as comparações: (plataforma, tela, passo, cache, polling) -> mediana em ms.
    medians = {}  # (platform, screen, step, cache, polling) -> median ms
    # Para cada plataforma...
    for name, events, device, resources in platforms:
        # ...os eventos de carregamento...
        loads = [e for e in events if e.get("type") == "load"]
        # ...os eventos de falha...
        failures = [e for e in events if e.get("type") == "failure"]
        # ...e se houve alguma medição com cache.
        has_warm = any(e["cache"] == "warm" for e in loads)
        # Scenarios measured on this platform (a round may skip polling: "on:0")
        # Cenários que realmente foram medidos nesta plataforma (uma rodada pode não ter polling).
        scenarios = [s for s in SCENARIOS if (has_warm or s[0] == "cold") and any(e["polling"] == s[1] for e in loads)]
        # Subtítulo com o nome do ambiente.
        out += [f"## {device_label(name, device)}", ""]
        # Se não houve medição com cache (é a Web)...
        if not has_warm:
            # ...explica que todas as medições são sem cache.
            out += ["Sem banco local nesta plataforma: todas as medições são *sem cache*.", ""]
        # Se não houve medição com polling...
        if not any(e["polling"] for e in loads):
            # ...explica o motivo (a cota grátis não comporta o polling).
            out += ["Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo "
                    "sozinho (~46/min) esgota a cota.", ""]

        # Cabeçalho da tabela de tempos: uma coluna por cenário medido.
        out.append("| Tela | " + " | ".join(SCENARIO_NAMES[s] for s in scenarios) + " |")
        # Alinhamento.
        out.append("|---|" + "---|" * len(scenarios))
        # Para cada passo (tela)...
        for screen, step, label in STEPS:
            # ...células da linha.
            cells = []
            # Para cada cenário...
            for cache, polling in scenarios:
                # ...os carregamentos desse passo nesse cenário
                group = [e for e in loads if e["screen"] == screen and e["step"] == step
                         # (mesma tela, passo, cache e polling)...
                         and e["cache"] == cache and e["polling"] == polling]
                # ...os tempos dos que deram certo...
                ok = [e["ms"] for e in group if e["status"] == "ok"]
                # ...e as estatísticas desses tempos.
                st = stats(ok)
                # Se houve alguma tentativa...
                if group:
                    # ...acrescenta a linha do summary.csv:
                    summary_rows.append({
                        # identificação (plataforma, tela, passo, cache, polling),
                        "platform": name, "screen": screen, "step": step, "cache": cache, "polling": polling,
                        # tentativas e sucessos,
                        "attempts": len(group), "ok": len(ok),
                        # mediana (vazia se nenhum sucesso),
                        "median_ms": round(st["median"], 1) if st else "",
                        # média,
                        "mean_ms": round(st["mean"], 1) if st else "",
                        # desvio padrão,
                        "sd_ms": round(st["sd"], 1) if st else "",
                        # total de requisições HTTP,
                        "requests": sum(e["requests"] for e in group),
                        # e total de respostas 429.
                        "rate_limited": sum(e["rateLimited"] for e in group),
                    # Fim do dicionário.
                    })
                # Se há estatística...
                if st:
                    # ...guarda a mediana para as comparações...
                    medians[(name, screen, step, cache, polling)] = st["median"]
                    # ...e escreve a célula "mediana (média ± desvio) n=sucessos/tentativas".
                    cells.append(f"{fmt_s(st['median'])} ({fmt_s(st['mean'])} ± {fmt_s(st['sd'])}) n={len(ok)}/{len(group)}")
                # Senão...
                else:
                    # ...escreve "—" (com n=0/tentativas se houve tentativas).
                    cells.append(f"— n=0/{len(group)}" if group else "—")
            # Acrescenta a linha da tela.
            out.append(f"| {label} | " + " | ".join(cells) + " |")
        # Linha em branco.
        out.append("")

        # Cache gain and polling cost, on the medians of each screen
        # Mediana, entre as telas, da razão "tempo no cenário B ÷ tempo no cenário A".
        def ratio(cache_a, poll_a, cache_b, poll_b):
            # Pares (mediana no cenário A, mediana no cenário B) de cada tela
            pairs = [(medians.get((name, s, st, cache_a, poll_a)), medians.get((name, s, st, cache_b, poll_b)))
                     # percorrendo todos os passos.
                     for s, st, _ in STEPS]
            # Só os pares em que os dois lados existem.
            pairs = [(a, b) for a, b in pairs if a and b]
            # Mediana das razões B ÷ A, ou None se não há pares.
            return statistics.median(b / a for a, b in pairs) if pairs else None

        # Frases de resumo.
        notes = []
        # Se houve medição com cache...
        if has_warm:
            # ...razão com cache ÷ sem cache...
            r = ratio("cold", False, "warm", False)
            # ...e, se calculada...
            if r:
                # ...a frase "com cache, a mediana das telas cai para X% do tempo sem cache" (os 2% a 3% do TCC).
                notes.append(f"com cache, a mediana das telas cai para **{fmt(r * 100, 0)}%** do tempo sem cache")
        # Razão com polling ÷ sem polling (sem cache).
        r = ratio("cold", False, "cold", True)
        # Se calculada...
        if r:
            # ...acrescenta a frase do custo do polling.
            notes.append(f"com polling ativo, o tempo sem cache fica em **{fmt(r * 100, 0)}%** do tempo sem polling")
        # Se há alguma frase...
        if notes:
            # ...acrescenta o parágrafo de resumo.
            out += ["Mediana das razões por tela: " + "; ".join(notes) + ".", ""]

        # Requests and rate limiting per scenario
        # Cabeçalho da tabela de requisições e recusas por cenário.
        out += ["| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |", "|---|---|---|---|"]
        # Para cada cenário...
        for cache, polling in scenarios:
            # ...os carregamentos do cenário...
            group = [e for e in loads if e["cache"] == cache and e["polling"] == polling]
            # ...quantos não deram certo...
            bad = sum(1 for e in group if e["status"] != "ok")
            # ...e a linha: requisições, respostas 429
            out.append(f"| {SCENARIO_NAMES[(cache, polling)]} | {sum(e['requests'] for e in group)} | "
                       # e falhas do total.
                       f"{sum(e['rateLimited'] for e in group)} | {bad} de {len(group)} |")
        # Se houve exceções durante as medições...
        if failures:
            # ...informa quantas.
            out.append(f"\nExceções durante as medições: {len(failures)} (ver `loads.jsonl`).")
        # Nota sobre o que entra na contagem de requisições.
        out += ["", "Requisições contadas durante as medições (as do polling entre medições não entram).", ""]

        # Resources per phase
        # Se há amostras de memória e CPU...
        if resources:
            # ...hora de início de cada fase registrada.
            phase_t = {e["phase"]: e["t"] for e in events if e.get("type") == "phase"}
            # Each phase ends where the next recorded one starts (a round without polling has no polling phases)
            # Horas de início em ordem: cada fase termina onde começa a próxima.
            starts = sorted(phase_t.values())
            # Only Android has a second memory figure (PSS besides RSS)
            # Só o Android tem duas medidas de memória (PSS e RSS).
            android = device.get("platform") == "android"
            # Nome da medida de memória principal.
            mem_label = "PSS (MB)" if android else "RSS (MB)"
            # Coluna extra de RSS, só no Android.
            rss_header = " RSS (MB) média |" if android else ""
            # Cabeçalho da tabela de recursos
            out += [f"| Fase | {mem_label} média | {mem_label} pico |{rss_header} CPU média (%) | CPU p95 (%) |",
                    # e o alinhamento (com uma coluna a mais no Android).
                    "|---|---|---|" + ("---|" if android else "") + "---|---|"]
            # Para cada fase...
            for phase, label in PHASES:
                # ...quando ela começou...
                start = phase_t.get(phase)
                # ...e quando terminou (início da fase seguinte).
                end = next((t for t in starts if start is not None and t > start), None)
                # Se a fase não foi registrada ou não tem fim...
                if start is None or end is None:
                    # ...pula.
                    continue
                # Amostras tiradas durante a fase.
                window = [r for r in resources if start <= r["t_ms"] < end]
                # Se não há amostra...
                if not window:
                    # ...pula.
                    continue
                # Memória principal de cada amostra.
                mem = [r["memory_mb"] for r in window]
                # Memória residente de cada amostra (ou a principal, se não houver).
                rss = [r.get("rss_mb", r["memory_mb"]) for r in window]
                # CPU de cada amostra, em ordem crescente.
                cpu = sorted(r["cpu_pct"] for r in window)
                # Percentil 95 da CPU (valor abaixo do qual ficam 95% das amostras).
                p95 = cpu[min(len(cpu) - 1, int(round(0.95 * (len(cpu) - 1))))]
                # Célula de RSS média, só no Android.
                rss_cell = f" {fmt(statistics.fmean(rss))} |" if android else ""
                # Linha da fase: memória média e pico,
                out.append(f"| {label} | {fmt(statistics.fmean(mem))} | {fmt(max(mem))} |{rss_cell} "
                           # CPU média e percentil 95.
                           f"{fmt(statistics.fmean(cpu))} | {fmt(p95)} |")
            # Nota explicando como a CPU e a memória foram medidas (três partes de texto de um parágrafo).
            out += ["", "CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app "
                        "(Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; "
                        "iOS simulador e Web: `ps` no processo do app / renderer do Chrome).", ""]

    # Cross-platform comparison without cache and without polling
    # Se há mais de uma plataforma...
    if len(platforms) > 1:
        # ...monta a tabela de comparação entre plataformas (sem cache e sem polling):
        out += ["## Comparação entre plataformas (sem cache, sem polling — mediana em s)", "",
                # cabeçalho com uma coluna por plataforma
                "| Tela | " + " | ".join(f"`{n}`" for n, *_ in platforms) + " |",
                # e o alinhamento.
                "|---|" + "---|" * len(platforms)]
        # Para cada passo (tela)...
        for screen, step, label in STEPS:
            # ...a mediana sem cache e sem polling em cada plataforma (ou "—" se não medida)
            cells = [fmt_s(medians[(n, screen, step, "cold", False)]) if (n, screen, step, "cold", False) in medians else "—"
                     # percorrendo as plataformas.
                     for n, *_ in platforms]
            # Acrescenta a linha da tela.
            out.append(f"| {label} | " + " | ".join(cells) + " |")
        # Linha em branco.
        out.append("")

    # Grava o README.md do desempenho.
    (base / "README.md").write_text("\n".join(out))
    # Abre o summary.csv para escrita.
    with (base / "summary.csv").open("w", newline="") as f:
        # Cria o escritor de CSV com as colunas da primeira linha (ou só "platform", se não houver linhas).
        writer = csv.DictWriter(f, fieldnames=list(summary_rows[0].keys()) if summary_rows else ["platform"])
        # Escreve o cabeçalho.
        writer.writeheader()
        # Escreve todas as linhas.
        writer.writerows(summary_rows)
    # Mostra o que foi gravado.
    print(f"wrote {base / 'README.md'} and summary.csv ({len(platforms)} platforms)")


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
