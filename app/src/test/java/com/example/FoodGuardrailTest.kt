package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.gemini.GeminiFoodService
import com.example.data.model.AmbiguousFoodException
import com.example.data.model.NonFoodException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FoodGuardrailTest {
    private fun response(json: String) = JSONObject().put("candidates", org.json.JSONArray().put(
        JSONObject().put("content", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", json))))
    )).toString()

    private fun image() = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until width) for (y in 0 until height) {
            setPixel(x, y, if (x < width / 2) Color.GREEN else Color.WHITE)
        }
    }

    private val food = """{"is_food":true,"food_confidence":0.95,"detected_foods":[{"name":"Boiled Egg","estimated_weight_grams":50}]}"""

    @Test fun `missing invalid and low confidence never become a meal`() {
        val service = GeminiFoodService()
        for (json in listOf(
            """{"detected_foods":[{"name":"Phone"}]}""",
            """{"is_food":"true","food_confidence":0.9}""",
            """{"is_food":true,"food_confidence":5}""",
            """{"is_food":true,"food_confidence":0.2,"detected_foods":[{"name":"Rice"}]}""",
            """{"is_food":false,"food_confidence":0.3,"rejection_reason":"Ambiguous image"}"""
        )) assertThrows(AmbiguousFoodException::class.java) { service.parseGeminiResponse(response(json)) }
        for (json in listOf(
            """{"is_food":false,"food_confidence":0,"rejection_reason":"Electronics"}""",
            """{"is_food":true,"food_confidence":0.95,"detected_foods":[]}"""
        )) assertThrows(NonFoodException::class.java) { service.parseGeminiResponse(response(json)) }
        assertEquals("Boiled Egg", service.parseGeminiResponse(response(food)).foods.single().name)
    }

    @Test fun `both scan paths reject non food without retries or demo substitution`() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response("""{"is_food":false,"food_confidence":0,"rejection_reason":"Electronics detected"}""")
                    .toResponseBody("application/json".toMediaType())).build()
        }.build()
        val service = GeminiFoodService(client = client)
        assertTrue(service.analyzeFoodImage(image(), sampleContextHint = "biryani", providedApiKey = "test-key").exceptionOrNull() is NonFoodException)
        assertTrue(service.analyzeAdditionalFoodImage(image(), providedApiKey = "test-key").exceptionOrNull() is NonFoodException)
        assertEquals(2, calls)
    }

    @Test fun `tiny and blank photos are rejected locally in both scan paths`() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { error("Validation must run before the network") }.build()
        val service = GeminiFoodService(client = client)
        for (bitmap in listOf(Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888), Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888))) {
            assertTrue(service.analyzeFoodImage(bitmap, providedApiKey = "test-key").exceptionOrNull() is NonFoodException)
            assertTrue(service.analyzeAdditionalFoodImage(bitmap, providedApiKey = "test-key").exceptionOrNull() is NonFoodException)
        }
    }

    @Test fun `all studio operations use only 3 point 8 flash with low thinking`() = runBlocking {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            assertEquals("/v1beta/models/gemini-3.8-flash:generateContent", chain.request().url.encodedPath)
            val buffer = okio.Buffer()
            chain.request().body!!.writeTo(buffer)
            val config = JSONObject(buffer.readUtf8()).getJSONObject("generationConfig")
            assertEquals("LOW", config.getJSONObject("thinkingConfig").getString("thinkingLevel"))
            assertFalse(config.has("temperature"))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(response(if (calls <= 2) food else if (calls == 3)
                    """{"headline":"Balanced","positives":["Protein"],"concerns":[],"suggestions":["Add vegetables"]}"""
                    else "Eggs provide protein.").toResponseBody("application/json".toMediaType())).build()
        }.build()
        val service = GeminiFoodService(client = client)
        val meal = service.analyzeFoodImage(image(), providedApiKey = "test-key").getOrThrow()
        service.analyzeAdditionalFoodImage(image(), providedApiKey = "test-key").getOrThrow()
        service.generateNutritionInsights(meal, "test-key").getOrThrow()
        service.askAboutFood("Protein?", meal, emptyList(), null, "test-key").getOrThrow()
        service.validateApiKey("test-key").getOrThrow()
        assertEquals(5, calls)
    }
}
