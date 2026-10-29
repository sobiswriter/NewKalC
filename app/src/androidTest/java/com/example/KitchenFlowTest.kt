package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.auth.ApiKeyRepository
import com.example.data.kitchen.*
import com.example.ui.FoodCalorieViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class KitchenFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun setup() {
        context.getSharedPreferences("kalc_profile",0).edit().putBoolean("onboarded",true).commit()
        ApiKeyRepository.getInstance(context).setStartupPromptDismissed(true)
        HistoryRepository.get(context).apply {clear();setEnabled(true)}
    }
    private fun click(text: String) {
        compose.onNodeWithTag("kitchen_content").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.externalCacheDir,"$name.png").outputStream().use {image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        image.recycle()
    }
    @Test fun manualPantryAndBriefAreNativeAndDoNotRequireAi() {
        setup()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("nav_make_a_meal").performClick()
            compose.onNodeWithText("Good things\nstart here.").assertIsDisplayed()
            screenshot("v14-kitchen")
            click("Add ingredients by hand")
            compose.onNodeWithTag("add_ingredient").performClick()
            compose.onNodeWithTag("ingredient_name").performTextInput("chickpeas")
            compose.onNodeWithTag("ingredient_quantity").performTextInput("200")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            compose.onNodeWithText("cooked",useUnmergedTree=true).performScrollTo().performClick()
            compose.onNodeWithTag("save_ingredient").performClick()
            try { compose.waitUntil(5000) {compose.onAllNodesWithTag("ingredient_name").fetchSemanticsNodes().isEmpty()} }
            finally {screenshot("v14-confirmation")}
            compose.onNodeWithTag("confirm_ingredients").performScrollTo().performClick()
            click("Set the mood")
            compose.onNodeWithText("Your meal,\nyour way.").assertIsDisplayed()
            screenshot("v14-brief")
        }
    }
    @Test fun savedCookbookCooksAndConfirmsOfflineThenReopensAfterRestart() {
        setup()
        val ingredient=KitchenIngredient("chickpeas",200.0,"g","cooked")
        val summary=RecipeSummary(title="Offline chickpea bowl",description="A saved test dinner",servings=2,totalMinutes=8,activeMinutes=8,style="Mediterranean-inspired",effort="Easy",equipment=emptyList(),ingredients=listOf(ingredient))
        val recipe=CookbookRecipe(summary,listOf(CookingStep("Combine","Combine the cooked chickpeas.",listOf("chickpeas")),CookingStep("Serve","Divide between two bowls.",listOf("chickpeas"))))
        val repo=HistoryRepository.get(context)
        val saved=repo.save(HistoryRecord(kind=HistoryKind.RECIPE,title=summary.title,recipe=recipe,servings=2))
        var cookedId:String?=null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> val vm=ViewModelProvider(activity)[FoodCalorieViewModel::class.java];vm.kitchen.editIngredient(-1,ingredient);vm.kitchen.confirmPantry();vm.openHistory();vm.kitchen.openHistory(saved.id) }
            compose.waitUntil(10000) {compose.onAllNodesWithText("Full recipe / make again").fetchSemanticsNodes().isNotEmpty()}
            click("Full recipe / make again")
            click("Start cooking")
            compose.waitUntil(10000) {compose.onAllNodesWithText("Next step").fetchSemanticsNodes().isNotEmpty()}
            screenshot("v14-cooking")
            click("Next step")
            click("Finish")
            click("Confirm cooked")
            compose.waitUntil(10000) {repo.rows(HistoryKind.COOKED).isNotEmpty()}
            cookedId=repo.rows(HistoryKind.COOKED).first().id
            screenshot("v14-finished")
            assertNotNull(repo.get(cookedId!!)!!.recipe)
        }
        ActivityScenario.launch(MainActivity::class.java).use {scenario ->
            scenario.onActivity {activity -> val vm=ViewModelProvider(activity)[FoodCalorieViewModel::class.java];vm.openHistory();vm.kitchen.openHistory(cookedId!!)}
            compose.waitUntil(10000) {compose.onAllNodesWithText("Full recipe / make again").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Offline chickpea bowl").assertExists()
            screenshot("v14-history")
        }
    }
}
