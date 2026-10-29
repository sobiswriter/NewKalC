package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AiProvider
import com.example.data.auth.ApiKeyRepository
import com.example.data.auth.VertexAccessException
import com.example.data.auth.VertexAccessRepository
import com.example.data.gemini.GeminiFoodService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VertexAccessTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun reset() {
        context.getSharedPreferences("kalc_ai_connection", Context.MODE_PRIVATE).edit().clear().commit()
        ApiKeyRepository.getInstance(context).clearApiKey()
    }

    private fun repository(beforeResponse: (String) -> Unit = {}): VertexAccessRepository {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val payload = JSONObject(chain.request().body!!.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            })
            val correct = payload.optString("passkey") == "correct-passkey"
            beforeResponse(payload.optString("passkey"))
            val json = if (correct) JSONObject().put("token", "test.session")
                .put("expiresAt", System.currentTimeMillis() + 60000).toString()
            else """{"error":{"message":"Incorrect passkey. Server access denied."}}"""
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (correct) 200 else 401).message("test")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return VertexAccessRepository(context, "https://kalc.example", client).also { it.selectProvider(AiProvider.VERTEX) }
    }

    @Test fun `wrong passkey never unlocks and clears any previous session`() = runBlocking<Unit> {
        val repo = repository()
        assertTrue(repo.unlock("correct-passkey").isSuccess)
        assertTrue(repo.state.value.unlocked)
        assertTrue(repo.unlock("wrong").isFailure)
        assertFalse(repo.state.value.unlocked)
        assertThrows(VertexAccessException::class.java) { repo.generationRequest(JSONObject()) }
    }

    @Test fun `unlock supplies only a temporary bearer token and process restart locks server`() = runBlocking<Unit> {
        val repo = repository()
        repo.unlock("correct-passkey").getOrThrow()
        val request = repo.generationRequest(JSONObject().put("contents", "sample"))
        assertEquals("https://kalc.example/generate", request.url.toString())
        assertEquals("Bearer test.session", request.header("Authorization"))
        assertNull(request.header("x-goog-api-key"))
        assertFalse(VertexAccessRepository(context, "https://kalc.example").state.value.unlocked)
    }

    @Test fun `provider switching preserves Studio key and locks cloud session`() = runBlocking<Unit> {
        val apiKeys = ApiKeyRepository.getInstance(context)
        apiKeys.saveApiKey("my-studio-key")
        val repo = repository()
        repo.unlock("correct-passkey").getOrThrow()
        repo.selectProvider(AiProvider.AI_STUDIO)
        assertFalse(repo.state.value.unlocked)
        assertEquals("my-studio-key", apiKeys.getUserApiKey())
        assertEquals(AiProvider.AI_STUDIO, VertexAccessRepository(context).provider)
        repo.selectProvider(AiProvider.VERTEX)
        assertThrows(VertexAccessException::class.java) { repo.generationRequest(JSONObject()) }
    }

    @Test fun `passkey is never submitted over cleartext HTTP`() = runBlocking<Unit> {
        val repo = VertexAccessRepository(context, "http://kalc.example")
        repo.selectProvider(AiProvider.VERTEX)
        assertTrue(repo.unlock("correct-passkey").isFailure)
        assertFalse(repo.state.value.unlocked)
    }

    @Test fun `late successful unlock cannot undo a later wrong passkey`() = runBlocking<Unit> {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val repo = repository { passkey ->
            if (passkey == "correct-passkey") {
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
        }
        val oldAttempt = async(Dispatchers.Default) { repo.unlock("correct-passkey") }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        assertTrue(repo.unlock("wrong").isFailure)
        release.countDown()
        assertTrue(oldAttempt.await().isFailure)
        assertFalse(repo.state.value.unlocked)
    }

    @Test fun `all four AI paths deny locked Vertex even when a Studio key exists`() = runBlocking<Unit> {
        ApiKeyRepository.getInstance(context).saveApiKey("studio-key-must-not-be-used")
        val repo = repository()
        val service = GeminiFoodService(repo)
        val meal = service.getDeterministicFallbackAnalysis("rice dal salad")
        val pixels = IntArray(160 * 160) { index ->
            android.graphics.Color.rgb((index * 37) % 256, (index * 71) % 256, (index * 113) % 256)
        }
        val bitmap = Bitmap.createBitmap(pixels, 160, 160, Bitmap.Config.ARGB_8888)
        assertTrue(service.analyzeFoodImage(bitmap, providedApiKey = "studio-key").exceptionOrNull() is VertexAccessException)
        assertTrue(service.analyzeAdditionalFoodImage(bitmap, providedApiKey = "studio-key").exceptionOrNull() is VertexAccessException)
        assertTrue(service.generateNutritionInsights(meal, "studio-key").exceptionOrNull() is VertexAccessException)
        assertTrue(service.askAboutFood("Protein?", meal, emptyList(), null, "studio-key").exceptionOrNull() is VertexAccessException)
        bitmap.recycle()
    }
}
