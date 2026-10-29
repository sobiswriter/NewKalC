# KalC Vertex server

The Android app offers **Vertex AI server** and **Google AI Studio**. Only Vertex requires the shared server passkey. AI Studio requests go directly to Google with the user's own key. Google API charges and quotas still apply to the selected provider.

The Vertex backend defaults to `gemini-3.1-flash-lite` in the `global` location. Live comparison with the project's thali image returned complete food JSON in about 4.5 seconds; `gemini-3.8-flash` intermittently exceeded the server deadline. The server owns model selection, so this can be changed without shipping a new APK. Image requests receive a bounded 4096-token budget to avoid truncated food JSON from legacy client limits.

## Contract

- `GET /health`: public process health; no generation or configuration details.
- `POST /session`, JSON `{ "passkey": "..." }`: verifies the shared passkey using scrypt and returns `{ "token": "...", "expiresAt": 1234567890000 }`. Incorrect passkeys return 401; too many attempts return 429. The token expires after 12 hours.
- `POST /generate`, bearer session token and a Gemini `contents`/`generationConfig` payload: verifies the signature and expiry before forwarding to the fixed Vertex project and model. It accepts text/inline images, not client-selected URLs, projects, tools, or models. Returns the existing Gemini `candidates` shape. Missing/invalid sessions return 401 without calling Vertex.

The server never receives AI Studio keys. The app retains its session only in memory, locks on restart, provider change, explicit disconnect, expiry, or a rejected session, and never persists the raw passkey. Wrong passkey attempts also clear any prior local session.

## Run checks

Requires Node 22 or newer. No third-party runtime dependencies.

```powershell
cd server
node --test
```

After deployment, `./server/smoke-test.ps1 -Meal` verifies wrong/anonymous access, unlock, both image operations using the actual app prompts and a sample photo, nutrition insights, and chat. These live checks use Vertex quota; they never print credentials.

## Owner setup and first deployment

Run `./server/deploy.ps1` from the owner machine with an authorized gcloud account. It targets **On Sundays**, `gen-lang-client-0100408368`, and deploys `kalc-vertex` in `us-central1` privately first. Google Cloud billing must be enabled. Source deployment requires the build account to have the documented Cloud Run builder permissions.

`create-secrets.mjs` generates a random 192-bit passkey and a separate signing key. The passkey is saved to `%LOCALAPPDATA%/KalC/server-passkey.txt`, outside the repository and OneDrive; it is never printed. Only the scrypt hash, salt, and signing key go into Secret Manager as `kalc-server-auth`. The service identity has Vertex AI User access and read access to this single secret. No service-account JSON key is created.

Verify the private revision using an authorized Cloud Run identity (`X-Serverless-Authorization` for Cloud Run IAM) while putting the app session in `Authorization`. Check wrong passkey → 401, anonymous generation → 401, correct passkey → session, then one authorized Vertex request. After these pass, enable app invocations:

```powershell
gcloud run services add-iam-policy-binding kalc-vertex --project=gen-lang-client-0100408368 --region=us-central1 --member=allUsers --role=roles/run.invoker
```

This makes the HTTP service reachable by phones. **It does not bypass the server's passkey/session verification**: `/generate` still refuses unauthenticated callers. Put the HTTPS service URL in `VERTEX_SERVER_URL` in `.env.example` (public build default) or your ignored `.env` override; never put passkeys there. Build the Android app with `./gradlew.bat testDebugUnitTest assembleDebug`.

## Limits and ownership

The deployment uses one instance and scale-to-zero. Per-process throttling is a baseline abuse control (10 session attempts/minute, 60 AI requests/minute); restarts and overlapping revisions reset/duplicate those counters. A shared limiter or an edge policy is required before scaling or wider public distribution. Output is capped at 4096 tokens and incoming requests at 4 MB. Vertex requests time out after 55 seconds. These controls reduce exposure but are not a hard billing cap; anyone you share the passkey with can incur usage in On Sundays.

To revoke all sessions and rotate the passkey, generate a new owner configuration with a fresh signing key, upload a new Secret Manager version, and deploy a new revision; drain the old revision. Keep the owner secret files private and back them up securely. Logs omit request bodies, credentials, and upstream error details.

References: [Cloud Run service identity](https://docs.cloud.google.com/run/docs/securing/service-identity), [Secret Manager integration](https://docs.cloud.google.com/run/docs/configuring/services/secrets), [Vertex generateContent](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/model-reference/inference).
