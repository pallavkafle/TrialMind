package com.example.trailmind.data.repository

import com.example.trailmind.data.remote.ApiClient
import com.example.trailmind.data.remote.dto.CreateReviewRequest
import com.example.trailmind.data.remote.dto.ReviewDto
import com.example.trailmind.data.remote.dto.ReviewListResponse
import com.example.trailmind.util.Resource

class ReviewRepository {

    suspend fun reviews(destination: String): Resource<ReviewListResponse> =
        cachedBodyCall("reviews:$destination", "Could not load reviews") { ApiClient.gatewayApi.reviews(destination, 20) }

    suspend fun mine(): Resource<List<ReviewDto>> =
        safeApiCall("Could not load your reviews") { ApiClient.gatewayApi.myReviews() }

    suspend fun create(destination: String, rating: Int, text: String): Resource<ReviewDto> =
        safeApiCall("Could not post the review") {
            ApiClient.gatewayApi.createReview(CreateReviewRequest(destination, rating, text))
        }

    suspend fun delete(id: String): Resource<Unit> =
        safeActionCall("Could not delete the review") { ApiClient.gatewayApi.deleteReview(id) }
}