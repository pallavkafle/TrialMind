package com.example.trailmind.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.remote.dto.WeatherDayDto
import com.example.trailmind.data.remote.dto.WeatherDto
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.util.Resource
import com.example.trailmind.util.formatDayLabel

private val TextMuted: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textMuted
private val TextDark: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textDark
private val RainBlue: Color @Composable get() = com.example.trailmind.ui.theme.Pal.rain
private val Amber: Color @Composable get() = com.example.trailmind.ui.theme.Pal.amber

private fun degrees(v: Double?): String = v?.let { "%.0f°".format(it) } ?: "-"
private fun tempC(v: Double?): String = v?.let { "%.0f°C".format(it) } ?: "-"

// highestM = the highest point of the destination, shown next to the forecast point's height
@Composable
fun WeatherCard(
    state: Resource<WeatherDto>?,
    highestM: Int?,
    onRetry: () -> Unit
) {
    SectionCard(title = "Weather forecast", icon = Icons.Default.WbSunny) {
        when (state) {
            null, is Resource.Loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }

            is Resource.Error -> {
                ErrorBox(state.message)
                TextButton(onClick = onRetry) { Text("Retry") }
            }

            is Resource.Success -> WeatherContent(state.data, highestM)
        }
    }
}

@Composable
private fun WeatherContent(w: WeatherDto, highestM: Int?) {
    val cur = w.current
    if (cur != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cur.emoji.orEmpty(), fontSize = 34.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    tempC(cur.temperature_c) + (cur.summary?.let { " · $it" } ?: ""),
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark
                )
                val extra = listOfNotNull(
                    cur.humidity_pct?.let { "Humidity ${"%.0f".format(it)}%" },
                    cur.wind_kmh?.let { "Wind ${"%.0f".format(it)} km/h" }
                ).joinToString(" · ")
                if (extra.isNotEmpty()) Text(extra, fontSize = 12.sp, color = TextMuted)
            }
        }
    }

    val el = w.elevation_m
    if (el != null) {
        Spacer(Modifier.height(10.dp))
        Text("Forecast for about ${"%,.0f".format(el)} m above sea level.", fontSize = 12.sp, color = TextMuted)
        if (highestM != null && highestM - el > 300) {
            Text(
                "The highest point here is ${"%,d".format(highestM)} m, so expect it to be colder and windier higher up.",
                fontSize = 12.sp, color = TextMuted
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    w.daily.orEmpty().forEach { DayRow(it) }

    val advisories = w.advisories.orEmpty()
    if (advisories.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(com.example.trailmind.ui.theme.Pal.amberBg)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                advisories.forEach {
                    Text("•  $it", fontSize = 12.sp, color = com.example.trailmind.ui.theme.Pal.amberText, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    if (w.stale == true) {
        Text("Showing a saved forecast; it could not be refreshed just now.", fontSize = 11.sp, color = Amber)
    }
    Text(
        "Mountain weather changes fast. Check Nepal's Department of Hydrology and Meteorology before you trek.",
        fontSize = 11.sp, color = TextMuted
    )
    w.attribution?.let {
        Text(it, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun DayRow(d: WeatherDayDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(formatDayLabel(d.date), fontSize = 13.sp, color = TextDark, modifier = Modifier.width(84.dp))
        Text(d.emoji.orEmpty(), fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            d.summary.orEmpty(),
            fontSize = 12.sp, color = TextMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${degrees(d.temp_min_c)} / ${degrees(d.temp_max_c)}",
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark
            )
            d.rain_chance_pct?.let {
                Text(
                    "Rain ${"%.0f".format(it)}%",
                    fontSize = 11.sp,
                    color = if (it >= 60) RainBlue else TextMuted
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}