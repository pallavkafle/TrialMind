package com.example.trailmind.data.remote.dto

data class ReviewSentimentDto(
    val label: String?,
    val confidence: Double?,
    val positiveProbability: Double?,
    val safetyFlag: String?,
    val matchedKeywords: List<String>?
)

data class ReviewDto(
    val id: String,
    val destination: String,
    val rating: Int,
    val text: String,
    val sentiment: ReviewSentimentDto?,
    val author: String?,
    val createdAt: String?
)

data class ReviewSummaryDto(
    val destination: String?,
    val count: Int,
    val avgRating: Double?,
    val analyzed: Int,
    val positivePct: Double?,
    val score100: Double?,
    val warningCount: Int,
    val cautionCount: Int
)

// the public list answers { success, summary, data }, not the usual { success, data } envelope
data class ReviewListResponse(
    val success: Boolean,
    val summary: ReviewSummaryDto?,
    val data: List<ReviewDto>?
)

data class CreateReviewRequest(
    val destination: String,
    val rating: Int,
    val text: String
)