# Make a meal implementation — 1.4

Approved scope: docs/make-a-meal/PRODUCT_STORYBOARD.md, including local scan/cooked/recipe history and optional finished-plate review. Existing provider authentication, profile consent and non-food guards remain mandatory.

- [x] Ingredient/recipe contracts, strict parsing, deterministic portions/availability and focused tests.
- [x] App-private indexed history, complete snapshots/photos, offline reopen and deletion controls.
- [x] Selected-provider ingredient recognition, recipe discovery/detail and recipe-aware chat.
- [x] Native capture/confirmation, pantry and meal brief; three groups plus Quick/Healthy Grab.
- [x] Cookbook, exact shopping list, cooking progress/timers, substitutions and saved versions.
- [x] Confirm cooked, optional KalC plate scan, bounded review and linked history.
- [x] Navigation, recent items, profile-purpose consent and stale-result protection.
- [x] Unit/backend/instrumentation verification, native visual checks and release documentation.
- [x] Signed installable update with existing certificate and upgrade verification.

Physical camera, TalkBack and vendor-specific behavior must be reported separately if no real device is available. Do not mark emulator checks as physical-device validation.
