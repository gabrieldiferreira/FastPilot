#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: conta as linhas de código com o cloc e calcula quanto do código é compartilhado entre as plataformas (os 96,1% do TCC); grava metrics/shared_code.md.
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
# Importa o módulo que lê CSV (o cloc entrega o resultado nesse formato).
import csv
# Importa o módulo io, para ler um texto como se fosse um arquivo.
import io
# Importa o módulo que roda programas externos (o cloc).
import subprocess
# Importa a classe de data, para registrar quando a medição foi feita.
from datetime import date
# Importa a classe Path, para montar caminhos.
from pathlib import Path

# Pasta raiz do repositório.
ROOT = Path(__file__).resolve().parent.parent

# Languages counted as hand-written application source code
# Linguagens que contam como código do app escrito à mão.
SOURCE_LANGUAGES = {"Kotlin", "SQL", "Swift", "HTML"}

# Pastas do código comum (compartilhado entre todas as plataformas).
SHARED = ["shared/src/commonMain", "composeApp/src/commonMain"]

# Pastas do código específico de cada plataforma.
PLATFORMS = {
    # Android: as partes androidMain dos dois módulos.
    "Android": ["shared/src/androidMain", "composeApp/src/androidMain"],
    # iOS: as partes iosMain e o projeto Xcode que hospeda a tela (iosApp).
    "iOS": ["shared/src/iosMain", "composeApp/src/iosMain", "iosApp/iosApp"],
    # Web: as partes wasmJsMain.
    "Web (Wasm)": ["shared/src/wasmJsMain", "composeApp/src/wasmJsMain"],
    # Desktop: alvo extra, usado no desenvolvimento e na avaliação.
    "Desktop (JVM)": ["shared/src/desktopMain", "composeApp/src/desktopMain"],
# Fim do dicionário.
}

# Pastas de testes (contadas à parte, fora da porcentagem).
TESTS = ["shared/src/commonTest", "shared/src/desktopTest", "composeApp/src/commonTest"]

# The platforms promised in TCC I are Android, iOS and Web; Desktop is an extra target
# Plataformas prometidas no TCC I (o desktop fica de fora do número principal).
PROMISED = ["Android", "iOS", "Web (Wasm)"]


# Roda o cloc nas pastas indicadas e devolve {linguagem: linhas de código}.
def cloc(paths):
    """Returns {language: code_lines} for the given paths, source languages only."""
    # Executa o cloc e guarda a saída de texto
    out = subprocess.run(
        # (sem mensagens extras, em CSV, tratando arquivos .sq e .sqm do SQLDelight como SQL, nas pastas pedidas),
        ["cloc", "--quiet", "--csv", "--force-lang=SQL,sq", "--force-lang=SQL,sqm", *paths],
        # a partir da raiz do repositório, capturando a saída como texto e falhando se o cloc der erro.
        cwd=ROOT, capture_output=True, text=True, check=True,
    # Pega só a saída padrão.
    ).stdout
    # Contagem por linguagem.
    counts = {}
    # Lê cada linha do CSV do cloc como um dicionário coluna -> valor.
    for row in csv.DictReader(io.StringIO(out.strip())):
        # Linguagem da linha.
        lang = row.get("language")
        # Se é uma das linguagens de código do app...
        if lang in SOURCE_LANGUAGES:
            # ...soma as linhas de código (sem brancos e comentários) dessa linguagem.
            counts[lang] = counts.get(lang, 0) + int(row["code"])
    # Devolve a contagem.
    return counts


# Total de linhas somando todas as linguagens.
def total(counts):
    # Soma os valores do dicionário.
    return sum(counts.values())


# Monta um texto do tipo "Kotlin 91, Swift 24", da linguagem com mais linhas para a com menos.
def fmt_langs(counts):
    # Junta "linguagem quantidade" em ordem decrescente; "-" se não houver nada.
    return ", ".join(f"{lang} {n}" for lang, n in sorted(counts.items(), key=lambda kv: -kv[1])) or "-"


# Porcentagem com uma casa decimal.
def pct(part, whole):
    # 100 × parte ÷ todo, formatado como "96.1%".
    return f"{100 * part / whole:.1f}%"


# Função principal do script.
def main():
    # Versão do cloc instalada (registrada no relatório).
    cloc_version = subprocess.run(["cloc", "--version"], capture_output=True, text=True).stdout.strip()

    # Linhas do código comum.
    shared = cloc(SHARED)
    # Linhas do código comum, separadas por módulo.
    shared_by_module = {path: cloc([path]) for path in SHARED}
    # Linhas específicas de cada plataforma.
    platform = {name: cloc(paths) for name, paths in PLATFORMS.items()}
    # Linhas de testes.
    tests = cloc(TESTS)

    # Total do código comum (7.992 na medição do TCC).
    shared_total = total(shared)
    # Total específico de todas as plataformas, incluindo o desktop.
    platform_total = sum(total(c) for c in platform.values())
    # Total específico só de Android, iOS e Web (328 na medição do TCC).
    promised_platform_total = sum(total(platform[p]) for p in PROMISED)

    # Linhas de Kotlin no código comum.
    kotlin_shared = shared.get("Kotlin", 0)
    # Linhas de Kotlin no código específico de todas as plataformas.
    kotlin_platform = sum(c.get("Kotlin", 0) for c in platform.values())

    # Linhas do relatório em Markdown.
    lines = [
        # Título.
        "# Código compartilhado — FastPilot",
        # Linha em branco.
        "",
        # Data da medição e versão do cloc.
        f"Gerado por `python3 scripts/measure_shared_code.py` em {date.today().isoformat()} com cloc {cloc_version}.",
        # Unidade usada.
        "Unidade: linhas de código (sem linhas em branco e comentários). Metodologia no cabeçalho do script.",
        # Linha em branco.
        "",
        # Subtítulo.
        "## Resultado",
        # Linha em branco.
        "",
        # Cabeçalho da tabela.
        "| Medida | Compartilhado | Específico | % compartilhado |",
        # Alinhamento.
        "|---|---:|---:|---:|",
        # Número principal: comum ÷ (comum + específico de Android, iOS e Web)
        f"| Android + iOS + Web (plataformas do TCC) | {shared_total} | {promised_platform_total} | "
        # (o 96,1%, em negrito).
        f"**{pct(shared_total, shared_total + promised_platform_total)}** |",
        # Mesma conta incluindo o desktop
        f"| Todos os alvos (inclui Desktop) | {shared_total} | {platform_total} | "
        # (porcentagem).
        f"{pct(shared_total, shared_total + platform_total)} |",
        # Mesma conta contando só Kotlin
        f"| Só Kotlin, todos os alvos | {kotlin_shared} | {kotlin_platform} | "
        # (porcentagem).
        f"{pct(kotlin_shared, kotlin_shared + kotlin_platform)} |",
        # Linha em branco.
        "",
        # Subtítulo da tabela por plataforma.
        "## Por plataforma",
        # Linha em branco.
        "",
        # Explicação da conta por plataforma.
        "Quanto do código de cada app vem do código comum: compartilhado ÷ (compartilhado + específico daquele alvo).",
        # Linha em branco.
        "",
        # Cabeçalho.
        "| Plataforma | Código específico | Linguagens | % do app que é compartilhado |",
        # Alinhamento.
        "|---|---:|---|---:|",
    # Fim da lista inicial.
    ]
    # Para cada plataforma...
    for name, counts in platform.items():
        # ...acrescenta: linhas específicas, linguagens e comum ÷ (comum + específico dela).
        lines.append(f"| {name} | {total(counts)} | {fmt_langs(counts)} | {pct(shared_total, shared_total + total(counts))} |")

    # Acrescenta a tabela da composição do código comum.
    lines += [
        # Linha em branco.
        "",
        # Subtítulo.
        "## Composição do código compartilhado",
        # Linha em branco.
        "",
        # Cabeçalho.
        "| Módulo | Linhas | Linguagens |",
        # Alinhamento.
        "|---|---:|---|",
    # Fim da lista.
    ]
    # Para cada pasta do código comum...
    for path, counts in shared_by_module.items():
        # ...acrescenta o nome da pasta, as linhas e as linguagens.
        lines.append(f"| `{path}` | {total(counts)} | {fmt_langs(counts)} |")

    # Acrescenta a seção do que ficou fora da conta.
    lines += [
        # Linha em branco.
        "",
        # Subtítulo.
        "## Fora da conta",
        # Linha em branco.
        "",
        # Pastas de testes e quantas linhas têm.
        f"- Testes (`{'`, `'.join(TESTS)}`): {total(tests)} linhas ({fmt_langs(tests)}).",
        # Outros itens excluídos (primeira linha).
        "- Código gerado (SQLDelight em `build/`), recursos Android, Info.plist, catálogos de assets,",
        # Outros itens excluídos (continuação).
        "  scripts Gradle e o `.xcodeproj` gerado pelo XcodeGen.",
        # Linha em branco.
        "",
    # Fim da lista.
    ]

    # Junta as linhas em um único texto.
    report = "\n".join(lines)
    # Caminho do relatório.
    out_path = ROOT / "metrics" / "shared_code.md"
    # Cria a pasta metrics/, se não existir.
    out_path.parent.mkdir(exist_ok=True)
    # Grava o relatório.
    out_path.write_text(report, encoding="utf-8")
    # Mostra o relatório no terminal.
    print(report)


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
