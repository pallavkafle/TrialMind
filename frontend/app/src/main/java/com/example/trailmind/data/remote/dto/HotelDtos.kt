package com.example.trailmind.data.remote.dto

data class HotelDto(
    val id: String?,
    val name: String?,
    val type: String?,
    val lat: Double,
    val lon: Double,
    val stars: Int?,
    val phone: String?,
    val website: String?,
    val distance_km: Double
)

data class HotelOriginDto(
    val lat: Double,
    val lon: Double,
    val destination: String?
)

data class NearbyHotelsResponse(
    val origin: HotelOriginDto,
    val radius_km: Double,
    val count: Int,
    val total_in_radius: Int?,
    val hotels: List<HotelDto>?,
    val nearest_overall: HotelDto?,
    val message: String?,
    val notes: List<String>?,
    val attribution: String?,
    val dataset_size: Int?
)