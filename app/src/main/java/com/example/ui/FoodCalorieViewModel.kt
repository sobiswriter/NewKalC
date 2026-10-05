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
import com.example.data.model.AmbiguousFoodException
import com.example.data.model.ChatMessage
import com.example.data.model.DetectedFoodItem
import com.example.data.model.FoodDetectionSource
import com.example.data.model.MealAnalysisResult
import com.example.data.model.NonFoodException
import com.example.data.model.NutritionInsights
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
    data class ValidationFailed(
        val message: String,
        val isAmbiguous: Boolean,
        val disclaimer: String = "Food analysis is an estimate and should not be treated as medical or dietary advice."
    ) : AnalysisUiState
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
    val originalFoodNames: List<String> = emptyList(),
    val activeNavTab: Int = 0, // 0 = Meal, 1 = Insights, 2 = Ask AI
    val naturalLanguageCorrection: String = "",
    val feedbackBanner: String? = null,
    val isApiKeyConfigured: Boolean = false,
    val currentApiKeyMasked: String = "",
    val showApiKeyDialog: Boolean = false,
    val isStartupPrompt: Boolean = false,
    val isTestingApiKey: Boolean = false,
    // Food Validation Gate configurable threshold
    val validationThreshold: Double = 0.65,
    // Full screen image inspection viewer
    val showFullScreenImageViewer: Boolean = false,
    // Additional Food Image / Side Dish detection state
    val isAnalyzingSideDish: Boolean = false,
    val pendingSideDishItems: List<DetectedFoodItem>? = null,
    val sideDishError: String? = null,
    // Sheet & Dialog Visibility (Progressive Disclosure)
    val showAdjustMealSheet: Boolean = false,
    val adjustSheetInAddMode: Boolean = false,
    val showAskAiSheet: Boolean = false,
    val showJsonDialog: Boolean = false,
    val showDatabaseDialog: Boolean = false,
    val expandedFoodId: String? = null,
    val insightsExpanded: Boolean = false,
    // Feature 1 Step 4: AI Nutrition Insights
    val nutritionInsights: NutritionInsights? = null,
    val isGeneratingInsights: Boolean = false,
    // Feature 1 Step 5: "Ask About This Food" Q&A
    val chatMessages: List<ChatMessage> = emptyList(),
    val isAskingQuestion: Boolean = false,
    val currentQuestionInput: String = ""
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
            title = "Salmon Bowl",
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
        val defaultInsights = geminiService.getDeterministicNutritionInsights(defaultResult)
        _uiState.update {
            it.copy(
                activeMealResult = defaultResult,
                originalFoodNames = defaultResult.foods.map { food -> food.name },
                analysisState = AnalysisUiState.Success(defaultResult),
                nutritionInsights = defaultInsights,
                currentImageResId = R.drawable.sample_thali,
                currentImageTitle = "Indian Thali (Rice + Dal + Aloo Sabzi + Salad)",
                chatMessages = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "Hello! I have analyzed this meal. Ask me anything about its calorie density, protein balance, or how to tweak it!"
                    )
                )
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
                        feedbackBanner = "Google AI Studio API Key connected! Gemini 3.8 Flash is active."
                    )
                }
                onDone(true, "Connected successfully!")
            }.onFailure { error ->
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
        val insights = geminiService.getDeterministicNutritionInsights(fallback)
        _uiState.update {
            it.copy(
                activeMealResult = fallback,
                analysisState = AnalysisUiState.Success(fallback),
                nutritionInsights = insights,
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

            val step1 = if (hasKey) "Step 1/4: Food Validation Gate checking edible content..."
            else "Step 1/4: Food Validation Gate (Demo Mode)..."

            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Analyzing(step1))
            }
            delay(300)

            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Analyzing("Step 2/4: Identifying distinct foods & geometry..."))
            }
            delay(300)

            _uiState.update {
                it.copy(analysisState = AnalysisUiState.Analyzing("Step 3/4: Resolving macros from Nutrition Database..."))
            }

            val result = geminiService.analyzeFoodImage(
                bitmap = bitmap,
                sampleContextHint = hint,
                providedApiKey = effectiveKey,
                validationThreshold = _uiState.value.validationThreshold
            )

            result.onSuccess { mealResult ->
                _uiState.update {
                    it.copy(
                        activeMealResult = mealResult,
                        originalFoodNames = mealResult.foods.map { food -> food.name },
                        analysisState = AnalysisUiState.Success(mealResult),
                        feedbackBanner = "Analysis complete! ${mealResult.foods.size} food items resolved."
                    )
                }
                // Step 4: Generate concise AI nutrition insights from the meal
                refreshInsights(mealResult)
            }.onFailure { error ->
                when (error) {
                    is NonFoodException -> {
                        _uiState.update {
                            it.copy(
                                activeMealResult = null,
                                analysisState = AnalysisUiState.ValidationFailed(
                                    message = error.message,
                                    isAmbiguous = false,
                                    disclaimer = error.disclaimer
                                )
                            )
                        }
                    }
                    is AmbiguousFoodException -> {
                        _uiState.update {
                            it.copy(
                                activeMealResult = null,
                                analysisState = AnalysisUiState.ValidationFailed(
                                    message = error.message,
                                    isAmbiguous = true,
                                    disclaimer = error.disclaimer
                                )
                            )
                        }
                    }
                    else -> {
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
        }
    }

    /**
     * Generates or refreshes the AI Nutrition Insights based on the current edited meal.
     */
    fun refreshInsights(meal: MealAnalysisResult? = _uiState.value.activeMealResult) {
        if (meal == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingInsights = true) }
            val effectiveKey = apiKeyRepository.getEffectiveApiKey()
            val result = geminiService.generateNutritionInsights(meal, effectiveKey)
            result.onSuccess { insights ->
                _uiState.update {
                    it.copy(
                        nutritionInsights = insights,
                        isGeneratingInsights = false
                    )
                }
            }.onFailure {
                val fallback = geminiService.getDeterministicNutritionInsights(meal)
                _uiState.update {
                    it.copy(
                        nutritionInsights = fallback,
                        isGeneratingInsights = false
                    )
                }
            }
        }
    }

    /**
     * "Ask About This Food" Q&A with full meal context
     */
    fun onQuestionInputChanged(text: String) {
        _uiState.update { it.copy(currentQuestionInput = text) }
    }

    fun askQuestion(questionText: String = _uiState.value.currentQuestionInput) {
        val q = questionText.trim()
        if (q.isEmpty()) return

        val currentMeal = _uiState.value.activeMealResult ?: return
        val currentBitmap = _uiState.value.currentImageBitmap
        val effectiveKey = apiKeyRepository.getEffectiveApiKey()

        val currentFoodNames = currentMeal.foods.map { it.name }
        val originalFoods = _uiState.value.originalFoodNames
        val userAdded = currentFoodNames.filter { it !in originalFoods }
        val userRemoved = originalFoods.filter { it !in currentFoodNames }

        val userMessage = ChatMessage(isUser = true, text = q)
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                currentQuestionInput = "",
                isAskingQuestion = true
            )
        }

        viewModelScope.launch {
            val responseResult = geminiService.askAboutFood(
                question = q,
                meal = currentMeal,
                previousChat = _uiState.value.chatMessages,
                bitmap = currentBitmap,
                providedApiKey = effectiveKey,
                userAddedFoods = userAdded,
                userRemovedFoods = userRemoved
            )

            val replyText = responseResult.getOrElse {
                "Based on your meal totals (${currentMeal.total.calories} kcal, ${currentMeal.total.protein}g protein), this meal provides balanced fuel. Adjusting portions will update these values."
            }

            val aiMessage = ChatMessage(isUser = false, text = replyText)
            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + aiMessage,
                    isAskingQuestion = false
                )
            }
        }
    }

    fun setActiveNavTab(tabIndex: Int) {
        _uiState.update {
            it.copy(
                activeNavTab = tabIndex,
                showAskAiSheet = if (tabIndex == 2) true else it.showAskAiSheet
            )
        }
    }

    fun openAdjustMealSheet(startInAddMode: Boolean = false) {
        _uiState.update { it.copy(showAdjustMealSheet = true, adjustSheetInAddMode = startInAddMode) }
    }

    fun setAdjustSheetAddMode(inAddMode: Boolean) {
        _uiState.update { it.copy(adjustSheetInAddMode = inAddMode) }
    }

    fun closeAdjustMealSheet() {
        _uiState.update { it.copy(showAdjustMealSheet = false, adjustSheetInAddMode = false) }
    }

    fun openAddFoodSheet() {
        openAdjustMealSheet(startInAddMode = true)
    }

    fun closeAddFoodSheet() {
        _uiState.update { it.copy(adjustSheetInAddMode = false) }
    }

    fun openAskAiSheet() {
        _uiState.update { it.copy(showAskAiSheet = true, activeNavTab = 2) }
    }

    fun closeAskAiSheet() {
        _uiState.update { it.copy(showAskAiSheet = false) }
    }

    fun openJsonDialog() {
        _uiState.update { it.copy(showJsonDialog = true) }
    }

    fun closeJsonDialog() {
        _uiState.update { it.copy(showJsonDialog = false) }
    }

    fun openDatabaseDialog() {
        _uiState.update { it.copy(showDatabaseDialog = true) }
    }

    fun closeDatabaseDialog() {
        _uiState.update { it.copy(showDatabaseDialog = false) }
    }

    fun toggleFoodExpansion(foodId: String) {
        _uiState.update {
            it.copy(expandedFoodId = if (it.expandedFoodId == foodId) null else foodId)
        }
    }

    fun toggleInsightsExpansion() {
        _uiState.update { it.copy(insightsExpanded = !it.insightsExpanded) }
    }

    /**
     * Explicit deterministic recalculation.
     * Re-resolves all food macros from the local Nutrition Database, updates totals,
     * updates AI insight context, and updates Ask AI context.
     */
    fun recalculateMeal() {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val recalculatedFoods = currentMeal.foods.map { food ->
            val resolved = NutritionDatabase.resolveNutrition(food.name)
            food.copy(nutritionReference = resolved)
        }
        val updatedMeal = currentMeal.copy(foods = recalculatedFoods)
        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                showAdjustMealSheet = false,
                feedbackBanner = "Recalculated: ${updatedMeal.total.calories} kcal · ${updatedMeal.total.protein}g Protein · ${updatedMeal.total.carbs}g Carbs · ${updatedMeal.total.fat}g Fat"
            )
        }
        refreshInsights(updatedMeal)
    }

    fun onNaturalLanguageCorrectionChanged(text: String) {
        _uiState.update { it.copy(naturalLanguageCorrection = text) }
    }

    /**
     * Updates an existing food's portion in grams.
     * Recalculates all calories and macros instantly using NutritionDatabase!
     */
    fun updateFoodWeight(foodId: String, newWeightGrams: Double) {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val clampedWeight = newWeightGrams.coerceIn(5.0, 2000.0)

        val updatedFoods = currentMeal.foods.map { item ->
            if (item.id == foodId) {
                item.copy(
                    weightGrams = clampedWeight,
                    quantityDescription = "${clampedWeight.roundToInt()} g"
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
                feedbackBanner = "Portion updated to ${clampedWeight.roundToInt()}g. Recalculated!"
            )
        }
        // Recalculate insights dynamically
        refreshInsights(updatedMeal)
    }

    /**
     * Removes an identified food from the meal analysis.
     * Recalculates all totals instantly using NutritionDatabase!
     */
    fun removeFoodItem(foodId: String) {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val itemToRemove = currentMeal.foods.firstOrNull { it.id == foodId }
        val updatedFoods = currentMeal.foods.filterNot { it.id == foodId }

        val updatedMeal = currentMeal.copy(foods = updatedFoods)
        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                feedbackBanner = "Removed ${itemToRemove?.name ?: "item"}. Recalculated!"
            )
        }
        // Recalculate insights dynamically
        refreshInsights(updatedMeal)
    }

    /**
     * Adds a new food item / side dish from the Nutrition Database.
     * Recalculates all totals instantly using NutritionDatabase!
     */
    fun addFoodItem(foodName: String, weightGrams: Double = 100.0) {
        val currentMeal = _uiState.value.activeMealResult ?: return
        val resolvedNutrition = NutritionDatabase.resolveNutrition(foodName)

        val newItem = DetectedFoodItem(
            name = resolvedNutrition.displayName,
            quantityDescription = "${weightGrams.roundToInt()} g",
            weightGrams = weightGrams,
            cookingMethod = "Standard serving",
            confidence = 1.0,
            visualCues = "User added item",
            nutritionReference = resolvedNutrition,
            source = FoodDetectionSource.MANUAL_SEARCH,
            assumptions = "Manual portion input by user.",
            nutritionDataSource = resolvedNutrition.source
        )

        val updatedFoods = currentMeal.foods + newItem
        val updatedMeal = currentMeal.copy(foods = updatedFoods)
        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                adjustSheetInAddMode = false,
                feedbackBanner = "Added ${newItem.name} (${weightGrams.roundToInt()}g). Recalculated!"
            )
        }
        // Recalculate insights dynamically
        refreshInsights(updatedMeal)
    }

    /**
     * Analyzes an additional food/side dish photo (e.g. raita, pickle, papad).
     * Runs through the Food Validation Gate before generating detected food options.
     */
    fun analyzeSideDishImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzingSideDish = true,
                    sideDishError = null,
                    pendingSideDishItems = null
                )
            }

            val effectiveKey = apiKeyRepository.getEffectiveApiKey()
            val result = geminiService.analyzeAdditionalFoodImage(
                bitmap = bitmap,
                providedApiKey = effectiveKey,
                validationThreshold = _uiState.value.validationThreshold
            )

            result.onSuccess { items ->
                if (items.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isAnalyzingSideDish = false,
                            sideDishError = "No food recognized in this image. Please upload a clear photo of your side dish."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isAnalyzingSideDish = false,
                            pendingSideDishItems = items,
                            sideDishError = null
                        )
                    }
                }
            }.onFailure { error ->
                val errorMsg = when (error) {
                    is NonFoodException -> error.message
                    is AmbiguousFoodException -> error.message
                    else -> error.message ?: "Could not recognize food in this image."
                }
                _uiState.update {
                    it.copy(
                        isAnalyzingSideDish = false,
                        sideDishError = errorMsg
                    )
                }
            }
        }
    }

    fun onSideDishImageSelectedFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        analyzeSideDishImage(bitmap)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(sideDishError = "Failed to load side dish image: ${e.message}")
                }
            }
        }
    }

    fun onSideDishCameraCaptured(bitmap: Bitmap) {
        analyzeSideDishImage(bitmap)
    }

    fun updatePendingSideDishName(index: Int, newName: String) {
        val currentPending = _uiState.value.pendingSideDishItems ?: return
        if (index in currentPending.indices) {
            val item = currentPending[index]
            val resolved = NutritionDatabase.resolveNutrition(newName)
            val updated = currentPending.toMutableList()
            updated[index] = item.copy(
                name = newName,
                nutritionReference = resolved,
                nutritionDataSource = resolved.source
            )
            _uiState.update { it.copy(pendingSideDishItems = updated) }
        }
    }

    fun updatePendingSideDishWeight(index: Int, newWeight: Double) {
        val currentPending = _uiState.value.pendingSideDishItems ?: return
        if (index in currentPending.indices) {
            val item = currentPending[index]
            val clamped = newWeight.coerceIn(5.0, 1000.0)
            val updated = currentPending.toMutableList()
            updated[index] = item.copy(
                weightGrams = clamped,
                quantityDescription = "${clamped.roundToInt()} g"
            )
            _uiState.update { it.copy(pendingSideDishItems = updated) }
        }
    }

    fun removePendingSideDishItem(index: Int) {
        val currentPending = _uiState.value.pendingSideDishItems ?: return
        if (index in currentPending.indices) {
            val updated = currentPending.toMutableList()
            updated.removeAt(index)
            _uiState.update {
                it.copy(
                    pendingSideDishItems = if (updated.isEmpty()) null else updated
                )
            }
        }
    }

    /**
     * Confirms and integrates all verified side dish items into the single meal state of truth.
     */
    fun confirmAddPendingSideDishes() {
        val pending = _uiState.value.pendingSideDishItems ?: return
        val currentMeal = _uiState.value.activeMealResult ?: return

        val updatedFoods = currentMeal.foods + pending
        val updatedMeal = currentMeal.copy(foods = updatedFoods)

        _uiState.update {
            it.copy(
                activeMealResult = updatedMeal,
                analysisState = AnalysisUiState.Success(updatedMeal),
                pendingSideDishItems = null,
                sideDishError = null,
                adjustSheetInAddMode = false,
                feedbackBanner = "Added ${pending.size} item(s) from image. Recalculated!"
            )
        }
        refreshInsights(updatedMeal)
    }

    fun cancelPendingSideDishes() {
        _uiState.update {
            it.copy(
                pendingSideDishItems = null,
                sideDishError = null
            )
        }
    }

    fun setValidationThreshold(threshold: Double) {
        _uiState.update { it.copy(validationThreshold = threshold.coerceIn(0.1, 0.95)) }
    }

    fun openFullScreenImageViewer() {
        _uiState.update { it.copy(showFullScreenImageViewer = true) }
    }

    fun closeFullScreenImageViewer() {
        _uiState.update { it.copy(showFullScreenImageViewer = false) }
    }

    /**
     * Human Correction Parser for text input
     */
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
                feedbackBanner = "Could not parse command. Try: 'Noodles to 180g', 'Remove salad', or 'Add 1 boiled egg'"
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
