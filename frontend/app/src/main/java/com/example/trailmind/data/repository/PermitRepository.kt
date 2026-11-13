package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.PermitCheckRequest
import com.example.trailmind.data.remote.dto.PermitCheckResponse
import com.example.trailmind.util.Resource

class PermitRepository {
    suspend fun check(request: PermitCheckRequest): Resource<PermitCheckResponse> =
        cachedApiCall("permits:${request.destinations.sorted().joinToString("|")}:${request.duration_days}:${request.group_size}:${request.travel_month}:${request.nationality_category}", "Could not check permits") { ApiClient.gatewayApi.checkPermits(request) }
}