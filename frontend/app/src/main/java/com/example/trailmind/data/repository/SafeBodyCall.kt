package com.example.trailmind.data.repository

import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import kotlinx.coroutines.CancellationException
import retrofit2.Response

// for endpoints whose answer is not the usual { success, data } envelope
suspend fun <T> safeBodyCall(
    fallback: String,
    call: suspend () -> Response<T>
): Resource<T> {
    return try {
        val res = call()
        val body = res.body()
        when {
            res.isSuccessful && body != null -> Resource.Success(body)
            res.code() == 401 -> Resource.Error("Your session expired. Please log in again.")
            else -> Resource.Error(
                parseApiError(res.errorBody()?.string(), "$fallback (${res.code()})")
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error(networkErrorMessage(e))
    }
}