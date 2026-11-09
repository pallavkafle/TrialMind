package com.example.trailmind.ui.trips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.TripSummaryDto
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.plan.ItineraryScreen
import com.example.trailmind.ui.plan.PlanTripViewModel
import com.example.trailmind.util.Resource

private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)

private fun prettyDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        java.time.OffsetDateTime.parse(iso)
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    } catch (e: Exception) {
        iso.take(10)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripsTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TrailBlue,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
fun MyTripsScreen(
    onBack: () -> Unit,
    onOpenTrip: (String) -> Unit,
    viewModel: MyTripsViewModel = viewModel()
) {
    val state by viewModel.trips.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var toDelete by remember { mutableStateOf<TripSummaryDto?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TripsTopBar("My Trips", onBack) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                null, is Resource.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is Resource.Error -> Column(Modifier.padding(20.dp)) {
                    ErrorBox(s.message)
                    TextButton(onClick = { viewModel.load() }) { Text("Retry") }
                }

                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        Column(
                            Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No saved trips yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(Modifier.height(6.dp))
                            Text("Plan a trip from Home and tap Save trip.", fontSize = 13.sp, color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.data, key = { it.id }) { trip ->
                                TripCard(
                                    trip = trip,
                                    onClick = { onOpenTrip(trip.id) },
                                    onDelete = { toDelete = trip }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this trip?") },
            text = { Text("\"${trip.title}\" will be removed from your saved trips.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(trip.id)
                    toDelete = null
                }) { Text("Delete", color = Color(0xFFD32F2F)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TripCard(trip: TripSummaryDto, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    trip.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    trip.stops.joinToString(" → "), fontSize = 12.sp, color = TextMuted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${trip.durationDays} days · ${trip.groupSize} people · NPR ${"%,.0f".format(trip.totalNpr)}",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TrailBlue
                )
                val date = prettyDate(trip.createdAt)
                if (date.isNotEmpty()) Text("Saved $date", fontSize = 11.sp, color = TextMuted)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete trip", tint = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun SavedTripScreen(
    id: String,
    onBack: () -> Unit,
    planVm: PlanTripViewModel,
    viewModel: SavedTripViewModel = viewModel()
) {
    val state by viewModel.trip.collectAsState()
    LaunchedEffect(id) { viewModel.load(id) }

    when (val s = state) {
        is Resource.Success -> ItineraryScreen(
            data = s.data.itinerary,
            request = s.data.request,
            onBack = onBack,
            viewModel = planVm,
            readOnly = true
        )

        is Resource.Error -> Scaffold(topBar = { TripsTopBar("Saved trip", onBack) }) { padding ->
            Column(Modifier.padding(padding).padding(20.dp)) {
                ErrorBox(s.message)
                TextButton(onClick = { viewModel.retry(id) }) { Text("Retry") }
            }
        }

        else -> Scaffold(topBar = { TripsTopBar("Saved trip", onBack) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}