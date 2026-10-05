package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.ApiKeyRepository
import com.example.data.gemini.GeminiFoodService
import com.example.data.model.DetectedFoodItem
import com.example.data.model.MealAnalysisResult
import com.example.data.nutrition.NutritionDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Food Calorie AI", appName)
    }

    @Test
    fun `test api key repository save and clear`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ApiKeyRepository(context)

        repo.clearApiKey()
        repo.saveApiKey("AIzaSyTestKey12345")
        assertTrue(repo.hasValidApiKey())
        assertEquals("AIzaSyTestKey12345", repo.getEffectiveApiKey())

        repo.clearApiKey()
        assertEquals("", repo.getEffectiveApiKey())
    }

    @Test
    fun `test nutrition resolution and portion calculation`() {
        // Feature 1 Step 1: Spec verification
        // Detected: Chicken biryani, 250 g, ~200 kcal / 100 g => ~500 kcal
        val nutritionRef = NutritionDatabase.resolveNutrition("Chicken Biryani")
        assertNotNull(nutritionRef)
        assertEquals(200.0, nutritionRef.caloriesPer100g, 0.01)

        val foodItem = DetectedFoodItem(
            name = "Chicken Biryani",
            quantityDescription = "1 medium bowl",
            weightGrams = 250.0,
            nutritionReference = nutritionRef
        )

        assertEquals(500, foodItem.calories)
        assertEquals(22.0, foodItem.protein, 0.1)
        assertEquals(58.0, foodItem.carbs, 0.1)
        assertEquals(19.0, foodItem.fat, 0.1)

        val meal = MealAnalysisResult(
            foods = listOf(foodItem),
            overallConfidence = 0.81,
            uncertaintyExplanation = "Portion size is the largest source of uncertainty."
        )

        assertEquals(500, meal.total.calories)
        assertEquals(0.81, meal.overallConfidence, 0.01)
    }

    @Test
    fun `test meal editing and instant DB recalculation`() {
        // Feature 1 Step 2 & 3:
        // Edit portion (250g -> 180g), Add side dish (1 Boiled Egg 50g), Verify DB calculation
        val biryaniRef = NutritionDatabase.resolveNutrition("Chicken Biryani")
        val eggRef = NutritionDatabase.resolveNutrition("Boiled Egg")

        val biryani = DetectedFoodItem(
            name = "Chicken Biryani",
            quantityDescription = "180 g",
            weightGrams = 180.0,
            nutritionReference = biryaniRef
        )
        // 180g * 200 kcal / 100g = 360 kcal
        assertEquals(360, biryani.calories)

        val egg = DetectedFoodItem(
            name = "Boiled Egg",
            quantityDescription = "50 g",
            weightGrams = 50.0,
            nutritionReference = eggRef
        )
        // 50g * 155 kcal / 100g = 78 kcal
        assertEquals(78, egg.calories)

        val editedMeal = MealAnalysisResult(
            foods = listOf(biryani, egg),
            overallConfidence = 0.85,
            uncertaintyExplanation = "Portion adjusted."
        )

        // Totals: 360 + 78 = 438 kcal
        assertEquals(438, editedMeal.total.calories)
        assertTrue(editedMeal.total.protein > 20.0)
    }

    @Test
    fun `test AI nutrition insights and Q&A`() = runBlocking {
        // Feature 1 Step 4 & 5: Insights and Q&A
        val service = GeminiFoodService()
        val biryaniRef = NutritionDatabase.resolveNutrition("Chicken Biryani")
        val meal = MealAnalysisResult(
            foods = listOf(
                DetectedFoodItem(
                    name = "Chicken Biryani",
                    quantityDescription = "250 g",
                    weightGrams = 250.0,
                    nutritionReference = biryaniRef
                )
            ),
            overallConfidence = 0.82,
            uncertaintyExplanation = "Test"
        )

        val insights = service.getDeterministicNutritionInsights(meal)
        assertNotNull(insights.headline)
        assertTrue(insights.positives.isNotEmpty())
        assertTrue(insights.suggestions.isNotEmpty())

        val answer = service.askAboutFood(
            question = "Is this meal good for fat loss?",
            meal = meal,
            previousChat = emptyList(),
            bitmap = null
        ).getOrNull()

        assertNotNull(answer)
        assertTrue(answer!!.contains("kcal"))
    }
}
