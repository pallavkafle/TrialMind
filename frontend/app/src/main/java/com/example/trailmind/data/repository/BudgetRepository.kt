package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.util.Resource

class BudgetRepository {

    suspend fun predict(request: BudgetRequest): Resource<BudgetResponse> =
        safeApiCall("Prediction failed") { ApiClient.gatewayApi.budget(request) }
}