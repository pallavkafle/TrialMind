package com.example.trailmind.data.remote

import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// every Node response looks like { success, message?, data? }
data class ApiEnvelope<T>(val success: Boolean, val message: String? = null, val data: T? = null)

interface GatewayApi {
    @POST("api/ml/budget")
    suspend fun budget(@Body body: BudgetRequest): Response<ApiEnvelope<BudgetResponse>>
}