#!/usr/bin/env python3
"""
Builds metrics/performance/README.md from the runs recorded by scripts/perf_run.py
(one folder per platform/device under metrics/performance/; folders starting with "_" are ignored).

    scripts/perf_report.py [--dir metrics/performance]

Tables: load time per screen without/with cache and without/with live polling, OpenF1 rate
limiting (429) per scenario, and memory/CPU per benchmark phase. Also writes summary.csv.
"""

import argparse
import csv
import json
import statistics
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

STEPS = [
    ("dashboard", "open", "Dashboard"),
    ("telemetry", "open", "Telemetria — abertura"),
    ("telemetry", "lap", "Telemetria — volta do piloto¹"),
    ("strategy", "open", "Estratégia — stints"),
    ("strategy", "metrics", "Estratégia — ritmo/degradação²"),
    ("duel", "open", "Duelo — abertura"),
    ("duel", "compare", "Duelo — comparação¹"),
    ("radio", "open", "Rádio"),
    ("weather", "open", "Clima"),
    ("standings", "open", "Classificação"),
]
SCENARIOS = [("cold", False), ("warm", False), ("cold", True), ("warm", True)]
SCENARIO_NAMES = {
    ("cold", False): "Sem cache",
    ("warm", False): "Com cache",
    ("cold", True): "Sem cache + polling",
    ("warm", True): "Com cache + polling",
}
PHASES = [
    ("idle", "Ocioso"),
    ("loads_polling_off", "Carregando telas"),
    ("idle_polling_on", "Ocioso + polling"),
    ("loads_polling_on", "Carregando telas + polling"),
]


def fmt_s(ms: float) -> str:
    return f"{ms / 1000:.2f}".replace(".", ",")


def fmt(x: float, digits: int = 1) -> str:
    return f"{x:.{digits}f}".replace(".", ",")


def load_platform(folder: Path):
    events = [json.loads(line) for line in (folder / "loads.jsonl").read_text().splitlines() if line.strip()]
    device = json.loads((folder / "device.json").read_text()) if (folder / "device.json").exists() else {}
    resources = []
    if (folder / "resources.csv").exists():
        with (folder / "resources.csv").open() as f:
            for row in csv.DictReader(f):
                resources.append({k: float(v) for k, v in row.items() if v != ""})
    return events, device, resources


def stats(values):
    if not values:
        return None
    return {
        "n": len(values),
        "median": statistics.median(values),
        "mean": statistics.fmean(values),
        "sd": statistics.stdev(values) if len(values) > 1 else 0.0,
    }


def device_label(name: str, device: dict) -> str:
    if device.get("platform") == "android":
        kind = "emulador" if device.get("emulator") else "dispositivo físico"
        return f"Android {device.get('android')} — {device.get('model')} ({kind})"
    if device.get("platform") == "ios":
        return f"iOS {device.get('ios', '').replace('iOS-', '').replace('-', '.')} — {device.get('model')} (simulador)"
    if device.get("platform") == "web":
        return f"Web — {device.get('browser')} headless"
    return name


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", default=str(ROOT / "metrics/performance"))
    args = parser.parse_args()
    base = Path(args.dir)

    platforms = []
    for folder in sorted(p for p in base.iterdir() if p.is_dir() and not p.name.startswith("_")):
        if (folder / "loads.jsonl").exists():
            platforms.append((folder.name, *load_platform(folder)))

    out = ["# Desempenho — tempos de carregamento, memória e CPU", ""]
    out += [
        "Gerado por `scripts/perf_report.py` a partir das execuções de `scripts/perf_run.py` "
        "(benchmark embutido no app: `composeApp/src/commonMain/kotlin/com/fastpilot/ui/perf/LoadBenchmark.kt`).",
        "",
        "**Metodologia.** Cada tela é aberta criando o seu ViewModel, como faz a navegação, e o tempo vai até o "
        "estado que exibe o conteúdo (renderização do quadro não incluída). *Sem cache*: banco local (SQLDelight) "
        "e cache HTTP da plataforma limpos antes da medição; *com cache*: a mesma tela logo em seguida, com o banco "
        "preenchido (a sessão medida já terminou: o app guarda pilotos, voltas, stints, posições finais, intervalos, "
        "telemetria e posições na pista da volta, rádio, clima, direção de prova, pit stops, ultrapassagens e a "
        "classificação; só os áudios de rádio não entram). *Polling*: o modo ao vivo do Dashboard simulado em "
        "segundo plano, direto na API (posições e intervalos a cada 4 s, direção de prova a cada 10 s, pit stops e "
        "ultrapassagens a cada 15 s, clima a cada 60 s). "
        "Pausa antes de cada medição (15 s nas rodadas 1–2, 20 s na 3) para respeitar a cota grátis da OpenF1 "
        "(3 requisições/s e 30/min). Desde a rodada 3 o cliente espaça as requisições (uma a cada 400 ms, no máximo "
        "29 em 61 s) em vez de receber 429; respostas 429 que ainda ocorram são repetidas após 2, 4, 8, 16 e 16 s. Sessão: a corrida mais recente no dia da medição "
        "(GP de Singapura 2026), a mesma que as telas escolhem por padrão. Valores em segundos: mediana "
        "(média ± desvio padrão), apenas medições bem-sucedidas; n = sucessos/tentativas.",
        "",
        "¹ Tempo a partir da ação do usuário (selecionar piloto / os dois pilotos). "
        "² Tempo a partir da abertura da tela (as métricas continuam carregando depois dos stints).",
        "",
        "## Ambientes",
        "",
        "| Pasta | Ambiente | Build |",
        "|---|---|---|",
    ]
    for name, events, device, _ in platforms:
        out.append(f"| `{name}` | {device_label(name, device)} | {device.get('build', '')} |")
    out.append("")

    summary_rows = []
    medians = {}  # (platform, screen, step, cache, polling) -> median ms
    for name, events, device, resources in platforms:
        loads = [e for e in events if e.get("type") == "load"]
        failures = [e for e in events if e.get("type") == "failure"]
        has_warm = any(e["cache"] == "warm" for e in loads)
        # Scenarios measured on this platform (a round may skip polling: "on:0")
        scenarios = [s for s in SCENARIOS if (has_warm or s[0] == "cold") and any(e["polling"] == s[1] for e in loads)]
        out += [f"## {device_label(name, device)}", ""]
        if not has_warm:
            out += ["Sem banco local nesta plataforma: todas as medições são *sem cache*.", ""]
        if not any(e["polling"] for e in loads):
            out += ["Rodada sem polling: no plano grátis da OpenF1 (30 requisições por minuto) o polling ao vivo "
                    "sozinho (~46/min) esgota a cota.", ""]

        out.append("| Tela | " + " | ".join(SCENARIO_NAMES[s] for s in scenarios) + " |")
        out.append("|---|" + "---|" * len(scenarios))
        for screen, step, label in STEPS:
            cells = []
            for cache, polling in scenarios:
                group = [e for e in loads if e["screen"] == screen and e["step"] == step
                         and e["cache"] == cache and e["polling"] == polling]
                ok = [e["ms"] for e in group if e["status"] == "ok"]
                st = stats(ok)
                if group:
                    summary_rows.append({
                        "platform": name, "screen": screen, "step": step, "cache": cache, "polling": polling,
                        "attempts": len(group), "ok": len(ok),
                        "median_ms": round(st["median"], 1) if st else "",
                        "mean_ms": round(st["mean"], 1) if st else "",
                        "sd_ms": round(st["sd"], 1) if st else "",
                        "requests": sum(e["requests"] for e in group),
                        "rate_limited": sum(e["rateLimited"] for e in group),
                    })
                if st:
                    medians[(name, screen, step, cache, polling)] = st["median"]
                    cells.append(f"{fmt_s(st['median'])} ({fmt_s(st['mean'])} ± {fmt_s(st['sd'])}) n={len(ok)}/{len(group)}")
                else:
                    cells.append(f"— n=0/{len(group)}" if group else "—")
            out.append(f"| {label} | " + " | ".join(cells) + " |")
        out.append("")

        # Cache gain and polling cost, on the medians of each screen
        def ratio(cache_a, poll_a, cache_b, poll_b):
            pairs = [(medians.get((name, s, st, cache_a, poll_a)), medians.get((name, s, st, cache_b, poll_b)))
                     for s, st, _ in STEPS]
            pairs = [(a, b) for a, b in pairs if a and b]
            return statistics.median(b / a for a, b in pairs) if pairs else None

        notes = []
        if has_warm:
            r = ratio("cold", False, "warm", False)
            if r:
                notes.append(f"com cache, a mediana das telas cai para **{fmt(r * 100, 0)}%** do tempo sem cache")
        r = ratio("cold", False, "cold", True)
        if r:
            notes.append(f"com polling ativo, o tempo sem cache fica em **{fmt(r * 100, 0)}%** do tempo sem polling")
        if notes:
            out += ["Mediana das razões por tela: " + "; ".join(notes) + ".", ""]

        # Requests and rate limiting per scenario
        out += ["| Cenário | Requisições HTTP | Respostas 429 | Falhas/tempo esgotado |", "|---|---|---|---|"]
        for cache, polling in scenarios:
            group = [e for e in loads if e["cache"] == cache and e["polling"] == polling]
            bad = sum(1 for e in group if e["status"] != "ok")
            out.append(f"| {SCENARIO_NAMES[(cache, polling)]} | {sum(e['requests'] for e in group)} | "
                       f"{sum(e['rateLimited'] for e in group)} | {bad} de {len(group)} |")
        if failures:
            out.append(f"\nExceções durante as medições: {len(failures)} (ver `loads.jsonl`).")
        out += ["", "Requisições contadas durante as medições (as do polling entre medições não entram).", ""]

        # Resources per phase
        if resources:
            phase_t = {e["phase"]: e["t"] for e in events if e.get("type") == "phase"}
            # Each phase ends where the next recorded one starts (a round without polling has no polling phases)
            starts = sorted(phase_t.values())
            # Only Android has a second memory figure (PSS besides RSS)
            android = device.get("platform") == "android"
            mem_label = "PSS (MB)" if android else "RSS (MB)"
            rss_header = " RSS (MB) média |" if android else ""
            out += [f"| Fase | {mem_label} média | {mem_label} pico |{rss_header} CPU média (%) | CPU p95 (%) |",
                    "|---|---|---|" + ("---|" if android else "") + "---|---|"]
            for phase, label in PHASES:
                start = phase_t.get(phase)
                end = next((t for t in starts if start is not None and t > start), None)
                if start is None or end is None:
                    continue
                window = [r for r in resources if start <= r["t_ms"] < end]
                if not window:
                    continue
                mem = [r["memory_mb"] for r in window]
                rss = [r.get("rss_mb", r["memory_mb"]) for r in window]
                cpu = sorted(r["cpu_pct"] for r in window)
                p95 = cpu[min(len(cpu) - 1, int(round(0.95 * (len(cpu) - 1))))]
                rss_cell = f" {fmt(statistics.fmean(rss))} |" if android else ""
                out.append(f"| {label} | {fmt(statistics.fmean(mem))} | {fmt(max(mem))} |{rss_cell} "
                           f"{fmt(statistics.fmean(cpu))} | {fmt(p95)} |")
            out += ["", "CPU: 100% = um núcleo. Amostras a cada 2 s do processo do app "
                        "(Android: RSS e CPU pelo `top`, PSS pelo `dumpsys meminfo` a cada 30 s porque ele força um GC no app; "
                        "iOS simulador e Web: `ps` no processo do app / renderer do Chrome).", ""]

    # Cross-platform comparison without cache and without polling
    if len(platforms) > 1:
        out += ["## Comparação entre plataformas (sem cache, sem polling — mediana em s)", "",
                "| Tela | " + " | ".join(f"`{n}`" for n, *_ in platforms) + " |",
                "|---|" + "---|" * len(platforms)]
        for screen, step, label in STEPS:
            cells = [fmt_s(medians[(n, screen, step, "cold", False)]) if (n, screen, step, "cold", False) in medians else "—"
                     for n, *_ in platforms]
            out.append(f"| {label} | " + " | ".join(cells) + " |")
        out.append("")

    (base / "README.md").write_text("\n".join(out))
    with (base / "summary.csv").open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=list(summary_rows[0].keys()) if summary_rows else ["platform"])
        writer.writeheader()
        writer.writerows(summary_rows)
    print(f"wrote {base / 'README.md'} and summary.csv ({len(platforms)} platforms)")


if __name__ == "__main__":
    main()
