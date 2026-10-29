import { createServer } from 'node:http';
import { createHmac, randomBytes, scrypt, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';
import { pathToFileURL } from 'node:url';

const deriveKey = promisify(scrypt);
const sessionMs = 12 * 60 * 60 * 1000;
const fail = (status, message) => Object.assign(new Error(message), { status });
const equal = (a, b) => a.length === b.length && timingSafeEqual(a, b);

async function readJson(request, limit) {
  if (!request.headers['content-type']?.startsWith('application/json')) throw fail(415, 'JSON required.');
  let size = 0;
  const chunks = [];
  for await (const chunk of request.iterator({ destroyOnReturn: false })) {
    size += chunk.length;
    if (size > limit) throw fail(413, 'Request too large.');
    chunks.push(chunk);
  }
  try { return JSON.parse(Buffer.concat(chunks).toString('utf8')); }
  catch { throw fail(400, 'Invalid JSON.'); }
}

function validatePayload(payload) {
  if (!payload || Object.keys(payload).some(key => !['contents', 'generationConfig'].includes(key)) ||
      !Array.isArray(payload.contents) || payload.contents.length < 1 || payload.contents.length > 16) {
    throw fail(400, 'Invalid generation request.');
  }
  for (const content of payload.contents) {
    if (!content || Object.keys(content).some(key => !['role', 'parts'].includes(key)) ||
        (content.role !== undefined && !['user', 'model'].includes(content.role)) ||
        !Array.isArray(content.parts) || content.parts.length < 1 || content.parts.length > 8) throw fail(400, 'Invalid content.');
    for (const part of content.parts) {
      if (!part || Object.keys(part).length !== 1) throw fail(400, 'Invalid content part.');
      if (typeof part.text === 'string' && part.text.length > 0 && part.text.length <= 32000) continue;
      const image = part.inlineData;
      if (image && Object.keys(image).every(key => ['mimeType', 'data'].includes(key)) &&
          ['image/jpeg', 'image/png', 'image/webp'].includes(image.mimeType) &&
          typeof image.data === 'string' && image.data.length > 0 && image.data.length <= 3_000_000 &&
          image.data.length % 4 === 0 && /^[A-Za-z0-9+/]+={0,2}$/.test(image.data)) continue;
      throw fail(400, 'Only text and inline food images are supported.');
    }
  }
  const settings = payload.generationConfig ?? {};
  if (!settings || typeof settings !== 'object' || Array.isArray(settings) ||
      Object.keys(settings).some(key => !['temperature', 'maxOutputTokens', 'responseMimeType'].includes(key)) ||
      (settings.temperature !== undefined && (!Number.isFinite(settings.temperature) || settings.temperature < 0 || settings.temperature > 2)) ||
      (settings.maxOutputTokens !== undefined && (!Number.isInteger(settings.maxOutputTokens) || settings.maxOutputTokens < 1)) ||
      (settings.responseMimeType !== undefined && !['application/json', 'text/plain'].includes(settings.responseMimeType))) throw fail(400, 'Invalid generation settings.');
  const hasImage = payload.contents.some(content => content.parts.some(part => part.inlineData));
  return {
    contents: payload.contents.map(content => ({ ...content, role: content.role ?? 'user' })),
    // Vision analysis needs room for reasoning and the complete food JSON, including legacy 1024-token requests.
    generationConfig: { ...settings, maxOutputTokens: hasImage ? 4096 : Math.min(settings.maxOutputTokens ?? 4096, 4096) }
  };
}

export function createHandler({ config, project, model = 'gemini-3.1-flash-lite', generate, now = Date.now }) {
  if (!config || !/^[a-f0-9]{32}$/.test(config.salt) || !/^[a-f0-9]{128}$/.test(config.hash) ||
      typeof config.sessionKey !== 'string' || config.sessionKey.length < 32) throw new Error('Valid server authentication configuration is required.');
  if (!/^[a-z][a-z0-9-]{4,61}[a-z0-9]$/.test(project ?? '') || !/^gemini-[a-z0-9.-]+$/.test(model)) throw new Error('Invalid Vertex configuration.');
  const sign = value => createHmac('sha256', config.sessionKey).update(value).digest('base64url');
  const expectedHash = Buffer.from(config.hash, 'hex');
  let loginWindow = 0, loginCount = 0, generationWindow = 0, generationCount = 0;
  let accessToken = null;

  function authorize(header) {
    if (typeof header !== 'string' || header.length > 2048 || !header.startsWith('Bearer ')) return false;
    const pieces = header.slice(7).split('.');
    if (pieces.length !== 2 || !pieces.every(piece => /^[A-Za-z0-9_-]+$/.test(piece))) return false;
    if (!equal(Buffer.from(sign(pieces[0])), Buffer.from(pieces[1]))) return false;
    try {
      const session = JSON.parse(Buffer.from(pieces[0], 'base64url').toString());
      return Number.isSafeInteger(session.expiresAt) && session.expiresAt > now() && session.expiresAt <= now() + sessionMs;
    } catch { return false; }
  }

  async function callVertex(payload) {
    if (!accessToken || accessToken.expiresAt < now() + 60000) {
      const response = await fetch('http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token', {
        headers: { 'Metadata-Flavor': 'Google' }, signal: AbortSignal.timeout(5000), redirect: 'error'
      });
      if (!response.ok) throw new Error('Service identity unavailable');
      const identity = await response.json();
      if (typeof identity.access_token !== 'string' || !Number.isFinite(identity.expires_in)) throw new Error('Invalid service identity');
      accessToken = { value: identity.access_token, expiresAt: now() + identity.expires_in * 1000 };
    }
    const response = await fetch(`https://aiplatform.googleapis.com/v1/projects/${project}/locations/global/publishers/google/models/${model}:generateContent`, {
      method: 'POST', headers: { authorization: `Bearer ${accessToken.value}`, 'content-type': 'application/json' },
      body: JSON.stringify(payload), signal: AbortSignal.timeout(55000), redirect: 'error'
    });
    if (!response.ok) {
      await response.body?.cancel();
      console.error(JSON.stringify({ event: 'vertex_upstream_failed', status: response.status }));
      if (response.status === 429) throw fail(429, 'Vertex AI is busy or its quota was reached. Please try again shortly.');
      throw new Error('Vertex request failed');
    }
    const result = await response.json();
    if (!Array.isArray(result.candidates)) throw new Error('Invalid Vertex response');
    // Only generated content crosses back to the app; upstream metadata and credentials stay here.
    return { candidates: result.candidates };
  }

  return async (request, response) => {
    const send = (status, data) => {
      response.writeHead(status, {
        'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store',
        'x-content-type-options': 'nosniff', 'strict-transport-security': 'max-age=31536000',
        'content-security-policy': "default-src 'none'; frame-ancestors 'none'", 'x-frame-options': 'DENY',
        ...(status === 429 ? { 'retry-after': '60' } : {})
      });
      response.end(JSON.stringify(data));
    };
    try {
      if (request.method === 'GET' && request.url === '/health') return send(200, { status: 'ok' });
      if (request.method !== 'POST' || !['/session', '/generate'].includes(request.url)) throw fail(404, 'Not found.');
      if (request.headers.origin) throw fail(403, 'Use the KalC Android app.');
      if (request.url === '/session') {
        // shortcut: process-local throttling assumes a single Cloud Run instance; use a shared limiter before scaling out.
        if (now() - loginWindow >= 60000) { loginWindow = now(); loginCount = 0; }
        if (++loginCount > 10) throw fail(429, 'Too many unlock attempts. Wait a minute and retry.');
        const input = await readJson(request, 2048);
        if (!input || typeof input.passkey !== 'string' || input.passkey.length < 1 || input.passkey.length > 512) throw fail(400, 'Passkey required.');
        const actualHash = await deriveKey(input.passkey, config.salt, 64);
        if (!equal(actualHash, expectedHash)) throw fail(401, 'Incorrect passkey. Server access denied.');
        const expiresAt = now() + sessionMs;
        const value = Buffer.from(JSON.stringify({ expiresAt, nonce: randomBytes(16).toString('hex') })).toString('base64url');
        return send(200, { token: `${value}.${sign(value)}`, expiresAt });
      }
      if (!authorize(request.headers.authorization)) throw fail(401, 'Server locked. Enter your passkey again in AI settings.');
      if (now() - generationWindow >= 60000) { generationWindow = now(); generationCount = 0; }
      if (++generationCount > 60) throw fail(429, 'Too many requests. Please retry shortly.');
      const payload = validatePayload(await readJson(request, 4_000_000));
      return send(200, await (generate ?? callVertex)(payload));
    } catch (error) {
      request.resume();
      const status = error.status ?? 502;
      if (status >= 500) console.error(JSON.stringify({ event: 'vertex_request_failed', status, kind: error.name }));
      send(status, { error: { message: error.status ? error.message : 'Vertex AI is unavailable. Please retry shortly.' } });
    }
  };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const config = JSON.parse(process.env.AUTH_CONFIG ?? '{}');
  const server = createServer({ requestTimeout: 15000, headersTimeout: 10000, maxHeaderSize: 8192 }, createHandler({
    config, project: process.env.GOOGLE_CLOUD_PROJECT, model: process.env.VERTEX_MODEL ?? 'gemini-3.1-flash-lite'
  }));
  server.listen(Number(process.env.PORT ?? 8080), '0.0.0.0');
}
