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
        val coconut=recipe().copy(ingredients=listOf(KitchenIngredient("coconut milk",100.0,"ml","ready")))
        assertTrue(RecipeMatcher.match(coconut,emptyList(),MealBrief(avoid=listOf("dairy"))).blocked.isEmpty())
        val shrimp=recipe().copy(ingredients=listOf(KitchenIngredient("prawns",100.0,"g","raw")))
        assertTrue(RecipeMatcher.match(shrimp,emptyList(),MealBrief(avoid=listOf("shellfish"))).blocked.isNotEmpty())
    }
    @Test fun plantMilkAvoidancesAndEggplantAreMatchedCorrectly() {
        fun blocked(name:String,avoid:String)=RecipeMatcher.match(recipe().copy(ingredients=listOf(KitchenIngredient(name,100.0))),emptyList(),MealBrief(avoid=listOf(avoid))).blocked.isNotEmpty()
        assertTrue(blocked("almond milk","nuts"))
        assertTrue(blocked("soy milk","soy"))
        assertFalse(blocked("coconut milk","dairy"))
        assertFalse(blocked("eggplant","egg"))
        assertTrue(blocked("salmon","fish"))
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
        repo.saveSession(CookingSession(recipe=cookbook(),servings=4,step=0,timerEndsAt=1234,brief=MealBrief(avoid=listOf("peanut")) ))
        assertEquals(4,repo.session()!!.servings)
        assertEquals(listOf("peanut"),repo.session()!!.brief!!.avoid)
        repo.setEnabled(false); assertFalse(repo.enabled)
        assertNotNull(repo.get(cooked.id))
        repo.delete(cooked.id)
        assertNull(repo.get(scan.id)!!.linkedCookedId)
        repo.clear()
    }
    @Test fun boundedPhotosCanBeRemovedWithoutLosingTheMeal() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo=HistoryRepository.get(context)
        val image=android.graphics.Bitmap.createBitmap(2400,1200,android.graphics.Bitmap.Config.ARGB_8888)
        val record=repo.save(HistoryRecord(kind=HistoryKind.SCANNED,title="Photo fixture"),image)
        val file=repo.photo(record)!!
        val bounds=android.graphics.BitmapFactory.Options().apply {inJustDecodeBounds=true}
        android.graphics.BitmapFactory.decodeFile(file.absolutePath,bounds)
        assertTrue(bounds.outWidth<=1000 && bounds.outHeight<=1000)
        repo.removePhoto(record.id)
        assertNull(repo.get(record.id)!!.imagePath)
        assertEquals("Photo fixture",repo.get(record.id)!!.title)
        assertFalse(file.exists())
        repo.clear();image.recycle()
    }
    @Test fun scanSnapshotsRoundTripCalculatedTotalsAndOriginalBreakdown() {
        val repo=HistoryRepository.get(ApplicationProvider.getApplicationContext())
        val meal=com.example.data.gemini.GeminiFoodService().getDeterministicFallbackAnalysis("rice dal aloo sabzi salad")
        val record=repo.save(HistoryRecord(kind=HistoryKind.SCANNED,title="Scan fixture",meal=meal,originalMeal=meal))
        val reopened=repo.get(record.id)!!
        assertEquals(meal.total,reopened.meal!!.total)
        assertEquals(meal.foods.map {it.name},reopened.originalMeal!!.foods.map {it.name})
        repo.delete(record.id)
    }
    @Test fun galleryDecodeBoundsPixelsAndAcceptsImagesWithoutExif() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val file=java.io.File.createTempFile("kitchen-image-",".png",context.cacheDir)
        val source=android.graphics.Bitmap.createBitmap(2400,1200,android.graphics.Bitmap.Config.ARGB_8888)
        file.outputStream().use {source.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        val decoded=KitchenImages.decode(context,android.net.Uri.fromFile(file))
        assertTrue(decoded.width in 1..1600 && decoded.height in 1..1600)
        decoded.recycle();source.recycle();file.delete()
    }
}
