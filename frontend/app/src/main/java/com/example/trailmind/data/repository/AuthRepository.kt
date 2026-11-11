package com.example.trailmind.data.repository

import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.AuthResponse
import com.example.trailmind.data.remote.dto.LoginRequest
import com.example.trailmind.data.remote.dto.RegisterRequest
import com.example.trailmind.data.remote.dto.UserDto
import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// result of checking the saved login at app start
enum class SessionState {
    LOGGED_OUT,   // no saved login, or the server says it expired
    VALID,        // the server accepted it
    UNKNOWN       // the server could not be reached: let the user in, the next request will tell
}

class AuthRepository(private val tokenStore: TokenStore) {

    suspend fun login(email: String, password: String): Resource<UserDto> =
        safeCall { ApiClient.authApi.login(LoginRequest(email, password)) }

    suspend fun register(
        name: String, email: String, password: String, nationality: String
    ): Resource<UserDto> =
        safeCall {
            ApiClient.authApi.register(RegisterRequest(name, email, password, nationality))
        }

    // asks the server whether the saved token still works (GET /api/auth/me)
    suspend fun validateSession(): SessionState {
        val token = tokenStore.getToken() ?: return SessionState.LOGGED_OUT
        return try {
            val res = ApiClient.authApi.me("Bearer $token")
            when {
                res.isSuccessful -> {
                    res.body()?.user?.let { tokenStore.saveUser(it) }
                    SessionState.VALID
                }
                res.code() == 401 -> {
                    tokenStore.clear()
                    SessionState.LOGGED_OUT
                }
                else -> SessionState.UNKNOWN
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SessionState.UNKNOWN
        }
    }

    private suspend fun safeCall(
        call: suspend () -> Response<AuthResponse>
    ): Resource<UserDto> {
        return try {
            val res = call()
            val body = res.body()
            if (res.isSuccessful && body?.token != null && body.user != null) {
                tokenStore.saveToken(body.token)
                tokenStore.saveUser(body.user)
                Resource.Success(body.user)
            } else {
                Resource.Error(parseApiError(res.errorBody()?.string(), "Request failed (${res.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            Resource.Error("Cannot reach the server. Is the backend running?")
        } catch (e: ConnectException) {
            Resource.Error("Cannot reach the server. Is the backend running?")
        } catch (e: SocketTimeoutException) {
            Resource.Error("The request timed out.")
        } catch (e: IOException) {
            Resource.Error("Network problem. Please check your connection and try again.")
        } catch (e: Exception) {
            Resource.Error("Something went wrong. Please try again.")
        }
    }
}