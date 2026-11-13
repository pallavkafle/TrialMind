package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.NearbyHotelsResponse
import com.example.trailmind.data.remote.dto.NearestFacilitiesResponse
import com.example.trailmind.util.Resource

// the hospital and hotel layers of the map (saved in the offline cache like every other read)
class MapRepository {

    suspend fun facilities(lat: Double, lon: Double): Resource<NearestFacilitiesResponse> =
        cachedApiCall("mapfac:${"%.2f".format(lat)},${"%.2f".format(lon)}", "Could not load hospitals") {
            ApiClient.gatewayApi.nearestFacilities(null, lat, lon, null, 20, 25)
        }

    suspend fun hotels(lat: Double, lon: Double): Resource<NearbyHotelsResponse> =
        cachedApiCall("maphotel:${"%.2f".format(lat)},${"%.2f".format(lon)}", "Could not load places to stay") {
            ApiClient.gatewayApi.nearbyHotels(null, lat, lon, null, 25, 15)
        }
}