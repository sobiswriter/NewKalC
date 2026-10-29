package com.example.data.auth

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiProvider { VERTEX, AI_STUDIO }

data class AiConnectionState(val provider: AiProvider, val unlocked: Boolean = false, val expiresAt: Long = 0)

class VertexAccessException(message: String) : Exception(message)

class VertexAccessRepository(
    context: Context,
    private val serverUrl: String = BuildConfig.VERTEX_SERVER_URL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
) {
    private val prefs = context.getSharedPreferences("kalc_ai_connection", Context.MODE_PRIVATE)
    private val initialProvider = prefs.getString("provider", null)?.let { saved ->
        AiProvider.entries.firstOrNull { it.name == saved }
    } ?: if (ApiKeyRepository.getInstance(context).hasValidApiKey()) AiProvider.AI_STUDIO else AiProvider.VERTEX
    private val _state = MutableStateFlow(AiConnectionState(initialProvider))
    val state = _state.asStateFlow()
    @Volatile private var token: String? = null
    @Volatile private var expiresAt: Long = 0
    private var sessionVersion = 0L

    val provider: AiProvider get() = _state.value.provider

    @Synchronized
    fun selectProvider(provider: AiProvider) {
        lock()
        prefs.edit().putString("provider", provider.name).apply()
        _state.value = AiConnectionState(provider)
    }

    @Synchronized
    fun lock() {
        sessionVersion++
        token = null
        expiresAt = 0
        _state.value = _state.value.copy(unlocked = false, expiresAt = 0)
    }

    suspend fun unlock(passkey: String): Result<Unit> = withContext(Dispatchers.IO) {
        val attempt = synchronized(this@VertexAccessRepository) {
            lock()
            sessionVersion
        }
        try {
            if (passkey.isEmpty() || passkey.length > 512) throw VertexAccessException("Enter the server passkey.")
            val request = Request.Builder().url(endpoint("session"))
                .post(JSONObject().put("passkey", passkey).toString().toRequestBody(JSON))
                .build()
            client.newCall(request).execute().use { response ->
                val payload = JSONObject(response.body?.string() ?: "{}")
                if (!response.isSuccessful) throw VertexAccessException(
                    payload.optJSONObject("error")?.optString("message")?.take(250)
                        ?: "Server access denied. Check your passkey."
                )
                val sessionToken = payload.optString("token")
                val expiry = payload.optLong("expiresAt")
                if (sessionToken.isBlank() || sessionToken.length > 2048 || expiry <= System.currentTimeMillis()) {
                    throw VertexAccessException("The server returned an invalid session.")
                }
                synchronized(this@VertexAccessRepository) {
                    if (provider != AiProvider.VERTEX || attempt != sessionVersion) {
                        throw VertexAccessException("Connection changed. Unlock Vertex AI again.")
                    }
                    token = sessionToken
                    expiresAt = expiry
                    _state.value = AiConnectionState(provider, unlocked = true, expiresAt = expiry)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(if (e is VertexAccessException) e else VertexAccessException("Could not unlock the server. Check your connection and retry."))
        }
    }

    @Synchronized
    fun generationRequest(payload: JSONObject): Request {
        val sessionToken = token
        if (sessionToken == null || System.currentTimeMillis() >= expiresAt) {
            lock()
            throw VertexAccessException("Vertex AI is locked. Enter the server passkey in AI settings.")
        }
        return Request.Builder().url(endpoint("generate"))
            .header("Authorization", "Bearer $sessionToken")
            .post(payload.toString().toRequestBody(JSON)).build()
    }

    private fun endpoint(path: String): String {
        val uri = android.net.Uri.parse(serverUrl)
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null ||
            uri.query != null || uri.fragment != null) throw VertexAccessException("The Vertex AI server has not been configured.")
        return "${serverUrl.trimEnd('/')}/$path"
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
