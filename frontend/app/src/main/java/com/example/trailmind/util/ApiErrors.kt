package com.example.trailmind.util

import com.google.gson.JsonParser

// reads the "message" field the Node backend puts in every error response
fun parseApiError(json: String?, fallback: String): String {
    if (json.isNullOrBlank()) return fallback
    return try {
        JsonParser.parseString(json).asJsonObject.get("message")?.asString ?: fallback
    } catch (e: Exception) {
        fallback
    }
}