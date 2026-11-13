package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.WeatherDto
import com.example.trailmind.util.Resource

class WeatherRepository {
    suspend fun forecast(lat: Double, lon: Double): Resource<WeatherDto> =
        cachedApiCall("weather:${"%.2f".format(lat)},${"%.2f".format(lon)}", "Could not load the weather", maxAgeMs = ONE_DAY_MS) { ApiClient.gatewayApi.weather(lat, lon) }
}