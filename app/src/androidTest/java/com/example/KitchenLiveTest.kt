package com.example

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.auth.AiProvider
import com.example.data.auth.VertexAccessRepository
import com.example.data.gemini.GeminiFoodService
import com.example.data.kitchen.*
import com.example.data.profile.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.ViewModelProvider
import com.example.ui.FoodCalorieViewModel
import com.example.ui.AnalysisUiState
import com.example.data.auth.ApiKeyRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay

@RunWith(AndroidJUnit4::class)
class KitchenLiveTest {
    @Test fun livePlateScanIsSavedWithItsCookedRecipeLink() = runBlocking {
        val passkey=InstrumentationRegistry.getArguments().getString("serverPasskey")
        assumeTrue("Owner runtime credentials required.",!passkey.isNullOrBlank())
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        ApiKeyRepository.getInstance(context).setStartupPromptDismissed(true)
        context.getSharedPreferences("kalc_profile",0).edit().putBoolean("onboarded",true).commit()
        val repo=HistoryRepository.get(context);repo.setEnabled(true)
        val ingredient=KitchenIngredient("chickpeas",200.0,"g","cooked")
        val summary=RecipeSummary(title="Synthetic cooking link",description="Test only, no real cooking",servings=2,totalMinutes=8,activeMinutes=8,style="Any",effort="Easy",equipment=emptyList(),ingredients=listOf(ingredient))
        val recipe=CookbookRecipe(summary,listOf(CookingStep("Combine","Combine cooked chickpeas",listOf("chickpeas"))))
        val cooked=repo.save(HistoryRecord(kind=HistoryKind.COOKED,title=summary.title,recipe=recipe))
        ActivityScenario.launch(MainActivity::class.java).use {scenario ->
            lateinit var vm:FoodCalorieViewModel
            val unlocked=CompletableDeferred<Boolean>()
            scenario.onActivity {activity -> vm=ViewModelProvider(activity)[FoodCalorieViewModel::class.java];vm.selectAiProvider(AiProvider.VERTEX);vm.unlockVertex(passkey!!) {ok,_->unlocked.complete(ok)} }
            assertTrue(withTimeout(35000) {unlocked.await()})
            val plate=BitmapFactory.decodeResource(context.resources,R.drawable.sample_thali)
            scenario.onActivity {vm.scanCookedPlate(plate,cooked.id)}
            withTimeout(90000) {while(vm.uiState.value.analysisState is AnalysisUiState.Analyzing)delay(200)}
            assertTrue("A real plate analysis must succeed",vm.uiState.value.analysisState is AnalysisUiState.Success)
            withTimeout(10000) {while(repo.linked(cooked.id).isEmpty())delay(100)}
            val saved=repo.linked(cooked.id).first()
            assertNotNull(saved.meal);assertNotNull(repo.photo(saved));assertEquals(saved.meal!!.total,repo.get(saved.id)!!.meal!!.total)
        }
    }
    @Test fun liveIngredientRecipeCookbookChatAndReviewContracts() = runBlocking {
        val passkey=InstrumentationRegistry.getArguments().getString("serverPasskey")
        assumeTrue("Owner runtime credentials required for live verification.",!passkey.isNullOrBlank())
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val access=VertexAccessRepository(context);val previous=access.provider
        try {
            access.selectProvider(AiProvider.VERTEX);access.unlock(passkey!!).getOrThrow()
            val service=GeminiFoodService(access);val ai=KitchenAiService(service)
            val sheet=BitmapFactory.decodeResource(context.resources,R.drawable.recipe_editorial)
            val image=android.graphics.Bitmap.createBitmap(sheet,0,0,sheet.width/4,sheet.height/2)
            val detected=ai.recognize(listOf(image),"");assertTrue(detected.isNotEmpty())
            val pantry=listOf(KitchenIngredient("chickpeas",400.0,"g","cooked"),KitchenIngredient("tomato",300.0,"g","raw"),KitchenIngredient("spinach",150.0,"g","raw"),KitchenIngredient("oil",30.0,"ml","ready"),KitchenIngredient("salt",2.0,"tsp","ready"),KitchenIngredient("cumin",2.0,"tsp","dry"))
            val brief=MealBrief(minutes=45,equipment=listOf("hob"),avoid=listOf("peanut"))
            val summaries=ai.suggest(pantry,brief,UserProfile(),"",emptyList());assertTrue(summaries.isNotEmpty())
            val recipe=ai.detail(summaries.first(),"");recipe.validated()
            val answer=ai.chat(recipe,brief,0,"How can I make this milder?",UserProfile(),"");assertTrue(answer.isNotBlank())
            val cooked=HistoryRecord(kind=HistoryKind.COOKED,title=recipe.summary.title,recipe=recipe,note="Synthetic test: a little dry, mild seasoning; no real meal was cooked.")
            val scan=HistoryRecord(kind=HistoryKind.SCANNED,title="Synthetic plate",meal=service.getDeterministicFallbackAnalysis("rice dal aloo sabzi salad"))
            val review=ai.review(cooked,scan,"");assertTrue(review.uncertain.isNotEmpty());assertTrue(review.nextTime.isNotEmpty())
        } finally { access.lock();access.selectProvider(previous) }
    }
}
