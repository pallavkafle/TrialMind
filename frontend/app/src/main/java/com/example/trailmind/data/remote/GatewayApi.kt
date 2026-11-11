package com.example.trailmind.data.remote

import com.example.trailmind.data.remote.dto.BudgetRequest
import com.example.trailmind.data.remote.dto.BudgetResponse
import com.example.trailmind.data.remote.dto.CreateReviewRequest
import com.example.trailmind.data.remote.dto.CurrencyRatesDto
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.data.remote.dto.ItineraryResponse
import com.example.trailmind.data.remote.dto.NearbyHotelsResponse
import com.example.trailmind.data.remote.dto.NearestFacilitiesResponse
import com.example.trailmind.data.remote.dto.PermitCheckRequest
import com.example.trailmind.data.remote.dto.PermitCheckResponse
import com.example.trailmind.data.remote.dto.ReviewDto
import com.example.trailmind.data.remote.dto.ReviewListResponse
import com.example.trailmind.data.remote.dto.SaveTripRequest
import com.example.trailmind.data.remote.dto.SavedTripDto
import com.example.trailmind.data.remote.dto.TripDetailDto
import com.example.trailmind.data.remote.dto.TripSummaryDto
import com.example.trailmind.data.remote.dto.WeatherDto
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

    // public: answers { success, summary, data }
    @GET("api/reviews")
    suspend fun reviews(
        @Query("destination") destination: String,
        @Query("limit") limit: Int
    ): Response<ReviewListResponse>

    @GET("api/reviews/mine")
    suspend fun myReviews(): Response<ApiEnvelope<List<ReviewDto>>>

    @POST("api/reviews")
    suspend fun createReview(@Body body: CreateReviewRequest): Response<ApiEnvelope<ReviewDto>>

    @DELETE("api/reviews/{id}")
    suspend fun deleteReview(@Path("id") id: String): Response<ApiEnvelope<Unit>>

    @GET("api/weather")
    suspend fun weather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<ApiEnvelope<WeatherDto>>

    @GET("api/currency/rates")
    suspend fun currencyRates(): Response<ApiEnvelope<CurrencyRatesDto>>

    // login required; give destination OR lat + lon
    @GET("api/hotels/nearby")
    suspend fun nearbyHotels(
        @Query("destination") destination: String?,
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?,
        @Query("type") type: String?,
        @Query("limit") limit: Int?,
        @Query("radius_km") radiusKm: Int?
    ): Response<ApiEnvelope<NearbyHotelsResponse>>
}