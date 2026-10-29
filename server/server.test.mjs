import { test } from 'node:test';
import assert from 'node:assert/strict';
import { scryptSync } from 'node:crypto';
import { createServer } from 'node:http';
import { createHandler } from './index.mjs';

const config = {
  salt: '0123456789abcdef0123456789abcdef',
  hash: scryptSync('test-passkey-for-tests-only', '0123456789abcdef0123456789abcdef', 64).toString('hex'),
  sessionKey: 'test-signing-key-which-is-at-least-32-bytes-long'
};
const body = { contents: [{ parts: [{ text: 'Describe a meal' }] }] };

async function fixture(t, options = {}) {
  let calls = 0;
  const handler = createHandler({
    config, project: 'gen-lang-client-0100408368',
    generate: async request => { calls++; return { candidates: [{ content: { parts: [{ text: JSON.stringify(request) }] } }] }; },
    ...options
  });
  const server = createServer(handler);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  t.after(() => new Promise(resolve => server.close(resolve)));
  const base = `http://127.0.0.1:${server.address().port}`;
  const post = (route, data, token) => fetch(base + route, {
    method: 'POST', headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(data)
  });
  const login = async () => (await (await post('/session', { passkey: 'test-passkey-for-tests-only' })).json()).token;
  return { post, login, calls: () => calls };
}

test('wrong passkeys and anonymous requests never invoke Vertex', async t => {
  const f = await fixture(t);
  assert.equal((await f.post('/session', { passkey: 'wrong' })).status, 401);
  assert.equal((await f.post('/generate', body)).status, 401);
  assert.equal((await f.post('/generate', body, 'forged.token')).status, 401);
  assert.equal(f.calls(), 0);
});

test('valid passkey unlocks generation with bounded output and no secret in response', async t => {
  const f = await fixture(t);
  const token = await f.login();
  assert.equal(typeof token, 'string');
  const response = await f.post('/generate', { ...body, generationConfig: { maxOutputTokens: 999999 } }, token);
  assert.equal(response.status, 200);
  const text = JSON.stringify(await response.json());
  assert.ok(text.includes('4096'));
  const forwarded = JSON.parse(JSON.parse(text).candidates[0].content.parts[0].text);
  assert.equal(forwarded.contents[0].role, 'user');
  assert.deepEqual(forwarded.generationConfig.thinkingConfig, { thinkingLevel: 'LOW' });
  assert.ok(!text.includes(config.sessionKey));
  assert.equal(response.headers.get('cache-control'), 'no-store');
  assert.equal(f.calls(), 1);
});

test('expired and tampered sessions are rejected', async t => {
  let now = 1_000_000;
  const f = await fixture(t, { now: () => now });
  const token = await f.login();
  assert.equal((await f.post('/generate', body, token + 'a')).status, 401);
  now += 12 * 60 * 60 * 1000 + 1;
  assert.equal((await f.post('/generate', body, token)).status, 401);
  assert.equal(f.calls(), 0);
});

test('rotation revokes previously signed sessions', async t => {
  const old = await fixture(t);
  const token = await old.login();
  const rotated = await fixture(t, { config: { ...config, sessionKey: 'another-signing-key-which-is-at-least-32-bytes' } });
  assert.equal((await rotated.post('/generate', body, token)).status, 401);
});

test('client cannot choose project, tools, remote files, or arbitrary upstream URLs', async t => {
  const f = await fixture(t);
  const token = await f.login();
  for (const invalid of [
    { ...body, project: 'another-project' },
    { ...body, tools: [{ googleSearch: {} }] },
    { contents: [{ parts: [{ fileData: { fileUri: 'http://metadata.google.internal' } }] }] },
    { contents: [] },
    { contents: [{ parts: [{ inlineData: { mimeType: 'text/html', data: 'AAAA' } }] }] }
  ]) assert.equal((await f.post('/generate', invalid, token)).status, 400);
  assert.equal(f.calls(), 0);
});

test('too many login attempts are throttled', async t => {
  const f = await fixture(t);
  for (let i = 0; i < 10; i++) await f.post('/session', { passkey: 'wrong' });
  assert.equal((await f.post('/session', { passkey: 'wrong' })).status, 429);
  assert.equal(f.calls(), 0);
});

test('upstream credentials and errors are never returned to the app', async t => {
  const f = await fixture(t, { generate: async () => { throw new Error('PRIVATE_ACCESS_TOKEN'); } });
  const response = await f.post('/generate', body, await f.login());
  assert.equal(response.status, 502);
  assert.ok(!(await response.text()).includes('PRIVATE_ACCESS_TOKEN'));
});

test('unconfigured authentication fails closed', () => {
  assert.throws(() => createHandler({ config: {} }));
});

test('legacy Android image requests have enough output budget for complete food JSON', async t => {
  const f = await fixture(t);
  const request = {
    contents: [{ parts: [{ text: 'Analyze this meal' }, { inlineData: { mimeType: 'image/jpeg', data: 'AAAA' } }] }],
    generationConfig: { maxOutputTokens: 1024, responseMimeType: 'application/json' }
  };
  const response = await f.post('/generate', request, await f.login());
  assert.equal(response.status, 200);
  const result = await response.json();
  const forwarded = JSON.parse(result.candidates[0].content.parts[0].text);
  assert.equal(forwarded.generationConfig.maxOutputTokens, 4096);
});

test('oversized credentials are rejected without reaching Vertex', async t => {
  const f = await fixture(t);
  assert.equal((await f.post('/session', { passkey: 'a'.repeat(3000) })).status, 413);
  assert.equal(f.calls(), 0);
});
