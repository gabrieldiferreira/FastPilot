#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3 quando rodado como programa.
# Resumo: servidor local da versão Web; responde os endereços das telas (ex.: /telemetria) com o index.html.
"""Serves the Web build locally, answering the app's screen paths in every language (/telemetria, /telemetry...) with
index.html like the rewrite in vercel.json, so links and reloads on a screen work.

    python3 scripts/serve_web.py [port]      # default 8080, after :composeApp:wasmJsBrowserDistribution
"""
# Importa o módulo da biblioteca padrão que implementa um servidor HTTP simples.
import http.server
# Importa o módulo de expressões regulares, usado para reconhecer os endereços das telas.
import re
# Importa o módulo sys, usado para ler os argumentos da linha de comando (a porta).
import sys
# Importa a classe Path, que monta caminhos de pastas e arquivos de forma portátil.
from pathlib import Path

# Pasta onde o Gradle grava a versão Web compilada (resultado de :composeApp:wasmJsBrowserDistribution).
DIST = Path(__file__).resolve().parent.parent / "composeApp/build/dist/wasmJs/productionExecutable"
# Every screen path of every app language (Routes in composeApp/.../ui/i18n), as in vercel.json
# Expressão regular com o endereço de cada tela nos três idiomas (pt, en, es), com ou sem "/" no fim.
ROUTES = re.compile(r"^/(entrar|login|acceder|painel|dashboard|panel|telemetria|telemetry|estrategia|strategy|duelo|duel|radio|clima|weather|classificacao|standings|clasificacion)/?$")


# Classe que trata cada requisição; herda o comportamento de servir arquivos de uma pasta.
class Handler(http.server.SimpleHTTPRequestHandler):
    # Copia o mapa de extensões padrão e acrescenta o tipo correto para arquivos .wasm (WebAssembly).
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map, ".wasm": "application/wasm"}

    # Construtor: chamado a cada requisição recebida.
    def __init__(self, *args, **kwargs):
        # Chama o construtor da classe-mãe dizendo que os arquivos devem ser servidos da pasta DIST.
        super().__init__(*args, directory=str(DIST), **kwargs)

    # Método chamado para cada requisição GET (abrir uma página ou arquivo).
    def do_GET(self):
        # Se o caminho pedido (sem a parte depois de "?") é o endereço de uma tela do app...
        if ROUTES.match(self.path.split("?")[0]):
            # ...troca o caminho por /index.html, para o app carregar e abrir a tela certa.
            self.path = "/index.html"
        # Deixa a classe-mãe enviar o arquivo pedido (ou o index.html).
        super().do_GET()


# Este bloco só roda quando o arquivo é executado diretamente (e não importado).
if __name__ == "__main__":
    # Usa a porta passada como primeiro argumento; se não houver, usa 8080.
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    # Mostra no terminal o endereço para abrir no navegador.
    print(f"http://localhost:{port}")
    # Cria um servidor que atende várias requisições ao mesmo tempo, em todas as interfaces, e roda para sempre.
    http.server.ThreadingHTTPServer(("", port), Handler).serve_forever()
