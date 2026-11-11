package com.example.trailmind.util

private val SAARC = setOf("AF", "BD", "BT", "IN", "MV", "PK", "LK")

// profile nationality code (for example "NP") -> the category the budget, permit and itinerary calls use
fun nationalityCategory(code: String?): String {
    val c = code?.trim()?.uppercase().orEmpty()
    return when {
        c.isEmpty() || c == "NP" -> "Domestic"
        c in SAARC -> "SAARC"
        else -> "Foreign"
    }
}