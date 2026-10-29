package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.auth.ApiKeyRepository
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ProviderConnectionTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun providerSelectionAndServerAuthorization() {
        val passkey = InstrumentationRegistry.getArguments().getString("serverPasskey")
        assumeTrue("Live verification requires the owner's passkey at runtime.", !passkey.isNullOrEmpty())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        context.getSharedPreferences("kalc_profile", 0).edit().putBoolean("onboarded", true).commit()
        context.getSharedPreferences("kalc_ai_connection", 0).edit().clear().commit()
        context.getSharedPreferences("food_calorie_ai_prefs", 0).edit().clear().commit()
        val apiKeys = ApiKeyRepository.getInstance(context)
        apiKeys.saveApiKey("saved-studio-key-for-test-only")

        ActivityScenario.launch(MainActivity::class.java).use {
            openSettings()
            compose.onNodeWithTag("provider_vertex").performClick()
            compose.onNodeWithTag("ai_credential_input").performTextInput("wrong-passkey-test")
            compose.onNodeWithTag("connect_ai_provider").performClick()
            compose.waitUntil(30000) {
                compose.onAllNodesWithText("Incorrect passkey. Server access denied.").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Incorrect passkey. Server access denied.").assertIsDisplayed()
            assertEquals("saved-studio-key-for-test-only", apiKeys.getUserApiKey())

            compose.onNodeWithTag("ai_credential_input").performTextClearance()
            compose.onNodeWithTag("ai_credential_input").performTextInput(passkey!!)
            compose.onNodeWithTag("connect_ai_provider").performClick()
            compose.waitUntil(30000) {
                compose.onAllNodesWithText("Vertex AI · On Sundays").fetchSemanticsNodes().isNotEmpty()
            }
            openSettings()
            compose.onNodeWithText("Server unlocked · On Sundays").assertIsDisplayed()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            FileOutputStream(File(context.externalCacheDir, "provider-settings-verified.png")).use { output ->
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            }
            screenshot.recycle()
            compose.onNodeWithTag("provider_ai_studio").performClick()
            assertEquals("saved-studio-key-for-test-only", apiKeys.getUserApiKey())
            compose.onNodeWithTag("provider_vertex").performClick()
            compose.onAllNodesWithText("Server unlocked · On Sundays").fetchSemanticsNodes().let { nodes ->
                assertEquals(0, nodes.size)
            }
        }
        apiKeys.clearApiKey()
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("API Key & AI Settings").performClick()
    }
}
