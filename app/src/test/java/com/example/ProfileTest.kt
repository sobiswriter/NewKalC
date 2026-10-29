package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.gemini.GeminiFoodService
import com.example.data.profile.ProfileRepository
import com.example.data.profile.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val key = KeyGenerator.getInstance("AES").apply { init(128) }.generateKey()
    private val profile = UserProfile(name = "LocalNameOnly", age = 29, heightCm = 171.0, weightKg = 73.0,
        goal = "Build strength", preferences = "Peanut allergy", healthNotes = "SensitiveNotesFixture")
    private fun repository() = ProfileRepository(context) { key }
    @Before fun reset() { context.getSharedPreferences("kalc_profile", 0).edit().clear().commit() }

    @Test fun `default is off and disabled profiles never yield AI context`() {
        assertFalse(UserProfile().personalizationEnabled)
        assertEquals("", profile.aiContext())
        val context = profile.copy(personalizationEnabled = true).aiContext()
        assertTrue(context.contains("SensitiveNotesFixture"))
        assertTrue(context.contains("Peanut allergy"))
        assertTrue(context.contains("not instructions"))
        assertFalse(context.contains("LocalNameOnly"))
    }

    @Test fun `encrypted storage round trips with random IV and persists consent`() {
        val repo = repository()
        repo.save(profile.copy(personalizationEnabled = true)).getOrThrow()
        val prefs = context.getSharedPreferences("kalc_profile", 0)
        val encrypted = prefs.getString("encrypted", "")!!
        assertFalse(encrypted.contains("SensitiveNotesFixture"))
        assertFalse(encrypted.contains("LocalNameOnly"))
        assertEquals(profile.copy(personalizationEnabled = true), repository().load().getOrThrow())
        assertTrue(repo.onboarded)
        repo.save(profile).getOrThrow()
        assertNotEquals(encrypted, prefs.getString("encrypted", ""))
        assertFalse(repository().load().getOrThrow().personalizationEnabled)
    }

    @Test fun `deleting removes profile and consent without restarting onboarding`() {
        val repo = repository()
        repo.save(profile.copy(personalizationEnabled = true)).getOrThrow()
        repo.delete().getOrThrow()
        assertEquals(UserProfile(), repository().load().getOrThrow())
        assertTrue(repository().onboarded)
        assertFalse(context.getSharedPreferences("kalc_profile", 0).contains("encrypted"))
    }

    @Test fun `skip stores no draft or health data`() {
        repository().skipOnboarding().getOrThrow()
        assertTrue(repository().onboarded)
        assertEquals(UserProfile(), repository().load().getOrThrow())
        assertEquals(setOf("onboarded"), context.getSharedPreferences("kalc_profile", 0).all.keys)
    }

    @Test fun `invalid numbers and oversized notes do not overwrite saved data`() {
        val repo = repository()
        repo.save(profile).getOrThrow()
        for (invalid in listOf(profile.copy(age = -1), profile.copy(heightCm = Double.NaN), profile.copy(weightKg = -10.0), profile.copy(goal = "x".repeat(501)))) {
            assertTrue(repo.save(invalid).isFailure)
        }
        assertEquals(profile, repo.load().getOrThrow())
    }

    @Test fun `wrong encryption key fails closed without destroying ciphertext`() {
        repository().save(profile).getOrThrow()
        val other = KeyGenerator.getInstance("AES").apply { init(128) }.generateKey()
        assertTrue(ProfileRepository(context) { other }.load().isFailure)
        assertEquals(profile, repository().load().getOrThrow())
    }

    @Test fun `actual insight and chat requests include health details only when opted in`() = runBlocking {
        val requests = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer(); chain.request().body!!.writeTo(buffer)
            requests.add(buffer.readUtf8())
            val reply = if (requests.size == 5) """{"is_food":true,"food_confidence":0.95,"detected_foods":[{"name":"Rice","estimated_weight_grams":100}]}"""
                else if (requests.size % 2 == 1) """{"headline":"Balanced","positives":[],"concerns":[],"suggestions":["Add vegetables"]}""" else "A balanced portion may help."
            val response = JSONObject().put("candidates", org.json.JSONArray().put(JSONObject().put("content", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", reply))))))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response.toString().toResponseBody("application/json".toMediaType())).build()
        }.build()
        val service = GeminiFoodService(client = client)
        val meal = service.getDeterministicFallbackAnalysis("biryani")
        for (enabled in listOf(false, true)) {
            val current = profile.copy(personalizationEnabled = enabled)
            service.generateNutritionInsights(meal, "test-key", current).getOrThrow()
            service.askAboutFood("Any suggestions?", meal, emptyList(), null, "test-key", profile = current).getOrThrow()
        }
        for (request in requests.take(2)) assertFalse(request.contains("SensitiveNotesFixture"))
        for (request in requests.takeLast(2)) {
            assertTrue(request.contains("SensitiveNotesFixture"))
            assertFalse(request.contains("LocalNameOnly"))
        }
        val image = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until width) for (y in 0 until height) setPixel(x, y, if (x < 40) Color.GREEN else Color.WHITE)
        }
        service.analyzeFoodImage(image, providedApiKey = "test-key").getOrThrow()
        assertFalse(requests.last().contains("SensitiveNotesFixture"))
        assertFalse(requests.last().contains("OPTIONAL USER PROFILE"))
    }

    @Test fun `personalized network failure is not disguised as personalized success`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(503).message("Unavailable")
                .body("{}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val service = GeminiFoodService(client = client)
        val meal = service.getDeterministicFallbackAnalysis("biryani")
        val enabled = profile.copy(personalizationEnabled = true)
        assertTrue(service.generateNutritionInsights(meal, "test-key", enabled).isFailure)
        assertTrue(service.askAboutFood("Any suggestions?", meal, emptyList(), null, "test-key", profile = enabled).isFailure)
    }
}
