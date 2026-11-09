package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiEnvelope
import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

suspend fun <T> safeApiCall(
    fallback: String,
    call: suspend () -> Response<ApiEnvelope<T>>
): Resource<T> {
    return try {
        val res = call()
        val data = res.body()?.data
        when {
            res.isSuccessful && data != null -> Resource.Success(data)
            res.code() == 401 -> Resource.Error("Session expired. Please log out and log in again.")
            else -> Resource.Error(
                parseApiError(res.errorBody()?.string(), "$fallback (${res.code()})")
            )
        }
    } catch (e: UnknownHostException) {
        Resource.Error("Cannot reach the server. Is the backend running?")
    } catch (e: ConnectException) {
        Resource.Error("Cannot reach the server. Is the backend running?")
    } catch (e: SocketTimeoutException) {
        Resource.Error("The request timed out.")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unexpected error")
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
            res.code() == 401 -> Resource.Error("Session expired. Please log out and log in again.")
            else -> Resource.Error(
                parseApiError(res.errorBody()?.string(), "$fallback (${res.code()})")
            )
        }
    } catch (e: UnknownHostException) {
        Resource.Error("Cannot reach the server. Is the backend running?")
    } catch (e: ConnectException) {
        Resource.Error("Cannot reach the server. Is the backend running?")
    } catch (e: SocketTimeoutException) {
        Resource.Error("The request timed out.")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unexpected error")
    }
}