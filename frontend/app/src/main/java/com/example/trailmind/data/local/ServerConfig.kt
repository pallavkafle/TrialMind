package com.example.trailmind.data.local

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// which backend the app talks to: a saved address wins, then the emulator address, then the public address
object ServerConfig {
    const val PUBLIC_URL = "https://trailmind.pallavkafle.com.np/"
    const val EMULATOR_URL = "http://10.0.2.2:5000/"
    private const val FILE = "trailmind_settings"
    private const val KEY = "server_url"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun saved(c: Context): String? = prefs(c).getString(KEY, null)?.takeIf { it.isNotBlank() }

    fun save(c: Context, url: String?) {
        prefs(c).edit().apply { if (url == null) remove(KEY) else putString(KEY, url) }.apply()
    }

    // "abc.com" -> "https://abc.com/"; null when it is not a usable http(s) address
    fun normalize(raw: String): String? {
        var t = raw.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() }) return null
        if (!t.contains("://")) t = "https://$t"
        val u = t.toHttpUrlOrNull() ?: return null
        if (u.host.isBlank() || u.encodedQuery != null || u.fragment != null) return null
        return if (t.endsWith("/")) t else "$t/"
    }

    // call from a background thread
    fun test(base: String): String = try {
        val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
        client.newCall(Request.Builder().url(base + "api/health").build()).execute().use { r ->
            when {
                r.isSuccessful -> "Connected (HTTP ${r.code})"
                r.code in listOf(502, 503, 504, 523, 524, 530) ->
                    "Server not reachable (HTTP ${r.code}): the host PC or the tunnel may be off"
                else -> "Server answered HTTP ${r.code}"
            }
        }
    } catch (e: Exception) {
        "Could not connect: ${e.javaClass.simpleName}"
    }
}