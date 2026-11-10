package com.example.trailmind.util

// "2026-11-07T17:08:32.215Z" -> "7 Nov 2026"
fun formatIsoDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        java.time.OffsetDateTime.parse(iso)
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    } catch (e: Exception) {
        iso.take(10)
    }
}