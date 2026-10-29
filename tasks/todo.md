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

## 4. Visual overhaul proposal delivered; implementation is future work

- [x] Deliver and browser-verify `docs/visual-overhaul-preview.html` with `docs/UI_OVERHAUL_PLAN.md`.
- [ ] Translate accepted design into existing Compose components; retain current editing/validation features.
- [ ] Verify light/dark mode, 360dp width, large text, TalkBack, permission denial, and offline/error states.
- Acceptance: complete meal workflow stays usable, provider status is clear, all controls have 48dp touch targets.
- Verification: Android build plus emulator/device review; prototype does not count as shipped UI.
- Files: current screen, theme tokens/type, targeted UI checks; scope: medium increments. Depends on design review, separate from migration.
