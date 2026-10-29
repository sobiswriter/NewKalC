# KalC: Vertex AI migration and visual direction

## Requested outcome

Use the Google Cloud project **On Sundays** (`gen-lang-client-0100408368`) for Vertex AI, preserve AI Studio with a user-supplied API key, protect access with a passkey, and prepare a visual overhaul. The project name, enabled Vertex AI API, and active billing were confirmed through the signed-in Google Cloud CLI on 2026-10-29. A dedicated `kalc-vertex` Cloud Run service, runtime identity, and Secret Manager configuration implement the protected server route.

## Decisions

- Add an explicit provider choice between **On Sundays / Vertex AI** and **Google AI Studio**; retain the existing sample experience. Never silently fall back from one paid provider to another.
- Reuse the existing prompts, response parsing, food validation, and nutrition calculations. Route all four remote operations through the selected provider: main photo, additional foods, insights, and chat.
- Keep cloud credentials on a small Cloud Run backend with an attached service identity. Never ship a service-account key, Google Cloud access token, or project password in the APK.
- Authenticate and authorize every backend AI request. A local password dialog alone does not protect the endpoint or project billing.
- Confirmed by the user: an in-app choice between the Vertex server and AI Studio. Vertex requires one shared passkey; wrong passkeys grant no access. Use server-side scrypt verification and signed 12-hour sessions. No user accounts or whole-app password gate. AI Studio requires only the user's own API key.
- Keep AI Studio keys independent of the cloud login. Retain saved keys when switching providers. Do not send them to the cloud backend.
- Do not ask the user to paste passwords or service-account JSON into chat. Use the provider's secure account setup or a local secret prompt.
- Prepare the visual direction as a reviewable prototype before replacing the Android UI.

## Change surface

| Area | Existing code to reach | Required behavior |
|---|---|---|
| Provider credentials | `ApiKeyRepository.kt`, new cloud session repository | Existing API keys continue working; cloud login never impersonates a Studio key |
| Requests | `GeminiFoodService.kt`, small shared request transport | Same payload/parser for each provider; backend controls Vertex project, region, and model |
| App state | `FoodCalorieViewModel.kt` | Provider, connection, login, expiry, and errors reflect actual state |
| UI | `FoodCalorieScreen.kt`, provider/login component | Choose provider, connect, disconnect; keep meal workflow accessible |
| Configuration | Gradle catalog/build file, `.env.example`, manifest | Public endpoint configuration only; no embedded cloud secrets |
| Credential persistence | Android backup rules | Exclude credentials/session material from backup; avoid persisting raw passwords |
| Server | New isolated backend directory | Verify access before AI, bound input/output and timeouts; restrict model/project; no open proxy |
| Verification | Existing Robolectric tests plus provider/backend tests | Both providers and denied/expired sessions covered without paid calls |
| Design | Existing design system plus `docs/UI_OVERHAUL_PLAN.md` | Reviewable direction preserving current features |

## Trust boundaries and risks

The app, its settings, and submitted photos/text are untrusted backend inputs. The server alone chooses its project and permitted model. Google identity credentials stay in the runtime identity. Logs must not include image contents, user passwords, API keys, authorization headers, or full upstream error bodies.

The current service duplicates request construction and assumes API-key availability in several places; replacing only the primary image URL would leave the migration incomplete. Some existing fallback paths return demo answers after failures, so the UI must identify estimates and provider failures honestly. Existing numeric calculations and source labels must not become more authoritative than their underlying reference data.

Password scope is resolved. Deployment must not publish an endpoint until denied-access tests and an authenticated smoke test pass. Cloud invocation can incur charges; scale-to-zero and bounded requests are the default. Do not change unrelated resources in On Sundays.

## Implementation order

1. Resolve password scope; restore the missing Gradle launchers and establish the build baseline.
2. Add and test one authenticated Vertex request through the backend; keep the service identity limited to its required access.
3. Route photo, side dish, insights, and chat requests through the shared transport; verify AI Studio compatibility.
4. Add provider selection, login/session status, disconnect, and expiry handling to Android; test without embedding credentials.
5. Verify build, tests, and security behavior, then deploy the reviewed service and run one authenticated Vertex smoke test in On Sundays.
6. Review the visual prototype and implement the accepted design in existing Compose components as a separate change.

## Verification baseline

The initial checkout included `gradle-wrapper.properties` requesting Gradle 9.3.1 but lacked its launchers and wrapper JAR, so the original check could not run. These have been restored from Gradle's official release and checksum-verified; the distribution checksum is pinned. The complete 12-test Android unit suite now passes locally and in Cloud Build. Debug app and instrumentation APKs build successfully, and the emulator provider/passkey flow passes against the live server.

## Sources

- [Cloud Run service identity and Application Default Credentials](https://docs.cloud.google.com/run/docs/securing/service-identity)
- [Vertex generateContent request/response reference](https://docs.cloud.google.com/vertex-ai/generative-ai/docs/model-reference/inference)
- [Firebase App Check for AI Logic](https://firebase.google.com/docs/ai-logic/app-check) — app attestation is separate from a user's password and authorization.

## Current status

**Version 1.3:** the premium refinement and optional profile feature are delivered. Duplicate bottom photo actions are removed. Four skippable onboarding steps collect optional, validated profile details, encrypted locally with Android Keystore and excluded from backups. Personalization defaults OFF and gates context exclusively in insights/chat; changing profile/consent clears old advice/chat and ignores stale responses. Twenty-four unit tests, ten backend tests, six core device tests, a live synthetic-profile Vertex check, and four dark/360dp/130% text checks pass. The signed 1.2 → 1.3 upgrade retains connection preferences and shows onboarding once. See `docs/PROFILE_RELEASE_1_3.md` for artifact, privacy, screenshots, and limits.

The migration and 3.8 Flash follow-up are complete. The visual proposal is now implemented in the native app as version 1.2, after the user correctly reported that 1.1 still used the old UI. Both providers continue to use only Gemini 3.8 Flash with LOW thinking and the existing passkey/food guardrails. The 16 Android unit tests and three instrumentation tests pass, with an additional 360dp/130% text dark-mode flow check. The signed non-debug 1.2 APK uses the same owner certificate as 1.1 for in-place installation. See `docs/MIGRATION_VERIFICATION.md` for backend evidence and `docs/VISUAL_RELEASE_1_2.md` for native screenshots, artifact, and upgrade checks.
