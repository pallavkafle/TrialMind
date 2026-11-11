package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.NearbyHotelsResponse
import com.example.trailmind.util.Resource

class HotelRepository {

    // give either a destination name or lat + lon; type is null for all kinds of lodging
    suspend fun nearby(
        destination: String?,
        lat: Double?,
        lon: Double?,
        type: String?,
        radiusKm: Int
    ): Resource<NearbyHotelsResponse> =
        safeApiCall("Could not load places to stay") {
            ApiClient.gatewayApi.nearbyHotels(destination, lat, lon, type, LIMIT, radiusKm)
        }

    companion object {
        private const val LIMIT = 15
    }
}