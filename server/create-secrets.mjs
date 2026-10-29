import { randomBytes, scryptSync } from 'node:crypto';
import { mkdir, stat, writeFile } from 'node:fs/promises';
import { join } from 'node:path';

if (!process.env.LOCALAPPDATA) throw new Error('Run secret setup on the owner Windows machine.');
const directory = join(process.env.LOCALAPPDATA, 'KalC');
await mkdir(directory, { recursive: true, mode: 0o700 });
const passkeyPath = join(directory, 'server-passkey.txt');
const configPath = join(directory, 'server-auth.json');
const exists = async path => stat(path).then(() => true, () => false);
const passkeyExists = await exists(passkeyPath);
const configExists = await exists(configPath);
if (passkeyExists !== configExists) throw new Error('Incomplete secret setup. Restore the owner files before proceeding.');
if (!passkeyExists) {
  const passkey = randomBytes(24).toString('base64url');
  const salt = randomBytes(16).toString('hex');
  const config = { salt, hash: scryptSync(passkey, salt, 64).toString('hex'), sessionKey: randomBytes(32).toString('hex') };
  await writeFile(configPath, JSON.stringify(config), { flag: 'wx', mode: 0o600 });
  await writeFile(passkeyPath, passkey + '\n', { flag: 'wx', mode: 0o600 });
}
console.log(`Owner passkey file: ${passkeyPath}`);
console.log(`Secret Manager upload file: ${configPath}`);
