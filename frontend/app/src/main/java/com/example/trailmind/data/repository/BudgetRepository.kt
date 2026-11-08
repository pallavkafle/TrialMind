package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.util.Resource
import com.example.trailmind.util.parseApiError
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BudgetRepository {

    suspend fun predict(request: BudgetRequest): Resource<BudgetResponse> {
        return try {
            val res = ApiClient.gatewayApi.budget(request)
            val data = res.body()?.data
            when {
                res.isSuccessful && data != null -> Resource.Success(data)
                res.code() == 401 -> Resource.Error("Session expired. Please log out and log in again.")
                else -> Resource.Error(
                    parseApiError(res.errorBody()?.string(), "Prediction failed (${res.code()})")
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
}