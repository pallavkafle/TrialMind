package com.example.trailmind.data.remote

import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.data.remote.dto.ItineraryResponse
import com.example.trailmind.data.remote.dto.NearestFacilitiesResponse
import com.example.trailmind.data.remote.dto.PermitCheckRequest
import com.example.trailmind.data.remote.dto.PermitCheckResponse
import com.example.trailmind.data.remote.dto.SaveTripRequest
import com.example.trailmind.data.remote.dto.SavedTripDto
import com.example.trailmind.data.remote.dto.TripDetailDto
import com.example.trailmind.data.remote.dto.TripSummaryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// every Node response looks like { success, message?, data? }
data class ApiEnvelope<T>(val success: Boolean, val message: String? = null, val data: T? = null)

interface GatewayApi {
    @POST("api/ml/budget")
    suspend fun budget(@Body body: BudgetRequest): Response<ApiEnvelope<BudgetResponse>>

    @GET("api/ml/destinations")
    suspend fun destinations(): Response<ApiEnvelope<List<DestinationDto>>>

    @POST("api/ml/itinerary")
    suspend fun itinerary(@Body body: ItineraryRequest): Response<ApiEnvelope<ItineraryResponse>>

    @POST("api/trips")
    suspend fun saveTrip(@Body body: SaveTripRequest): Response<ApiEnvelope<SavedTripDto>>

    @GET("api/trips")
    suspend fun trips(): Response<ApiEnvelope<List<TripSummaryDto>>>

    @GET("api/trips/{id}")
    suspend fun trip(@Path("id") id: String): Response<ApiEnvelope<TripDetailDto>>

    @DELETE("api/trips/{id}")
    suspend fun deleteTrip(@Path("id") id: String): Response<ApiEnvelope<Unit>>

    @POST("api/permits/check")
    suspend fun checkPermits(@Body body: PermitCheckRequest): Response<ApiEnvelope<PermitCheckResponse>>

    // public route: works without a valid login
    @GET("api/health-facilities/nearest")
    suspend fun nearestFacilities(
        @Query("destination") destination: String?,
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
        @Query("type") type: String?,
        @Query("limit") limit: Int?,
        @Query("radius_km") radiusKm: Int?
    ): Response<ApiEnvelope<NearestFacilitiesResponse>>
}