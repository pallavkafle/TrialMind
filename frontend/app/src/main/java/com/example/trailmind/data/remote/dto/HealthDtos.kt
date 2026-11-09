package com.example.trailmind.data.remote.dto

data class FacilityDto(
    val id: String,
    val name: String,
    val type: String,
    val lat: Double,
    val lon: Double,
    val phone: String?,
    val emergency: Boolean,
    val distance_km: Double
)

data class HealthOriginDto(
    val lat: Double,
    val lon: Double,
    val destination: String?
)

data class NearestFacilitiesResponse(
    val origin: HealthOriginDto,
    val radius_km: Double,
    val count: Int,
    val facilities: List<FacilityDto>,
    val nearest_overall: FacilityDto?,
    val message: String?,
    val notes: List<String>,
    val attribution: String
)