package com.example.trailmind.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.trailmind.data.remote.dto.UserDto
import com.google.gson.Gson
import java.security.KeyStore

class TokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val gson = Gson()
    private val prefs: SharedPreferences by lazy { openPrefs() }

    private fun createEncrypted(): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // if the saved data cannot be decrypted (a restored backup, a changed lock screen), start clean
    private fun openPrefs(): SharedPreferences =
        try {
            createEncrypted()
        } catch (e: Exception) {
            resetStorage()
            createEncrypted()
        }

    private fun resetStorage() {
        try {
            appContext.deleteSharedPreferences(FILE)
        } catch (e: Exception) {
            // nothing to delete
        }
        try {
            appContext.deleteSharedPreferences(KEYSET_FILE)
        } catch (e: Exception) {
            // nothing to delete
        }
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            ks.deleteEntry(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            // key already gone
        }
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY, token).apply()

    fun getToken(): String? =
        try {
            prefs.getString(KEY, null)
        } catch (e: Exception) {
            null
        }

    fun saveUser(user: UserDto) = prefs.edit().putString(USER_KEY, gson.toJson(user)).apply()

    fun getUser(): UserDto? =
        try {
            prefs.getString(USER_KEY, null)?.let { gson.fromJson(it, UserDto::class.java) }
        } catch (e: Exception) {
            null
        }

    // opens the encrypted storage now (called on a background thread at app start)
    fun warmUp() {
        prefs.contains(KEY)
    }

    fun clear() {
        prefs.edit().clear().apply()
        OfflineCache.clearAsync()
    }

    companion object {
        @Volatile
        private var instance: TokenStore? = null

        fun get(context: Context): TokenStore =
            instance ?: synchronized(this) {
                instance ?: TokenStore(context.applicationContext).also { instance = it }
            }

        private const val FILE = "trailmind_secure_prefs"
        private const val KEYSET_FILE = "__androidx_security_crypto_encrypted_prefs__"
        private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
        private const val KEY = "jwt_token"
        private const val USER_KEY = "user_json"
    }
}