package com.example.trailmind.data.remote.dto

data class PermitCheckRequest(
    val destinations: List<String>,
    val duration_days: Int,
    val group_size: Int,
    val travel_month: String,
    val nationality_category: String? = null
)

data class PermitItemDto(
    val id: String,
    val name: String,
    val authority: String?,
    val confidence: String,
    val note: String?,
    val fee_per_person_npr: Double?,
    val fee_total_npr: Double?
)

data class PermitDestinationDto(
    val destination: String,
    val permit_ids: List<String>,
    val notes: List<String>
)

data class PermitCheckResponse(
    val as_of: String,
    val nationality_category: String,
    val group_size: Int,
    val duration_days: Int,
    val travel_month: String,
    val permits: List<PermitItemDto>,
    val total_per_person_npr: Double,
    val total_npr: Double,
    val destinations: List<PermitDestinationDto>,
    val needs_verification: List<String>,
    val advisories: List<String>
)