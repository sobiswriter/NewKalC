# KalC — a calmer way to understand your plate

Status: implemented in the native Android app in **version 1.2**. Version 1.1 shipped only the provider migration; the HTML file was a design prototype. The installed 1.2 app now uses this direction in Compose, including the meal header, nutrition summary, navigation, editing, insights, chat, and AI connection screens. The original `visual-overhaul-preview.html` remains a historical design reference; screenshots of the native app are recorded in `VISUAL_RELEASE_1_2.md`.

**Version 1.3 refinement:** removes the duplicate bottom scan/gallery strip, retains top-right photo actions, simplifies the meal title and nutrition panel, and adds the Profile destination. Optional onboarding and default-off personalization are documented in `PROFILE_RELEASE_1_3.md`.

## Design contract

The meal screen answers three questions in order: what am I eating, what is the estimated nutrition, and what can I adjust? One dominant action starts a new scan. Portion changes and adding sides stay contextual. The existing Meal / Insights / Ask AI navigation remains recognizable.

Evolve the existing emerald identity into deep forest green, warm ivory, and a small citrus accent. Use an editorial serif for the brand and key headings, with a highly readable sans serif for numbers, labels, and controls. Food photography carries the visual interest. Avoid decorative dashboards, fake daily goals, invented historical data, and confidence claims unsupported by the model.

| Token | Proposed value | Use |
|---|---|---|
| Canvas | `#F6F3EB` | Warm background |
| Surface | `#FFFEFA` | Food list and controls |
| Primary / ink | `#183D30` | Main actions and readable headings |
| Secondary text | `#626B5C` | Supporting copy |
| Border | `#D5D9CC` | Fine dividers |
| Accent | `#DDE9A8` | Small highlights; dark text only |
| Warning | `#8D4D17` | Needs-review and uncertainty copy |

The app ships explicit light and dark tokens and follows the system's light/dark preference. Wallpaper-derived dynamic colors are disabled so they cannot override the forest/ivory identity. Typography uses Android's built-in serif and sans-serif families, requiring no downloads. Nutrition values and connection choices wrap at narrow widths and large text sizes.

## Proposed screens

1. **Meal:** restrained KalC wordmark, provider/status pill, full-width food photo, large estimated calories, aligned macro values, food breakdown, one-tap portion adjustment, and a clearly placed Scan meal action. Label sample content as sample content.
2. **Adjust:** keep the bottom sheet. Food names, grams, and recalculated calories are visible together. Manual add and photo-based sides remain two explicit choices. Confirm detected side dishes before merging them.
3. **Insights:** emphasize two or three relevant observations tied to the current edited meal. Show when an insight is generated versus a local estimate. Never present a clinical diagnosis.
4. **Ask AI:** show current meal context above the conversation, useful suggested prompts, an accessible composer, loading state, and explicit connection errors.
5. **AI connection:** Vertex / On Sundays is the intended cloud route. Show unlocked or locked state and a lock action. The user enters the shared server passkey; no account is needed. AI Studio remains available with its own masked API-key form. Switching providers does not delete the saved Studio key or meal and locks the cloud session. No automatic provider change on errors.

## Required states

- Fresh launch: explain scan or sample meals without pretending a sample is the user's meal.
- Camera/gallery permission denied: offer the supported alternate input.
- Analyzing: real stages with an accessible progress announcement; no fabricated timing.
- Non-food or ambiguous result: actionable retake copy and no fabricated calorie result.
- Locked/expired Vertex session: stop cloud requests and ask the user to sign in again.
- Upstream quota/network failure: clear retry behavior; do not quietly bill another provider.
- Small screens and large text: macros wrap, food labels remain readable, sheets scroll, controls stay reachable.

## Rollout

Version 1.2 applies the theme and native meal header, with the existing editing sheets, insights/chat, and connection forms adopting the same palette/type. A persistent Scan a meal action and gallery shortcut sit above the three navigation tabs. Sample meals stay explicitly labeled. Nutrition models, calculations, AI provider/session logic, and food guardrails are retained. The signed APK uses the same owner certificate as 1.1 so it upgrades in place.

## Prototype checks

The prototype uses the project's existing thali photograph. Meal totals derive from a small explicit sample fixture. Its portion control recalculates calories/macros; tabs and provider previews are interactive. Connection controls only demonstrate presentation and do not collect credentials, authenticate, or call Google.

Verified in a real browser: the photo loads, embedded self-checks pass, portion adjustment updates the totals, all three tabs work, suggested chat prompts update the sample response, and switching providers updates the connection presentation. No console warnings/errors were recorded. At the 360px viewport check the document had no horizontal overflow. A full-page capture is saved in `images/visual-overhaul-preview.png`. Android runtime verification remains separate.
