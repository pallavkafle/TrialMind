package com.example.trailmind.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// "2026-11-07T17:08:32.215Z" -> "7 Nov 2026"
fun formatIsoDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    } catch (e: Exception) {
        iso.take(10)
    }
}

// "2026-11-10" -> "Tue 10 Nov"
fun formatDayLabel(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return ""
    return try {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("EEE d MMM"))
    } catch (e: Exception) {
        isoDate
    }
}

// "Tue, 10 Nov 2026 00:02:31 +0000" -> "10 Nov 2026"
fun formatRfcDate(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return try {
        ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME)
            .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    } catch (e: Exception) {
        text.take(16)
    }
}