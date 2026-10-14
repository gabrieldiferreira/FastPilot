// Drives the Web build in Chrome over the DevTools protocol for scripts/perf_run.py:
// opens the benchmark URL, relays the page console to stdout and every <interval> seconds
// prints "SAMPLE <epoch ms> <JS heap MB> <main thread CPU %>".
// node --experimental-websocket perf_web.mjs <cdp port> <url> <interval s>
const [port, url, intervalS] = process.argv.slice(2);
const targets = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
const page = targets.find(t => t.type === 'page');
const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise(r => (ws.onopen = r));

let id = 0;
const pending = new Map();
let done = false;
ws.onmessage = e => {
  const m = JSON.parse(e.data);
  if (m.id && pending.has(m.id)) {
    pending.get(m.id)(m.result);
    pending.delete(m.id);
  } else if (m.method === 'Runtime.consoleAPICalled') {
    const text = m.params.args.map(a => a.value ?? a.description ?? '').join(' ');
    console.log(text);
    if (text.includes('"phase":"done"')) done = true;
  }
};
const send = (method, params = {}) =>
  new Promise(r => {
    const i = ++id;
    pending.set(i, r);
    ws.send(JSON.stringify({ id: i, method, params }));
  });

await send('Runtime.enable');
// No local database on Web, so every load is "without cache": keep the browser HTTP cache out too
await send('Network.enable');
await send('Network.setCacheDisabled', { cacheDisabled: true });
await send('Performance.enable', { timeDomain: 'timeTicks' });
await send('Page.navigate', { url });

let last = null;
while (!done) {
  const { metrics } = await send('Performance.getMetrics');
  const m = Object.fromEntries(metrics.map(x => [x.name, x.value]));
  const wall = Date.now();
  // TaskDuration: cumulative seconds the renderer main thread spent running tasks
  if (last) {
    const cpu = ((m.TaskDuration - last.task) / ((wall - last.wall) / 1000)) * 100;
    console.log(`SAMPLE ${wall} ${(m.JSHeapUsedSize / 1048576).toFixed(1)} ${cpu.toFixed(1)}`);
  }
  last = { task: m.TaskDuration, wall };
  await new Promise(r => setTimeout(r, Number(intervalS) * 1000));
}
ws.close();
