# Migration tasks

- [x] Inspect all AI callers and identify On Sundays in the signed-in Google Cloud account.
- [x] Confirm Vertex AI API and billing are enabled; inspect existing Cloud Run services in us-central1.
- [x] Record the migration architecture, affected files, trust boundaries, and verification requirements.
- [x] Prepare a visual overhaul proposal and reviewable mobile prototype.

## 1. Confirm access boundary and establish build baseline

- [x] Resolve whether the password protects Vertex access, the whole app, or an existing login.
- [x] Restore the configured Gradle wrapper and verify `testDebugUnitTest`.
- Acceptance: intended access boundary documented; baseline build result recorded.
- Files: wrapper launchers/JAR, plan; scope: small. Dependencies: user answer for auth scope.

## 2. Authenticated Vertex request

- [x] Add the smallest Cloud Run service using runtime identity for On Sundays.
- [x] Verify login/session and user authorization before every AI request; limit request size and time.
- [x] Reject anonymous, invalid, expired, and unauthorized requests in automated tests.
- Acceptance: unauthenticated traffic cannot reach Vertex; no cloud credentials in the client.
- Verification: backend tests and local contract test; no paid calls required.
- Files: backend source, tests, deployment instructions; no external runtime dependencies. Depends on 1.

## 3. Android provider integration

- [x] Add provider/session settings while retaining saved Studio keys.
- [x] Route main analysis, side dishes, insights, and chat through selected transport.
- [x] Show login, expiry, disconnect, failure, and demo state accurately.
- Acceptance: both providers work independently; no silent cross-provider fallback.
- Verification: focused Robolectric tests and debug APK build.
- Files: repository, service, ViewModel, provider UI, relevant config/tests; split transport and UI into separate increments. Depends on 2.

## Checkpoint: before deployment

- [x] Tests and build pass; no raw passwords/tokens/service-account keys in APK or Git.
- [x] Backend credentials and shared-passkey access configured through secure setup.
- [x] Deploy reviewed service; verify denied access plus one authorized Vertex call.
- [x] Exercise all four Vertex AI paths live; verify Studio selection and key preservation. A live Studio API call requires the user's own valid key and was not performed.

## Completed follow-up — 2026-10-29

- [x] Use only Gemini 3.8 Flash on both providers, including key validation; set LOW thinking.
- [x] Rotate the owner-requested passkey plus session signing key and deploy revision 00007.
- [x] Retain food guards; fail closed on malformed validation fields; test both scan paths.
- [x] Run image decoding/validation off the main thread; record live and release startup timings.
- [x] Pass 10 backend tests, 16 Android unit tests, two emulator instrumentation tests, and live food/non-food checks.
- [x] Build and signature-verify non-debug release 1.1, install/start it, and copy the APK to Downloads.
- [x] Update installation, model, credentials, performance, and verification documentation.

## Profile and premium refinement — version 1.3

- [x] Remove duplicate bottom scan/gallery actions; refine summary/title/navigation.
- [x] Add skippable four-step onboarding and an editable Profile section.
- [x] Default personalization OFF; send context only to insights/chat with explicit consent.
- [x] Encrypt local profile data, exclude backups, validate fields, and provide deletion.
- [x] Clear old advice/chat and reject stale profile-generation results on changes.
- [x] Pass 24 unit tests, 10 backend tests, six core device tests, and four dark/360dp/130% text checks.
- [x] Build signed version 1.3 with the existing release certificate.
- [x] Verify live opted-in synthetic-profile insights/chat and an in-place signed 1.2 upgrade.

## Version 1.2 baseline

- [x] Deliver and browser-verify `docs/visual-overhaul-preview.html` with `docs/UI_OVERHAUL_PLAN.md`.
- [x] Translate the visual direction into native Compose, including theme, meal summary, editing, navigation, insights, chat, and provider settings.
- [x] Verify portion recalculation, navigation, provider/passkey flow, and 360dp/130% text in dark mode on the emulator.
- [x] Retain Gemini 3.8 Flash routing and food-validation logic; run the full unit suite.
- [x] Build signed version 1.2 with the same release certificate as 1.1.
- [ ] Physical-device camera, TalkBack, and vendor-specific Android rendering checks remain unverified.
- Acceptance: complete meal workflow stays usable, provider status is clear, all controls have 48dp touch targets.
- Verification: real Compose instrumentation, native screenshots, signed release build, and upgrade installation. See `docs/VISUAL_RELEASE_1_2.md`.
- Files: current screen, theme tokens/type, targeted UI checks; scope: medium increments. Depends on design review, separate from migration.
