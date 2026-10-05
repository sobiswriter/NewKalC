# Food Calorie AI — UI Design System & Architecture Specification

> **Version:** 1.0.0  
> **Status:** Active Standard / Single Source of Truth  
> **Target Platform:** Android (Material Design 3 with Jetpack Compose)

---

## 1. Core Philosophy & Navigation Principles

### 1.1 The Golden Rule
> **"Show the user what matters now. Reveal secondary information only when needed. Prefer contextual menus, expandable sections, sheets, and smart navigation over adding more buttons."**

### 1.2 Information Hierarchy & Progressive Disclosure
1. **Primary Focus (What matters now):**
   - The meal image and high-level calorie + macronutrient summary (`Estimated 437 kcal`, `14P · 68C · 13F`).
   - The breakdown of detected items with concise macro previews (`What's in your meal`).
   - Key immediate actions: Adjust meal portions or switch between Meal, Insights, and Ask AI.
2. **Secondary Level (Revealed on intent):**
   - Item details (confidence, visual cues, reference nutrition source) revealed when an item row is tapped.
   - Meal adjustment controls (quantity steppers, grams slider, direct input, remove, add food) contained in a dedicated, distraction-free **Adjust Meal Bottom Sheet**.
   - AI Nutrition Insights (positive highlights, balance concerns, practical suggestions) displayed cleanly below meal data or accessed via quick navigation.
3. **Tertiary Level (On-demand conversational & developer details):**
   - **Ask AI About This Meal:** Persistent compact bottom bar / action button opening a focused full-height or half-height conversational sheet with current meal context.
   - **Developer / Debug Information (Raw JSON, Nutrition Database Explorer):** Accessible via a subtle top-app-bar overflow / details action menu, keeping the main consumer UI uncluttered and clean.

---

## 2. Visual Style & Brand Theme

### 2.1 Aesthetic Direction: "Nourish & Clarity"
- **Tone:** Crisp, organic, clinical-yet-friendly wellness aesthetic. Clean surfaces with emerald accents, warm amber highlights, and soft neutrals that make food imagery pop.
- **Lighting & Depth:** Soft tonal elevations (`tonalElevation = 1.dp` to `3.dp`), gentle borders (`1.dp` solid or subtle alpha), and rounded geometry (`16.dp` to `24.dp` corners).
- **Edge-to-Edge:** Full bleed status bar and navigation bar insets handled cleanly with `WindowInsets.safeDrawing`, `statusBarsPadding()`, and `navigationBarsPadding()`.

### 2.2 Color System (Material 3 Tokens)

| Token Name | Hex Code | Semantic Role |
| :--- | :--- | :--- |
| **Primary (Emerald 40)** | `#059669` | Primary actions, key badges, brand identity |
| **Primary Container** | `#D1FAE5` | Active chips, selected state backgrounds, item highlights |
| **On Primary** | `#FFFFFF` | Text/icons on primary buttons |
| **Secondary (Teal)** | `#0D9488` | Secondary affordances, nutrition badges |
| **Background (Light)** | `#F8FAFC` | Base surface for light theme (clean cool gray-white) |
| **Surface (Light)** | `#FFFFFF` | Card containers and elevated panels |
| **Surface Variant** | `#F1F5F9` | Tonal containers, search fields, disabled chips |
| **Outline / Border** | `#E2E8F0` | Subtle structural dividers and card strokes |
| **Calories Accent** | `#EA580C` | Orange accent for kcal totals and energy tags |
| **Protein Accent** | `#2563EB` | Blue accent for protein grams and ratios |
| **Carbs Accent** | `#CA8A04` | Warm amber accent for carbohydrates |
| **Fat Accent** | `#DC2626` | Crimson/rose accent for healthy/total fats |
| **Confidence High** | `#16A34A` | Green pill badge (Confidence > 80%) |
| **Confidence Medium** | `#D97706` | Amber pill badge (Confidence 50–80%) |
| **Confidence Low** | `#DC2626` | Red/coral pill badge (Confidence < 50%) |

---

## 3. Typography & Text Hierarchy

All text styles scale proportionally with Android accessibility font scale settings (`sp` units only).

| Role | Size | Weight | Line Height | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Display Large** | `32.sp` | Bold (700) | `38.sp` | Collapsing header expanded total calories |
| **Headline Medium** | `22.sp` | SemiBold (600) | `28.sp` | Section headers ("What's in your meal", "AI Insights") |
| **Title Medium** | `16.sp` | SemiBold (600) | `22.sp` | Food item title, bottom sheet titles |
| **Title Small** | `14.sp` | Medium (500) | `20.sp` | Macro labels, chip text, item subtitles |
| **Body Large** | `15.sp` | Normal (400) | `22.sp` | AI insight narratives, chat messages |
| **Body Medium** | `13.sp` | Normal (400) | `18.sp` | Item quantity descriptions, secondary metadata |
| **Label Medium** | `12.sp` | SemiBold (600) | `16.sp` | Macro tag badges (P / C / F), confidence pills |
| **Label Small** | `10.sp` | Medium (500) | `14.sp` | Collapsed header mini-tags, helper annotations |

---

## 4. Spacing, Sizing & Layout Grid

Adhere strictly to an **8.dp base grid** (with occasional 4.dp micro-increments):
- **Micro Spacing:** `4.dp` (icon-to-text spacing, tight badge padding).
- **Small Spacing:** `8.dp` (internal chip padding, horizontal item spacing).
- **Medium Spacing:** `12.dp` – `16.dp` (standard card padding, screen edge margins).
- **Large Spacing:** `20.dp` – `24.dp` (section separators, sheet top headers).
- **Touch Targets:** Minimum interactive area of **48.dp × 48.dp** (`minimumInteractiveComponentSize()`) for all buttons, steppers, and row targets.
- **Max Content Width:** On tablets/foldables, restrict main content to `Modifier.widthIn(max = 640.dp)` centered horizontally.

---

## 5. Component Patterns & Interactive States

### 5.1 Collapsing Food Analysis Header
- **Expanded State:**
  - Displays thumbnail of the captured/selected food image.
  - Large total calorie estimate: `Estimated 437 kcal`.
  - Full macro breakdown row: `14g Protein · 68g Carbs · 13g Fat`.
  - Confidence indicator badge with uncertainty tooltip/expansion: `Medium Confidence (±15%)`.
- **Collapsed State (Triggered on scroll or compact mode):**
  - Sticky or pinned mini-bar showing: `437 kcal · 14P · 68C · 13F` alongside quick action pills.
  - Frees vertical viewport so the user can easily view food items and insights.

### 5.2 Food Item Breakdown Rows ("What's in your meal")
- **Default (Collapsed) Row:**
  - Left: Food name + approximate serving size (e.g. `Steamed Rice (180 g)`).
  - Right: Calorie contribution tag (`234 kcal`) + chevron toggle.
- **Expanded Detail Row:**
  - Smooth vertical expansion (`animateContentSize()`).
  - Shows micro-macro pills: `Protein: 4.8g | Carbs: 50.4g | Fat: 0.6g`.
  - Cooking method inferred from visual cues (e.g., `Steamed`, `Deep fried`).
  - Estimation rationale and confidence level.
  - Quick action: "Adjust" (opens the bottom sheet pre-focused on this item).

### 5.3 Adjust Meal Bottom Sheet (Contained Editing Surface)
- Rather than cluttering the main screen with numerous numeric steppers and sliders, meal editing is contained in a sleek Modal Bottom Sheet.
- **Single-Sheet Navigation Rule:** Never stack multiple `ModalBottomSheet`s on top of each other. When adding food from within the Adjust Meal sheet, transition the internal sheet content inline with a top Back Arrow navigation affordance (`< Add Food to Meal`) rather than opening a second overlapping sheet.
- **Portion Controls Layout Rule:**
  - Stepper `[-] 250 g [+]` must occupy its own dedicated horizontal row with at least 40.dp buttons and a clear centered grams badge.
  - Quick portion preset chips (`50g`, `100g`, `150g`, `200g`, etc.) must be placed on a separate horizontal scrollable row (`horizontalScroll`) to prevent horizontal constraint squishing and clipped text on small mobile screens.
  - Direct item removal via a prominent trash icon on the top right of each item card.
- **Components within Sheet:**
  - Header with live-updating total preview (`Live Total: 520 kcal · 24P · 70C · 18F`).
  - List of current items with portion steppers, quick chips, and remove affordance.
  - **"+ Add Food / Side Dish"** action button opening the inline search view.
  - Primary button: **"Recalculate & Done"** (smoothly closes sheet and updates all dependent state).

### 5.4 Add Food Flow
- Compact search-first interface seamlessly integrated inline within the Adjust Meal sheet (or accessible directly from the main meal header).
- Displays a prominent Back button (`<`) when opened from inside Adjust Meal so users can return to the active meal items list at any time.
- Instant search filtering over verified Nutrition Database entries (e.g., *Raita, Boiled Egg, Roti, Salad*).
- Default serving selection (e.g. `100 g`, `1 piece (50g)`).
- Instant inclusion into the active meal with deterministic nutrition calculation.

### 5.5 Recalculation Engine (Deterministic Rule)
- **Calculation Formula:** `Item Calories = (Weight in grams / 100.0) * CaloriesPer100g`.
- When an item is adjusted, added, or removed:
  1. Active meal model totals are recalculated instantly via local verified Nutrition Database.
  2. The UI state updates reactively without AI latency.
  3. AI Nutrition Insights and Ask AI conversation context receive the updated meal payload.
  4. Zero random number regeneration.

### 5.6 AI Nutrition Insights (Progressive Disclosure)
- Positioned directly beneath the item list or accessible via the "Insights" smart navigation tab.
- **Headline Summary:** One concise takeaway (e.g. `"Balanced carbohydrates with moderate protein"`).
- **Categorized Insight Cards:**
  - 🟢 **Positives:** Nutrient-dense choices, lean proteins, high fiber.
  - 🟡 **Concerns / Notes:** High sodium, cooking oils, saturated fat proportion.
  - 💡 **Actionable Suggestions:** Practical tweaks based on the current meal composition.
- **Expand / Collapse:** Shows the top 2 points by default; "Show more insights" expands remaining items smoothly.
- **Dynamic Context:** Automatically reflects user edits (e.g., if chicken was added, highlights the boost in protein).

### 5.7 Ask AI About This Meal (Contextual Conversational Sheet)
- Accessible via a dedicated smart navigation tab or persistent floating action bar.
- Opens an expandable bottom sheet (from 50% height up to 90% full conversational view).
- **Pre-loaded Context:**
  - Complete edited meal summary: all active items, weights, total calories, macro split, user additions/deletions.
  - Original food image reference.
- **Suggested Quick Prompts (Horizontal Chips):**
  - `"Why is this high in calories?"`
  - `"Is this good post-workout?"`
  - `"What could I add to increase protein?"`
  - `"What contributes most to fat?"`
- Clean message bubbles with typing indicators and Markdown-friendly rendering.

### 5.8 Smart Navigation Bar (Meal | Insights | Ask AI)
- Replaces legacy `Meal | JSON | Database` tabs.
- Clean segmented navigation bar featuring exactly 3 balanced tabs:
  1. **Meal:** The core inspection, image header, and item breakdown.
  2. **Insights:** Direct scroll / focus on the AI nutrition insights and breakdown charts.
  3. **Ask AI:** Instantly opens the conversational meal assistant sheet.
- **Text Wrapping & Density Protection Rule:** Do NOT place auxiliary action buttons (such as "Edit") into the bottom navigation bar row. Squeezing extra buttons shrinks the tab item widths and forces multi-line text wrapping (e.g. "Insight\ns"). The 3 tabs must span the available width with `maxLines = 1` and `softWrap = false`.
- **Details & Developer Tools:** Secondary overflow menu in TopAppBar (`⋮` icon) containing:
  - View Raw JSON
  - Browse Nutrition Database
  - API Key & AI Settings
  - Reset Meal to Original

---

## 6. Motion & Animation Standards

1. **Sheet Animations:** Default spring spec with standard damping (`dampingRatio = 0.85f, stiffness = 400f`).
2. **Expand / Collapse Rows:** `Modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))`.
3. **Macro Progress Bars:** Animated float progress over 450ms when portions change.
4. **Haptic Feedback:** Subtle tactile tick on steppers (`[-]` and `[+]`) and item removal.

---

## 7. Accessibility Checklist (WCAG AA & Android Standards)

- Minimum contrast ratio of 4.5:1 for all text against card/surface backgrounds.
- Explicit `contentDescription` on all icon buttons (`"Decrease portion"`, `"Remove food"`, `"Close sheet"`).
- Test tags (`Modifier.testTag()`) added to all primary interactive elements (`adjust_meal_button`, `add_food_button`, `recalculate_button`, `ask_ai_input`).
- Touch target padding applied to ensure `48.dp` interactive footprints.

---

## 8. Summary of Reusable Component Architecture

- `CollapsingMealHeader`: Adaptive calorie & macro hero card.
- `CompactFoodItemRow`: Expandable individual food breakdown item.
- `AdjustMealBottomSheet`: Dedicated non-scroll-fatiguing editing modal.
- `AddFoodBottomSheet`: Compact searchable nutrition database picker.
- `NutritionInsightsSection`: Progressive disclosure cards for health pointers.
- `AskAiBottomSheet`: Conversational bottom sheet with meal context injection.
- `SmartNavigationBar`: Compact tri-mode switcher (`Meal` | `Insights` | `Ask AI`).
- `MealDetailsMenu`: Overflow menu for developer JSON and reference database.
