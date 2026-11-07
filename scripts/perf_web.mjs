// Drives the Web build in Chrome over the DevTools protocol for scripts/perf_run.py:
// opens the benchmark URL, relays the page console to stdout and every <interval> seconds
// prints "SAMPLE <epoch ms> <JS heap MB> <main thread CPU %>".
// node --experimental-websocket perf_web.mjs <cdp port> <url> <interval s>
// Resumo: controla o Chrome pelo protocolo DevTools para medir a versão Web; repassa o console da página e imprime amostras de memória e CPU.
// Lê os três argumentos da linha de comando: porta do DevTools, endereço do benchmark e intervalo entre amostras.
const [port, url, intervalS] = process.argv.slice(2);
// Pede ao Chrome a lista de abas e janelas abertas (endpoint /json do DevTools) e converte a resposta de JSON.
const targets = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
// Escolhe a primeira entrada que é uma página (aba).
const page = targets.find(t => t.type === 'page');
// Abre uma conexão WebSocket com essa aba, pela qual os comandos do DevTools são enviados.
const ws = new WebSocket(page.webSocketDebuggerUrl);
// Espera a conexão abrir antes de continuar.
await new Promise(r => (ws.onopen = r));

// Contador que dá um número único a cada comando enviado.
let id = 0;
// Mapa de comandos que ainda esperam resposta: número do comando -> função que entrega o resultado.
const pending = new Map();
// Indica se o benchmark já terminou (o app avisa com "phase":"done" no console).
let done = false;
// Função chamada a cada mensagem que chega do Chrome.
ws.onmessage = e => {
  // Converte a mensagem de texto JSON em objeto.
  const m = JSON.parse(e.data);
  // Se é a resposta de um comando que enviamos e ainda está pendente...
  if (m.id && pending.has(m.id)) {
    // ...entrega o resultado para quem estava esperando...
    pending.get(m.id)(m.result);
    // ...e tira o comando da lista de pendentes.
    pending.delete(m.id);
  // Senão, se é um aviso de que a página escreveu algo no console...
  } else if (m.method === 'Runtime.consoleAPICalled') {
    // ...junta os argumentos do console.log da página em um único texto...
    const text = m.params.args.map(a => a.value ?? a.description ?? '').join(' ');
    // ...repassa esse texto para a saída deste script (que o perf_run.py lê)...
    console.log(text);
    // ...e, se o texto indica o fim do benchmark, marca como terminado.
    if (text.includes('"phase":"done"')) done = true;
  // Fim do if/else.
  }
// Fim da função de mensagens.
};
// Função auxiliar que envia um comando do DevTools e devolve uma promessa com a resposta.
const send = (method, params = {}) =>
  // Cria a promessa que será resolvida quando a resposta chegar.
  new Promise(r => {
    // Gera o próximo número de comando.
    const i = ++id;
    // Guarda a função de resposta associada a esse número.
    pending.set(i, r);
    // Envia o comando em JSON pela conexão.
    ws.send(JSON.stringify({ id: i, method, params }));
  // Fim da promessa.
  });

// Liga o domínio Runtime, para receber as mensagens do console da página.
await send('Runtime.enable');
// No local database on Web, so every load is "without cache": keep the browser HTTP cache out too
// Liga o domínio Network, necessário para controlar o cache HTTP.
await send('Network.enable');
// Desliga o cache HTTP do navegador, para que toda carga seja realmente "sem cache".
await send('Network.setCacheDisabled', { cacheDisabled: true });
// Liga a coleta de métricas de desempenho, com o tempo medido em ticks.
await send('Performance.enable', { timeDomain: 'timeTicks' });
// Manda a aba abrir o endereço do benchmark.
await send('Page.navigate', { url });

// Guarda a amostra anterior, para calcular o uso de CPU entre duas amostras.
let last = null;
// Repete enquanto o benchmark não terminar.
while (!done) {
  // Pede ao Chrome as métricas de desempenho atuais da página.
  const { metrics } = await send('Performance.getMetrics');
  // Converte a lista de métricas em um objeto nome -> valor.
  const m = Object.fromEntries(metrics.map(x => [x.name, x.value]));
  // Hora atual do relógio, em milissegundos.
  const wall = Date.now();
  // TaskDuration: cumulative seconds the renderer main thread spent running tasks
  // A partir da segunda volta (quando já existe uma amostra anterior)...
  if (last) {
    // ...CPU % = tempo de tarefas gasto desde a última amostra ÷ tempo de relógio passado, vezes 100.
    const cpu = ((m.TaskDuration - last.task) / ((wall - last.wall) / 1000)) * 100;
    // ...imprime a linha SAMPLE com a hora, a memória JS em MB (bytes ÷ 1048576) e a CPU.
    console.log(`SAMPLE ${wall} ${(m.JSHeapUsedSize / 1048576).toFixed(1)} ${cpu.toFixed(1)}`);
  // Fim do if.
  }
  // Guarda os valores desta amostra para a próxima comparação.
  last = { task: m.TaskDuration, wall };
  // Espera o intervalo pedido (em segundos, convertido para milissegundos) antes da próxima amostra.
  await new Promise(r => setTimeout(r, Number(intervalS) * 1000));
// Fim do laço.
}
// Fecha a conexão com o Chrome.
ws.close();
