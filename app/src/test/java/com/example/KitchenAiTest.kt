package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.gemini.GeminiFoodService
import com.example.data.kitchen.*
import com.example.data.model.NonFoodException
import com.example.data.profile.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36])
class KitchenAiTest {
    private fun service(reply: String, finish: String = "STOP", inspect: (Request)->Unit = {}): GeminiFoodService {
        return GeminiFoodService(client=OkHttpClient.Builder().addInterceptor {chain ->
            inspect(chain.request())
            val body=JSONObject().put("candidates",JSONArray().put(JSONObject().put("finishReason",finish).put("content",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",reply))))))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toString().toResponseBody("application/json".toMediaType())).build()
        }.build())
    }
    @Test fun selectedStudioModelAndBoundedJsonAreUsedWithoutFallback() = runBlocking {
        var count=0
        val service=service("""{"answer":"Use a pinch less chilli."}""") {request ->
            count++
            assertTrue(request.url.toString().contains("gemini-3.8-flash:generateContent"))
            assertEquals("fixture-key",request.header("x-goog-api-key"))
            val buffer=okio.Buffer();request.body!!.writeTo(buffer)
            val settings=JSONObject(buffer.readUtf8()).getJSONObject("generationConfig")
            assertEquals(4096,settings.getInt("maxOutputTokens"))
            assertEquals("LOW",settings.getJSONObject("thinkingConfig").getString("thinkingLevel"))
        }
        assertEquals("Use a pinch less chilli.",service.generateKitchenJson("Question",emptyList(),"fixture-key").getString("answer"))
        assertEquals(1,count)
        assertTrue(runCatching {service("{}","MAX_TOKENS").generateKitchenJson("Question",emptyList(),"fixture-key")}.isFailure)
    }
    @Test fun foodRecognitionFailsClosedAndNeverReadsAProfile() = runBlocking {
        val bitmap=Bitmap.createBitmap(80,80,Bitmap.Config.ARGB_8888).apply { for(x in 0 until width)for(y in 0 until height)setPixel(x,y,if(x<40)Color.GREEN else Color.WHITE) }
        val missing=KitchenAiService(service("""{"ingredients":[]}"""))
        assertTrue(runCatching {missing.recognize(listOf(bitmap),"fixture-key")}.isFailure)
        val nonfood=KitchenAiService(service("""{"is_food":false,"food_confidence":0.99,"ingredients":[]}"""))
        assertTrue(runCatching {nonfood.recognize(listOf(bitmap),"fixture-key")}.exceptionOrNull() is NonFoodException)
    }
    @Test fun cookingProfileRequiresBothMasterAndSessionConsentAndOmitsBodyAndName() {
        val ai=KitchenAiService(service("{}"))
        val profile=UserProfile(name="PrivateLocalName",age=30,heightCm=172.0,weightKg=70.0,goal="More vegetables",healthNotes="PrivateFixtureNote")
        assertFalse(ai.profileContext(profile,false).contains("PrivateFixtureNote"))
        assertFalse(ai.profileContext(profile,true).contains("PrivateFixtureNote"))
        assertFalse(ai.profileContext(profile.copy(personalizationEnabled=true),false).contains("PrivateFixtureNote"))
        val permitted=ai.profileContext(profile.copy(personalizationEnabled=true),true)
        assertTrue(permitted.contains("PrivateFixtureNote"));assertFalse(permitted.contains("PrivateLocalName"));assertFalse(permitted.contains("172.0"));assertFalse(permitted.contains("70.0"))
    }
}
