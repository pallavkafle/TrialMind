package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.data.remote.dto.ItineraryResponse
import com.example.trailmind.data.remote.dto.SaveTripRequest
import com.example.trailmind.data.remote.dto.SavedTripDto
import com.example.trailmind.data.remote.dto.TripDetailDto
import com.example.trailmind.data.remote.dto.TripSummaryDto
import com.example.trailmind.util.Resource

class TripRepository {

    suspend fun destinations(): Resource<List<DestinationDto>> =
        safeApiCall("Could not load destinations") { ApiClient.gatewayApi.destinations() }

    suspend fun itinerary(request: ItineraryRequest): Resource<ItineraryResponse> =
        safeApiCall("Could not create the itinerary") { ApiClient.gatewayApi.itinerary(request) }

    suspend fun saveTrip(body: SaveTripRequest): Resource<SavedTripDto> =
        safeApiCall("Could not save the trip") { ApiClient.gatewayApi.saveTrip(body) }

    suspend fun myTrips(): Resource<List<TripSummaryDto>> =
        safeApiCall("Could not load your trips") { ApiClient.gatewayApi.trips() }

    suspend fun trip(id: String): Resource<TripDetailDto> =
        safeApiCall("Could not load the trip") { ApiClient.gatewayApi.trip(id) }

    suspend fun deleteTrip(id: String): Resource<Unit> =
        safeActionCall("Could not delete the trip") { ApiClient.gatewayApi.deleteTrip(id) }
}