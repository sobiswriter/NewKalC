package com.example

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.auth.ApiKeyRepository
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VisualFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.externalCacheDir, "$name.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }

    @Test fun redesignedMealEditingNavigationAndConnectionRemainUsable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("kalc_profile", 0).edit().putBoolean("onboarded", true).commit()
        val keys = ApiKeyRepository.getInstance(context)
        keys.clearApiKey()
        keys.setStartupPromptDismissed(true)
        context.getSharedPreferences("kalc_ai_connection", 0).edit().putString("provider", "AI_STUDIO").commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onAllNodesWithText("KalC").onFirst().assertIsDisplayed()
            compose.onNodeWithText("SAMPLE MEAL").assertIsDisplayed()
            compose.onNodeWithTag("scan_meal_button").assertDoesNotExist()
            compose.onNodeWithTag("camera_button").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithTag("gallery_button").assertIsDisplayed().assertHasClickAction()
            capture("redesign-meal")
            compose.onNodeWithTag("hero_adjust_button").performScrollTo().performClick()
            val before = compose.onNodeWithText("Live Total:", substring = true).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
            compose.onAllNodesWithContentDescription("Increase portion by 25g").onFirst().performClick()
            val after = compose.onNodeWithText("Live Total:", substring = true).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
            assertNotEquals(before, after)
            capture("redesign-adjust")
            compose.onNodeWithTag("recalculate_button").performClick()
            compose.onNodeWithTag("nav_insights").performClick()
            compose.onNodeWithText("A little perspective").assertIsDisplayed()
            capture("redesign-insights")
            // The recalculation snackbar temporarily overlays the content footer.
            compose.waitUntil(10000) {
                compose.onAllNodesWithText("Recalculated:", substring = true).fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithTag("meal_content").performScrollToNode(hasTestTag("ask_ai_action_button"))
            compose.onNodeWithTag("ask_ai_action_button").performClick()
            try {
                compose.waitUntil(10000) { runCatching { compose.onNodeWithTag("ask_ai_input_field").assertIsDisplayed() }.isSuccess }
            } finally { capture("redesign-chat") }
            compose.onNodeWithTag("ask_ai_input_field").assertIsDisplayed()
            capture("redesign-chat")
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithContentDescription("More options").performClick()
            compose.onNodeWithText("API Key & AI Settings").performClick()
            compose.onNodeWithTag("provider_vertex").performClick()
            compose.onNodeWithTag("ai_credential_input").assertIsDisplayed()
            capture("redesign-connection")
        }
    }
}
