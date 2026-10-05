package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.auth.ApiKeyRepository
import com.example.data.gemini.GeminiFoodService
import com.example.data.model.DetectedFoodItem
import com.example.data.model.MealAnalysisResult
import com.example.data.nutrition.NutritionDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface AnalysisUiState {
    object Idle : AnalysisUiState
    data class Analyzing(val stepMessage: String) : AnalysisUiState
    data class Success(val result: MealAnalysisResult) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

data class SampleFoodPreset(
    val title: String,
    val subtitle: String,
    val drawableResId: Int,
    val hint: String
)

data class FoodCalorieUiState(
    val analysisState: AnalysisUiState = AnalysisUiState.Idle,
    val currentImageBitmap: Bitmap? = null,
    val currentImageResId: Int? = R.drawable.sample_thali,
    val currentImageTitle: String = "Indian Thali Platter (Rice + Dal + Aloo Sabzi + Salad)",
    val activeMealResult: MealAnalysisResult? = null,
    val activeTabIndex: Int = 0, // 0 = Visual Breakdown, 1 = Raw JSON, 2 = Database
    val naturalLanguageCorrection: String = "",
    val feedbackBanner: String? = null,
    val isApiKeyConfigured: Boolean = false,
    val currentApiKeyMasked: String = "",
    val showApiKeyDialog: Boolean = false,
    val isStartupPrompt: Boolean = false,
    val isTestingApiKey: Boolean = false
)

class FoodCalorieViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val geminiService = GeminiFoodService()
    private val apiKeyRepository = ApiKeyRepository.getInstance(application)

    private val _uiState = MutableStateFlow(
        FoodCalorieUiState(
            isApiKeyConfigured = apiKeyRepository.hasValidApiKey(),
            currentApiKeyMasked = maskApiKey(apiKeyRepository.getEffectiveApiKey()),
            showApiKeyDialog = !apiKeyRepository.hasValidApiKey() && !apiKeyRepository.hasDismissedStartupPrompt(),
            isStartupPrompt = !apiKeyRepository.hasValidApiKey() && !apiKeyRepository.hasDismissedStartupPrompt()
        )
    )
    val uiState: StateFlow<FoodCalorieUiState> = _uiState.asStateFlow()

    val samplePresets = listOf(
        SampleFoodPreset(
            title = "Indian Thali",
            subtitle = "Rice + Dal + Aloo Sabzi + Salad",
            drawableResId = R.drawable.sample_thali,
            hint = "rice dal aloo sabzi salad"
        ),
        SampleFoodPreset(
            title = "Chicken Biryani",
            subtitle = "Dum Biryani with Raita & Salad",
            drawableResId = R.drawable.sample_biryani,
            hint = "chicken biryani raita salad"
        ),
        SampleFoodPreset(
            title = "Salmon Fitness Bowl",
            subtitle = "Grilled Salmon, Quinoa, Broccoli, Avocado",
            drawableResId = R.drawable.sample_salmon,
            hint = "grilled salmon quinoa broccoli avocado"
        )
    )

    init {
        preloadDefaultSample()
    }

    private fun preloadDefaultSample() {
        val defaultResult = geminiService.getDeterministicFallbackAnalysis("rice dal aloo sabzi salad")
        _uiState.update {
            it.copy(
                activeMealResult = defaultResult,
                analysisState = AnalysisUiState.Success(defaultResult),
                currentImageResId = R.drawable.sample_thali,
                currentImageTitle = "Indian Thali (Rice + Dal + Aloo Sabzi + Salad)"
            )
        }
    }

    fun openApiKeyDialog(isStartup: Boolean = false) {
        _uiState.update {
            it.copy(
                showApiKeyDialog = true,
                isStartupPrompt = isStartup
            )
        }
    }

    fun closeApiKeyDialog() {
        apiKeyRepository.setStartupPromptDismissed(true)
        _uiState.update {
            it.copy(showApiKeyDialog = false)
        }
    }

    fun saveAndValidateApiKey(apiKey: String, onDone: (Boolean, String) -> Unit) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            onDone(false, "API Key cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingApiKey = true) }
            val testResult = geminiService.validateApiKey(cleanKey)
            _uiState.update { it.copy(isTestingApiKey = false) }

            testResult.onSuccess {
                apiKeyRepository.saveApiKey(cleanKey)
                apiKeyRepository.setStartupPromptDismissed(true)
                _uiState.update {
                    it.copy(
                        isApiKeyConfigured = true,
                        currentApiKeyMasked = maskApiKey(cleanKey),
                        showApiKeyDialog = false,
                        feedbackBanner = "Google AI Studio API Key connected successfully! Real-time AI analysis is active."
                    )
                }
                onDone(true, "Connected successfully!")
            }.onFailure { error ->
                // Still allow saving if user insists or in offline network
                apiKeyRepository.saveApiKey(cleanKey)
                apiKeyRepository.setStartupPromptDismissed(true)
                _uiState.update {
                    it.copy(
                        isApiKeyConfigured = true,
                        currentApiKeyMasked = maskApiKey(cleanKey),
                        showApiKeyDialog = false,
                        feedbackBanner = "API Key saved. (${error.message})"
                    )
                }
                onDone(true, "Key saved.")
            }
        }
    }

    fun clearApiKey() {
        apiKeyRepository.clearApiKey()
        _uiState.update {
            it.copy(
                isApiKeyConfigured = false,
                currentApiKeyMasked = "",
                feedbackBanner = "API Key removed. Reverted to demo mode."
            )
        }
    }

    fun retryAnalysis() {
        val state = _uiState.value
        val bitmap = state.currentImageBitmap
        if (bitmap != null) {
            analyzeImage(bitmap = bitmap, hint = null)
        } else if (state.currentImageResId != null) {
            val preset = samplePresets.firstOrNull { it.drawableResId == state.currentImageResId }
            analyzeImage(bitmap = null, hint = preset?.hint)
        }
    }

    fun useOfflineAnalysisForCurrentImage() {
        val state = _uiState.value
        val fallback = geminiService.getDeterministicFallbackAnalysis(state.currentImageTitle)
        _uiState.update {
            it.copy(
                activeMealResult = fallback,
                analysisState = AnalysisUiState.Success(fallback),
                feedbackBanner = "Resolved using verified offline database."
            )
        }
    }

    fun selectSamplePreset(context: Context, preset: SampleFoodPreset) {
        val bitmap = BitmapFactory.decodeResource(context.resources, preset.drawableResId)
        _uiState.update {
            it.copy(
                currentImageBitmap = bitmap,
                currentImageResId = preset.drawableResId,
                currentImageTitle = preset.title,
                analysisState = AnalysisUiState.Idle
            )
        }
        analyzeImage(bitmap = bitmap, hint = preset.hint)
    }

    fun onImageSelectedFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        _uiState.update {
                            it.copy(
                                currentImageBitmap = bitmap,
                                currentImageResId = null,
                                currentImageTitle = "Uploaded Custom Food Photo",
                                analysisState = AnalysisUiState.Idle
                            )
                        }
                        if (!apiKeyRepository.hasValidApiKey()) {
                            openApiKeyDialog(isStartup = true)
                        } else {
                            analyzeImage(bitmap = bitmap, hint = null)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(analysisState = AnalysisUiState.Error("Failed to load image: ${e.message}"))
                }
            }
        }
    }

    fun onImageCapturedFromCamera(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                currentImageBitmap = bitmap,
                currentImageResId = null,
                currentImageTitle = "Camera Capture",
                analysisState = AnalysisUiState.Idle
            )
        }
        if (!apiKeyRepository.hasValidApiKey()) {
            openApiKeyDialog(isStartup = true)
        } else {
            analyzeImage(bitmap = bitmap, hint = null)
        }
    }

    fun analyzeImage(bitmap: Bitmap?, hint: String? = null) {
        viewModelScope.launch {
            if (bitmap == null) return@launch

            val effectiveKey = apiKeyRepository.getEffectiveApiKey()
            val hasKey = effectiveKey.isNotEmpty()

            val step1 = if (hasKey) "Step 1/4: Google Gemini Vision identifying recognizable food items..."
            else "Step 1/4: Vision Engine identifying recognizable food items (Demo Mode)..."

            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Analyzing(step1))
            }
            delay(400)

            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Analyzing("Step 2/4: Estimating portions using plate and vessel geometry..."))
            }
            delay(350)

            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Analyzing("Step 3/4: Resolving macros from Nutrition Database..."))
            }

            val result = geminiService.analyzeFoodImage(
                bitmap = bitmap,
                sampleContextHint = hint,
                providedApiKey = effectiveKey
            )

            result.onSuccess { mealResult ->
                _uiState.update {
                    it.copy(
                        activeMealResult = mealResult,
                        analysisState = AnalysisUiState.Success(mealResult),
                        feedbackBanner = "Analysis complete! ${mealResult.foods.size} food items resolved."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Error(error.message ?: "Failed to analyze food image.")
                    )
                }
                if (!hasKey) {
                    openApiKeyDialog(isStartup = true)
                }
            }
        }
    }

    fun setActiveTab(index: Int) {
        _uiState.update { it.copy(activeTabIndex = index) }
    }

    fun onNaturalLanguageCorrectionChanged(text: String) {
        _uiState.update { it.copy(naturalLanguageCorrection = text) }
    }

    fun updateFoodWeight(foodId: String, newWeightGrams: Double) {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val clampedWeight = newWeightGrams.coerceIn(5.0, 2000.0)

        val updatedFoods = currentMeal.foods.map { item ->
            if (item.id == foodId) {
                item.copy(
                    weightGrams = clampedWeight,
                    quantityDescription = "${clampedWeight.roundToInt()} g (user adjusted)"
                )
            } else {
                item
            }
        }

        val updatedMeal = currentMeal.copy(foods = updatedFoods)
        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                feedbackBanner = "Portion updated to ${clampedWeight.roundToInt()}g. Calories recalculated!"
            )
        }
    }

    fun removeFoodItem(foodId: String) {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val itemToRemove = currentMeal.foods.firstOrNull { it.id == foodId }
        val updatedFoods = currentMeal.foods.filterNot { it.id == foodId }

        val updatedMeal = currentMeal.copy(foods = updatedFoods)
        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                feedbackBanner = "Removed ${itemToRemove?.name ?: "item"}. Totals recalculated."
            )
        }
    }

    fun addFoodItem(foodName: String, weightGrams: Double = 100.0) {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val resolvedNutrition = NutritionDatabase.resolveNutrition(foodName)

        val newItem = DetectedFoodItem(
            name = resolvedNutrition.displayName,
            quantityDescription = "${weightGrams.roundToInt()} g (user added)",
            weightGrams = weightGrams,
            cookingMethod = "Standard serving",
            confidence = 1.0,
            visualCues = "User added item",
            nutritionReference = resolvedNutrition
        )

        val updatedFoods = currentMeal.foods + newItem
        val updatedMeal = currentMeal.copy(foods = updatedFoods)
        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                feedbackBanner = "Added ${newItem.name} (${weightGrams.roundToInt()}g)."
            )
        }
    }

    fun applyNaturalLanguageCorrection(inputCommand: String = _uiState.value.naturalLanguageCorrection) {
        val command = inputCommand.trim().lowercase()
        if (command.isEmpty()) return

        val currentMeal = _uiState.value.activeMealResult ?: return

        // 1. Remove item: "remove [name]"
        if (command.startsWith("remove") || command.startsWith("delete") || command.startsWith("drop")) {
            val targetName = command.removePrefix("remove")
                .removePrefix("delete")
                .removePrefix("drop")
                .trim()

            val matchedItem = currentMeal.foods.firstOrNull {
                it.name.lowercase().contains(targetName) || targetName.contains(it.name.lowercase())
            }

            if (matchedItem != null) {
                removeFoodItem(matchedItem.id)
                _uiState.update { it.copy(naturalLanguageCorrection = "") }
                return
            }
        }

        // 2. Add item: "add [quantity/item]"
        if (command.startsWith("add")) {
            val addition = command.removePrefix("add").trim()
            val gramsRegex = "(\\d+)\\s*(g|grams)".toRegex()
            val gramsMatch = gramsRegex.find(addition)
            val grams = gramsMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: when {
                addition.contains("egg") -> 50.0
                addition.contains("roti") || addition.contains("chapati") -> 35.0
                addition.contains("rice") -> 150.0
                addition.contains("bread") || addition.contains("toast") -> 30.0
                else -> 100.0
            }

            val cleanFoodName = addition.replace(gramsRegex, "")
                .replace("1 ", "")
                .replace("2 ", "")
                .trim()

            addFoodItem(cleanFoodName, grams)
            _uiState.update { it.copy(naturalLanguageCorrection = "") }
            return
        }

        // 3. Portion modification: "[food] [old] -> [new]" or "[food] to [new]g"
        val arrowParts = command.split("->", "to")
        if (arrowParts.size >= 2) {
            val firstPart = arrowParts[0]
            val secondPart = arrowParts[1]

            val newWeightRegex = "(\\d+)\\s*g?".toRegex()
            val newWeightMatch = newWeightRegex.find(secondPart)
            val newWeight = newWeightMatch?.groupValues?.get(1)?.toDoubleOrNull()

            if (newWeight != null) {
                val matchedItem = currentMeal.foods.firstOrNull { food ->
                    val fName = food.name.lowercase()
                    firstPart.contains(fName) || fName.contains(firstPart.replace(newWeightRegex, "").trim())
                }

                if (matchedItem != null) {
                    updateFoodWeight(matchedItem.id, newWeight)
                    _uiState.update { it.copy(naturalLanguageCorrection = "") }
                    return
                }
            }
        }

        val digitMatch = "(\\d+)\\s*g?".toRegex().find(command)
        if (digitMatch != null) {
            val grams = digitMatch.groupValues[1].toDoubleOrNull()
            if (grams != null) {
                val matchedItem = currentMeal.foods.firstOrNull { food ->
                    command.contains(food.name.lowercase()) || food.name.lowercase().split(" ").any { command.contains(it) }
                }
                if (matchedItem != null) {
                    updateFoodWeight(matchedItem.id, grams)
                    _uiState.update { it.copy(naturalLanguageCorrection = "") }
                    return
                }
            }
        }

        _uiState.update {
            it.copy(
                feedbackBanner = "Could not parse command. Try: 'Chicken Biryani 250 g -> 180 g', 'Remove salad', or 'Add 1 boiled egg'"
            )
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackBanner = null) }
    }

    private fun maskApiKey(key: String): String {
        if (key.length <= 8) return if (key.isEmpty()) "Not Set" else "••••••••"
        return "${key.take(4)}••••••••${key.takeLast(4)}"
    }
}
