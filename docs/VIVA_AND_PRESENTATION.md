# KalC — viva and presentation guide

Use this alongside the [README technical explanation](../README.md#deep-learning-and-project-architecture). The aim is to explain the implemented system and distinguish verified behavior from assumptions. The outline below is a speaking aid, not a claim of model training or measured nutritional accuracy.

## A clear opening

KalC is an Android application that uses a pretrained multimodal model to interpret meal and ingredient photos. It combines those predictions with nutrition calculations, food-validation rules and a guided cooking workflow. Users can correct portions, generate recipes from confirmed ingredients, and revisit scans and cooked meals locally. Personalization is optional.

## Suggested presentation — 8 to 10 minutes

| Slide | What to show | What to explain |
| --- | --- | --- |
| 1. Problem and objective | A meal photo and the app's two main sections | Manual food logging takes effort; available ingredients also need to become practical meal ideas. The project addresses both tasks. |
| 2. User journey | Scan → review → adjust; ingredients → recipes → cook | Show what the user gains before explaining implementation. |
| 3. Deep learning approach | Image + prompt → pretrained Gemini → structured fields | Explain multimodal inference, pretrained representations and prompt design. Distinguish inference from training. |
| 4. Architecture | The README architecture diagram | Trace Studio versus protected Vertex, response validation, local arithmetic and persistence. |
| 5. Guardrails | Non-food/unclear examples and parser checks | Explain early heuristics, the food flag, confidence threshold and rejection. Rules reduce errors without guaranteeing perfect detection. |
| 6. Nutrition | The 180g rice worked example | Separate uncertain recognition/weight/reference inputs from deterministic multiplication. Demonstrate a portion edit. |
| 7. Make a meal | Pantry, recipe groups, cookbook and preparation checklist | Explain model-generated candidates plus local ingredient/state/equipment constraints and confirmed cooking history. |
| 8. Privacy and access | Provider settings, personalization switch and history | Explain memory-only Vertex sessions, own-key Studio, consent and local storage. Never show credentials. |
| 9. Testing and limitations | Latest documented screen matrix and test counts | Report functional checks as functional checks; explain that recognition accuracy and handset certification are separate. |
| 10. Conclusion and future evaluation | Current outcome and a small evaluation plan | State what works, what remains uncertain and how it could be measured. |

For a shorter slot, combine slides 2/7, 3/4 and 8/9. Use native app screenshots from the README and release docs. Label synthetic test fixtures when presenting them.

## Demo sequence

Before the presentation, install the signed APK, check the network, unlock the selected provider privately and prepare sample images. Keep screenshots and a saved recipe available as a backup. Saved-history browsing works offline; a saved result is not a fresh recognition demo.

1. Scan a clear food photo and explain the items, estimated weights and nutrition sources.
2. Adjust one portion and show the recalculated total.
3. Try a non-food image and show the rejection. Explain that one example is a demonstration, not a measured false-positive rate.
4. Open Make a meal, confirm ingredients and show the brief and recipe groups. If using a previously saved recipe to save time, say so.
5. Open the cookbook, confirm preparation requirements and begin cooking. Advance through the demo steps; explain that a simulated completion is not evidence that the recipe was physically cooked or tested.
6. Reopen saved history. Show the optional personalization switch without exposing personal health notes.

## Likely viva questions

### Where is the deep learning in this project?

Gemini supplies the pretrained image-and-language inference used for food identification, ingredient extraction and recipe/advice generation. The app supplies prompts, transport, validation, UI, calculations, local constraints and persistence. There is no custom neural-network training loop in this repository.

### Did you build or train Gemini? What dataset did you use?

No. Google supplies the pretrained model. This project does not provide its training dataset, weights or a fine-tuned checkpoint. Local nutrition references, saved history and demo images have different purposes; they are not a training dataset. The project should be presented as applied deep learning and system engineering.

### Why use a pretrained multimodal model?

It accepts photo and text inputs through one interface and supports recognition and conversational tasks. This avoids building a food-specific training pipeline for this implementation. The trade-offs are provider dependence, network latency, usage costs, privacy boundaries and limited visibility into the model's internals.

### How does attention work?

At a conceptual level, attention computes how strongly one representation should consider other representations when producing context-aware output. The original transformer paper describes query/key/value attention. This explains the general concept; the project does not reproduce or inspect Gemini's internal architecture. See [Attention Is All You Need](https://arxiv.org/abs/1706.03762).

### Is prompt engineering the same as training?

No. A prompt supplies instructions and context at inference time. Training changes model parameters through optimization. KalC changes prompts and validates outputs without updating Google's model weights.

### Is this segmentation, object detection or classification?

The meal flow requests structured food identities and portions from an image. It does not implement a dedicated pixel-mask segmenter or trained bounding-box detector. Multiple food items in a response should not be described as proof of pixel-level segmentation.

### How can a flat photo provide grams?

The model estimates portions from visible appearance, containers and packaging context. There is no direct weight measurement, calibrated depth reconstruction or weighing sensor. Users can correct grams, and hidden ingredients or perspective can produce errors.

### Why not ask the model for the final calorie total?

The app uses explicit arithmetic from weights and nutrition references so totals respond consistently to edits and are traceable. Recognition, grams and unmatched reference values can still be wrong; arithmetic consistency does not establish nutritional accuracy.

### Is every nutrition value USDA verified?

No. The app has a local reference catalogue containing different source labels, and unmatched foods may use model-supplied per-100g references. It does not query USDA live. Avoid describing the whole catalogue as independently verified or every result as exact.

### What does a confidence score mean?

It is model-reported confidence used by an application threshold, default 0.65 for meal validation. It is not a measured accuracy percentage or a calibrated probability. Establishing calibration requires a separate labeled evaluation set.

### What if the response is valid JSON but contains nonsense?

JSON formatting only makes the response parseable. Application validation checks required fields and acceptance rules, and kitchen matching checks local constraints. Semantic errors can still remain, which is why user review and explicit uncertainty matter. Google's [structured output guide](https://ai.google.dev/gemini-api/docs/structured-output) describes formatting support; it does not replace application validation.

### Why are there two Google connection options?

Studio lets users supply their own key. Vertex routes requests through a backend in the owner's Cloud project after passkey verification. Both select the same configured model; the difference is authentication, request routing and who owns the provider usage.

### How is the server protected?

The server verifies the shared passkey and issues a signed, expiring session token. Generation requires a valid token. The app keeps the token in memory and does not persist the raw passkey. Backend request bounds and throttling reduce abuse; they are not a hard billing cap. See the [server guide](../server/README.md).

### How are the recipe groups determined?

AI proposes recipe candidates, while local matching checks confirmed ingredients, amounts, compatible units/states, equipment and avoidances. Unknown amounts do not establish readiness. The preparation checklist resolves required items before cooking begins; generated suggestions alone do not prove that everything is available.

### Does personalization train a model on the user?

No. When enabled for the relevant purpose, permitted profile context is included in requests. It is inference-time context, not local model training. Personalization defaults off and the kitchen has a separate session choice.

### What works offline?

Saved history, saved recipe details and local calculations can be accessed without a fresh AI call. New image recognition, generated recipes and AI conversation require the selected provider. Demo samples should be identified as preset examples.

### What did the tests prove?

The latest release records 35 unit/Robolectric passes, zero release-lint errors and 20 distinct emulator layout/flow combinations across successful runs. These check application behavior and representative UI layouts. They do not prove clinical nutrition accuracy, every phone's compatibility or recipe quality. See [the exact results and limits](COOKBOOK_1_4_2.md).

## How to evaluate it academically

This is a proposed evaluation plan, not completed experimental results:

| Area | Method | What to report |
| --- | --- | --- |
| Food/non-food gate | Label diverse food, non-food and ambiguous images; keep evaluation examples separate from prompt-tuning examples. | Precision, recall, false accepts/rejects and sample counts. |
| Food identity | Compare accepted item predictions with annotated visible foods. | Defined matching rules, per-category results and failure examples. |
| Portion and energy estimates | Use weighed portions and documented reference values, including mixed dishes. | Mean absolute gram/kcal error, relative error where meaningful and uncertainty sources. |
| Confidence | Compare score ranges with observed correctness on labeled examples. | Calibration plot or binned correctness; do not equate confidence directly with accuracy. |
| Recipe readiness | Test missing quantities, raw/cooked differences, units, equipment and dietary exclusions. | False-ready cases and constraint-test results. |
| Latency | Repeat cold/warm requests under documented device/network conditions. | Median and tail latency, request count, failures and provider conditions. |
| Usability | Observe users completing scan, portion correction and start-cooking tasks. | Completion rate, time, errors and readability feedback. |

Do not invent an accuracy percentage, training loss, dataset size or speed guarantee. A future custom classifier, calibrated portion model, audited catalogue or wider device study must be labeled future work until implemented and evaluated.
