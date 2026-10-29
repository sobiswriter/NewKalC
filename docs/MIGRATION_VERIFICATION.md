# Vertex migration verification — 2026-10-29

## Delivered behavior

In **API Key & AI Settings**, choose **Vertex AI server** or **AI Studio**. Vertex uses the On Sundays project (`gen-lang-client-0100408368`) and requires the shared server passkey. An incorrect passkey never obtains a session, and unauthenticated generation never invokes Vertex. AI Studio uses the user's own API key directly; it does not require or contact the protected server.

Switching providers retains the Studio key and meal but locks the cloud session. Server sessions expire after 12 hours, remain only in memory, and are cleared on process restart. No Google Cloud credential or raw server passkey is embedded in the APK or committed to Git.

## Evidence

| Check | Result |
|---|---|
| Backend `node --test` | 10 tests passed |
| Android `gradlew.bat testDebugUnitTest` | 12 tests passed locally; also passed in Cloud Build |
| `assembleDebug assembleDebugAndroidTest` | Both APKs built successfully |
| Complete Android instrumentation suite | 2 tests passed on the API 36 emulator |
| Emulator provider/passkey test | Wrong passkey rejected, correct passkey unlocks, Studio key retained, switching locks Vertex |
| Public server wrong passkey | HTTP 401 |
| Public server anonymous generation | HTTP 401 |
| Public server correct passkey | HTTP 200, temporary session |
| Public server authorized generation | HTTP 200 |
| Main photo, actual app prompt and sample thali | HTTP 200, complete JSON with 5 foods |
| Additional foods, actual app prompt and sample thali | HTTP 200, complete JSON with 5 foods |
| Nutrition insights | HTTP 200, valid headline/suggestions JSON |
| Meal chat | HTTP 200, nonempty response |

The complete live check is reproducible with `server/smoke-test.ps1 -Meal`. It reads the owner passkey privately and does not print credentials. Android unit results are under `app/build/test-results/testDebugUnitTest/`. Runtime logs and APK copies are also saved under `%LOCALAPPDATA%/KalC/artifacts/`.

The security review found and prompted a fix for delayed successful unlocks arriving after a newer lock/failed attempt. A synchronized session counter and regression test cover this case. Review of the fix found no remaining actionable issue.

## Deployment and artifacts

- Cloud Run service: `kalc-vertex`, `us-central1`, project `gen-lang-client-0100408368`.
- Phone endpoint: `https://kalc-vertex-157534643202.us-central1.run.app`.
- Verified deployment: `kalc-vertex-00005-tql`, serving `gemini-3.1-flash-lite` in `global`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- APK SHA-256: `e687df0d90b483f8eaa7185406323b9637877c7d92f47771c83777ed621add80`.
- Owner passkey: `%LOCALAPPDATA%/KalC/server-passkey.txt`, outside Git and OneDrive.
- Verified native screen: `docs/images/provider-settings-verified.png`.
- Visual proposal: `docs/visual-overhaul-preview.html`; its interactions and 360px layout were checked in a real browser.

Cloud Build `a9354225-0ee4-42e4-a677-afd87cb48457` independently built both APKs and passed the unit suite. The final APK was rebuilt locally with the workspace debug signing key after provider-neutral error-copy refinements, then installed and checked in the emulator.

## Practical limits

No live Studio API request was tested because a valid user-owned Studio key was not supplied. Release signing, Play Store distribution, and a physical-device camera check were not part of this verification. Windows assembly completed despite a KSP/AWT background warning; the resulting APK installed and passed the device access test. Native-library stripping warnings did not prevent either build.

The service scales to zero and is limited to one instance. Its process-local throttle must be replaced with a shared limiter before scaling out. People holding the shared passkey can incur Vertex usage in On Sundays. The visual refresh is a proposal, not an applied native redesign.
