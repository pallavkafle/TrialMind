package com.example.trailmind.data.remote.dto

// PUT /api/auth/me: only the fields you send are changed
data class UpdateProfileRequest(val name: String?, val nationality: String?)

data class FavoriteToggleRequest(val destination: String)
data class FavoriteToggleDto(val destination: String?, val favorite: Boolean)

// "from NPR X per person for N days"; basis says what the price assumes
data class TypicalBudgetDto(val days: Int?, val per_person_npr: Double?, val basis: String?)

data class FeaturedPlaceDto(
    val name: String,
    val category: String?,
    val altitude_m: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val is_restricted: Boolean?,
    val review_count: Int?,
    val avg_rating: Double?,
    val score_100: Double?,
    val typical: TypicalBudgetDto?
)

data class FeaturedDataDto(val count: Int?, val as_of: String?, val places: List<FeaturedPlaceDto>?)