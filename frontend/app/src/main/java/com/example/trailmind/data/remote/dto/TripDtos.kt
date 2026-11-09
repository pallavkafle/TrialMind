package com.example.trailmind.data.remote.dto

data class DestinationDto(
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val altitude_m: Int,
    val is_restricted: Boolean,
    val has_porter: Boolean
)

data class ItineraryRequest(
    val query: String,
    val stops: List<String>,
    val starting_city: String,
    val duration_days: Int,
    val group_size: Int,
    val travel_month: String,
    val nationality_category: String? = null,
    val traveler_type: String,
    val accommodation_tier: String,
    val transport_mode: String,
    val use_guide: Boolean,
    val use_porter: Boolean,
    val use_llm: Boolean = true
)

data class DayDto(
    val day: Int,
    val location: String,
    val title: String,
    val activities: List<String>,
    val tips: String?
)

data class RouteDto(
    val stops: List<String>,
    val leg_km: List<Double>,
    val macro_km: Double,
    val micro_km: Double,
    val total_km: Double
)

data class TripBudgetDto(
    val permit_npr: Double,
    val guide_npr: Double,
    val porter_npr: Double,
    val lodging_npr: Double,
    val meals_npr: Double,
    val transport_npr: Double,
    val total_npr: Double,
    val per_person_npr: Double
)

data class ItineraryResponse(
    val title: String,
    val summary: String,
    val days: List<DayDto>,
    val days_per_stop: List<Int>,
    val safety_notes: List<String>,
    val route: RouteDto,
    val budget: TripBudgetDto,
    val source: String,
    val model: String?,
    val warnings: List<String>
)
data class SaveTripRequest(
    val title: String,
    val request: ItineraryRequest,
    val itinerary: ItineraryResponse
)

data class SavedTripDto(val id: String, val title: String)

data class TripSummaryDto(
    val id: String,
    val title: String,
    val stops: List<String>,
    val startingCity: String?,
    val durationDays: Int,
    val groupSize: Int,
    val totalNpr: Double,
    val createdAt: String?
)

data class TripDetailDto(
    val id: String,
    val title: String,
    val request: ItineraryRequest,
    val itinerary: ItineraryResponse
)