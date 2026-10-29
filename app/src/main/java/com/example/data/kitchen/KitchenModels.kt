package com.example.data.kitchen

import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

data class KitchenIngredient(
    val name: String, val quantity: Double? = null, val unit: String = "g",
    val state: String = "unknown", val optional: Boolean = false,
    val grams: Double? = null, val useSoon: Boolean = false
) {
    val key: String get() = canonical(name)
    fun validated(): KitchenIngredient {
        require(name.isNotBlank() && name.length <= 100 && key.isNotBlank()) { "Use a short ingredient name." }
        require(quantity == null || quantity.isFinite() && quantity > 0 && quantity <= 100000) { "Enter a positive ingredient amount." }
        require(unit in units && state in states) { "Review the ingredient unit and preparation state." }
        require(grams == null || grams.isFinite() && grams > 0 && grams <= 100000) { "Invalid gram estimate." }
        return copy(name = name.trim())
    }
    companion object {
        val units = listOf("g", "kg", "ml", "l", "count", "tsp", "tbsp")
        val states = listOf("unknown", "raw", "dry", "cooked", "ready")
    }
}

fun canonical(name: String): String {
    val clean = name.lowercase().trim().replace(Regex("[^\\p{L}\\p{N} ]"), "").replace(Regex("\\s+"), " ")
    return when (clean) {
        "garbanzo beans", "garbanzo", "chickpea", "chana" -> "chickpeas"
        "tomatoes" -> "tomato"
        "onions" -> "onion"
        "eggs" -> "egg"
        "yoghurt", "curd" -> "yogurt"
        "coriander leaves" -> "cilantro"
        else -> clean
    }
}

data class MealBrief(
    val servings: Int = 2, val minutes: Int = 30, val style: String = "Surprise me",
    val avoid: List<String> = emptyList(), val equipment: List<String> = emptyList(),
    val heat: String = "Mild", val profileConsent: Boolean = false
) {
    fun validated(): MealBrief {
        require(servings in 1..12 && minutes in 5..240) { "Choose 1–12 portions and 5–240 minutes." }
        require(style.length <= 100 && avoid.size <= 20 && avoid.all { it.length in 1..80 })
        require(equipment.size <= 10 && equipment.all { it.length in 1..50 })
        return this
    }
}

data class RecipeSummary(
    val id: String = UUID.randomUUID().toString(), val title: String, val description: String,
    val servings: Int, val totalMinutes: Int, val activeMinutes: Int,
    val style: String, val effort: String, val equipment: List<String>,
    val ingredients: List<KitchenIngredient>, val allergens: List<String> = emptyList(),
    val balancedReason: String = "", val imageIndex: Int = 0
) {
    fun validated(): RecipeSummary {
        require(id.length in 1..100 && title.length in 1..100 && description.length in 1..800)
        require(servings in 1..12 && totalMinutes in 1..480 && activeMinutes in 1..totalMinutes)
        require(effort in listOf("Easy", "Everyday", "Project") && style.length <= 100)
        require(equipment.size <= 10 && equipment.all { it.length in 1..50 })
        require(ingredients.size in 1..35 && allergens.size <= 20 && allergens.all { it.length in 1..60 })
        ingredients.forEach { require(it.validated().quantity != null) { "Recipe ingredients need exact quantities." } }
        require(ingredients.map { it.key to it.state }.distinct().size == ingredients.size) { "Duplicate recipe ingredients." }
        require(balancedReason.length <= 500)
        return this
    }
}

data class CookingStep(val title: String, val instruction: String, val ingredientKeys: List<String>, val timerMinutes: Int = 0)
data class CookbookRecipe(
    val summary: RecipeSummary, val steps: List<CookingStep>, val notes: List<String> = emptyList(),
    val version: String = UUID.randomUUID().toString()
) {
    fun validated(): CookbookRecipe {
        summary.validated()
        require(steps.size in 1..25 && notes.size <= 10 && notes.all { it.length in 1..600 })
        val keys = summary.ingredients.map { it.key }.toSet()
        steps.forEach {
            require(it.title.length in 1..100 && it.instruction.length in 1..1800 && it.timerMinutes in 0..240)
            require(it.ingredientKeys.all { k -> k in keys }) { "A step refers to an ingredient outside the recipe." }
        }
        require(summary.ingredients.filterNot { it.optional }.all { ingredient -> steps.any { ingredient.key in it.ingredientKeys } }) {
            "Every required ingredient must appear in the method."
        }
        return this
    }
}

enum class RecipeGroup(val label: String) { NOW("Cook now"), EXTRAS("A few extras"), EFFORT("Worth the effort") }
data class RecipeMatch(val recipe: RecipeSummary, val group: RecipeGroup, val missing: List<KitchenIngredient>, val blocked: List<String>, val withinTime: Boolean) {
    val ready: Boolean get() = missing.isEmpty() && blocked.isEmpty() && withinTime
}

object RecipeMatcher {
    private fun base(unit: String) = when(unit) { "kg" -> "g"; "l" -> "ml"; "tbsp" -> "tsp"; else -> unit }
    private fun factor(unit: String) = when(unit) { "kg", "l" -> 1000.0; "tbsp" -> 3.0; else -> 1.0 }
    fun scaled(i: KitchenIngredient, from: Int, to: Int): KitchenIngredient {
        val q = i.quantity?.times(to.toDouble() / from)
        return i.copy(quantity = if (i.key == "egg" && i.unit == "count" && q != null) ceil(q) else q, grams = i.grams?.times(to.toDouble() / from))
    }
    fun match(recipe: RecipeSummary, pantry: List<KitchenIngredient>, brief: MealBrief): RecipeMatch {
        val missing = recipe.ingredients.filterNot { it.optional }.mapNotNull { raw ->
            val need = scaled(raw, recipe.servings, brief.servings)
            val stock = pantry.filter { it.key == need.key && base(it.unit) == base(need.unit) &&
                (need.state == "unknown" || it.state == need.state || need.state in listOf("ready", "cooked") && it.state in listOf("ready", "cooked")) }
                .sumOf { (it.quantity ?: 0.0) * factor(it.unit) }
            val short = max(0.0, need.quantity!! * factor(need.unit) - stock) / factor(need.unit)
            if(short > 0.001) need.copy(quantity = short) else null
        }
        val blocked = mutableListOf<String>()
        val ingredientNames=recipe.ingredients.joinToString(" ") {it.name}.lowercase()
            .replace(Regex("(coconut|almond|oat|soy|rice) milk"),"plant drink")
            .replace(Regex("(peanut|almond|sunflower) butter"),"plant spread")
        val names = ingredientNames+" "+recipe.allergens.joinToString(" ").lowercase()
        brief.avoid.forEach { avoid ->
            val meat=listOf("chicken","beef","pork","lamb","mutton","fish","salmon","tuna","shrimp","prawn","crab","lobster","gelatin","meat")
            val dairy=listOf("dairy", "milk", "yogurt", "yoghurt", "cheese", "butter", "ghee", "cream")
            val tokens = when(avoid.lowercase()) {
                "dairy" -> dairy
                "gluten" -> listOf("gluten", "wheat", "flour", "bread", "pasta", "soy sauce", "barley", "rye")
                "nuts" -> listOf("nut", "almond", "cashew", "walnut", "pistachio")
                "shellfish" -> listOf("shellfish","shrimp","prawn","crab","lobster","mussel","clam","oyster")
                "peanut" -> listOf("peanut","groundnut","arachis")
                "soy" -> listOf("soy","tofu","edamame","miso")
                "sesame" -> listOf("sesame","tahini","gingelly")
                "vegetarian" -> meat
                "vegan" -> meat+dairy+listOf("egg","honey")
                else -> listOf(canonical(avoid))
            }
            if(tokens.any { it.isNotBlank() && names.contains(it) }) blocked += "Avoids $avoid"
        }
        recipe.equipment.filterNot { tool -> brief.equipment.any { it.equals(tool, true) } }.forEach { blocked += "Needs $it" }
        val group = if(recipe.effort == "Project" || recipe.totalMinutes > 45) RecipeGroup.EFFORT else if(missing.isNotEmpty()) RecipeGroup.EXTRAS else RecipeGroup.NOW
        return RecipeMatch(recipe, group, missing, blocked, recipe.totalMinutes <= brief.minutes)
    }
    fun quick(matches: List<RecipeMatch>) = matches.filter { it.ready && it.group == RecipeGroup.NOW }.minByOrNull { it.recipe.totalMinutes }
    fun healthy(matches: List<RecipeMatch>) = matches.filter { it.ready && it.group == RecipeGroup.NOW }.maxByOrNull { balancedScore(it.recipe) }
    private fun balancedScore(r: RecipeSummary): Int {
        val names = r.ingredients.joinToString { it.key }
        return listOf("spinach", "broccoli", "carrot", "tomato", "chickpeas", "lentil", "bean", "tofu", "quinoa").count { it in names }
    }
}

data class CookingSession(
    val id: String = UUID.randomUUID().toString(), val recipe: CookbookRecipe,
    val servings: Int, val step: Int = 0, val timerEndsAt: Long? = null,
    val confirmedCooked: Boolean = false, val note: String = "", val brief: MealBrief? = null
)

data class CookingReview(val observations: List<String>, val uncertain: List<String>, val nextTime: List<String>)
