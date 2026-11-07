#!/usr/bin/env python3
# Linha acima (shebang): faz o sistema executar este arquivo com o Python 3.
# Resumo: roda o benchmark de carregamento embutido no app em uma plataforma (Android, iOS ou Web) e mede memória e CPU enquanto ele roda; grava tudo em metrics/performance/.
"""
Runs the in-app load benchmark (composeApp/.../ui/perf/LoadBenchmark.kt) on one platform
and samples the app's memory and CPU while it runs (TCC I §4.8, Fase 5 of TCC II).

    scripts/perf_run.py android [--serial SERIAL] [--spec off:5,on:3] [--label NAME]
    scripts/perf_run.py ios     [--device UDID]   [--spec ...]
    scripts/perf_run.py web     [--spec ...]

Builds are not triggered here; build first:
    android: ./gradlew :composeApp:assembleBenchmark        (release code, debug key)
    ios:     xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release \
                 -sdk iphonesimulator -derivedDataPath build/ios-perf build
    web:     ./gradlew :composeApp:wasmJsBrowserDistribution

Output in metrics/performance/<label>/ (label defaults to the platform):
    loads.jsonl     every FASTPILOT_PERF line printed by the app
    resources.csv   t_ms, memory_mb, cpu_pct, rss_mb sampled every --interval seconds
                    (memory_mb is PSS on Android, read every 30 s because dumpsys meminfo
                    forces a GC in the app, and RSS elsewhere; rss_mb is RSS on every
                    platform, for comparisons; CPU from top / ps of the app or renderer
                    process, 100% = one core)
    js_heap.csv     Web only: JS heap and renderer main-thread busy % from Chrome DevTools
    device.json     device / OS / build details
Only one platform should run at a time: all of them share the OpenF1 request budget of this machine.
"""

# Importa o leitor de opções da linha de comando.
import argparse
# Importa o módulo que escreve CSV.
import csv
# Importa o módulo que lê e escreve JSON.
import json
# Importa o módulo os, usado para saber o número do processo deste script.
import os
# Importa o módulo de expressões regulares, usado para ler a memória no texto do dumpsys.
import re
# Importa o módulo shutil, usado para achar programas no PATH e apagar pastas.
import shutil
# Importa o módulo signal, usado para tratar o Ctrl+C.
import signal
# Importa o módulo que roda programas externos (adb, xcrun, Chrome, node...).
import subprocess
# Importa o módulo sys (argumentos, saída de erro, código de saída).
import sys
# Importa o módulo que cria pastas temporárias (perfil do Chrome).
import tempfile
# Importa o módulo de threads, para ler a saída e amostrar recursos ao mesmo tempo.
import threading
# Importa o módulo time (relógio e esperas).
import time
# Importa a classe Path, para montar caminhos.
from pathlib import Path

# Pasta raiz do repositório.
ROOT = Path(__file__).resolve().parent.parent
# Identificador do app no Android e no iOS.
PACKAGE = "com.fastpilot.app"
# Prefixo que o benchmark do app escreve antes de cada linha de resultado.
MARKER = "FASTPILOT_PERF "
# Caminho do adb (ferramenta do Android): o do PATH, ou o do SDK padrão no Mac.
ADB = shutil.which("adb") or str(Path.home() / "Library/Android/sdk/platform-tools/adb")
# Caminho do Node.js: o do PATH, ou o que o Gradle baixou.
NODE = shutil.which("node") or str(Path.home() / ".gradle/nodejs/node-v22.0.0-darwin-arm64/bin/node")
# Caminho do Google Chrome no Mac.
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"


# Hora atual em milissegundos.
def now_ms() -> int:
    # Segundos desde 1970 × 1000, como número inteiro.
    return int(time.time() * 1000)


# Roda um comando e devolve a saída de texto, sem espaços nas pontas.
def sh(*cmd: str, check: bool = True) -> str:
    # Executa o comando; check=True faz dar erro se ele falhar; errors="replace" evita quebra com caracteres inválidos.
    return subprocess.run(cmd, check=check, capture_output=True, text=True, errors="replace").stdout.strip()


# Classe que recebe as linhas do benchmark, grava no loads.jsonl e avisa quando o benchmark termina.
class Recorder:
    """Collects benchmark lines from a text stream and stops when the 'done' phase arrives."""

    # Construtor.
    def __init__(self, out: Path):
        # Abre o arquivo loads.jsonl para escrita.
        self.file = (out / "loads.jsonl").open("w")
        # Sinal que fica "ligado" quando o benchmark termina.
        self.done = threading.Event()
        # Contador de linhas gravadas.
        self.lines = 0

    # Trata uma linha de texto vinda do app.
    def feed(self, text: str):
        # Procura o prefixo FASTPILOT_PERF na linha.
        idx = text.find(MARKER)
        # Se não achou...
        if idx < 0:
            # ...a linha não é do benchmark; ignora.
            return
        # Pega o que vem depois do prefixo (o JSON do evento).
        payload = text[idx + len(MARKER):].strip()
        # Tenta converter o JSON...
        try:
            # ...em um dicionário.
            event = json.loads(payload)
        # Se o JSON estiver incompleto ou inválido...
        except json.JSONDecodeError:
            # ...ignora a linha.
            return
        # Grava o evento no arquivo, uma linha por evento.
        self.file.write(payload + "\n")
        # Força a gravação no disco imediatamente.
        self.file.flush()
        # Conta mais uma linha.
        self.lines += 1
        # Se o evento é o resultado de um carregamento de tela...
        if event.get("type") == "load":
            # ...mostra no terminal: tela, passo, com/sem cache, se havia busca ao vivo, número da rodada,
            print(f"  {event['screen']:<9} {event['step']:<7} {event['cache']:<4} "
                  f"{'poll' if event['polling'] else '    '} run {event['run']}: "
                  # tempo em segundos e situação,
                  f"{event['ms'] / 1000:6.2f} s {event['status']}"
                  # e, se houve respostas 429, quantas.
                  f"{' (429 x' + str(event['rateLimited']) + ')' if event['rateLimited'] else ''}", flush=True)
        # Senão, se é mudança de fase, início ou falha...
        elif event.get("type") in ("phase", "start", "failure"):
            # ...mostra o nome da fase (e, em caso de falha, o evento completo).
            print(f"  [{event.get('phase', event['type'])}]", payload if event["type"] == "failure" else "", flush=True)
        # Se é a fase "done" (fim do benchmark)...
        if event.get("type") == "phase" and event.get("phase") == "done":
            # ...liga o sinal de terminado.
            self.done.set()

    # Lê um fluxo de texto linha a linha até o benchmark terminar.
    def follow(self, stream):
        # Para cada linha lida (para quando a leitura devolve texto vazio, o fim do fluxo)...
        for line in iter(stream.readline, ""):
            # ...trata a linha...
            self.feed(line)
            # ...e, se o benchmark terminou...
            if self.done.is_set():
                # ...para de ler.
                break


# Laço que mede memória e CPU de tempos em tempos e grava no resources.csv.
def sample_loop(out: Path, sampler, interval: float, stop: threading.Event):
    # Abre o resources.csv para escrita.
    with (out / "resources.csv").open("w", newline="") as f:
        # Cria o escritor de CSV.
        writer = csv.writer(f)
        # Escreve o cabeçalho: hora, memória, CPU e memória residente.
        writer.writerow(["t_ms", "memory_mb", "cpu_pct", "rss_mb"])
        # Hora da amostra anterior.
        last = None
        # Repete até pedirem para parar.
        while not stop.is_set():
            # Hora de início desta amostra.
            started = time.time()
            # A long gap means the machine was suspended: the timings around it are not valid
            # Se passou muito tempo desde a última amostra, o computador provavelmente dormiu...
            if last and started - last > 3 * interval + 5:
                # ...e avisa, porque os tempos medidos nesse trecho não valem.
                print(f"  WARNING: no sample for {started - last:.0f} s (machine asleep?)", flush=True)
            # Guarda a hora desta amostra.
            last = started
            # Tenta medir...
            try:
                # ...chamando a função de medição da plataforma.
                sample = sampler()
            # Se a medição falhar (o app pode estar reiniciando)...
            except Exception as e:  # the app may be restarting; keep going
                # ...mostra o erro...
                print("  sample failed:", e, file=sys.stderr)
                # ...e segue sem amostra.
                sample = None
            # Se a medição deu certo...
            if sample:
                # ...separa memória, CPU e memória residente...
                memory, cpu, rss = sample
                # ...grava a linha com uma casa decimal...
                writer.writerow([now_ms(), f"{memory:.1f}", f"{cpu:.1f}", f"{rss:.1f}"])
                # ...e força a gravação no disco.
                f.flush()
            # Espera o restante do intervalo (ou sai antes, se pedirem para parar).
            stop.wait(max(0.0, interval - (time.time() - started)))


# ── Android ────────────────────────────────────────────────────────────────────

# Prepara e inicia o benchmark no Android (aparelho ou emulador).
def run_android(args, out: Path, recorder: Recorder):
    # Comando adb, com o número de série do aparelho se foi informado.
    adb = [ADB] + (["-s", args.serial] if args.serial else [])
    # Caminho do APK de benchmark gerado pelo Gradle.
    apk = ROOT / "composeApp/build/outputs/apk/benchmark/composeApp-benchmark.apk"
    # Instala (ou reinstala) o APK.
    sh(*adb, "install", "-r", str(apk))
    # Apaga os dados do app (banco local e estado), para começar sem cache.
    sh(*adb, "shell", "pm", "clear", PACKAGE)  # empty local cache and app state
    # Limpa o log do Android.
    sh(*adb, "logcat", "-c")
    # Função curta que lê uma propriedade do sistema Android.
    prop = lambda p: sh(*adb, "shell", "getprop", p)
    # Grava o device.json com os dados do aparelho:
    (out / "device.json").write_text(json.dumps({
        # plataforma,
        "platform": "android",
        # modelo,
        "model": prop("ro.product.model"),
        # fabricante,
        "manufacturer": prop("ro.product.manufacturer"),
        # versão do Android,
        "android": prop("ro.build.version.release"),
        # nível da API (SDK),
        "sdk": prop("ro.build.version.sdk"),
        # se é emulador,
        "emulator": prop("ro.kernel.qemu") == "1" or "emulator" in (args.serial or ""),
        # número de núcleos de CPU,
        "cpu_cores": sh(*adb, "shell", "nproc"),
        # e o tipo de build usado.
        "build": "benchmark (release, not debuggable)",
    # Fim do dicionário, gravado com recuo de 2 espaços.
    }, indent=2))

    # Começa a ler o log do Android, só as mensagens de System.out (onde o app escreve o FASTPILOT_PERF)
    logcat = subprocess.Popen(adb + ["logcat", "-v", "raw", "-s", "System.out:I"],
                              # com a saída disponível para este script como texto.
                              stdout=subprocess.PIPE, text=True)
    # Abre o app passando a configuração do benchmark (ex.: off:5,on:3) e espera ele iniciar.
    sh(*adb, "shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity", "-e", "perf", args.spec)
    # Número do processo do app.
    pid = sh(*adb, "shell", "pidof", PACKAGE)

    # `dumpsys meminfo` makes the app run an explicit GC, so PSS is read only every 30 s;
    # RSS and CPU come from `top`, which does not touch the process
    # Último valor de PSS lido e quando foi lido (o dumpsys força uma coleta de lixo, por isso só a cada 30 s).
    pss = {"value": 0.0, "at": 0.0}

    # Função que mede memória e CPU do app no Android.
    def sampler():
        # Se a última leitura de PSS tem 30 s ou mais...
        if time.time() - pss["at"] >= 30:
            # ...lê o relatório de memória do processo...
            meminfo = sh(*adb, "shell", "dumpsys", "meminfo", pid)
            # ...extrai o "TOTAL PSS" (em KB), converte para MB e guarda com a hora.
            pss.update(value=int(re.search(r"TOTAL PSS:\s+(\d+)", meminfo).group(1)) / 1024, at=time.time())
        # Lê do top a memória residente e a CPU do processo.
        res, cpu = sh(*adb, "shell", "top", "-b", "-n", "1", "-p", pid, "-o", "RES,%CPU", "-q").split()
        # Fatores para converter K, M e G em MB.
        units = {"K": 1 / 1024, "M": 1, "G": 1024}
        # Converte a memória residente para MB (com unidade no fim, ou em KB se vier só o número).
        rss = float(res[:-1]) * units[res[-1]] if res[-1] in units else int(res) / 1024
        # Devolve PSS, CPU e memória residente.
        return pss["value"], float(cpu), rss

    # Devolve o leitor do log, a função de medição e a função que encerra (para o log e fecha o app).
    return logcat, sampler, lambda: (logcat.terminate(), sh(*adb, "shell", "am", "force-stop", PACKAGE))


# ── iOS (simulator) ────────────────────────────────────────────────────────────

# Prepara e inicia o benchmark no simulador do iOS.
def run_ios(args, out: Path, recorder: Recorder):
    # Caminho do app iOS compilado em Release.
    app = ROOT / "build/ios-perf/Build/Products/Release-iphonesimulator/iosApp.app"
    # Simulador escolhido, ou o que estiver ligado.
    device = args.device or "booted"
    # Desinstala o app (apaga o cache local); não dá erro se não estava instalado.
    sh("xcrun", "simctl", "uninstall", device, PACKAGE, check=False)  # empty local cache
    # Instala o app no simulador.
    sh("xcrun", "simctl", "install", device, str(app))
    # Lista os simuladores ligados, em JSON.
    devices = json.loads(sh("xcrun", "simctl", "list", "devices", "booted", "-j"))["devices"]
    # Pares (versão do iOS, simulador) dos que estão ligados.
    booted = [(runtime, d) for runtime, ds in devices.items() for d in ds if d["state"] == "Booted"]
    # Escolhe o simulador pedido; se não achar, o primeiro ligado.
    runtime, info = next(((r, d) for r, d in booted if d["udid"] == args.device), booted[0])
    # Grava o device.json:
    (out / "device.json").write_text(json.dumps({
        # plataforma,
        "platform": "ios",
        # nome do simulador (ex.: iPhone 17),
        "model": info["name"],
        # versão do iOS (última parte do nome do runtime),
        "ios": runtime.rsplit(".", 1)[-1],
        # que é simulador,
        "simulator": True,
        # o processador do Mac,
        "host": sh("sysctl", "-n", "machdep.cpu.brand_string"),
        # e o tipo de build.
        "build": "Release",
    # Fim do dicionário.
    }, indent=2))

    # Abre o app no simulador com a saída do console ligada, fechando uma execução anterior,
    launch = subprocess.Popen(["xcrun", "simctl", "launch", "--console-pty", "--terminate-running-process",
                               # passando a configuração do benchmark,
                               device, PACKAGE, "-perf", args.spec],
                              # e juntando a saída de erro à saída normal, como texto.
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    # Número do processo do app (ainda desconhecido).
    pid = ""
    # Tenta por até 30 segundos...
    for _ in range(30):
        # ...achar o processo do app pelo caminho do executável...
        pid = sh("pgrep", "-f", "iosApp.app/iosApp", check=False).split("\n")[0]
        # ...e, se achou...
        if pid:
            # ...para de procurar.
            break
        # Espera 1 segundo antes de tentar de novo.
        time.sleep(1)

    # Função que mede memória e CPU do app no simulador.
    def sampler():
        # The simulator runs the app as a host process: resident memory and CPU straight from ps
        # O simulador roda o app como um processo do Mac, então o ps dá a memória residente (KB) e a CPU.
        rss, cpu = sh("ps", "-o", "rss=,%cpu=", "-p", pid).split()
        # Devolve memória em MB, CPU e memória residente em MB.
        return int(rss) / 1024, float(cpu), int(rss) / 1024

    # Devolve o processo do app, a função de medição e a função que encerra o app.
    return launch, sampler, lambda: (launch.terminate(), sh("xcrun", "simctl", "terminate", device, PACKAGE, check=False))


# ── Web (Chrome headless) ──────────────────────────────────────────────────────

# Prepara e inicia o benchmark na versão Web, num Chrome sem janela.
def run_web(args, out: Path, recorder: Recorder):
    # Pasta da versão Web compilada.
    dist = ROOT / "composeApp/build/dist/wasmJs/productionExecutable"
    # Sobe um servidor HTTP simples servindo essa pasta, na porta escolhida,
    server = subprocess.Popen([sys.executable, "-m", "http.server", str(args.port)], cwd=dist,
                              # sem mostrar a saída dele.
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    # Cria uma pasta temporária para o perfil do Chrome (começa sem cache e sem histórico).
    profile = tempfile.mkdtemp(prefix="fastpilot-chrome-")
    # Abre o Chrome sem janela, com o DevTools liberado na porta escolhida,
    chrome = subprocess.Popen([CHROME, "--headless=new", f"--remote-debugging-port={args.cdp_port}",
                               # usando o perfil temporário, com tamanho de tela de celular, numa página em branco,
                               f"--user-data-dir={profile}", "--window-size=430,932", "about:blank"],
                              # sem mostrar a saída dele.
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    # Espera 3 segundos o Chrome subir.
    time.sleep(3)
    # Pergunta ao Chrome a versão dele.
    version = json.loads(sh("curl", "-s", f"http://127.0.0.1:{args.cdp_port}/json/version"))
    # Grava o device.json:
    (out / "device.json").write_text(json.dumps({
        # plataforma,
        "platform": "web",
        # versão do navegador,
        "browser": version.get("Browser"),
        # que roda sem janela,
        "headless": True,
        # o processador do Mac,
        "host": sh("sysctl", "-n", "machdep.cpu.brand_string"),
        # e o tipo de build.
        "build": "wasmJs production",
    # Fim do dicionário.
    }, indent=2))

    # perf_web.mjs opens the page, relays console lines and prints "SAMPLE t heap_mb main_thread_cpu" lines
    # Endereço do app com a configuração do benchmark na URL.
    url = f"http://127.0.0.1:{args.port}/index.html?perf={args.spec}"
    # Roda o perf_web.mjs no Node: ele abre a página, repassa o console e imprime as amostras SAMPLE,
    driver = subprocess.Popen([NODE, "--no-warnings", "--experimental-websocket", str(ROOT / "scripts/perf_web.mjs"),
                               # recebendo a porta do DevTools, o endereço e o intervalo,
                               str(args.cdp_port), url, str(args.interval)],
                              # e com a saída disponível para este script como texto.
                              stdout=subprocess.PIPE, text=True)
    # Abre o js_heap.csv para escrita.
    samples = (out / "js_heap.csv").open("w", newline="")
    # Cria o escritor de CSV.
    writer = csv.writer(samples)
    # Escreve o cabeçalho: hora, memória JS e CPU da thread principal.
    writer.writerow(["t_ms", "js_heap_mb", "main_thread_cpu_pct"])

    # Função que mede memória e CPU da aba do Chrome.
    def sampler():
        # Like the iOS simulator: resident memory and CPU of the tab's renderer process(es),
        # which include the Wasm linear memory (Skia) that the JS heap figure leaves out
        # Lista os processos filhos do Chrome.
        children = sh("pgrep", "-P", str(chrome.pid), check=False).split()
        # Começa a somar memória e CPU do zero.
        rss = cpu = 0.0
        # Para cada processo filho (memória, CPU e linha de comando)...
        for line in sh("ps", "-o", "rss=,%cpu=,command=", "-p", ",".join(children)).splitlines():
            # ...separa os três campos (a linha de comando fica inteira no terceiro)...
            fields = line.split(None, 2)
            # ...e, se é um processo que desenha páginas (renderer) e não de extensão...
            if "--type=renderer" in fields[2] and "--extension-process" not in fields[2]:
                # ...soma a memória residente (em MB)...
                rss += int(fields[0]) / 1024
                # ...e a CPU.
                cpu += float(fields[1])
        # Devolve memória, CPU e memória residente (as duas memórias iguais na Web).
        return rss, cpu, rss

    # Classe que separa as linhas SAMPLE (gravadas no js_heap.csv) das linhas do benchmark.
    class Splitter:
        # Construtor: guarda o fluxo de saída do perf_web.mjs.
        def __init__(self, stream):
            # Guarda o fluxo.
            self.stream = stream

        # Lê a próxima linha que não seja SAMPLE.
        def readline(self):
            # Repete até achar uma linha que não seja amostra.
            while True:
                # Lê uma linha.
                line = self.stream.readline()
                # Se é uma amostra...
                if line.startswith("SAMPLE "):
                    # ...separa hora, memória e CPU (ignorando a palavra SAMPLE)...
                    _, t, mem, cpu = line.split()
                    # ...grava no js_heap.csv...
                    writer.writerow([t, mem, cpu])
                    # ...força a gravação no disco...
                    samples.flush()
                    # ...e lê a próxima linha.
                    continue
                # Devolve a linha (do benchmark, ou vazia no fim do fluxo).
                return line

    # Função que encerra tudo o que foi aberto para a Web.
    def stop():
        # Encerra o perf_web.mjs.
        driver.terminate()
        # Fecha o Chrome.
        chrome.terminate()
        # Para o servidor HTTP.
        server.terminate()
        # Fecha o js_heap.csv.
        samples.close()
        # Apaga o perfil temporário do Chrome, sem dar erro se falhar.
        shutil.rmtree(profile, ignore_errors=True)

    # Devolve o leitor que separa as amostras, a função de medição e a função que encerra.
    return Splitter(driver.stdout), sampler, stop


# Função principal do script.
def main():
    # Cria o leitor de opções, usando o texto do topo do arquivo como ajuda.
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    # Argumento obrigatório: a plataforma.
    parser.add_argument("platform", choices=["android", "ios", "web"])
    # Configuração do benchmark: quantas rodadas sem busca ao vivo (off) e com busca ao vivo (on).
    parser.add_argument("--spec", default="off:5,on:3", help="LoadBenchmark.Config spec")
    # Nome da pasta de saída (padrão: o nome da plataforma).
    parser.add_argument("--label", help="output folder name (default: platform)")
    # Intervalo entre as medições de memória e CPU, em segundos.
    parser.add_argument("--interval", type=float, default=2.0, help="resource sampling period (s)")
    # Número de série do aparelho Android.
    parser.add_argument("--serial", help="adb serial (android)")
    # Identificador do simulador iOS.
    parser.add_argument("--device", help="simulator UDID (ios; default: booted)")
    # Porta do servidor HTTP da versão Web.
    parser.add_argument("--port", type=int, default=8765)
    # Porta do DevTools do Chrome.
    parser.add_argument("--cdp-port", type=int, default=9333)
    # Tempo máximo de espera pelo fim do benchmark, em minutos.
    parser.add_argument("--max-minutes", type=float, default=120)
    # Lê as opções passadas.
    args = parser.parse_args()

    # Idle sleep would freeze the emulator/simulator/browser in the middle of a measurement
    # No Mac...
    if sys.platform == "darwin":
        # ...impede o computador de dormir enquanto este script estiver rodando (dormir congelaria a medição).
        subprocess.Popen(["caffeinate", "-ims", "-w", str(os.getpid())])

    # Pasta de saída: metrics/performance/<label ou plataforma>.
    out = ROOT / "metrics/performance" / (args.label or args.platform)
    # Cria a pasta, se não existir.
    out.mkdir(parents=True, exist_ok=True)
    # Cria o gravador dos eventos do benchmark.
    recorder = Recorder(out)
    # Mostra o que vai rodar e onde vai gravar.
    print(f"{args.platform}: benchmark {args.spec} -> {out.relative_to(ROOT)}", flush=True)

    # Chama a função da plataforma escolhida, que devolve: o fluxo de saída, a função de medição e a de encerramento.
    stream, sampler, stop = {"android": run_android, "ios": run_ios, "web": run_web}[args.platform](args, out, recorder)
    # Sinal para parar a medição de recursos.
    stop_sampling = threading.Event()
    # Se a plataforma tem função de medição...
    if sampler:
        # ...inicia, em segundo plano, o laço que mede memória e CPU.
        threading.Thread(target=sample_loop, args=(out, sampler, args.interval, stop_sampling), daemon=True).start()

    # Cria, em segundo plano, a leitura das linhas do benchmark (do processo ou do separador da Web).
    reader = threading.Thread(target=recorder.follow, args=(stream.stdout if hasattr(stream, "stdout") else stream,), daemon=True)
    # Começa a ler.
    reader.start()
    # Se o usuário apertar Ctrl+C, considera o benchmark terminado (para encerrar com limpeza).
    signal.signal(signal.SIGINT, lambda *_: recorder.done.set())
    # Espera o benchmark terminar, até o tempo máximo; finished diz se terminou a tempo.
    finished = recorder.done.wait(timeout=args.max_minutes * 60)
    # Para a medição de recursos.
    stop_sampling.set()
    # Encerra o app, o navegador e o que mais foi aberto.
    stop()
    # Mostra se terminou ou estourou o tempo, e quantos eventos foram gravados.
    print(f"{'done' if finished else 'TIMED OUT'}: {recorder.lines} events in {out.relative_to(ROOT)}")
    # Sai com código 0 (sucesso) ou 1 (estourou o tempo).
    sys.exit(0 if finished else 1)


# Este bloco só roda quando o arquivo é executado diretamente.
if __name__ == "__main__":
    # Chama a função principal.
    main()
