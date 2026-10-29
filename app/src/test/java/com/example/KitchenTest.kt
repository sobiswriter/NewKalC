package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.kitchen.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KitchenTest {
    private val chickpeas = KitchenIngredient("chickpeas", 200.0, "g", "cooked")
    private fun recipe() = RecipeSummary(title="Chickpea bowl", description="A simple bowl", servings=2, totalMinutes=8,
        activeMinutes=8, style="Mediterranean", effort="Easy", equipment=emptyList(), ingredients=listOf(chickpeas))
    private fun cookbook() = CookbookRecipe(recipe(), listOf(CookingStep("Combine", "Combine the cooked chickpeas.", listOf("chickpeas"))))
    @Test fun quantitiesStatesUnitsAndEquipmentAreNotAssumed() {
        val r = recipe()
        assertTrue(RecipeMatcher.match(r, listOf(chickpeas), MealBrief()).ready)
        assertEquals(200.0, RecipeMatcher.match(r, listOf(chickpeas), MealBrief(servings=4)).missing.single().quantity!!, .01)
        assertFalse(RecipeMatcher.match(r, listOf(chickpeas.copy(state="dry")), MealBrief()).ready)
        assertFalse(RecipeMatcher.match(r, listOf(chickpeas.copy(quantity=null)), MealBrief()).ready)
        assertTrue(RecipeMatcher.match(r, listOf(chickpeas.copy(quantity=.2,unit="kg")), MealBrief()).ready)
        assertTrue(RecipeMatcher.match(r.copy(equipment=listOf("oven")), listOf(chickpeas), MealBrief()).blocked.isNotEmpty())
        assertEquals(null, RecipeMatcher.quick(listOf(RecipeMatcher.match(r, listOf(chickpeas), MealBrief(minutes=5)))))
    }
    @Test fun shortagesDoNotMakeEasyRecipesPremiumAndConstraintsExclude() {
        val missing = RecipeMatcher.match(recipe(), emptyList(), MealBrief())
        assertEquals(RecipeGroup.EXTRAS, missing.group)
        val milk = recipe().copy(ingredients=listOf(KitchenIngredient("yogurt",100.0)), allergens=listOf("dairy"))
        assertTrue(RecipeMatcher.match(milk,emptyList(),MealBrief(avoid=listOf("dairy"))).blocked.isNotEmpty())
        assertEquals("chickpeas", canonical("Garbanzo beans"))
    }
    @Test fun methodRequiresEveryIngredientAndRejectsUnknownReferences() {
        cookbook().validated()
        assertThrows(IllegalArgumentException::class.java) { cookbook().copy(steps=listOf(CookingStep("Add", "Add salt",listOf("salt")))).validated() }
        assertThrows(IllegalArgumentException::class.java) { chickpeas.copy(quantity=Double.NaN).validated() }
    }
    @Test fun localHistoryRetainsVersionsAndDetachesLinkedScansOnDelete() {
        val repo=HistoryRepository.get(ApplicationProvider.getApplicationContext())
        repo.clear(); repo.setEnabled(true)
        val cooked=repo.save(HistoryRecord(kind=HistoryKind.COOKED,title="Dinner",recipe=cookbook()))
        val scan=repo.save(HistoryRecord(kind=HistoryKind.SCANNED,title="Plate",linkedCookedId=cooked.id))
        assertEquals(1,repo.rows().size)
        assertEquals(cookbook().summary.title,repo.get(cooked.id)!!.recipe!!.summary.title)
        assertEquals(scan.id,repo.linked(cooked.id).single().id)
        repo.saveSession(CookingSession(recipe=cookbook(),servings=4,step=0,timerEndsAt=1234))
        assertEquals(4,repo.session()!!.servings)
        repo.setEnabled(false); assertFalse(repo.enabled)
        assertNotNull(repo.get(cooked.id))
        repo.delete(cooked.id)
        assertNull(repo.get(scan.id)!!.linkedCookedId)
        repo.clear()
    }
}
