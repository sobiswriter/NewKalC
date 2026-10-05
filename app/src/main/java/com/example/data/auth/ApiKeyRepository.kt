package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiKeyRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _apiKeyFlow = MutableStateFlow(getEffectiveApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getEffectiveApiKey(): String {
        val userSavedKey = prefs.getString(KEY_USER_API_KEY, "")?.trim() ?: ""
        if (userSavedKey.isNotEmpty()) {
            return userSavedKey
        }
        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildConfigKey.isNotEmpty() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    fun hasValidApiKey(): Boolean {
        return getEffectiveApiKey().isNotEmpty()
    }

    fun saveApiKey(key: String) {
        val cleanKey = key.trim()
        prefs.edit().putString(KEY_USER_API_KEY, cleanKey).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_USER_API_KEY).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun hasDismissedStartupPrompt(): Boolean {
        return prefs.getBoolean(KEY_PROMPT_DISMISSED, false)
    }

    fun setStartupPromptDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_PROMPT_DISMISSED, dismissed).apply()
    }

    companion object {
        private const val PREFS_NAME = "food_calorie_ai_prefs"
        private const val KEY_USER_API_KEY = "user_gemini_api_key"
        private const val KEY_PROMPT_DISMISSED = "startup_prompt_dismissed"

        @Volatile
        private var instance: ApiKeyRepository? = null

        fun getInstance(context: Context): ApiKeyRepository {
            return instance ?: synchronized(this) {
                instance ?: ApiKeyRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
