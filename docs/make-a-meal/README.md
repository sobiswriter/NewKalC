# Make a meal — review package

Open `index.html` in a modern browser. It works from this folder with no install, network APIs or build step. Keep the CSS, JS and images together.

- `PRODUCT_STORYBOARD.md`: full product narrative, matching rules, personalization boundaries, ideas and proposed build slices.
- `storyboard-illustration.png`: six-screen generated concept board.
- `food-editorial-contact-sheet.png`: generated illustrative food imagery.
- `PROMPTS.md`: exact art prompts, references and review notes.
- `model.js`: fictional sample catalogue and deterministic availability/quantity rules.
- `storyboard.js` / `storyboard.css`: eight interactive scenes.
- `index.html#km-history-plan`: static planning panels for local scan history, confirmed cooked recipes and optional finished-plate reviews. No history or review implementation is included.
- `prototype-browser-check.jpg`: rendered browser view from verification; the checks and remaining limits are in the full product plan.

Try these:

1. Scan the sample pantry → confirm → pantry questions → brief → recipes.
2. Quick Grab opens the fastest eligible ready recipe (the sample 8-minute salad).
3. Add coconut milk and ginger in Pantry check: the curry moves from A few extras to Cook now.
4. Change to four people: quantities and categories change.
5. Unconfirm cooked chickpeas or apply egg-free/dairy-free choices: incompatible options disappear.
6. Healthy Grab stays general with personalization off; demo personalization only simulates an egg-free preference.
7. Open a recipe, check ingredients, enter cooking mode and try the example chat prompts.

This is a concept, not an APK or a live AI service. The camera, chat and timer are demonstrations. Recipes and timings are fictional original fixtures and are not kitchen-tested. The native implementation is documented separately in `../MAKE_A_MEAL_RELEASE_1_4.md`; these storyboard files remain sample-data design material.

The browser/server preview in the Codex task is loopback-only; this folder remains usable if that preview stops.
