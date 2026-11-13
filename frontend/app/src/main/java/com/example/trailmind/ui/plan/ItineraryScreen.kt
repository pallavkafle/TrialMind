package com.example.trailmind.ui.plan

import com.example.trailmind.ui.map.LocalMapActions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.data.remote.dto.ItineraryResponse
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.util.Resource

private val TextMuted: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textMuted
private val TextDark: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textDark

private fun npr(v: Double) = "NPR ${"%,.0f".format(v)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    data: ItineraryResponse,
    request: ItineraryRequest,
    onBack: () -> Unit,
    viewModel: PlanTripViewModel,
    readOnly: Boolean = false
) {
    val saveState by viewModel.saveState.collectAsState()
    val mapActions = LocalMapActions.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.resetSave() }
    LaunchedEffect(saveState) {
        val s = saveState
        if (s is Resource.Error) {
            snackbar.showSnackbar(s.message)
            viewModel.resetSave()
        }
    }

    val saving = saveState is Resource.Loading
    val saved = saveState is Resource.Success

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (readOnly) "Saved trip" else "Your itinerary", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrailBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (!readOnly) Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = { viewModel.saveTrip(request, data) },
                    enabled = !saving && !saved,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrailBlue,
                        disabledContainerColor = if (saved) TrailBlue.copy(alpha = 0.7f) else TrailBlue.copy(alpha = 0.5f),
                        disabledContentColor = Color.White
                    )
                ) {
                    when {
                        saving -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        saved -> {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Saved to My Trips", fontWeight = FontWeight.SemiBold)
                        }
                        else -> {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save trip", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(data.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(6.dp))
                    Text(data.summary, fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("${data.days.size} days")
                        Pill("${data.route.stops.size} stop(s)")
                        Pill("${"%.0f".format(data.route.total_km)} km")
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Estimated total", fontSize = 12.sp, color = TextMuted)
                    Text(npr(data.budget.total_npr), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
                    Text("${npr(data.budget.per_person_npr)} per person", fontSize = 13.sp, color = TextMuted)
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Budget breakdown", icon = Icons.Default.AccountBalanceWallet) {
                val b = data.budget
                CostRow(Icons.Default.Badge, "Permits & fees", b.permit_npr)
                if (b.guide_npr > 0) CostRow(Icons.Default.Hiking, "Guide", b.guide_npr)
                if (b.porter_npr > 0) CostRow(Icons.Default.Backpack, "Porter", b.porter_npr)
                CostRow(Icons.Default.Hotel, "Lodging", b.lodging_npr)
                CostRow(Icons.Default.Restaurant, "Meals", b.meals_npr)
                CostRow(Icons.Default.DirectionsBus, "Transport", b.transport_npr)
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Route", icon = Icons.Default.Map) {
                data.route.stops.forEachIndexed { i, stop ->
                    Text("${i + 1}. $stop", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    if (i < data.route.leg_km.size) {
                        Text(
                            "     ↓ ${"%.1f".format(data.route.leg_km[i])} km by road",
                            fontSize = 12.sp, color = TextMuted
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Walking around the stops: about ${"%.1f".format(data.route.micro_km)} km",
                    fontSize = 12.sp, color = TextMuted
                )
            }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { mapActions.showRoute(data.route.stops) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View route on map", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(18.dp))
            Text("Day by day", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(10.dp))

            data.days.forEach { day ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("DAY ${day.day} · ${day.location}", fontSize = 11.sp, color = TrailBlue, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(day.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(Modifier.height(8.dp))
                        day.activities.forEach {
                            Text("•  $it", fontSize = 13.sp, color = com.example.trailmind.ui.theme.Pal.textBody, modifier = Modifier.padding(vertical = 2.dp))
                        }
                        if (!day.tips.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Tip: ${day.tips}", fontSize = 12.sp, color = TextMuted, fontStyle = FontStyle.Italic)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            if (data.safety_notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                SectionCard(title = "Safety notes", icon = Icons.Default.WarningAmber) {
                    data.safety_notes.forEach {
                        Text("•  $it", fontSize = 13.sp, color = com.example.trailmind.ui.theme.Pal.textBody, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (data.source == "gemini") {
                Text(
                    "Itinerary text written by Gemini; distances and costs are calculated.",
                    fontSize = 12.sp, color = TextMuted
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(com.example.trailmind.ui.theme.Pal.amberBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = com.example.trailmind.ui.theme.Pal.amber, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "The AI writer was unavailable, so this plan uses a standard template. " +
                            "Distances, costs and the route are still calculated.",
                        fontSize = 12.sp, color = com.example.trailmind.ui.theme.Pal.amberText
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        fontSize = 12.sp, color = TrailBlue, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(TrailBlueFaint)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun CostRow(icon: ImageVector, label: String, value: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TrailBlue, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = com.example.trailmind.ui.theme.Pal.textBody, modifier = Modifier.weight(1f))
        Text(npr(value), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
    }
}