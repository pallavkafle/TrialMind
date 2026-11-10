package com.example.trailmind.data.repository

import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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