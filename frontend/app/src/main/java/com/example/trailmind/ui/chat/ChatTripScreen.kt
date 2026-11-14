package com.example.trailmind.ui.chat

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.ui.components.*
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.plan.PlanTripViewModel
import com.example.trailmind.ui.theme.Pal
import com.example.trailmind.util.Resource

private val cities = listOf("Kathmandu", "Chitwan", "Pokhara", "Lumbini", "Itahari", "Surkhet", "Nepalgunj")
private val months = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val nationalities = listOf("Domestic", "SAARC", "Foreign")
private val tiers = listOf("Budget", "Standard", "Luxury")
private val transports = listOf("Public Bus", "Private Jeep", "Flight")
private const val MAX_STOPS = 10

private val examples = listOf(
    "Temples and a safari" to
        "Visit Pashupatinath and Boudhanath, then a safari in Chitwan, 7 days, 4 people, October, from Pokhara, standard hotels, by bus, with a guide",
    "Everest in spring" to
        "Everest Base Camp for two weeks in spring, a group of 6, budget travel, flying from Kathmandu",
    "Weekend in Nagarkot" to
        "A relaxed weekend in Nagarkot and Bhaktapur for 2 people in November, standard hotel, by bus, starting from Kathmandu"
)

@Composable
private fun Needed(text: String) {
    Text(text, fontSize = 12.sp, color = Pal.amber, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatTripScreen(
    defaultNationality: String,
    onBack: () -> Unit,
    onResult: () -> Unit,
    onUseForm: () -> Unit,
    planVm: PlanTripViewModel,
    chatVm: ChatTripViewModel = viewModel()
) {
    var query by rememberSaveable { mutableStateOf("") }
    val selected = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }
    var days by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("") }
    var month by rememberSaveable { mutableStateOf("") }
    var startingCity by rememberSaveable { mutableStateOf("") }
    var nationality by rememberSaveable { mutableStateOf(defaultNationality) }
    var tier by rememberSaveable { mutableStateOf("Standard") }
    var transport by rememberSaveable { mutableStateOf("Public Bus") }
    var useGuide by rememberSaveable { mutableStateOf(true) }
    var usePorter by rememberSaveable { mutableStateOf(true) }
    var unknownPlaces by rememberSaveable { mutableStateOf("") }
    var understood by rememberSaveable { mutableStateOf("") }
    var readBy by rememberSaveable { mutableStateOf("") }
    var appliedSeq by rememberSaveable { mutableStateOf(-1) }
    var localError by remember { mutableStateOf<String?>(null) }

    val parseState by chatVm.state.collectAsState()
    val destinations by planVm.destinations.collectAsState()
    val itinerary by planVm.itinerary.collectAsState()
    val isParsing = parseState is Resource.Loading
    val generating = itinerary is Resource.Loading
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        planVm.clearItinerary()
        planVm.loadDestinations()
    }

    // a plan the user asked for is ready (also when they left this screen and came back)
    LaunchedEffect(itinerary) {
        if (planVm.takeReadyResult()) onResult()
    }

    val parsed = (parseState as? Resource.Success)?.data

    // fill the form once per reading; coming back from the itinerary keeps the user's edits
    LaunchedEffect(parsed) {
        val p = parsed
        if (p != null && appliedSeq != chatVm.seq) {
            appliedSeq = chatVm.seq
            selected.clear()
            selected.addAll(p.stops.orEmpty().take(MAX_STOPS))
            days = p.duration_days?.toString() ?: ""
            group = p.group_size?.toString() ?: ""
            month = p.travel_month ?: ""
            startingCity = p.starting_city ?: ""
            tier = p.accommodation_tier ?: "Standard"
            transport = p.transport_mode ?: "Public Bus"
            useGuide = p.use_guide ?: true
            usePorter = p.use_porter ?: true
            unknownPlaces = p.unknown_places.orEmpty().joinToString(", ")
            understood = p.understood.orEmpty()
            readBy = if (p.source == "gemini+rules") "Read with AI help" else "Read by our rules"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("AI Trip Guide", fontWeight = FontWeight.SemiBold) },
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
            if (parsed == null) {
                // ---------------- step 1: describe the trip ----------------
                SectionCard(title = "Describe your trip", icon = Icons.Default.Chat) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { if (it.length <= 1000) query = it },
                        label = { Text("Where, how long, who with…") },
                        placeholder = { Text("e.g. Temples in Kathmandu, then a safari in Chitwan, 7 days, 4 people, October, from Pokhara") },
                        minLines = 4,
                        enabled = !isParsing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("${query.trim().length}/1000") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrailBlue,
                            focusedLabelColor = TrailBlue
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Try an example", fontSize = 12.sp, color = Pal.textMuted)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        examples.forEach { (label, full) ->
                            AssistChip(
                                onClick = { query = full },
                                enabled = !isParsing,
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (query.trim().length < 5) {
                            localError = "Tell us a little more about your trip."
                        } else {
                            localError = null
                            keyboard?.hide()
                            chatVm.parse(query.trim())
                        }
                    },
                    enabled = !isParsing,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (isParsing) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Understand my trip", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (isParsing) {
                    Text("Reading your trip… this can take up to 20 seconds.", fontSize = 13.sp, color = Pal.textMuted)
                }
                (parseState as? Resource.Error)?.let { ErrorBox(it.message) }
                localError?.let { ErrorBox(it) }
                TextButton(onClick = onUseForm) { Text("Prefer a form? Use guided questions") }
            } else {
                // ---------------- step 2: check what was understood ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Pal.blueFaint)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Pal.blue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Here's what I understood", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pal.textDark)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(understood.ifBlank { "Check the details below." }, fontSize = 13.sp, color = Pal.textBody)
                        Spacer(Modifier.height(8.dp))
                        InfoTag(readBy.ifBlank { "Read by our rules" }, Pal.blue)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Check everything below and change anything that is wrong.",
                            fontSize = 12.sp, color = Pal.textMuted
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                SectionCard(title = "Places", icon = Icons.Default.Flag) {
                    DestinationChips(destinations, selected, MAX_STOPS) { planVm.loadDestinations() }
                    if (unknownPlaces.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "We don't have: $unknownPlaces. Please choose from the places above.",
                            fontSize = 12.sp, color = Pal.amber
                        )
                    }
                    if (selected.isEmpty()) Needed("Pick at least one place")
                }

                Spacer(Modifier.height(14.dp))

                SectionCard(title = "Trip details", icon = Icons.Default.CalendarMonth) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            NumberField("Days", days, Icons.Default.Schedule, Modifier.fillMaxWidth()) { days = it }
                            if ((days.toIntOrNull() ?: 0) !in 1..30) Needed("Enter 1 to 30 days")
                        }
                        Column(Modifier.weight(1f)) {
                            NumberField("Group size", group, Icons.Default.Groups, Modifier.fillMaxWidth()) { group = it }
                            if ((group.toIntOrNull() ?: 0) !in 1..20) Needed("Enter 1 to 20 people")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Dropdown("Travel month", month, months, Icons.Default.CalendarMonth) { month = it }
                    if (month.isBlank()) Needed("Choose the travel month")
                    Spacer(Modifier.height(12.dp))
                    Dropdown("Starting city", startingCity, cities, Icons.Default.MyLocation) { startingCity = it }
                    if (startingCity.isBlank()) Needed("Choose where you start")
                    Spacer(Modifier.height(12.dp))
                    Dropdown("Nationality", nationality, nationalities, Icons.Default.Public) { nationality = it }
                }

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

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        val d = days.toIntOrNull() ?: 0
                        val g = group.toIntOrNull() ?: 0
                        localError = when {
                            selected.isEmpty() -> "Pick at least one place."
                            d < 1 || d > 30 -> "Days must be between 1 and 30."
                            g < 1 || g > 20 -> "Group size must be between 1 and 20."
                            selected.size > d -> "${selected.size} stops need at least ${selected.size} days."
                            month.isBlank() -> "Choose the travel month."
                            startingCity.isBlank() -> "Choose where you start."
                            else -> null
                        }
                        if (localError == null) {
                            keyboard?.hide()
                            planVm.generate(
                                ItineraryRequest(
                                    query = query.trim(),
                                    stops = selected.toList(),
                                    starting_city = startingCity,
                                    duration_days = d,
                                    group_size = g,
                                    travel_month = month,
                                    nationality_category = nationality,
                                    traveler_type = if (g == 1) "Solo" else "Group",
                                    accommodation_tier = tier,
                                    transport_mode = transport,
                                    use_guide = useGuide,
                                    use_porter = usePorter
                                )
                            )
                        }
                    },
                    enabled = !generating,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    if (generating) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Create itinerary", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(Modifier.height(12.dp))
                localError?.let { ErrorBox(it) }
                when (val s = itinerary) {
                    is Resource.Loading ->
                        Text("Creating your itinerary… this can take a minute.", fontSize = 13.sp, color = Pal.textMuted)
                    is Resource.Error -> ErrorBox(s.message)
                    else -> {}
                }

                TextButton(onClick = { chatVm.reset() }, enabled = !generating) { Text("Edit my text") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}