#!/usr/bin/env python3
"""
Measures how much of FastPilot's source code is shared across platforms (TCC II, Fase 3).

Methodology
-----------
- Unit: "code" lines reported by cloc (blank lines and comments excluded).
- Shared: the commonMain source sets of :shared and :composeApp (Kotlin + SQLDelight .sq/.sqm).
- Platform-specific: every other *Main source set, plus the iOS SwiftUI wrapper (iosApp/iosApp/*.swift)
  and the Web host page (composeApp/src/wasmJsMain/resources/index.html).
- Excluded (reported separately, not part of the ratio): tests, build output and generated code,
  Android resources/manifests, Info.plist/asset catalogs, Gradle scripts, the XcodeGen-generated .xcodeproj.

Per-target share = shared / (shared + code that only that target compiles).
Overall share    = shared / (shared + all platform-specific code).

Usage: python3 scripts/measure_shared_code.py   (requires `brew install cloc`)
Writes metrics/shared_code.md and prints it.
"""
import csv
import io
import subprocess
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Languages counted as hand-written application source code
SOURCE_LANGUAGES = {"Kotlin", "SQL", "Swift", "HTML"}

SHARED = ["shared/src/commonMain", "composeApp/src/commonMain"]

PLATFORMS = {
    "Android": ["shared/src/androidMain", "composeApp/src/androidMain"],
    "iOS": ["shared/src/iosMain", "composeApp/src/iosMain", "iosApp/iosApp"],
    "Web (Wasm)": ["shared/src/wasmJsMain", "composeApp/src/wasmJsMain"],
    "Desktop (JVM)": ["shared/src/desktopMain", "composeApp/src/desktopMain"],
}

TESTS = ["shared/src/commonTest", "shared/src/desktopTest", "composeApp/src/commonTest"]

# The platforms promised in TCC I are Android, iOS and Web; Desktop is an extra target
PROMISED = ["Android", "iOS", "Web (Wasm)"]


def cloc(paths):
    """Returns {language: code_lines} for the given paths, source languages only."""
    out = subprocess.run(
        ["cloc", "--quiet", "--csv", "--force-lang=SQL,sq", "--force-lang=SQL,sqm", *paths],
        cwd=ROOT, capture_output=True, text=True, check=True,
    ).stdout
    counts = {}
    for row in csv.DictReader(io.StringIO(out.strip())):
        lang = row.get("language")
        if lang in SOURCE_LANGUAGES:
            counts[lang] = counts.get(lang, 0) + int(row["code"])
    return counts


def total(counts):
    return sum(counts.values())


def fmt_langs(counts):
    return ", ".join(f"{lang} {n}" for lang, n in sorted(counts.items(), key=lambda kv: -kv[1])) or "-"


def pct(part, whole):
    return f"{100 * part / whole:.1f}%"


def main():
    cloc_version = subprocess.run(["cloc", "--version"], capture_output=True, text=True).stdout.strip()

    shared = cloc(SHARED)
    shared_by_module = {path: cloc([path]) for path in SHARED}
    platform = {name: cloc(paths) for name, paths in PLATFORMS.items()}
    tests = cloc(TESTS)

    shared_total = total(shared)
    platform_total = sum(total(c) for c in platform.values())
    promised_platform_total = sum(total(platform[p]) for p in PROMISED)

    kotlin_shared = shared.get("Kotlin", 0)
    kotlin_platform = sum(c.get("Kotlin", 0) for c in platform.values())

    lines = [
        "# Código compartilhado — FastPilot",
        "",
        f"Gerado por `python3 scripts/measure_shared_code.py` em {date.today().isoformat()} com cloc {cloc_version}.",
        "Unidade: linhas de código (sem linhas em branco e comentários). Metodologia no cabeçalho do script.",
        "",
        "## Resultado",
        "",
        "| Medida | Compartilhado | Específico | % compartilhado |",
        "|---|---:|---:|---:|",
        f"| Android + iOS + Web (plataformas do TCC) | {shared_total} | {promised_platform_total} | "
        f"**{pct(shared_total, shared_total + promised_platform_total)}** |",
        f"| Todos os alvos (inclui Desktop) | {shared_total} | {platform_total} | "
        f"{pct(shared_total, shared_total + platform_total)} |",
        f"| Só Kotlin, todos os alvos | {kotlin_shared} | {kotlin_platform} | "
        f"{pct(kotlin_shared, kotlin_shared + kotlin_platform)} |",
        "",
        "## Por plataforma",
        "",
        "Quanto do código de cada app vem do código comum: compartilhado ÷ (compartilhado + específico daquele alvo).",
        "",
        "| Plataforma | Código específico | Linguagens | % do app que é compartilhado |",
        "|---|---:|---|---:|",
    ]
    for name, counts in platform.items():
        lines.append(f"| {name} | {total(counts)} | {fmt_langs(counts)} | {pct(shared_total, shared_total + total(counts))} |")

    lines += [
        "",
        "## Composição do código compartilhado",
        "",
        "| Módulo | Linhas | Linguagens |",
        "|---|---:|---|",
    ]
    for path, counts in shared_by_module.items():
        lines.append(f"| `{path}` | {total(counts)} | {fmt_langs(counts)} |")

    lines += [
        "",
        "## Fora da conta",
        "",
        f"- Testes (`{'`, `'.join(TESTS)}`): {total(tests)} linhas ({fmt_langs(tests)}).",
        "- Código gerado (SQLDelight em `build/`), recursos Android, Info.plist, catálogos de assets,",
        "  scripts Gradle e o `.xcodeproj` gerado pelo XcodeGen.",
        "",
    ]

    report = "\n".join(lines)
    out_path = ROOT / "metrics" / "shared_code.md"
    out_path.parent.mkdir(exist_ok=True)
    out_path.write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
