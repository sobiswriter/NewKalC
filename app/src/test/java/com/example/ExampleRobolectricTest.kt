package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.ApiKeyRepository
import com.example.data.model.DetectedFoodItem
import com.example.data.model.MealAnalysisResult
import com.example.data.nutrition.NutritionDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        // Spec verification:
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

        // Human correction test: 250g -> 180g
        val correctedItem = foodItem.copy(weightGrams = 180.0)
        assertEquals(360, correctedItem.calories)

        val meal = MealAnalysisResult(
            foods = listOf(foodItem),
            overallConfidence = 0.81,
            uncertaintyExplanation = "Portion size is the largest source of uncertainty."
        )

        assertEquals(500, meal.total.calories)
        assertEquals(0.81, meal.overallConfidence, 0.01)
    }
}
