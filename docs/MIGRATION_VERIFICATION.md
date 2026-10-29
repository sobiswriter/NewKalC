# Vertex migration verification — 2026-10-29

**Latest app:** version 1.3 refines the native design and adds optional profile personalization while retaining this backend/provider behavior. See [profile privacy, screenshots, and upgrade checks](PROFILE_RELEASE_1_3.md). The version 1.1 artifact/hash below records the migration release; [version 1.2](VISUAL_RELEASE_1_2.md) records the initial native overhaul.

## Delivered behavior

In **API Key & AI Settings**, choose **Vertex AI server** or **AI Studio**. Vertex uses the On Sundays project (`gen-lang-client-0100408368`) and requires the shared server passkey. An incorrect passkey never obtains a session, and unauthenticated generation never invokes Vertex. AI Studio uses the user's own API key directly; it does not require or contact the protected server.

Switching providers retains the Studio key and meal but locks the cloud session. Server sessions expire after 12 hours, remain only in memory, and are cleared on process restart. No Google Cloud credential or raw server passkey is embedded in the APK or committed to Git.

## Evidence

| Check | Result |
|---|---|
| Backend `node --test` | 10 tests passed |
| Android `gradlew.bat testDebugUnitTest` | 16 tests passed locally, including new guardrail/model tests |
| `assembleDebug assembleDebugAndroidTest assembleRelease` | Debug, test, and signed release APKs built successfully; release vital lint passed |
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
| Guardrail contract tests | Missing/invalid food fields, low confidence, non-food, empty detections rejected; valid food accepted |
| Both scan paths | Blank/tiny images rejected before network; model non-food result never retried or substituted with demo food |
| Model transport tests | All four Studio operations plus key validation request only 3.8 Flash with LOW thinking |
| Live non-food fixture | Both actual scan prompts rejected the non-food screenshot with empty detections |
| Release APK signature | Verified APK Signature Scheme v2, one private owner certificate |
| Release install/start | Installed on API 36; cold activity launch 898 ms; no AndroidRuntime crash; no DEBUGGABLE/TEST_ONLY package flags |

The complete live check is reproducible with `server/smoke-test.ps1 -Meal -NonFoodImage docs/images/nonfood-settings.jpg`. It reads the owner passkey privately and does not print credentials. Android unit results are under `app/build/test-results/testDebugUnitTest/`. Runtime logs and APK copies are also saved under `%LOCALAPPDATA%/KalC/artifacts/`. The non-food fixture is an Android settings screenshot from the test emulator; it contains no personal data.

## Model and responsiveness

Every AI operation uses **Gemini 3.8 Flash**. Both transports set LOW thinking, supported by [Google's model guide](https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/guides/gemini-3-8-flash). Studio's alternate-model chain was removed; a network/model failure cannot silently select a different Gemini model. Deterministic nutrition calculations, sample references, and local image validation remain local operations.

Three live runs on revision `kalc-vertex-00007-9t5` measured main-photo generation at 11.82, 11.82, and 14.61 seconds; side-photo generation at 9.28, 11.29, and 10.17 seconds; insights at 2.81, 3.17, and 12.57 seconds; chat at 8.21, 3.68, and 3.01 seconds. The final non-food fixture was rejected in 6.74 and 3.71 seconds. These include HTTP round trips from the development machine. LOW thinking keeps reasoning effort bounded; it does not promise constant service latency. The previous default-thinking 3.8 experiments sometimes exceeded the 55-second deadline; this is not a controlled percentile benchmark.

The local food gate previously ran twice per scan, including once on the main thread. It now runs once in the shared service's IO context; gallery decoding also runs off the UI thread. Existing 720px/JPEG upload compression and immediate meal display while insights load are retained. The release omits debug tooling, is approximately 22.6 MB, and cold-started in 898 ms on the API 36 emulator. Physical-device startup and cellular-network latency remain unmeasured.

Food response validation now fails closed when `is_food` or finite, in-range `food_confidence` is absent. Food names are disclosed only after validation passes. Blank/tiny images, face detection, model-based non-food rejection, configurable confidence threshold, and empty-detection rejection remain in the latest version. The default threshold stays 65%.

The security review found and prompted a fix for delayed successful unlocks arriving after a newer lock/failed attempt. A synchronized session counter and regression test cover this case. Review of the fix found no remaining actionable issue.

## Deployment and artifacts

- Cloud Run service: `kalc-vertex`, `us-central1`, project `gen-lang-client-0100408368`.
- Phone endpoint: `https://kalc-vertex-157534643202.us-central1.run.app`.
- Verified deployment: `kalc-vertex-00007-9t5`, serving `gemini-3.8-flash` in `global`, 100% traffic, authentication secret version 3.
- Signed release APK: `app/build/outputs/apk/release/app-release.apk`, version 1.1 (code 2); owner copies in Windows `Downloads/KalC-v1.1.apk` and `%LOCALAPPDATA%/KalC/artifacts/KalC-v1.1.apk`.
- Release APK SHA-256: `af4d921ca1d2ae36b48e1f1865f0804e551cf928643ca3bd1ec8a93abb99ef81`.
- Release certificate SHA-256: `8cc8a357ddfe4a6e0f596b020bdf0b118e7dd4d6032997bb181f15fff90a3e14`.
- Owner passkey: `%LOCALAPPDATA%/KalC/server-passkey.txt`, outside Git and OneDrive.
- Verified native screen: `docs/images/provider-settings-verified.png`.
- Visual proposal: `docs/visual-overhaul-preview.html`; its interactions and 360px layout were checked in a real browser.

Cloud Build `a9354225-0ee4-42e4-a677-afd87cb48457` independently verified the earlier migration baseline. The final 3.8 Flash app, complete 16-test suite, and signed release were built locally. Both instrumentation tests passed against the updated live server before the final release was installed and cold-started separately. The requested passkey and session signing key were rotated together; old sessions no longer authorize new-revision requests.

## Practical limits

No live Studio API request was tested because a valid user-owned Studio key was not supplied; all Studio request contracts are covered with a mock transport. A physical-device camera check, load testing, and Play Store distribution were not performed. Emulator tests and a few model calls cannot prove recognition accuracy for every possible image. Native-library stripping warnings did not prevent signed release assembly.

To install normally, transfer/download the signed APK onto the phone, open it, allow installation from that source if prompted, and tap Install. No USB debugging is needed. An older debug build must be uninstalled first because its signing certificate differs; that removes its settings. Future updates must retain the owner release signing key, stored privately outside Git.

The service scales to zero and is limited to one instance. Its process-local throttle must be replaced with a shared limiter before scaling out. People holding the shared passkey can incur Vertex usage in On Sundays. The visual refresh was a proposal in version 1.1 and is implemented in the native version 1.2 app.
