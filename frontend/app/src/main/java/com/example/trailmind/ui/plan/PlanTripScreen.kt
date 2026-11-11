package com.example.trailmind.ui.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.ui.components.*
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.util.Resource

private val TextMuted = Color(0xFF6B7280)

private val cities = listOf("Kathmandu", "Chitwan", "Pokhara", "Lumbini", "Itahari", "Surkhet", "Nepalgunj")
private val months = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val nationalities = listOf("Domestic", "SAARC", "Foreign")
private val travelerTypes = listOf("Solo", "Group")
private val tiers = listOf("Budget", "Standard", "Luxury")
private val transports = listOf("Public Bus", "Private Jeep", "Flight")
private const val MAX_STOPS = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTripScreen(
    isChat: Boolean,
    defaultNationality: String,
    onBack: () -> Unit,
    onResult: () -> Unit,
    viewModel: PlanTripViewModel
) {
    // rememberSaveable keeps the form when you come back from the itinerary screen
    var query by rememberSaveable { mutableStateOf("") }
    val selected = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }
    var startingCity by rememberSaveable { mutableStateOf(cities.first()) }
    var days by rememberSaveable { mutableStateOf("7") }
    var group by rememberSaveable { mutableStateOf("2") }
    var month by rememberSaveable { mutableStateOf(months.first()) }
    var nationality by rememberSaveable { mutableStateOf(defaultNationality) }
    var travelerType by rememberSaveable { mutableStateOf(travelerTypes.last()) }
    var tier by rememberSaveable { mutableStateOf(tiers[1]) }
    var transport by rememberSaveable { mutableStateOf(transports.first()) }
    var useGuide by rememberSaveable { mutableStateOf(true) }
    var usePorter by rememberSaveable { mutableStateOf(true) }
    var localError by remember { mutableStateOf<String?>(null) }

    val destinations by viewModel.destinations.collectAsState()
    val itinerary by viewModel.itinerary.collectAsState()
    val isLoading = itinerary is Resource.Loading
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.clearItinerary()
        viewModel.loadDestinations()
    }

    // a plan the user asked for is ready (also when they left this screen and came back)
    LaunchedEffect(itinerary) {
        if (viewModel.takeReadyResult()) onResult()
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isChat) "AI Trip Guide" else "Plan Your Trip", fontWeight = FontWeight.SemiBold)
                },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            if (isChat) {
                SectionCard(title = "Your trip", icon = Icons.Default.Chat) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Describe your trip") },
                        placeholder = { Text("e.g. Visit Pashupatinath and Boudhanath, then a safari in Chitwan") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrailBlue,
                            focusedLabelColor = TrailBlue
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mention place names such as Everest Base Camp, Lumbini or Chitwan.",
                        fontSize = 12.sp, color = TextMuted
                    )
                }
            } else {
                SectionCard(title = "Where to?", icon = Icons.Default.Flag) {
                    DestinationChips(destinations, selected, MAX_STOPS) { viewModel.loadDestinations() }
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Trip details", icon = Icons.Default.CalendarMonth) {
                Dropdown("Starting city", startingCity, cities, Icons.Default.MyLocation) { startingCity = it }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Days", days, Icons.Default.Schedule, Modifier.weight(1f)) { days = it }
                    NumberField("Group size", group, Icons.Default.Groups, Modifier.weight(1f)) { group = it }
                }
                Spacer(Modifier.height(12.dp))
                Dropdown("Travel month", month, months, Icons.Default.CalendarMonth) { month = it }
                Spacer(Modifier.height(12.dp))
                Dropdown("Nationality", nationality, nationalities, Icons.Default.Public) { nationality = it }
                if (!isChat) {
                    Spacer(Modifier.height(12.dp))
                    Dropdown("Traveler type", travelerType, travelerTypes, Icons.Default.Person) { travelerType = it }
                }
            }

            if (!isChat) {
                Spacer(Modifier.height(14.dp))
                SectionCard(title = "Preferences", icon = Icons.Default.Tune) {
                    Dropdown("Accommodation", tier, tiers, Icons.Default.Hotel) { tier = it }
                    Spacer(Modifier.height(12.dp))
                    Dropdown("Transport", transport, transports, Icons.Default.DirectionsBus) { transport = it }
                    Spacer(Modifier.height(16.dp))
                    ToggleRow(Icons.Default.Hiking, "Hire a guide", null, useGuide, true) { useGuide = it }
                    Spacer(Modifier.height(8.dp))
                    ToggleRow(
                        Icons.Default.Backpack, "Hire a porter",
                        "Only applies to trekking destinations", usePorter, true
                    ) { usePorter = it }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val d = days.toIntOrNull() ?: 0
                    val g = group.toIntOrNull() ?: 0
                    localError = when {
                        isChat && query.trim().length < 5 -> "Tell us where you'd like to go."
                        !isChat && selected.isEmpty() -> "Pick at least one destination."
                        d < 1 || d > 30 -> "Days must be between 1 and 30."
                        g < 1 || g > 20 -> "Group size must be between 1 and 20."
                        !isChat && selected.size > d -> "${selected.size} stops need at least ${selected.size} days."
                        else -> null
                    }
                    if (localError == null) {
                        keyboard?.hide()
                        viewModel.generate(
                            ItineraryRequest(
                                query = if (isChat) query.trim() else "",
                                stops = if (isChat) emptyList() else selected.toList(),
                                starting_city = startingCity,
                                duration_days = d,
                                group_size = g,
                                travel_month = month,
                                nationality_category = nationality,
                                traveler_type = if (isChat) (if (g == 1) "Solo" else "Group") else travelerType,
                                accommodation_tier = if (isChat) "Standard" else tier,
                                transport_mode = if (isChat) "Public Bus" else transport,
                                use_guide = if (isChat) true else useGuide,
                                use_porter = if (isChat) true else usePorter
                            )
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TrailBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Create itinerary", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            localError?.let { ErrorBox(it) }
            when (val s = itinerary) {
                is Resource.Loading ->
                    Text("Creating your itinerary… this can take a minute.", fontSize = 13.sp, color = TextMuted)
                is Resource.Error -> ErrorBox(s.message)
                else -> {}
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}