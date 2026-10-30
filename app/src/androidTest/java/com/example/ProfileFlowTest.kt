package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.auth.ApiKeyRepository
import com.example.data.profile.ProfileRepository
import com.example.data.profile.UserProfile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProfileFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = ProfileRepository(context)

    @Before fun setup() {
        context.getSharedPreferences("kalc_profile", 0).edit().clear().commit()
        val keys = ApiKeyRepository.getInstance(context)
        keys.clearApiKey(); keys.setStartupPromptDismissed(true)
        context.getSharedPreferences("kalc_ai_connection", 0).edit().putString("provider", "AI_STUDIO").commit()
    }
    private fun waitFor(tag: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(context.externalCacheDir, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }

    @Test fun skipDiscardsDraftAndDoesNotShowAgain() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("skip_profile")
            compose.onNodeWithTag("profile_goal").performTextInput("UnsavedDraftFixture")
            compose.onNodeWithTag("skip_profile").performClick()
            waitFor("nav_profile")
            compose.onNodeWithTag("scan_meal_button").assertDoesNotExist()
            assertEquals(UserProfile(), repository.load().getOrThrow())
            assertTrue(repository.onboarded)
            it.recreate()
            waitFor("nav_profile")
            compose.onNodeWithTag("skip_profile").assertDoesNotExist()
        }
    }

    @Test fun onboardingValidatesSavesOffAndAllowsConsentAndDeletion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("profile_name")
            capture("v13-onboarding-goal")
            compose.onNodeWithTag("profile_name").performTextInput("Alex")
            compose.onNodeWithTag("profile_goal").performTextInput("Build strength")
            compose.onNodeWithTag("profile_next").performClick()
            compose.onNodeWithTag("profile_age").performTextInput("29")
            compose.onNodeWithTag("profile_height").performTextInput("999")
            compose.onNodeWithTag("profile_next").performClick()
            compose.onNodeWithText("Enter a height between 30 and 260 cm, or leave it blank.").assertIsDisplayed()
            compose.onNodeWithTag("profile_height").performTextClearance()
            compose.onNodeWithTag("profile_height").performTextInput("170")
            compose.onNodeWithTag("profile_weight").performTextInput("68")
            compose.onNodeWithTag("profile_next").performClick()
            compose.onNodeWithTag("profile_preferences").performTextInput("Vegetarian")
            compose.onNodeWithTag("profile_health").performTextInput("Synthetic health note for test")
            compose.onNodeWithTag("profile_next").performClick()
            compose.onNodeWithTag("personalization_toggle").performScrollTo().assertIsOff()
            capture("v13-onboarding-consent")
            compose.onNodeWithTag("profile_next").performClick()
            waitFor("nav_profile")
            val saved = repository.load().getOrThrow()
            assertEquals(170.0, saved.heightCm!!, 0.0)
            assertFalse(saved.personalizationEnabled)
            val stored = context.getSharedPreferences("kalc_profile", 0).getString("encrypted", "")!!
            assertFalse(stored.contains("Synthetic health note"))
            compose.onNodeWithTag("nav_ask_ai").performClick()
            compose.onNodeWithTag("ask_ai_input_field").performTextInput("Is this enough protein?")
            compose.onNodeWithTag("ask_ai_send_button").performClick()
            compose.onNodeWithTag("meal_chat_messages").performScrollToNode(hasText("Is this enough protein?"))
            compose.onNodeWithText("Is this enough protein?").assertExists()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithTag("nav_profile").performClick()
            compose.onNodeWithTag("personalization_toggle").assertIsOff().performClick()
            compose.waitUntil(10000) { repository.load().getOrThrow().personalizationEnabled }
            compose.onNodeWithTag("personalization_toggle").assertIsOn().performClick()
            compose.waitUntil(10000) { !repository.load().getOrThrow().personalizationEnabled }
            capture("v13-profile")
            compose.onNodeWithContentDescription("Back to meal").performClick()
            compose.onNodeWithTag("nav_ask_ai").performClick()
            compose.onNodeWithText("Is this enough protein?").assertDoesNotExist()
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithTag("nav_profile").performClick()
            compose.onNodeWithTag("delete_profile").performScrollTo().performClick()
            compose.onNodeWithTag("confirm_delete_profile").performClick()
            compose.waitUntil(10000) { repository.load().getOrThrow() == UserProfile() }
            compose.onNodeWithTag("personalization_toggle").performScrollTo().assertIsOff()
        }
    }

    @Test fun savedProfileCanBeEditedWithoutEnablingPersonalization() {
        repository.save(UserProfile(name = "Alex", goal = "Balanced meals")).getOrThrow()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("nav_profile")
            compose.onNodeWithTag("nav_profile").performClick()
            compose.onNodeWithTag("edit_profile").performScrollTo().performClick()
            compose.onNodeWithTag("profile_goal").performTextClearance()
            compose.onNodeWithTag("profile_goal").performTextInput("More vegetables")
            repeat(3) { compose.onNodeWithTag("profile_next").performClick() }
            compose.onNodeWithTag("profile_next").performClick()
            compose.waitUntil(10000) { repository.load().getOrThrow().goal == "More vegetables" }
            assertFalse(repository.load().getOrThrow().personalizationEnabled)
        }
    }
}
