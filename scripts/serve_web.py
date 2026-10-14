#!/usr/bin/env python3
"""Serves the Web build locally, answering the app's screen paths in every language (/telemetria, /telemetry...) with
index.html like the rewrite in vercel.json, so links and reloads on a screen work.

    python3 scripts/serve_web.py [port]      # default 8080, after :composeApp:wasmJsBrowserDistribution
"""
import http.server
import re
import sys
from pathlib import Path

DIST = Path(__file__).resolve().parent.parent / "composeApp/build/dist/wasmJs/productionExecutable"
# Every screen path of every app language (Routes in composeApp/.../ui/i18n), as in vercel.json
ROUTES = re.compile(r"^/(entrar|login|acceder|painel|dashboard|panel|telemetria|telemetry|estrategia|strategy|duelo|duel|radio|clima|weather|classificacao|standings|clasificacion)/?$")


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map, ".wasm": "application/wasm"}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(DIST), **kwargs)

    def do_GET(self):
        if ROUTES.match(self.path.split("?")[0]):
            self.path = "/index.html"
        super().do_GET()


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"http://localhost:{port}")
    http.server.ThreadingHTTPServer(("", port), Handler).serve_forever()
