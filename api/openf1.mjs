// Vercel Function: OpenF1 proxy for real-time data.
//
// OpenF1 serves live sessions (30 min before until 30 min after) only to paid "Sponsor"
// accounts. This function keeps the account on the server (environment variables
// OPENF1_USERNAME and OPENF1_PASSWORD), exchanges it for an access token (valid for 1 hour)
// and forwards the app's GET requests with it. vercel.json rewrites
// /openf1/v1/<endpoint>?<query> to /api/openf1?endpoint=<endpoint>&<query>.
//
// Every user shares the account's quota (60 requests per minute), so successful answers are
// kept for a few seconds in Vercel's CDN: users polling the same endpoint get one cached reply.

const OPENF1 = 'https://api.openf1.org';

// Only the endpoints the app uses: the proxy must not become an open relay for the account
const ENDPOINTS = new Set([
  'sessions', 'meetings', 'drivers', 'car_data', 'laps', 'stints', 'position',
  'intervals', 'race_control', 'weather', 'team_radio', 'pit', 'overtakes', 'location',
]);

const CDN_CACHE = 'public, s-maxage=4, stale-while-revalidate=4';

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
};

// Reused while the function instance stays warm
let token = null;
let tokenExpiresAt = 0;

async function accessToken() {
  if (token && Date.now() < tokenExpiresAt) return token;
  const { OPENF1_USERNAME, OPENF1_PASSWORD } = process.env;
  if (!OPENF1_USERNAME || !OPENF1_PASSWORD) throw new Error('OPENF1_USERNAME/OPENF1_PASSWORD not set');
  const response = await fetch(`${OPENF1}/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username: OPENF1_USERNAME, password: OPENF1_PASSWORD }),
  });
  if (!response.ok) throw new Error(`OpenF1 token request failed: ${response.status}`);
  const body = await response.json();
  token = body.access_token;
  // Renew 5 minutes early
  tokenExpiresAt = Date.now() + (Number(body.expires_in) - 300) * 1000;
  return token;
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
  const params = new URL(request.url).searchParams;
  const endpoint = params.get('endpoint');
  if (!ENDPOINTS.has(endpoint)) return json(404, { detail: 'Unknown endpoint' });
  params.delete('endpoint');
  const upstream = `${OPENF1}/v1/${endpoint}?${params}`;

  let response;
  try {
    response = await fetch(upstream, { headers: { Authorization: `Bearer ${await accessToken()}` } });
    if (response.status === 401) {
      // Token revoked or expired early: get a new one once
      token = null;
      response = await fetch(upstream, { headers: { Authorization: `Bearer ${await accessToken()}` } });
    }
  } catch (error) {
    return json(502, { detail: String(error.message ?? error) });
  }

  return new Response(response.body, {
    status: response.status,
    headers: {
      ...CORS,
      'Content-Type': response.headers.get('Content-Type') ?? 'application/json',
      'Cache-Control': response.ok ? CDN_CACHE : 'no-store',
    },
  });
}
