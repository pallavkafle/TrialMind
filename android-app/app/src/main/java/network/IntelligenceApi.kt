package com.example.trailmind.network

import retrofit2.http.Body
import retrofit2.http.POST

data class BudgetRequest(
    val region: String,
    val purpose: String,
    val month: Int,
    val days: Int,
    val group_size: Int,
    val accommodation_tier: String,
    val transport_mode: String
)

data class BudgetResponse(
    val predicted_cost_npr: Double,
    val per_person: Double
)

interface IntelligenceApi {
    @POST("predict-budget")
    suspend fun predictBudget(@Body request: BudgetRequest): BudgetResponse
}