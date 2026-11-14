package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.ParseTripRequest
import com.example.trailmind.data.remote.dto.ParsedTripDto
import com.example.trailmind.util.Resource

class ChatRepository {
    suspend fun parse(text: String): Resource<ParsedTripDto> =
        safeApiCall("Could not read your trip") { ApiClient.gatewayApi.parseTrip(ParseTripRequest(text)) }
}