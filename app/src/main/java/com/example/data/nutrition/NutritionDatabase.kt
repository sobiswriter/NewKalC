package com.example.data.nutrition

import com.example.data.model.NutritionReference

object NutritionDatabase {

    private val database: List<NutritionReference> = listOf(
        // Indian Specialties
        NutritionReference(
            foodKey = "chicken_biryani",
            displayName = "Chicken Biryani",
            caloriesPer100g = 200.0,
            proteinPer100g = 8.8,
            carbsPer100g = 23.2,
            fatPer100g = 7.6,
            source = "USDA & IN Foods Data"
        ),
        NutritionReference(
            foodKey = "mutton_biryani",
            displayName = "Mutton Biryani",
            caloriesPer100g = 225.0,
            proteinPer100g = 9.5,
            carbsPer100g = 22.0,
            fatPer100g = 10.5,
            source = "Verified Indian Reference"
        ),
        NutritionReference(
            foodKey = "veg_biryani",
            displayName = "Vegetable Biryani / Pulao",
            caloriesPer100g = 165.0,
            proteinPer100g = 3.8,
            carbsPer100g = 27.5,
            fatPer100g = 4.2,
            source = "USDA Reference"
        ),
        NutritionReference(
            foodKey = "steamed_basmati_rice",
            displayName = "Steamed Basmati Rice",
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            carbsPer100g = 28.2,
            fatPer100g = 0.3,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "dal_tadka",
            displayName = "Yellow Dal Tadka",
            caloriesPer100g = 95.0,
            proteinPer100g = 5.8,
            carbsPer100g = 12.5,
            fatPer100g = 2.8,
            source = "IN Food Compendium"
        ),
        NutritionReference(
            foodKey = "dal_makhani",
            displayName = "Dal Makhani",
            caloriesPer100g = 145.0,
            proteinPer100g = 5.2,
            carbsPer100g = 14.0,
            fatPer100g = 7.8,
            source = "Verified Indian Reference"
        ),
        NutritionReference(
            foodKey = "aloo_sabzi",
            displayName = "Aloo Sabzi (Spiced Potatoes)",
            caloriesPer100g = 110.0,
            proteinPer100g = 2.1,
            carbsPer100g = 18.4,
            fatPer100g = 3.2,
            source = "Verified Indian Reference"
        ),
        NutritionReference(
            foodKey = "paneer_butter_masala",
            displayName = "Paneer Butter Masala",
            caloriesPer100g = 210.0,
            proteinPer100g = 9.5,
            carbsPer100g = 7.5,
            fatPer100g = 16.0,
            source = "Verified Reference DB"
        ),
        NutritionReference(
            foodKey = "butter_chicken",
            displayName = "Butter Chicken (Murgh Makhani)",
            caloriesPer100g = 175.0,
            proteinPer100g = 12.5,
            carbsPer100g = 6.0,
            fatPer100g = 11.2,
            source = "USDA Reference"
        ),
        NutritionReference(
            foodKey = "roti_chapati",
            displayName = "Whole Wheat Roti / Chapati",
            caloriesPer100g = 264.0,
            proteinPer100g = 9.0,
            carbsPer100g = 55.0,
            fatPer100g = 3.2,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "naan_bread",
            displayName = "Garlic / Butter Naan",
            caloriesPer100g = 290.0,
            proteinPer100g = 8.7,
            carbsPer100g = 50.5,
            fatPer100g = 6.2,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "cucumber_raita",
            displayName = "Cucumber Raita",
            caloriesPer100g = 60.0,
            proteinPer100g = 3.5,
            carbsPer100g = 4.5,
            fatPer100g = 3.2,
            source = "USDA Reference"
        ),
        NutritionReference(
            foodKey = "green_salad",
            displayName = "Fresh Green Salad (Cucumber, Tomato, Onion)",
            caloriesPer100g = 18.0,
            proteinPer100g = 0.8,
            carbsPer100g = 3.6,
            fatPer100g = 0.2,
            source = "USDA FoodData Central"
        ),

        // Healthy Fitness & Western Bowls
        NutritionReference(
            foodKey = "grilled_salmon",
            displayName = "Grilled Salmon Fillet",
            caloriesPer100g = 206.0,
            proteinPer100g = 22.1,
            carbsPer100g = 0.0,
            fatPer100g = 12.3,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "cooked_quinoa",
            displayName = "Cooked Fluffy Quinoa",
            caloriesPer100g = 120.0,
            proteinPer100g = 4.4,
            carbsPer100g = 21.3,
            fatPer100g = 1.9,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "steamed_broccoli",
            displayName = "Steamed Broccoli Florets",
            caloriesPer100g = 35.0,
            proteinPer100g = 2.8,
            carbsPer100g = 7.2,
            fatPer100g = 0.4,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "fresh_avocado",
            displayName = "Fresh Sliced Avocado",
            caloriesPer100g = 160.0,
            proteinPer100g = 2.0,
            carbsPer100g = 8.5,
            fatPer100g = 14.7,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "boiled_egg",
            displayName = "Boiled Egg",
            caloriesPer100g = 155.0,
            proteinPer100g = 12.6,
            carbsPer100g = 1.1,
            fatPer100g = 10.6,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "poached_egg",
            displayName = "Poached Egg",
            caloriesPer100g = 143.0,
            proteinPer100g = 12.5,
            carbsPer100g = 0.7,
            fatPer100g = 9.5,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "grilled_chicken_breast",
            displayName = "Grilled Chicken Breast",
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "sourdough_toast",
            displayName = "Toasted Sourdough Bread",
            caloriesPer100g = 260.0,
            proteinPer100g = 9.0,
            carbsPer100g = 49.0,
            fatPer100g = 2.5,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "cherry_tomatoes",
            displayName = "Cherry Tomatoes",
            caloriesPer100g = 18.0,
            proteinPer100g = 0.9,
            carbsPer100g = 3.9,
            fatPer100g = 0.2,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "olive_oil_dressing",
            displayName = "Extra Virgin Olive Oil / Dressing",
            caloriesPer100g = 884.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 100.0,
            source = "USDA FoodData Central"
        ),

        // Common Casual Meals & Fast Foods
        NutritionReference(
            foodKey = "beef_burger",
            displayName = "Cheeseburger / Beef Burger",
            caloriesPer100g = 254.0,
            proteinPer100g = 13.5,
            carbsPer100g = 24.0,
            fatPer100g = 12.0,
            source = "USDA Reference"
        ),
        NutritionReference(
            foodKey = "french_fries",
            displayName = "Crispy French Fries",
            caloriesPer100g = 312.0,
            proteinPer100g = 3.4,
            carbsPer100g = 41.4,
            fatPer100g = 15.0,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "cheese_pizza",
            displayName = "Cheese Pizza (Thin Crust)",
            caloriesPer100g = 266.0,
            proteinPer100g = 11.0,
            carbsPer100g = 33.0,
            fatPer100g = 10.0,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "hummus",
            displayName = "Hummus Dip",
            caloriesPer100g = 166.0,
            proteinPer100g = 7.9,
            carbsPer100g = 14.3,
            fatPer100g = 9.6,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "tofu_firm",
            displayName = "Pan-Fried / Firm Tofu",
            caloriesPer100g = 144.0,
            proteinPer100g = 15.0,
            carbsPer100g = 3.5,
            fatPer100g = 8.0,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "plain_greek_yogurt",
            displayName = "Plain Greek Yogurt (0% fat)",
            caloriesPer100g = 59.0,
            proteinPer100g = 10.0,
            carbsPer100g = 3.6,
            fatPer100g = 0.4,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "banana",
            displayName = "Fresh Banana",
            caloriesPer100g = 89.0,
            proteinPer100g = 1.1,
            carbsPer100g = 22.8,
            fatPer100g = 0.3,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "apple",
            displayName = "Fresh Apple",
            caloriesPer100g = 52.0,
            proteinPer100g = 0.3,
            carbsPer100g = 13.8,
            fatPer100g = 0.2,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "fried_noodles",
            displayName = "Spiced Stir-Fried Noodles / Chowmein",
            caloriesPer100g = 168.0,
            proteinPer100g = 5.5,
            carbsPer100g = 26.2,
            fatPer100g = 5.0,
            source = "USDA & Asian Foods Compendium"
        ),
        NutritionReference(
            foodKey = "egg_scramble_noodles",
            displayName = "Egg Scramble (Stir-Fried)",
            caloriesPer100g = 158.0,
            proteinPer100g = 12.0,
            carbsPer100g = 2.0,
            fatPer100g = 11.5,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "sauteed_vegetables",
            displayName = "Sautéed Vegetables (Onion, Cabbage, Capsicum)",
            caloriesPer100g = 55.0,
            proteinPer100g = 1.8,
            carbsPer100g = 7.5,
            fatPer100g = 2.2,
            source = "USDA FoodData Central"
        ),
        NutritionReference(
            foodKey = "kothu_roti",
            displayName = "Kothu Roti / Parotta",
            caloriesPer100g = 210.0,
            proteinPer100g = 7.5,
            carbsPer100g = 25.0,
            fatPer100g = 9.0,
            source = "Verified Indian Reference"
        )
    )

    fun getAllReferences(): List<NutritionReference> = database

    /**
     * Resolves nutrition from the database by fuzzy/keyword matching the food name.
     * If no direct match is found, creates an intelligent reference tagged with the source.
     */
    fun resolveNutrition(
        foodName: String,
        fallbackCaloriesPer100g: Double? = null,
        fallbackProteinPer100g: Double? = null,
        fallbackCarbsPer100g: Double? = null,
        fallbackFatPer100g: Double? = null
    ): NutritionReference {
        val normalized = foodName.lowercase().trim()

        // 1. Direct or partial match in verified catalog
        val matched = database.firstOrNull { ref ->
            val refKey = ref.foodKey.replace("_", " ").lowercase()
            val refDisplay = ref.displayName.lowercase()
            normalized.contains(refKey) || refKey.contains(normalized) ||
                    normalized.contains(refDisplay) || refDisplay.contains(normalized)
        }

        if (matched != null) return matched

        // 2. Keyword heuristic matching
        when {
            normalized.contains("noodle") || normalized.contains("chowmein") ||
                    normalized.contains("maggi") || normalized.contains("hakka") ||
                    normalized.contains("ramen") -> return database.first { it.foodKey == "fried_noodles" }
            normalized.contains("kothu") || normalized.contains("parotta") -> return database.first { it.foodKey == "kothu_roti" }
            normalized.contains("biryani") -> return database.first { it.foodKey == "chicken_biryani" }
            normalized.contains("rice") || normalized.contains("chawal") -> return database.first { it.foodKey == "steamed_basmati_rice" }
            normalized.contains("dal") || normalized.contains("daal") || normalized.contains("lentil") -> return database.first { it.foodKey == "dal_tadka" }
            normalized.contains("potato") || normalized.contains("aloo") -> return database.first { it.foodKey == "aloo_sabzi" }
            normalized.contains("salad") || normalized.contains("cucumber") -> return database.first { it.foodKey == "green_salad" }
            normalized.contains("egg") && normalized.contains("poached") -> return database.first { it.foodKey == "poached_egg" }
            normalized.contains("egg") -> return database.first { it.foodKey == "boiled_egg" }
            normalized.contains("salmon") || normalized.contains("fish") -> return database.first { it.foodKey == "grilled_salmon" }
            normalized.contains("chicken") -> return database.first { it.foodKey == "grilled_chicken_breast" }
            normalized.contains("quinoa") -> return database.first { it.foodKey == "cooked_quinoa" }
            normalized.contains("broccoli") -> return database.first { it.foodKey == "steamed_broccoli" }
            normalized.contains("avocado") -> return database.first { it.foodKey == "fresh_avocado" }
            normalized.contains("bread") || normalized.contains("toast") -> return database.first { it.foodKey == "sourdough_toast" }
            normalized.contains("roti") || normalized.contains("chapati") -> return database.first { it.foodKey == "roti_chapati" }
            normalized.contains("naan") -> return database.first { it.foodKey == "naan_bread" }
            normalized.contains("raita") -> return database.first { it.foodKey == "cucumber_raita" }
            normalized.contains("fries") -> return database.first { it.foodKey == "french_fries" }
            normalized.contains("burger") -> return database.first { it.foodKey == "beef_burger" }
            normalized.contains("pizza") -> return database.first { it.foodKey == "cheese_pizza" }
        }

        // 3. Fallback to AI-provided nutrition reference or standard meal averages
        val cal = fallbackCaloriesPer100g ?: 150.0
        val pro = fallbackProteinPer100g ?: 6.0
        val carb = fallbackCarbsPer100g ?: 20.0
        val fat = fallbackFatPer100g ?: 5.0

        return NutritionReference(
            foodKey = normalized.replace(" ", "_"),
            displayName = foodName.replaceFirstChar { it.uppercase() },
            caloriesPer100g = cal,
            proteinPer100g = pro,
            carbsPer100g = carb,
            fatPer100g = fat,
            source = "Reference DB (Heuristic / Vision Estimate)"
        )
    }
}
