# KalC — Make a meal

**Product storyboard and build proposal · 29 October 2026**

> KalC tells you what is on your plate. Make a meal helps you decide what goes on it.

This is a proposed next feature, not an implemented Android release. The app remains version 1.3. The illustrations are generated concepts; the interactive example uses seven original fictional recipes, a local sample pantry, scripted chat and preview timers. It uploads nothing and does not read the real profile.

## The experience in one line

**Show your ingredients → confirm what is there → answer a few useful questions → choose a recipe → cook with a companion.**

I interpreted “23 sections” as the **three** sections described: **Cook now**, **A few extras**, and **Worth the effort**. The third is the requested premium/ambitious category; it is not a proposed paywall.

- [Interactive eight-scene storyboard](index.html)
- [Six-screen illustrated direction](storyboard-illustration.png)
- [Food illustration sheet](food-editorial-contact-sheet.png)
- [Image-generation prompts and provenance](PROMPTS.md)

## The story: a real evening, not a recipe search box

A user opens the fridge and sees tomatoes, an onion, spinach, eggs, cooked chickpeas, rice and yogurt. They have enough food, but no clear idea. They want two portions and an uncomplicated dinner. The application helps turn that uncertainty into a specific, achievable meal.

| Beat | What the person does | What the app shows | What makes it work |
|---|---|---|---|
| **1. Your kitchen** | Opens the separate Make a meal section. | “Good things start here.” Scan ingredients, add photos, or type a list. | The first action is obvious. No forced account, profile completion, or repeated full onboarding. |
| **2. Show what you have** | Takes a collective fridge/counter photo, or adds up to three supporting images. | A photo strip; a close-up can clarify a label or obscured ingredient. | One collection, not unrelated scans. Deduplicate repeated items without confusing two different quantities or raw/cooked states. |
| **3. Confirm first** | Corrects names, amounts, and preparation state. Removes mistakes and adds what is missing. | Editable ingredient rows; “not sure” is allowed. “Chickpeas — cooked or dry?” is an important question. | Photos suggest candidates. The user confirms the usable ingredients and quantities before “ready” claims. |
| **4. A little context** | Confirms pantry staples, cookware, portions, time, style, heat preference and foods to avoid. | Two short question cards; only unresolved questions are asked. Global styles include Indian, Mediterranean, East Asian, Latin American and “Surprise me”. | Never assume oil, salt, spices, an oven, or that one person's preferences represent everyone eating. A style is a preference, not a false authenticity claim. |
| **5. Dinner, decided** | Browses the three sections or taps Quick Grab / Healthy Grab. | A useful set of varied options with real total time, equipment, what is missing, and a short reason it fits. | Availability and quantity rules determine categories. Diversity beats ten almost-identical curries. |
| **6. Your cookbook** | Opens one recipe. | A representative image, flavour description, quantities, required/optional ingredients, servings, equipment, prep/active/total time, skill, missing items, full steps and substitutions. | Complete instructions belong to the selected recipe, rather than generating twelve full cookbooks upfront. |
| **7. Before you start** | Checks the ingredients and gathers the extras. Changes servings if needed. | An exact missing-items list, editable quantities and a deliberate “I have these now” confirmation. | Recheck availability when portions or ingredients change. Do not quietly add an ingredient used only in the method. |
| **8. Cook with company** | Follows large, readable steps and starts relevant timers. | One step at a time, ingredient quantities for that step, next/previous and optional keep-screen-awake. | Cook mode must survive backgrounding/interruption. A timer is not a food-safety guarantee. |
| **9. Ask while cooking** | Asks “Can I make this less spicy?”, “What can replace yogurt?”, or “Is dry chickpea a direct swap?” | Chat knows the chosen recipe version, confirmed pantry, current step, servings and permitted profile context. | Propose changes; confirm them before altering the recipe. Recalculate missing items and estimates after an accepted change. |
| **10. Make it yours** | Finishes, saves a variation, or photographs the actual plate. | “Save my version”, “Make again”, and “See this meal in KalC”. | Close the loop: planned nutrition and the actual cooked plate are different estimates. Do not silently deduct pantry stock until the user confirms they cooked it. |

The prototype has eight scenes because capture is folded into the opening and confirmation screens, and preparation is folded into recipe detail. The illustration highlights six key moments.

## The three sections: rules, not marketing labels

### Cook now

Every required ingredient is present in sufficient confirmed quantity for the selected servings; the necessary cookware is available; explicit food restrictions are satisfied; the recipe fits the ordinary time brief. Optional garnish does not block a recipe, but must really be optional. Water can be explicitly treated as a household resource; spices, oils and other staples require confirmation.

Example: **Smoky chickpea skillet — 18 min, one pan.** With the sample quantities and confirmed cumin, oil and salt, nothing needs buying. “Cook now” describes ingredient/brief readiness, not verified freshness or allergy safety.

### A few extras

An otherwise suitable everyday recipe needs a small shopping list, ideally one or two extra ingredients. Name those items and their amounts on the card, not after the user has committed to cooking.

Example: **Coconut chickpea curry — add 200 ml coconut milk and 10 g ginger.** Confirming those extras moves this recipe into Cook now when all other requirements are met. If the user has only 100 g chickpeas but the recipe needs 200 g, the missing list must say **100 g more chickpeas**.

### Worth the effort

More involved methods or a longer total time, often with a longer ingredient list. A grocery shortage alone does not make an easy recipe a premium cooking project. Show time, active effort and equipment honestly. These are visible as deliberate longer projects, outside a short everyday brief; they must never win Quick Grab.

Example: **Weekend vegetable dum biryani — about 75 min in the illustration**, extra onion, mint, saffron, ghee and ginger in the fictional sample. The final product needs tested recipe timings and methods; the storyboard is not a validated kitchen recipe.

**Classification order:** reject constraint conflicts → account for portions and state → compute shortages → identify longer/advanced projects → few-extras vs ready. The model must not assign its own unsupported “100% match” score. If there is no honest Cook now option, say so and offer specific next choices.

## Quick Grab and Healthy Grab

**Quick Grab** means the shortest **total elapsed time**, including preparation, among eligible ready options. It cannot assume leftover rice, a preheated oven, soaked beans or pre-chopped vegetables unless confirmed. Explain the important assumption beside the pick. With the fictional sample, the eight-minute salad assumes ready-to-eat cooked chickpeas. Selecting four portions changes the stock check; a 200 g tin is not automatically enough for every recipe for four.

**Healthy Grab** means a balanced, suitable suggestion for the user's explicitly chosen context. It is not simply the lowest-calorie meal, a numerical health score, a diagnosis, or a guaranteed weight-loss outcome.

- **Profile OFF:** offer a clearly labeled general balanced pick and an optional route to Profile. No saved body details, goals or health notes are read into the AI request.
- **Profile ON:** the new feature must explain the new recipe-suggestion purpose and ask whether to use the enabled profile for this cooking session. The Profile switch remains the master control; a session choice cannot enable it.
- Use the minimum relevant fields: approved goal, dietary preferences and relevant confirmed restrictions. Do not dump the user's full health/body profile into ingredient recognition. The name stays local.
- Today's explicitly entered food restrictions apply independently of stored-profile personalization and are disclosed as part of the recipe request. Ask about the people actually eating.
- Turning the master switch off invalidates personalized recommendation/chat results and prevents new stored-profile context. Keep the chosen recipe unchanged; do not introduce new ingredients during that transition.

The demo uses an **egg-free preference only**, not actual health information. This is intentionally separate from the real saved profile.

## What belongs on a proper recipe page

1. Dish name, short description, adapted/inspired style where appropriate, and an honestly labeled representative image.
2. Why it is suggested: confirmed ingredients used, extra items, active brief match, and permitted preference match.
3. Servings, prep time, cooking/rest time, **total elapsed time**, active effort, skill and equipment.
4. Exact required ingredients, quantities and states; optional ingredients listed separately. Units are normalized and original display units preserved.
5. An exact shortages/shopping list for this recipe. Shopping is not the same as available stock.
6. A full method, with referenced ingredients and quantities, sequence, useful cues, and vetted safety notes where relevant.
7. Per-serving nutrition **estimates** from the app's ingredient database and confirmed quantities, with uncertainty. Never ask the language model to invent precise totals or present goals as medical prescriptions.
8. Explicit allergens/avoidances derived from the complete ingredient list, plus label/cross-contact checks where needed. An image is not proof that a food is safe for an allergy.
9. Accepted substitutions, their consequences, and recipe version history. Never say a substitution changes nothing when it changes flavour, method, time, nutrition or allergens.
10. Start cooking, ask about this recipe, save my variation and make again. Save is a user action, not implicit profiling.

## Ideas I would add

### Include early

- **Why this recipe?** A short factual explanation: “Uses your spinach, chickpeas and tomatoes; no extra shopping.” This makes recommendations understandable.
- **One exact shopping list.** Only for the selected recipe, with quantities and a “got it” check. No retailer or purchasing integration is needed.
- **Cook mode with interruption recovery.** A person's attention is on a hot pan. Recover the step and real timer state, and never block the saved recipe behind a fresh AI response.
- **Cook → KalC.** A finishing action opens the existing meal scanner for the actual plate.

### The next layer of delight

- **Use it first:** prioritise ingredients the user explicitly marked as needing use soon. Do not guess expiry or freshness from appearance.
- **Rescue mode:** a context-aware “too dry / too salty / what now?” question entry. Some mistakes need stopping or discarding, not an unsafe promise to rescue everything.
- **My version:** save the successful variation and the user's own note, then make it again without repeating the questionnaire.
- **Cook from leftovers:** explicitly distinguish cooked leftovers from raw ingredients and apply appropriate storage/handling guidance.
- **Global pantry aliases:** ingredient synonyms, metric/household measures and adapted styles without pretending every dish is culturally authentic.
- **Guest mode:** temporary food avoidances for this meal, with no new health profile or account for a guest.

### Leave for later

Price promises, grocery checkout, live video monitoring, voice listening, nutrition prescriptions, automatic pantry depletion, social feeds and a paid recipe tier. These multiply integration, privacy and reliability work without improving the first successful dinner.

## The guardrails that protect the experience

- Ingredient scanning gets its own typed contract. Reuse the selected Google provider and authentication, but do not route raw ingredients through the cooked-meal nutrition parser or weaken KalC's existing non-food gate.
- Non-food/empty/unclear images get an honest correction path. Sealed packaging, hidden food, labels and quantities require confirmation when unreadable or uncertain. No face/body photo is silently treated as ingredients.
- Preserve dry/raw/cooked states and quantities. Merge duplicate views carefully; never count the same carton twice because it appeared in two photos.
- Allergens and explicit avoidances are hard constraints. Low latency or a high match score must never override them. Checking ingredient labels remains necessary for food-allergy decisions. [FDA guidance](https://www.fda.gov/consumers/consumer-updates/have-food-allergies-read-label)
- Use reviewed ingredient-specific temperature/handling guidance in cooking steps. A photo, visual cue or timer must not certify safe cooking. [FoodSafety.gov](https://www.foodsafety.gov/food-safety-charts/safe-minimum-internal-temperatures)
- Model output is untrusted. Bound fields, validate schema/IDs/units, validate required-step references, reject contradictions, and compute matching and totals in code.
- Keep recipe generation on Gemini 3.8 Flash with the current selected-provider rules. No silent billing/provider/model fallback.
- Photos and pantry data go to the chosen AI provider only for the requested feature. Profile context is a separate opt-in, and sensitive notes are never included in logs.
- Free-text package labels, user notes and chat are context, not instructions granting tools, different upstream URLs or credential access.
- Food images are owned/licensed illustrations from an approved asset catalogue in the first release, with a neutral fallback. Gemini 3.8 Flash is used for text/vision; it is not assumed to provide a new runtime image-generation service. New per-recipe image generation would require a separate explicit design/cost decision.

## Make it fast without making things up

1. One bounded ingredient-recognition call for the selected images. Enforce the existing payload budget; multiple photos must not exceed it.
2. Ask the remaining questions locally from the returned uncertainties. **No AI round trip per checkbox.**
3. Generate a compact first set of roughly 6–9 diverse validated summaries within the current output cap; offer another batch when requested. Target 9–12 useful ideas overall rather than risking truncated full cookbooks. The prototype uses seven examples.
4. Fetch/generate the full selected recipe on demand. Keep images off the critical path and never pre-generate expensive images for discarded options.
5. Cache validated detail and preserve cooking progress locally with explicit controls. Recalculate quantity matching immediately in code when servings change.
6. Bind every result to pantry, brief, recipe and permitted-profile revisions. Cancel obsolete work and ignore stale results, as the current profile feature already does.
7. Retry only deliberately and stay on the selected provider/model. Recover from invalid/truncated structured output with a clear retry or smaller batch; do not show half a recipe as complete.
8. Measure scan-to-confirm, brief-to-first-valid-results, selected-recipe load, app responsiveness and interruption recovery. Set performance budgets after a working baseline; do not promise a fixed Google response time.

## Navigation proposal

Use three top-level destinations: **KalC · Make a meal · You**. Inside KalC, keep meal analysis, insights and its meal chat together. Inside Make a meal, keep pantry → recipe board → cookbook → cooking chat together. The You page owns the existing profile and privacy settings. This avoids a fifth competing bottom tab and keeps the two AI chat contexts distinct.

Do not ask users to re-enter their profile. Session questions can be skipped/reused only when the data is still explicit and appropriate for this meal; new photos and dietary constraints always need review where changed.

## Build in thin, reviewable slices

These are proposed tasks for the new feature, not a replacement for the existing `tasks/plan.md` or its remaining device-verification tasks.

| Slice | Acceptance criteria (max three) | Likely code boundary | Verification / dependencies |
|---|---|---|---|
| **A. Domain and matching** | Canonical ingredients retain state/units; quantities scale correctly; category/constraint/shortage rules are deterministic. | New recipe models, matcher, focused tests. | Pure tests: two vs four portions, duplicates, raw/cooked mismatch, optional garnish, unknown quantity. No AI dependency. |
| **B. Typed AI transport** | Both providers use existing authorization; new operations keep 3.8 Flash; legacy four AI flows stay working. | Existing request helper reused by typed ingredient/recipe operations; extract a small shared transport only if needed by separate services. | Existing transport/auth tests plus new contract fixtures. Depends on A. |
| **C. Ingredient capture** | Collective or up to three supporting photos and manual entry work; ambiguous items stay editable; confirmed quantities are required for ready claims. | Ingredient scan service, confirmation screen and dedicated MakeMealViewModel. | Non-food, cluttered photo, unreadable packet, duplicate photos, permission denial. Depends on A/B. |
| **D. Brief and consent** | Staples/equipment are not assumed; time/servings/global style and avoidances are explicit; new feature consent respects the existing master profile setting. | Brief screen/model and purpose-limited profile-context builder. | Default-off and global-off payload tests; session avoidance tests. Depends on A/C. |
| **E. Recipe board** | Valid summaries populate the three groups; missing quantities update instantly; Quick/Healthy do not bypass constraints or consent. | Recipe recommendation service, result screen, matcher integration. | Invalid/truncated JSON, zero options, revised brief and stale-response tests. Depends on A/B/D. |
| **F. Selected cookbook** | Full steps and required ingredients agree; owned imagery has a fallback; quantities and estimated nutrition track portions. | Recipe detail service, validator and detail screen. | Ingredient-reference, missing-item, unavailable-image and scaling tests. Depends on E. |
| **G. Cooking session** | Steps/checklists persist locally; real timers survive backgrounding; offline detail remains accessible once saved. | Cooking screen and session repository/timer integration. | Background/restart, denied notifications, screen sizes, large text, TalkBack. Depends on F. |
| **H. Cooking chat** | Chat uses the selected recipe/step/brief; replacements require confirmation; profile-off and recipe revision changes invalidate stale context. | Separate recipe-chat state using the selected transport. | Swap/allergy conflicts, wrong recipe, stale result, quota and locked-server tests. Depends on B/D/F/G. |
| **I. Finish and polish** | Completed plate can open KalC; save-my-version is explicit; the signed update preserves current user settings and profile consent. | Navigation, selected-recipe saving and documentation. | Full journey, regression suite, release install and real-device camera/timer check. Depends on preceding slices. |

**Review checkpoint after D:** prove capture → confirmation → brief and consent before spending time on recipe polish. **Checkpoint after F:** prove quantities, matching and complete instructions before timers/chat. A signed build and real-device cooking rehearsal come before claiming the feature is ready.

## Storyboard verification

The model includes self-checks for fastest eligible pick, balanced pick, extra-to-ready movement, demo profile avoidance, cooked-vs-dry state, quantity shortages for four people, an impossible time budget and unconfirmed inventory. The interactive page and browser checks are evidence for the proposal's mechanics, not Android/runtime AI or food-safety validation. Recipe descriptions, method text and timings in this concept are original fixtures and not kitchen-tested.

Verified on 29 October 2026:

- JavaScript syntax checks and the model's assert-based self-check passed.
- Browser walkthrough covered capture/confirmation, pantry questions, brief, recipe board, cookbook, cooking steps and contextual sample chat.
- The default pantry yields three ready recipes, two needing extras and two cooking projects. Adding coconut milk and ginger moves the curry into Cook now.
- Quick Grab selects the eight-minute bowl; Healthy Grab selects the sample balanced skillet with personalization off. Four portions plus the demo egg-free preference correctly leave no ready pick and explain the shortage.
- The standalone layout was inspected at 360px and normal panel width with no horizontal overflow or broken images. Browser warnings/errors were empty during the checks. `prototype-browser-check.jpg` records the rendered recipe board.
- The inline recipe board was checked with restored interaction state and updated quantities/preferences. All data remains fictional and local.

These checks do not cover Android camera permissions, real AI output, persistent timers, real-device accessibility or recipe execution. Those remain acceptance work in the proposed build slices.

## Decisions to make together

My recommended starting choices are: **three top-level destinations**, **three recipe groups**, **a compact first batch plus more ideas**, **licensed/owned illustrations first**, and **a complete simple dinner before advanced automation**. The main visual alternative worth testing later is a returning-user pantry-first home versus the guided first-use flow shown here.
