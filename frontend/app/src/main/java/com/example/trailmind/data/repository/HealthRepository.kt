package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.NearestFacilitiesResponse
import com.example.trailmind.util.Resource

class HealthRepository {

    // give either a destination name or lat + lon; type is hospital, clinic, doctors, rescue, or null for all
    suspend fun nearest(
        destination: String?,
        lat: Double?,
        lon: Double?,
        type: String?
    ): Resource<NearestFacilitiesResponse> =
        cachedApiCall("health:${destination ?: "%.2f,%.2f".format(lat, lon)}:${type ?: "all"}", "Could not load nearby help") {
            ApiClient.gatewayApi.nearestFacilities(destination, lat, lon, type, LIMIT, RADIUS_KM)
        }

    companion object {
        private const val LIMIT = 6
        private const val RADIUS_KM = 100
    }
}