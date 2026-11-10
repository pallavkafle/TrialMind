package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.WeatherDto
import com.example.trailmind.util.Resource

class WeatherRepository {
    suspend fun forecast(lat: Double, lon: Double): Resource<WeatherDto> =
        safeApiCall("Could not load the weather") { ApiClient.gatewayApi.weather(lat, lon) }
}