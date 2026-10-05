package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AmbiguousFoodException
import com.example.data.model.ChatMessage
import com.example.data.model.DetectedFoodItem
import com.example.data.model.FoodDetectionSource
import com.example.data.model.MealAnalysisResult
import com.example.data.model.NonFoodException
import com.example.data.model.NutritionInsights
import com.example.data.nutrition.NutritionDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class GeminiFoodService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Primary model as requested: gemini-3.8-flash with fallback options
    private val candidateModels = listOf(
        "gemini-3.8-flash",
        "gemini-flash-latest",
        "gemini-3.1-flash-lite-preview"
    )

    /**
     * Converts Bitmap to Base64 JPEG string, resizing if too large to ensure fast network payload.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1024
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Quickly verifies if a Google AI Studio API key is valid by sending a minimal test prompt.
     */
    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }

        val testModels = listOf("gemini-3.8-flash", "gemini-flash-latest")
        var lastErrorMsg = "Failed to connect"

        for (modelName in testModels) {
            try {
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", "ping")
                                })
                            })
                        })
                    })
                }

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$cleanKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    return@withContext Result.success(true)
                } else {
                    lastErrorMsg = try {
                        val root = JSONObject(responseBody)
                        root.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}"
                    }
                    Log.w("GeminiFoodService", "Validate API Key test with $modelName failed: $lastErrorMsg")
                }
            } catch (e: Exception) {
                lastErrorMsg = e.message ?: "Network error"
            }
        }

        Result.failure(Exception("API Key validation failed ($lastErrorMsg)"))
    }

    companion object {
        const val DEFAULT_FOOD_VALIDATION_THRESHOLD = 0.65
    }

    /**
     * Analyzes a food image using the updated Gemini Vision model (gemini-3.8-flash)
     * with a mandatory FOOD VALIDATION GATE to reject non-food and ambiguous images.
     */
    suspend fun analyzeFoodImage(
        bitmap: Bitmap,
        sampleContextHint: String? = null,
        providedApiKey: String? = null,
        validationThreshold: Double = DEFAULT_FOOD_VALIDATION_THRESHOLD
    ): Result<MealAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = (providedApiKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.GEMINI_API_KEY.trim())

        // Check if API key is valid or missing
        val isRealKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!isRealKey) {
            if (sampleContextHint != null) {
                Log.w("GeminiFoodService", "No custom GEMINI_API_KEY provided; using deterministic vision fallback for sample preset")
                return@withContext Result.success(getDeterministicFallbackAnalysis(sampleContextHint))
            } else {
                return@withContext Result.failure(
                    IllegalStateException("Google AI Studio API Key required. Please add your API key in the top settings to analyze custom images.")
                )
            }
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
You are a specialized food identification and nutrition analysis engine with a STRICT food-validation gate.

STEP 1: STRICT FOOD VALIDATION GATE (Mandatory)
Examine the image carefully:
- Does this image clearly depict recognizable FOOD, a MEAL, BEVERAGE, or EDIBLE INGREDIENTS?
- If the image contains a PERSON (selfie, face, human portrait, body, hands without food), ANIMAL/PET, OBJECT, COMPUTER, FURNITURE, LANDSCAPE, SCREENSHOT, or anything that is NOT food:
  Set "is_food": false, "food_confidence": 0.0, "detected_foods": [], and specify "rejection_reason" (e.g. "Human photo detected - no food").
- If the image is blurry, ambiguous, or food cannot be recognized with reasonable confidence (>= $validationThreshold):
  Set "is_food": false, "food_confidence": <score>, "detected_foods": [], and specify "rejection_reason" ("Ambiguous or unclear food").
- ONLY if clear food is identified, set "is_food": true and "food_confidence" to your confidence score.

STEP 2: FOOD IDENTIFICATION & PORTION ESTIMATION (ONLY IF is_food IS TRUE)
If is_food is true:
- Identify every distinct, recognizable food item visible on the plate/bowl/table.
- For each item determine:
   - food name
   - approximate quantity description (e.g. "1 medium bowl", "1 fillet", "1 cup")
   - estimated portion weight in grams (use visual cues: plate size ~26cm, bowl depth, food volume, density)
   - cooking method when visually inferable (e.g. steamed, pan-seared, stir-fried, dum-cooked, deep-fried, raw)
   - confidence score between 0.0 and 1.0
   - visual cues used for portion estimation
   - estimated reference calories per 100g
   - estimated reference protein per 100g
   - estimated reference carbs per 100g
   - estimated reference fat per 100g
- Calculate overall meal confidence (0.0 to 1.0).
- Provide an uncertainty explanation.

If is_food is false, "detected_foods" MUST be an empty array [].

Respond strictly with valid JSON without markdown fences matching this schema:
{
  "is_food": true,
  "food_confidence": 0.85,
  "rejection_reason": null,
  "detected_foods": [
    {
      "name": "Chicken Biryani",
      "quantity_description": "1 medium bowl",
      "estimated_weight_grams": 250.0,
      "cooking_method": "Dum-cooked with spices",
      "confidence": 0.81,
      "visual_cues": "Plate diameter ~26cm, bowl depth ~4cm",
      "ref_calories_per_100g": 200.0,
      "ref_protein_per_100g": 8.8,
      "ref_carbs_per_100g": 23.2,
      "ref_fat_per_100g": 7.6
    }
  ],
  "overall_confidence": 0.81,
  "uncertainty_explanation": "Portion size is the largest source of uncertainty."
}
""".trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            var lastError: Exception? = null

            // Try candidate models in order: gemini-3.8-flash first!
            for (modelName in candidateModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                    val request = Request.Builder()
                        .url(url)
                        .post(requestJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        Log.d("GeminiFoodService", "Successfully analyzed with model $modelName")
                        val parsedMeal = parseGeminiResponse(
                            responseJsonString = responseBody,
                            validationThreshold = validationThreshold,
                            source = FoodDetectionSource.ORIGINAL_IMAGE
                        )
                        return@withContext Result.success(parsedMeal)
                    } else {
                        val errMsg = try {
                            val root = JSONObject(responseBody ?: "")
                            root.optJSONObject("error")?.optString("message") ?: "API Error (${response.code})"
                        } catch (_: Exception) {
                            "API Error (${response.code})"
                        }
                        Log.w("GeminiFoodService", "Model $modelName failed: $errMsg. Trying next candidate...")
                        lastError = Exception(errMsg)
                    }
                } catch (e: Exception) {
                    Log.w("GeminiFoodService", "Exception with model $modelName: ${e.message}")
                    lastError = e
                    // If it's a validation exception (non-food/ambiguous), don't retry with other models, fail fast!
                    if (e is NonFoodException || e is AmbiguousFoodException) {
                        return@withContext Result.failure(e)
                    }
                }
            }

            Result.failure(lastError ?: Exception("Failed to analyze image with Gemini AI."))
        } catch (e: Exception) {
            Log.e("GeminiFoodService", "Exception during Gemini analysis: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Analyzes an additional food/side dish image (e.g. raita, papad, salad).
     * Runs through the strict food validation gate and returns detected items for user verification.
     */
    suspend fun analyzeAdditionalFoodImage(
        bitmap: Bitmap,
        providedApiKey: String? = null,
        validationThreshold: Double = DEFAULT_FOOD_VALIDATION_THRESHOLD
    ): Result<List<DetectedFoodItem>> = withContext(Dispatchers.IO) {
        val apiKey = (providedApiKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.GEMINI_API_KEY.trim())
        val isRealKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!isRealKey) {
            // In demo mode without API key, return verified side dish preset
            val raitaRef = NutritionDatabase.resolveNutrition("Cucumber Raita")
            val papadRef = NutritionDatabase.resolveNutrition("Roasted Papad")
            return@withContext Result.success(
                listOf(
                    DetectedFoodItem(
                        name = "Cucumber Raita",
                        quantityDescription = "1 small bowl (~100g)",
                        weightGrams = 100.0,
                        cookingMethod = "Whisked spiced yogurt with cucumber",
                        confidence = 0.90,
                        visualCues = "Side bowl diameter ~8cm",
                        nutritionReference = raitaRef,
                        source = FoodDetectionSource.IMAGE_ADDITION
                    ),
                    DetectedFoodItem(
                        name = "Roasted Papad",
                        quantityDescription = "1 crisp disc (~15g)",
                        weightGrams = 15.0,
                        cookingMethod = "Dry flame roasted",
                        confidence = 0.85,
                        visualCues = "Thin crispy round wafer ~15cm",
                        nutritionReference = papadRef,
                        source = FoodDetectionSource.IMAGE_ADDITION
                    )
                )
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
You are a specialized side dish food identification engine with a STRICT food-validation gate.

STEP 1: STRICT FOOD VALIDATION GATE (Mandatory)
Examine the image carefully:
- Does this image contain recognizable, edible FOOD, SIDE DISH, BEVERAGE, or SNACK?
- If the image contains a PERSON, selfie, face, body, animal, pet, object, document, or non-food:
  Set "is_food": false, "food_confidence": 0.0, "detected_foods": [], and specify "rejection_reason".
- If ambiguous or food cannot be recognized with reasonable confidence (>= $validationThreshold):
  Set "is_food": false, "food_confidence": <score>, "detected_foods": [], and specify "rejection_reason".

STEP 2: SIDE DISH IDENTIFICATION (Multiple items supported!)
Identify every distinct food item visible in this side dish photo (e.g. raita, pickle, papad, fries, bread, chutney).
For each item estimate portion weight in grams, quantity description, and cooking method.

Respond strictly with valid JSON without markdown fences matching this schema:
{
  "is_food": true,
  "food_confidence": 0.88,
  "rejection_reason": null,
  "detected_foods": [
    {
      "name": "Cucumber Raita",
      "quantity_description": "1 small bowl (~100g)",
      "estimated_weight_grams": 100.0,
      "cooking_method": "Whisked yogurt with cucumber",
      "confidence": 0.88,
      "visual_cues": "Side bowl diameter ~8cm",
      "ref_calories_per_100g": 60.0,
      "ref_protein_per_100g": 3.0,
      "ref_carbs_per_100g": 4.5,
      "ref_fat_per_100g": 3.2
    }
  ]
}
""".trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            for (modelName in candidateModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val parsedMeal = parseGeminiResponse(
                            responseJsonString = responseBody,
                            validationThreshold = validationThreshold,
                            source = FoodDetectionSource.IMAGE_ADDITION
                        )
                        return@withContext Result.success(parsedMeal.foods)
                    }
                } catch (e: Exception) {
                    if (e is NonFoodException || e is AmbiguousFoodException) {
                        return@withContext Result.failure(e)
                    }
                }
            }

            Result.failure(Exception("Failed to analyze side dish image."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses the JSON from Gemini, separating vision food identification from the Nutrition Database.
     * Enforces the Food Validation Gate to reject non-food and ambiguous images before nutrition calculation.
     */
    private fun parseGeminiResponse(
        responseJsonString: String,
        validationThreshold: Double = DEFAULT_FOOD_VALIDATION_THRESHOLD,
        source: FoodDetectionSource = FoodDetectionSource.ORIGINAL_IMAGE
    ): MealAnalysisResult {
        val root = JSONObject(responseJsonString)
        val candidates = root.optJSONArray("candidates")
        val candidate = candidates?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: "{}"

        val cleanJson = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsedObj = JSONObject(cleanJson)

        // 1. FOOD VALIDATION GATE (Mandatory)
        val isFood = parsedObj.optBoolean("is_food", true)
        val foodConfidence = parsedObj.optDouble("food_confidence", 1.0)
        val rejectionReason = parsedObj.optString("rejection_reason", "")

        if (!isFood) {
            val lowerReason = rejectionReason.lowercase()
            if (lowerReason.contains("ambiguous") || lowerReason.contains("unclear") || lowerReason.contains("confidence")) {
                throw AmbiguousFoodException(
                    message = "Couldn't confidently identify the food. Please upload a clearer image showing the food.",
                    confidence = foodConfidence
                )
            } else {
                throw NonFoodException(
                    message = "Please provide a clear image of food or a meal to analyze."
                )
            }
        }

        if (foodConfidence < validationThreshold) {
            throw AmbiguousFoodException(
                message = "Couldn't confidently identify the food. Please upload a clearer image showing the food.",
                confidence = foodConfidence
            )
        }

        val foodsArray = parsedObj.optJSONArray("detected_foods") ?: JSONArray()
        if (foodsArray.length() == 0) {
            throw NonFoodException(
                message = "Please provide a clear image of food or a meal to analyze."
            )
        }

        val overallConfidence = parsedObj.optDouble("overall_confidence", foodConfidence.coerceAtMost(0.85))
        val uncertaintyExplanation = parsedObj.optString(
            "uncertainty_explanation",
            "Portion volume and cooking oil absorption are the primary sources of uncertainty."
        )

        val foodItems = mutableListOf<DetectedFoodItem>()
        for (i in 0 until foodsArray.length()) {
            val item = foodsArray.getJSONObject(i)
            val name = item.optString("name", "Recognized Food")
            val quantityDesc = item.optString("quantity_description", "1 serving")
            val weight = item.optDouble("estimated_weight_grams", 150.0)
            val cookingMethod = item.optString("cooking_method", "Cooked")
            val confidence = item.optDouble("confidence", foodConfidence)
            val visualCues = item.optString("visual_cues", "Estimated from dish boundaries and visual scale")

            // NUTRITION RESOLUTION STEP:
            // Query the verified nutrition database for the identified food
            val resolvedNutrition = NutritionDatabase.resolveNutrition(
                foodName = name,
                fallbackCaloriesPer100g = item.optDouble("ref_calories_per_100g", 160.0),
                fallbackProteinPer100g = item.optDouble("ref_protein_per_100g", 6.0),
                fallbackCarbsPer100g = item.optDouble("ref_carbs_per_100g", 20.0),
                fallbackFatPer100g = item.optDouble("ref_fat_per_100g", 5.0)
            )

            foodItems.add(
                DetectedFoodItem(
                    name = name,
                    quantityDescription = quantityDesc,
                    weightGrams = weight,
                    cookingMethod = cookingMethod,
                    confidence = confidence,
                    visualCues = visualCues,
                    nutritionReference = resolvedNutrition,
                    source = source,
                    assumptions = "Portion estimated from visible vessel dimensions; visual estimation variance ±12%.",
                    nutritionDataSource = resolvedNutrition.source
                )
            )
        }

        return MealAnalysisResult(
            foods = foodItems,
            overallConfidence = overallConfidence,
            uncertaintyExplanation = uncertaintyExplanation
        )
    }

    /**
     * Deterministic, high-fidelity analysis for sample meals and demo mode,
     * adhering strictly to the user prompt's exact examples (e.g. Biryani 250g -> 500 kcal,
     * Indian Thali with Rice + Dal + Aloo Sabzi + Salad, Spiced Street Noodles).
     */
    fun getDeterministicFallbackAnalysis(hint: String?): MealAnalysisResult {
        val lower = hint?.lowercase() ?: ""

        return when {
            lower.contains("noodle") || lower.contains("maggi") || lower.contains("chowmein") -> {
                val noodlesNutrition = NutritionDatabase.resolveNutrition("Spiced Stir-Fried Noodles")
                val eggNutrition = NutritionDatabase.resolveNutrition("Egg Scramble (Stir-Fried)")
                val vegNutrition = NutritionDatabase.resolveNutrition("Sautéed Vegetables (Onion, Cabbage, Capsicum)")

                MealAnalysisResult(
                    foods = listOf(
                        DetectedFoodItem(
                            name = "Spiced Stir-Fried Noodles",
                            quantityDescription = "1 deep bowl portion (~220g)",
                            weightGrams = 220.0,
                            cookingMethod = "Pan-tossed with curry leaves, onions & chili spices",
                            confidence = 0.88,
                            visualCues = "Stainless bowl diameter ~20cm, noodle mound filling ~80% depth",
                            nutritionReference = noodlesNutrition
                        ),
                        DetectedFoodItem(
                            name = "Egg & Veggie Shreds",
                            quantityDescription = "Mixed scramble garnish (~60g)",
                            weightGrams = 60.0,
                            cookingMethod = "Scrambled and tossed into noodles with oil",
                            confidence = 0.83,
                            visualCues = "Evenly distributed egg ribbons and cabbage shreds",
                            nutritionReference = eggNutrition
                        )
                    ),
                    overallConfidence = 0.86,
                    uncertaintyExplanation = "Portion size estimated from bowl curvature; cooking oil tossed into noodles may adjust calories by ±12%."
                )
            }

            lower.contains("biryani") -> {
                val biryaniNutrition = NutritionDatabase.resolveNutrition("Chicken Biryani")
                val raitaNutrition = NutritionDatabase.resolveNutrition("Cucumber Raita")
                val saladNutrition = NutritionDatabase.resolveNutrition("Fresh Green Salad")

                MealAnalysisResult(
                    foods = listOf(
                        DetectedFoodItem(
                            name = "Chicken Biryani",
                            quantityDescription = "1 medium bowl (~250g)",
                            weightGrams = 250.0,
                            cookingMethod = "Dum-cooked spiced basmati rice with chicken",
                            confidence = 0.85,
                            visualCues = "Estimated against 18cm ceramic serving bowl; rice grain density ~1.1g/ml",
                            nutritionReference = biryaniNutrition
                        ),
                        DetectedFoodItem(
                            name = "Cucumber Raita",
                            quantityDescription = "1 small side katori (~80g)",
                            weightGrams = 80.0,
                            cookingMethod = "Whisked yogurt with diced cucumber and cumin",
                            confidence = 0.82,
                            visualCues = "Small side bowl rim ~8cm diameter",
                            nutritionReference = raitaNutrition
                        ),
                        DetectedFoodItem(
                            name = "Fresh Green Salad",
                            quantityDescription = "Side garnish portion (~40g)",
                            weightGrams = 40.0,
                            cookingMethod = "Raw sliced onion, cucumber, and lemon wedge",
                            confidence = 0.90,
                            visualCues = "4-5 thin slices visible on rim",
                            nutritionReference = saladNutrition
                        )
                    ),
                    overallConfidence = 0.84,
                    uncertaintyExplanation = "Portion size is the largest source of uncertainty; oil absorption in biryani rice may vary calories by ±10%."
                )
            }

            lower.contains("salmon") || lower.contains("quinoa") -> {
                val salmonNutrition = NutritionDatabase.resolveNutrition("Grilled Salmon Fillet")
                val quinoaNutrition = NutritionDatabase.resolveNutrition("Cooked Fluffy Quinoa")
                val broccoliNutrition = NutritionDatabase.resolveNutrition("Steamed Broccoli Florets")
                val avocadoNutrition = NutritionDatabase.resolveNutrition("Fresh Sliced Avocado")

                MealAnalysisResult(
                    foods = listOf(
                        DetectedFoodItem(
                            name = "Grilled Salmon Fillet",
                            quantityDescription = "1 palm-sized fillet (~150g)",
                            weightGrams = 150.0,
                            cookingMethod = "Pan-seared with light olive oil",
                            confidence = 0.89,
                            visualCues = "Thickness ~2.2cm, surface area roughly 12x7cm",
                            nutritionReference = salmonNutrition
                        ),
                        DetectedFoodItem(
                            name = "Cooked Fluffy Quinoa",
                            quantityDescription = "1/2 cup mound (~100g)",
                            weightGrams = 100.0,
                            cookingMethod = "Boiled / steamed",
                            confidence = 0.82,
                            visualCues = "Base bed filling ~30% of 26cm plate",
                            nutritionReference = quinoaNutrition
                        ),
                        DetectedFoodItem(
                            name = "Steamed Broccoli Florets",
                            quantityDescription = "1 cup florets (~85g)",
                            weightGrams = 85.0,
                            cookingMethod = "Lightly steamed",
                            confidence = 0.94,
                            visualCues = "5 distinct florets visible",
                            nutritionReference = broccoliNutrition
                        ),
                        DetectedFoodItem(
                            name = "Fresh Sliced Avocado",
                            quantityDescription = "1/4 avocado fan (~40g)",
                            weightGrams = 40.0,
                            cookingMethod = "Fresh raw sliced",
                            confidence = 0.87,
                            visualCues = "4 thin fan slices visible",
                            nutritionReference = avocadoNutrition
                        )
                    ),
                    overallConfidence = 0.88,
                    uncertaintyExplanation = "Portion estimation confidence is high due to clear food separation on flat 26cm plate."
                )
            }

            // Default: Prompt Example "Rice + Dal + Aloo Sabzi + Salad"
            else -> {
                val riceNutrition = NutritionDatabase.resolveNutrition("Steamed Basmati Rice")
                val dalNutrition = NutritionDatabase.resolveNutrition("Yellow Dal Tadka")
                val alooNutrition = NutritionDatabase.resolveNutrition("Aloo Sabzi")
                val saladNutrition = NutritionDatabase.resolveNutrition("Fresh Green Salad")

                MealAnalysisResult(
                    foods = listOf(
                        DetectedFoodItem(
                            name = "Steamed Basmati Rice",
                            quantityDescription = "1 cup serving (~180g)",
                            weightGrams = 180.0,
                            cookingMethod = "Steamed basmati",
                            confidence = 0.88,
                            visualCues = "Mounded portion taking ~35% of 26cm plate",
                            nutritionReference = riceNutrition
                        ),
                        DetectedFoodItem(
                            name = "Yellow Dal Tadka",
                            quantityDescription = "1 medium bowl / katori (~150g)",
                            weightGrams = 150.0,
                            cookingMethod = "Boiled yellow lentils tempered with ghee & cumin",
                            confidence = 0.82,
                            visualCues = "Standard 10cm stainless katori filled to 85% depth",
                            nutritionReference = dalNutrition
                        ),
                        DetectedFoodItem(
                            name = "Aloo Sabzi (Spiced Potatoes)",
                            quantityDescription = "1 small bowl (~120g)",
                            weightGrams = 120.0,
                            cookingMethod = "Sautéed with turmeric, chili & cumin",
                            confidence = 0.79,
                            visualCues = "Visible potato cube chunks ~2cm diameter",
                            nutritionReference = alooNutrition
                        ),
                        DetectedFoodItem(
                            name = "Fresh Green Salad",
                            quantityDescription = "Side portion (~50g)",
                            weightGrams = 50.0,
                            cookingMethod = "Raw cucumber, onion, tomato slices",
                            confidence = 0.92,
                            visualCues = "6 distinct slices spread on side",
                            nutritionReference = saladNutrition
                        )
                    ),
                    overallConfidence = 0.85,
                    uncertaintyExplanation = "Portion size is the largest source of uncertainty. Ghee/oil in dal and aloo sabzi may cause ±12% variation."
                )
            }
        }
    }

    /**
     * Generates concise, actionable health & nutrition pointers from the final edited meal.
     */
    suspend fun generateNutritionInsights(
        meal: MealAnalysisResult,
        providedApiKey: String? = null
    ): Result<NutritionInsights> = withContext(Dispatchers.IO) {
        val apiKey = (providedApiKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.GEMINI_API_KEY.trim())
        val isRealKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!isRealKey) {
            return@withContext Result.success(getDeterministicNutritionInsights(meal))
        }

        try {
            val total = meal.total
            val foodsSummary = meal.foods.joinToString(", ") {
                "${it.name} (${it.weightGrams.roundToInt()}g, ${it.calories} kcal, P:${it.protein}g C:${it.carbs}g F:${it.fat}g)"
            }

            val prompt = """
You are an expert clinical dietitian analyzing a user's final edited meal.
Meal items: $foodsSummary
Total: ${total.calories} kcal | Protein: ${total.protein}g | Carbs: ${total.carbs}g | Fat: ${total.fat}g

Generate concise, helpful nutrition pointers:
1. "headline": One punchy summary sentence about this meal's nutritional balance.
2. "positives": Array of 2-3 specific nutritional strengths of these foods and amounts.
3. "concerns": Array of 1-2 honest nutritional points to be mindful of (e.g. sodium, saturated fats, refined carb density).
4. "suggestions": Array of 2 practical, simple improvement swaps or adjustments.

Respond strictly with valid JSON without markdown fences matching this schema:
{
  "headline": "High-protein recovery meal with balanced slow-digesting carbs.",
  "positives": ["Rich in lean protein for muscle repair", "Good dietary fiber from veggies"],
  "concerns": ["Moderate cooking oil elevates saturated fat"],
  "suggestions": ["Swap refined rice for brown rice or quinoa", "Add a squeeze of fresh lemon for vitamin C"]
}
""".trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            for (modelName in candidateModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val parsed = parseNutritionInsights(responseBody)
                        return@withContext Result.success(parsed)
                    }
                } catch (e: Exception) {
                    Log.w("GeminiFoodService", "Insights call failed with $modelName: ${e.message}")
                }
            }

            Result.success(getDeterministicNutritionInsights(meal))
        } catch (e: Exception) {
            Log.e("GeminiFoodService", "Error generating insights: ${e.message}")
            Result.success(getDeterministicNutritionInsights(meal))
        }
    }

    private fun parseNutritionInsights(responseJsonString: String): NutritionInsights {
        val root = JSONObject(responseJsonString)
        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
        val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "{}"

        val clean = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(clean)
        val headline = json.optString("headline", "Balanced nutritional profile with good energy delivery.")

        val positives = mutableListOf<String>()
        val posArray = json.optJSONArray("positives")
        if (posArray != null) {
            for (i in 0 until posArray.length()) positives.add(posArray.getString(i))
        }

        val concerns = mutableListOf<String>()
        val conArray = json.optJSONArray("concerns")
        if (conArray != null) {
            for (i in 0 until conArray.length()) concerns.add(conArray.getString(i))
        }

        val suggestions = mutableListOf<String>()
        val sugArray = json.optJSONArray("suggestions")
        if (sugArray != null) {
            for (i in 0 until sugArray.length()) suggestions.add(sugArray.getString(i))
        }

        return NutritionInsights(
            headline = headline,
            positives = if (positives.isNotEmpty()) positives else listOf("Provides sustained caloric energy"),
            concerns = if (concerns.isNotEmpty()) concerns else listOf("Watch cooking oils/fats if targeting weight loss"),
            suggestions = if (suggestions.isNotEmpty()) suggestions else listOf("Hydrate well and pair with fresh greens")
        )
    }

    fun getDeterministicNutritionInsights(meal: MealAnalysisResult): NutritionInsights {
        val total = meal.total
        val foodNames = meal.foods.joinToString(", ") { it.name.lowercase() }

        val positives = mutableListOf<String>()
        val concerns = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // Protein analysis
        if (total.protein >= 24.0) {
            positives.add("Excellent protein density (${total.protein}g) supporting muscle repair and prolonged satiety.")
        } else if (total.protein < 14.0) {
            concerns.add("Relatively low in protein (${total.protein}g), which may cause earlier hunger.")
            suggestions.add("Add a boiled egg, tofu, or paneer (+6-12g protein) to improve satiety.")
        } else {
            positives.add("Moderate protein content (${total.protein}g) suitable for a daily balanced meal.")
        }

        // Carbohydrates & Fiber
        if (total.carbs > 65.0) {
            concerns.add("High carbohydrate load (${total.carbs}g). Great for workout fuel, but may cause an energy dip if inactive.")
            suggestions.add("Consider reducing grain/noodle portion by 20% and adding more fiber-rich greens.")
        } else {
            positives.add("Controlled carbohydrate portion (${total.carbs}g) promoting steady blood glucose levels.")
        }

        // Fats
        if (total.fat > 22.0) {
            concerns.add("Higher fat content (${total.fat}g), likely from cooking oils, sautéing, or animal fats.")
            suggestions.add("Use a light oil spray or air-fry preparation to save 80-120 calories.")
        } else {
            positives.add("Healthy moderate fat profile (${total.fat}g).")
        }

        // Veggies & Micro-nutrients
        if (foodNames.contains("salad") || foodNames.contains("broccoli") || foodNames.contains("vegetable")) {
            positives.add("Rich in essential micronutrients and dietary fiber from visible greens.")
        } else {
            suggestions.add("Add a side of raw cucumber, tomato, or leafy greens to slow gastric emptying.")
        }

        val headline = when {
            total.protein >= 25.0 -> "High-protein meal with solid nutritional density."
            total.calories < 450 -> "Light, calorie-controlled plate suitable for fat loss."
            total.carbs > 60.0 -> "Energy-dense meal loaded with complex carbohydrates."
            else -> "Well-proportioned meal with balanced macronutrients."
        }

        return NutritionInsights(
            headline = headline,
            positives = positives.take(3),
            concerns = concerns.take(2),
            suggestions = suggestions.take(2)
        )
    }

    /**
     * "Ask About This Food": Answers user questions dynamically based on the current
     * edited meal components, calculated nutrition, and image context.
     */
    suspend fun askAboutFood(
        question: String,
        meal: MealAnalysisResult,
        previousChat: List<ChatMessage>,
        bitmap: Bitmap?,
        providedApiKey: String? = null,
        userAddedFoods: List<String> = emptyList(),
        userRemovedFoods: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = (providedApiKey?.trim()?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.GEMINI_API_KEY.trim())
        val isRealKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        val total = meal.total
        val foodListDesc = meal.foods.joinToString("\n") {
            "- ${it.name}: ${it.weightGrams.roundToInt()}g (${it.calories} kcal, P:${it.protein}g, C:${it.carbs}g, F:${it.fat}g) [${it.cookingMethod}]"
        }

        val addedDesc = if (userAddedFoods.isNotEmpty()) "User added foods: ${userAddedFoods.joinToString(", ")}" else "No user-added foods"
        val removedDesc = if (userRemovedFoods.isNotEmpty()) "User removed foods: ${userRemovedFoods.joinToString(", ")}" else "No foods removed"

        if (!isRealKey) {
            return@withContext Result.success(getDeterministicAnswer(question, meal, userAddedFoods, userRemovedFoods))
        }

        try {
            val systemContext = """
You are a friendly, evidence-based nutrition AI assisting a user with their current meal.
CURRENT MEAL (User Edited):
$foodListDesc
TOTALS: ${total.calories} kcal | Protein: ${total.protein}g | Carbs: ${total.carbs}g | Fat: ${total.fat}g
$addedDesc
$removedDesc

Answer the user's question directly, accurately, and concisely (2 to 4 sentences). Base calculations on the exact numbers given above.
User Question: "$question"
""".trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemContext) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            for (modelName in candidateModels) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestJson.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val root = JSONObject(responseBody)
                        val text = root.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text")

                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                    }
                } catch (e: Exception) {
                    Log.w("GeminiFoodService", "Ask AI failed with $modelName: ${e.message}")
                }
            }

            Result.success(getDeterministicAnswer(question, meal, userAddedFoods, userRemovedFoods))
        } catch (e: Exception) {
            Log.e("GeminiFoodService", "Exception in askAboutFood: ${e.message}")
            Result.success(getDeterministicAnswer(question, meal, userAddedFoods, userRemovedFoods))
        }
    }

    private fun getDeterministicAnswer(
        question: String,
        meal: MealAnalysisResult,
        userAddedFoods: List<String> = emptyList(),
        userRemovedFoods: List<String> = emptyList()
    ): String {
        val q = question.lowercase()
        val total = meal.total
        val highestCalorieItem = meal.foods.maxByOrNull { it.calories }
        val highestFatItem = meal.foods.maxByOrNull { it.fat }
        val highestProteinItem = meal.foods.maxByOrNull { it.protein }

        return when {
            q.contains("fat loss") || q.contains("weight loss") || q.contains("deficit") || q.contains("diet") -> {
                if (total.calories <= 550) {
                    "Yes, this fits very well into a deficit at ${total.calories} kcal, delivering ${total.protein}g protein to maintain lean mass."
                } else {
                    "At ${total.calories} kcal, reducing ${highestCalorieItem?.name ?: "portions"} slightly will make it even friendlier for fat loss."
                }
            }
            q.contains("why") && (q.contains("high") || q.contains("calorie")) -> {
                if (highestCalorieItem != null) {
                    "${highestCalorieItem.name} contributes the most calories (${highestCalorieItem.calories} kcal of ${total.calories} kcal total). Moderating its portion by 20-30% is the fastest way to reduce overall energy density."
                } else {
                    "Total calories (${total.calories} kcal) are concentrated in carbohydrate sources and cooking fats."
                }
            }
            q.contains("workout") || q.contains("gym") || q.contains("post-workout") -> {
                if (total.protein >= 20.0) {
                    "Yes, this is great post-workout fuel! At ${total.calories} kcal, you have ${total.protein}g of protein for muscle synthesis and ${total.carbs}g of carbohydrates to replenish glycogen."
                } else {
                    "It provides good carb fuel (${total.carbs}g at ${total.calories} kcal), but post-workout recovery ideally targets 20-30g protein. Consider adding a boiled egg or protein side dish."
                }
            }
            q.contains("fat") || q.contains("lipid") -> {
                if (highestFatItem != null) {
                    "${highestFatItem.name} contributes the largest amount of fat (${highestFatItem.fat}g out of ${total.fat}g total, ~${(highestFatItem.fat * 9).roundToInt()} kcal), typically from cooking oils or natural lipids."
                } else {
                    "The total fat is ${total.fat}g (~${(total.fat * 9).roundToInt()} kcal), representing approx. ${(total.fat * 900 / total.calories.coerceAtLeast(1)).roundToInt()}% of the ${total.calories} kcal total energy."
                }
            }
            q.contains("protein") || q.contains("enough protein") -> {
                "This meal currently delivers ${total.protein}g of protein (~${(total.protein * 4).roundToInt()} kcal)" +
                        (if (highestProteinItem != null) " with ${highestProteinItem.name} being the primary contributor (${highestProteinItem.protein}g)." else ".") +
                        if (total.protein >= 20.0) " This is an adequate amount for a main meal." else " For higher satiety, adding an egg or paneer would boost it by 6-10g."
            }
            q.contains("healthier") || q.contains("better") || q.contains("improve") -> {
                "To optimize this plate: increase fiber by adding raw crunchy salad or steamed greens, and ensure cooking oils are kept modest to keep calories near ${total.calories} kcal."
            }
            else -> {
                "This meal provides ${total.calories} kcal with ${total.protein}g protein, ${total.carbs}g carbs, and ${total.fat}g fat across ${meal.foods.size} items. Adjusting portions in Adjust Meal will recalculate these numbers deterministically."
            }
        }
    }
}

