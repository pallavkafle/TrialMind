package com.example.trailmind.data.repository

import com.example.trailmind.data.local.OfflineCache
import com.example.trailmind.data.local.OfflineState
import com.example.trailmind.data.remote.ApiEnvelope
import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Type

const val ONE_DAY_MS = 24L * 60 * 60 * 1000

private val gson = Gson()

// Read-through cache: ask the server; on success remember the answer; when the server cannot be reached
// (no network, timeout, backend or ML service down) show the last saved answer instead.
suspend fun <R, T> cachedCore(
    key: String,
    type: Type,
    fallback: String,
    maxAgeMs: Long,
    call: suspend () -> Response<R>,
    extract: (R) -> T?
): Resource<T> {

    suspend fun fromCache(message: String): Resource<T> {
        val entry = OfflineCache.get(key)
        if (entry != null && System.currentTimeMillis() - entry.savedAt <= maxAgeMs) {
            try {
                val data: T = gson.fromJson<T>(entry.json, type)
                OfflineState.set(true)
                return Resource.Success(data, fromCache = true)
            } catch (e: Exception) {
                // the saved copy is unreadable: fall through to the error below
            }
        }
        return Resource.Error(message)
    }

    return try {
        val res = call()
        val data = res.body()?.let(extract)
        when {
            res.isSuccessful && data != null -> {
                OfflineCache.put(key, gson.toJson(data))
                OfflineState.set(false)
                Resource.Success(data)
            }
            res.code() == 401 -> Resource.Error("Your session expired. Please log in again.")
            res.code() in 502..504 ->
                fromCache(parseApiError(res.errorBody()?.string(), "$fallback (${res.code()})"))
            else -> Resource.Error(parseApiError(res.errorBody()?.string(), "$fallback (${res.code()})"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        fromCache(networkErrorMessage(e))
    } catch (e: Exception) {
        Resource.Error(networkErrorMessage(e))
    }
}

// for the usual { success, data } answers
suspend inline fun <reified T> cachedApiCall(
    key: String,
    fallback: String,
    maxAgeMs: Long = 7 * ONE_DAY_MS,
    noinline call: suspend () -> Response<ApiEnvelope<T>>
): Resource<T> =
    cachedCore(key, object : TypeToken<T>() {}.type, fallback, maxAgeMs, call) { it.data }

// for answers that are not wrapped in the envelope (the review list)
suspend inline fun <reified T> cachedBodyCall(
    key: String,
    fallback: String,
    maxAgeMs: Long = 7 * ONE_DAY_MS,
    noinline call: suspend () -> Response<T>
): Resource<T> =
    cachedCore(key, object : TypeToken<T>() {}.type, fallback, maxAgeMs, call) { it }