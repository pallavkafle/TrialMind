package com.example.trailmind.data.remote.dto

data class WeatherCurrentDto(
    val temperature_c: Double?,
    val humidity_pct: Double?,
    val wind_kmh: Double?,
    val summary: String?,
    val emoji: String?
)

data class WeatherDayDto(
    val date: String,
    val summary: String?,
    val emoji: String?,
    val temp_max_c: Double?,
    val temp_min_c: Double?,
    val rain_mm: Double?,
    val rain_chance_pct: Double?,
    val wind_max_kmh: Double?
)

data class WeatherDto(
    val elevation_m: Double?,
    val current: WeatherCurrentDto?,
    val daily: List<WeatherDayDto>?,
    val advisories: List<String>?,
    val fetched_at: String?,
    val stale: Boolean?,
    val attribution: String?
)