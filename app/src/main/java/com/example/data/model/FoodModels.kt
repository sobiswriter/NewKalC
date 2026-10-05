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
    val nutritionReference: NutritionReference
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
