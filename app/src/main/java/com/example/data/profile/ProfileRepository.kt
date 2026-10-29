package com.example.data.profile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ProfileRepository(
    context: Context,
    private val keyProvider: () -> SecretKey = { deviceKey() }
) {
    private val prefs = context.getSharedPreferences("kalc_profile", Context.MODE_PRIVATE)
    val onboarded: Boolean get() = prefs.getBoolean("onboarded", false)

    fun load(): Result<UserProfile> = runCatching {
        val stored = prefs.getString("encrypted", null) ?: return@runCatching UserProfile()
        val envelope = JSONObject(stored)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(128, Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)))
        val plain = cipher.doFinal(Base64.decode(envelope.getString("data"), Base64.NO_WRAP))
        UserProfile.fromJson(JSONObject(plain.toString(Charsets.UTF_8)))
    }

    fun save(profile: UserProfile): Result<Unit> = runCatching {
        val clean = profile.validated()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val encrypted = cipher.doFinal(clean.toJson().toString().toByteArray(Charsets.UTF_8))
        val envelope = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        check(prefs.edit().putString("encrypted", envelope.toString()).putBoolean("onboarded", true).commit()) { "Profile could not be saved. Please retry." }
    }

    fun skipOnboarding(): Result<Unit> = runCatching {
        check(prefs.edit().remove("encrypted").putBoolean("onboarded", true).commit()) { "Could not save your choice. Please retry." }
    }

    fun delete(): Result<Unit> = runCatching {
        check(prefs.edit().remove("encrypted").putBoolean("onboarded", true).commit()) { "Profile could not be deleted. Please retry." }
    }

    companion object {
        private fun deviceKey(): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey("kalc_profile_v1", null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("kalc_profile_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }
    }
}
