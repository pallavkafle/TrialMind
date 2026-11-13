package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.CurrencyRatesDto
import com.example.trailmind.util.Resource

class CurrencyRepository {
    suspend fun rates(): Resource<CurrencyRatesDto> =
        cachedApiCall("currency", "Could not load exchange rates", maxAgeMs = 3 * ONE_DAY_MS) { ApiClient.gatewayApi.currencyRates() }
}