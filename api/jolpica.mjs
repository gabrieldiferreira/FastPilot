// Vercel Function: Jolpica (Ergast) proxy with a CDN cache, for the standings.
//
// Jolpica answers from its own cache in ~0.1 s, but when that cache has expired (10 min) the
// answer takes from 0.5 s to over 20 s. Here every answer is kept in Vercel's CDN and, once
// stale, served at once while a fresh copy is fetched in the background (stale-while-revalidate),
// also while Jolpica is down (stale-if-error). The app calls /api/jolpica?path=<path> and goes
// straight to Jolpica when this proxy fails (JolpicaApi).

const JOLPICA = 'https://api.jolpi.ca/ergast/f1';

// Only the paths the app uses: the proxy must not become an open relay
const PATH = /^(seasons\.json(\?limit=\d{1,3})?|\d{4}\/(races|driverStandings|constructorStandings)\.json|\d{4}\/\d{1,2}\/results\.json)$/;

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
};

const HOUR = 3600;
const DAY = 24 * HOUR;

/** The current season changes after every race; past seasons no longer change. */
function cacheControl(path) {
  const year = Number(path.slice(0, 4));
  const current = Number.isNaN(year) || year >= new Date().getUTCFullYear();
  const fresh = current ? HOUR : 7 * DAY;
  return `public, max-age=0, s-maxage=${fresh}, stale-while-revalidate=${30 * DAY}, stale-if-error=${30 * DAY}`;
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, 'Content-Type': 'application/json', 'Cache-Control': 'no-store' },
  });
}

export function OPTIONS() {
  return new Response(null, { status: 204, headers: CORS });
}

export async function GET(request) {
  const path = new URL(request.url).searchParams.get('path') ?? '';
  if (!PATH.test(path)) return json(404, { detail: 'Unknown path' });

  let response;
  try {
    response = await fetch(`${JOLPICA}/${path}`, { signal: AbortSignal.timeout(25_000) });
  } catch (error) {
    return json(502, { detail: String(error.message ?? error) });
  }

  return new Response(response.body, {
    status: response.status,
    headers: {
      ...CORS,
      'Content-Type': response.headers.get('Content-Type') ?? 'application/json',
      'Cache-Control': response.ok ? cacheControl(path) : 'no-store',
    },
  });
}
