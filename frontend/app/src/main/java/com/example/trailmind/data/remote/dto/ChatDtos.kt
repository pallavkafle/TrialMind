package com.example.trailmind.data.remote.dto

data class ParseTripRequest(val text: String)

// the checked answer of POST /api/ml/parse-trip; null means "not stated"
data class ParsedTripDto(
    val stops: List<String>?,
    val duration_days: Int?,
    val group_size: Int?,
    val travel_month: String?,
    val starting_city: String?,
    val accommodation_tier: String?,
    val transport_mode: String?,
    val use_guide: Boolean?,
    val use_porter: Boolean?,
    val traveler_type: String?,
    val unknown_places: List<String>?,
    val understood: String?,
    val missing: List<String>?,
    val source: String?
)