package com.example.trailmind.data.remote.dto

data class CurrencyRateDto(
    val code: String,
    val name: String,
    val quote_unit: Int,
    val npr_per_unit: Double,
    val npr_for_quote_unit: Double
)

data class CurrencyRatesDto(
    val base: String?,
    val updated_at: String?,
    val next_update_at: String?,
    val rates: List<CurrencyRateDto>?,
    val budget_rate_usd_npr: Double?,
    val source: String?,
    val attribution_url: String?,
    val stale: Boolean?,
    val note: String?
)