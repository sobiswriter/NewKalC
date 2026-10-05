package com.example.data.model

import java.util.UUID
import kotlin.math.roundToInt

/**
 * Standard reference nutrition per 100g.
 * Represents the verified nutrition database source of truth.
 */
data class NutritionReference(
    val id: String = UUID.randomUUID().toString(),
    val foodKey: String,
    val displayName: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val source: String = "USDA FoodData Central"
)

enum class FoodDetectionSource {
    ORIGINAL_IMAGE,
    IMAGE_ADDITION,
    MANUAL_SEARCH
}

/**
 * Result of the pre-analysis Food Validation Gate.
 */
data class FoodValidationResult(
    val isFood: Boolean,
    val confidence: Double,
    val reason: String? = null,
    val detectedFoodNames: List<String> = emptyList()
)

/**
 * Specific validation exceptions thrown by the Food Validation Gate.
 */
class NonFoodException(
    override val message: String = "Please provide a clear image of food or a meal to analyze.",
    val disclaimer: String = "Food analysis is an estimate and should not be treated as medical or dietary advice."
) : Exception(message)

class AmbiguousFoodException(
    override val message: String = "Couldn't confidently identify the food. Please upload a clearer image showing the food.",
    val confidence: Double = 0.0,
    val disclaimer: String = "Food analysis is an estimate and should not be treated as medical or dietary advice."
) : Exception(message)

/**
 * An identified food item with detected amount, portion weight, visual reasoning, and resolved nutrition.
 */
data class DetectedFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val quantityDescription: String,
    val weightGrams: Double,
    val cookingMethod: String = "Standard preparation",
    val confidence: Double = 0.85,
    val visualCues: String = "Estimated from dish boundaries and depth",
    val nutritionReference: NutritionReference,
    val source: FoodDetectionSource = FoodDetectionSource.ORIGINAL_IMAGE,
    val assumptions: String = "Portion estimated from visible vessel dimensions; standard preparation assumed.",
    val nutritionDataSource: String = "Verified Nutrition Database (USDA FoodData Central reference)"
) {
    // Calculated directly from the Nutrition Database source of truth:
    val calories: Int
        get() = ((weightGrams / 100.0) * nutritionReference.caloriesPer100g).roundToInt()

    val protein: Double
        get() = ((weightGrams / 100.0) * nutritionReference.proteinPer100g * 10).roundToInt() / 10.0

    val carbs: Double
        get() = ((weightGrams / 100.0) * nutritionReference.carbsPer100g * 10).roundToInt() / 10.0

    val fat: Double
        get() = ((weightGrams / 100.0) * nutritionReference.fatPer100g * 10).roundToInt() / 10.0
}

/**
 * Aggregated meal totals calculated from all active food items.
 */
data class MealTotals(
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/**
 * Concise health and nutrition pointers derived from the final edited meal.
 */
data class NutritionInsights(
    val headline: String,
    val positives: List<String>,
    val concerns: List<String>,
    val suggestions: List<String>
)

/**
 * Chat Q&A message for the "Ask About This Food" feature.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Complete meal analysis result including items, totals, uncertainty explanation, and overall confidence.
 */
data class MealAnalysisResult(
    val foods: List<DetectedFoodItem>,
    val overallConfidence: Double,
    val uncertaintyExplanation: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val total: MealTotals
        get() = MealTotals(
            calories = foods.sumOf { it.calories },
            protein = (foods.sumOf { it.protein } * 10).roundToInt() / 10.0,
            carbs = (foods.sumOf { it.carbs } * 10).roundToInt() / 10.0,
            fat = (foods.sumOf { it.fat } * 10).roundToInt() / 10.0
        )

    /**
     * Converts to the exact JSON structure requested in the MVP specification.
     */
    fun toFormattedJson(): String {
        val total = this.total
        val foodsJson = foods.joinToString(separator = ",\n") { food ->
            """    {
      "name": "${food.name.replace("\"", "\\\"")}",
      "quantity": "${food.weightGrams.roundToInt()} g",
      "calories": ${food.calories},
      "protein": ${food.protein},
      "carbs": ${food.carbs},
      "fat": ${food.fat},
      "confidence": ${((food.confidence * 100).roundToInt()) / 100.0}
    }"""
        }

        return """{
  "foods": [
$foodsJson
  ],
  "total": {
    "calories": ${total.calories},
    "protein": ${total.protein},
    "carbs": ${total.carbs},
    "fat": ${total.fat}
  },
  "overall_confidence": ${((overallConfidence * 100).roundToInt()) / 100.0},
  "uncertainty_explanation": "${uncertaintyExplanation.replace("\"", "\\\"")}"
}"""
    }
}
