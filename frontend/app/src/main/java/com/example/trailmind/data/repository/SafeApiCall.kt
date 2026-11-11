package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiEnvelope
import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// one place that turns a failed call into a message a traveller can read
internal fun networkErrorMessage(e: Exception): String = when (e) {
    is UnknownHostException, is ConnectException -> "Cannot reach the server. Is the backend running?"
    is SocketTimeoutException -> "The request timed out."
    is IOException -> "Network problem. Please check your connection and try again."
    else -> "Something went wrong. Please try again."
}

suspend fun <T> safeApiCall(
    fallback: String,
    call: suspend () -> Response<ApiEnvelope<T>>
): Resource<T> {
    return try {
        val res = call()
        val data = res.body()?.data
        when {
            res.isSuccessful && data != null -> Resource.Success(data)
            res.code() == 401 -> Resource.Error("Your session expired. Please log in again.")
            else -> Resource.Error(
                parseApiError(res.errorBody()?.string(), "$fallback (${res.code()})")
            )
        }
    } catch (e: CancellationException) {
        throw e // a cancelled request is not an error to show
    } catch (e: Exception) {
        Resource.Error(networkErrorMessage(e))
    }
}

// for endpoints that answer only { success, message } (e.g. delete)
suspend fun safeActionCall(
    fallback: String,
    call: suspend () -> Response<ApiEnvelope<Unit>>
): Resource<Unit> {
    return try {
        val res = call()
        when {
            res.isSuccessful && res.body()?.success == true -> Resource.Success(Unit)
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