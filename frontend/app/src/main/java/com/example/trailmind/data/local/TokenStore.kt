package com.example.trailmind.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.trailmind.data.remote.dto.UserDto
import com.google.gson.Gson

class TokenStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "trailmind_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    fun saveToken(token: String) = prefs.edit().putString(KEY, token).apply()
    fun getToken(): String? = prefs.getString(KEY, null)

    fun saveUser(user: UserDto) = prefs.edit().putString(USER_KEY, gson.toJson(user)).apply()
    fun getUser(): UserDto? = prefs.getString(USER_KEY, null)?.let {
        try { gson.fromJson(it, UserDto::class.java) } catch (e: Exception) { null }
    }

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY = "jwt_token"
        private const val USER_KEY = "user_json"
    }
}