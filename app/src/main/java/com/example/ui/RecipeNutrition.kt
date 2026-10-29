package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.data.kitchen.*
import com.example.data.nutrition.NutritionDatabase
import kotlin.math.roundToInt

@Composable
internal fun RecipeNutrition(recipe: CookbookRecipe, servings: Int) {
    val refs=NutritionDatabase.getAllReferences()
    val required=recipe.summary.ingredients.filterNot {it.optional}
    val resolved=required.mapNotNull { ingredient ->
        // Exact catalogue matches only: a generic dish heuristic is not a raw ingredient reference.
        val ref=refs.firstOrNull {canonical(it.displayName)==ingredient.key || canonical(it.foodKey.replace('_',' '))==ingredient.key}?:return@mapNotNull null
        val scaled=RecipeMatcher.scaled(ingredient,recipe.summary.servings,servings)
        val grams=when(scaled.unit) { "g"->scaled.quantity;"kg"->scaled.quantity?.times(1000);else->scaled.grams }?:return@mapNotNull null
        ref to grams
    }
    Text("Planned nutrition",style=MaterialTheme.typography.titleLarge)
    if(resolved.size==required.size) {
        val calories=resolved.sumOf {it.first.caloriesPer100g*it.second/100}/servings
        val protein=resolved.sumOf {it.first.proteinPer100g*it.second/100}/servings
        val carbs=resolved.sumOf {it.first.carbsPer100g*it.second/100}/servings
        val fat=resolved.sumOf {it.first.fatPer100g*it.second/100}/servings
        Text("About ${calories.roundToInt()} kcal · ${protein.roundToInt()} g protein · ${carbs.roundToInt()} g carbs · ${fat.roundToInt()} g fat per serving")
        Text("Calculated from local reference values and estimated ingredient weights; optional garnishes excluded. This is not the measured finished plate.",style=MaterialTheme.typography.bodySmall)
    } else Text("A complete estimate is unavailable: ${required.size-resolved.size} ingredients need an exact local reference or usable gram weight. Scan the finished plate in KalC rather than inventing totals.",style=MaterialTheme.typography.bodySmall)
}
