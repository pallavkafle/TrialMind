package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.PermitCheckRequest
import com.example.trailmind.data.remote.dto.PermitCheckResponse
import com.example.trailmind.util.Resource

class PermitRepository {
    suspend fun check(request: PermitCheckRequest): Resource<PermitCheckResponse> =
        safeApiCall("Could not check permits") { ApiClient.gatewayApi.checkPermits(request) }
}