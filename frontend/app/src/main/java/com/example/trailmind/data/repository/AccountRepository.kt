package com.example.trailmind.data.repository

import com.example.trailmind.data.local.OfflineCache
import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.FavoriteToggleDto
import com.example.trailmind.data.remote.dto.FavoriteToggleRequest
import com.example.trailmind.data.remote.dto.FeaturedDataDto
import com.example.trailmind.data.remote.dto.UpdateProfileRequest
import com.example.trailmind.data.remote.dto.UserDto
import com.example.trailmind.util.Resource

class AccountRepository {

    suspend fun favorites(): Resource<List<String>> =
        cachedApiCall("favorites", "Could not load saved places") { ApiClient.gatewayApi.favorites() }

    suspend fun toggle(destination: String): Resource<FavoriteToggleDto> =
        safeApiCall("Could not update saved places") {
            ApiClient.gatewayApi.toggleFavorite(FavoriteToggleRequest(destination))
        }.also { if (it is Resource.Success) OfflineCache.remove("favorites") }

    suspend fun featured(): Resource<FeaturedDataDto> =
        cachedApiCall("featured:8", "Could not load popular places") {
            ApiClient.gatewayApi.featuredPlaces(8, null)
        }

    suspend fun updateProfile(name: String?, nationality: String?): Resource<UserDto> =
        when (val r = safeBodyCall("Could not save your profile") {
            ApiClient.gatewayApi.updateMe(UpdateProfileRequest(name, nationality))
        }) {
            is Resource.Success ->
                r.data.user?.let { Resource.Success(it) } ?: Resource.Error("Could not save your profile")
            is Resource.Error -> Resource.Error(r.message)
            else -> Resource.Loading
        }
}