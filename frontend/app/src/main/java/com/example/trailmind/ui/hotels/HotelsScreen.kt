package com.example.trailmind.ui.hotels

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.HotelDto
import com.example.trailmind.data.remote.dto.NearbyHotelsResponse
import com.example.trailmind.ui.components.Dropdown
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.InfoTag
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.ui.places.PlacesViewModel
import com.example.trailmind.util.LocationHelper
import com.example.trailmind.util.Resource
import kotlinx.coroutines.launch

private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)
private val Amber = Color(0xFFB26A00)

private val typeFilters: List<Pair<String, String?>> = listOf(
    "All" to null,
    "Hotels" to "hotel",
    "Guest houses" to "guest_house",
    "Hostels" to "hostel",
    "Mountain huts" to "alpine_hut",
    "Camping" to "camp_site"
)

private val radiusOptions = listOf(10, 25, 50)

// opens the dialer with the number filled in; it never places the call by itself
private fun dialIntent(number: String) =
    Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.filter { it.isDigit() || it == '+' }))

private fun mapIntent(h: HotelDto) =
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:${h.lat},${h.lon}?q=${h.lat},${h.lon}(${Uri.encode(h.name ?: "Place to stay")})")
    )

private fun webIntent(raw: String): Intent? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    val url = if (t.startsWith("http://", ignoreCase = true) || t.startsWith("https://", ignoreCase = true)) t else "https://$t"
    return Intent(Intent.ACTION_VIEW, Uri.parse(url))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HotelsScreen(
    initialDestination: String?,
    onBack: () -> Unit,
    placesVm: PlacesViewModel,
    viewModel: HotelsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val destinations by placesVm.destinations.collectAsState()
    val result by viewModel.result.collectAsState()
    val type by viewModel.type.collectAsState()
    val radius by viewModel.radius.collectAsState()
    val locating by viewModel.locating.collectAsState()

    var picked by rememberSaveable { mutableStateOf(initialDestination.orEmpty()) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            permissionDenied = false
            viewModel.searchMyLocation(context.applicationContext)
        } else {
            permissionDenied = true
        }
    }

    fun openApp(intent: Intent?, failMessage: String) {
        if (intent == null) return
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(failMessage) }
        }
    }

    LaunchedEffect(Unit) {
        placesVm.load()
        // coming from a place page: search that destination straight away (once)
        if (!initialDestination.isNullOrBlank() && viewModel.result.value == null) {
            viewModel.searchPlace(initialDestination)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Hotels", fontWeight = FontWeight.SemiBold) },
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

            SectionCard(title = "Find places to stay", icon = Icons.Default.Hotel) {
                Button(
                    onClick = {
                        picked = ""
                        if (LocationHelper.hasPermission(context)) {
                            permissionDenied = false
                            viewModel.searchMyLocation(context.applicationContext)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !locating,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                ) {
                    if (locating) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Finding your location…", color = Color.White)
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Use my location", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                if (permissionDenied) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Location permission was denied. You can still pick a destination below.",
                        fontSize = 12.sp, color = Color(0xFFC62828)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("or choose a destination", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(8.dp))

                when (val d = destinations) {
                    is Resource.Success -> Dropdown(
                        "Destination", picked, d.data.map { it.name }.sorted(), Icons.Default.Flag
                    ) {
                        picked = it
                        viewModel.searchPlace(it)
                    }
                    is Resource.Error -> {
                        ErrorBox(d.message)
                        TextButton(onClick = { placesVm.load() }) { Text("Retry") }
                    }
                    else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                Spacer(Modifier.height(16.dp))
                Text("Type", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    typeFilters.forEach { (label, value) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { viewModel.setType(value) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TrailBlueFaint,
                                selectedLabelColor = TrailBlue
                            )
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Search within", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    radiusOptions.forEach { km ->
                        FilterChip(
                            selected = radius == km,
                            onClick = { viewModel.setRadius(km) },
                            label = { Text("$km km", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TrailBlueFaint,
                                selectedLabelColor = TrailBlue
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            when (val r = result) {
                is Resource.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is Resource.Error -> ErrorBox(r.message)

                is Resource.Success -> ResultSection(
                    r = r.data,
                    onCall = { openApp(dialIntent(it), "No phone app found on this device.") },
                    onWeb = { openApp(webIntent(it), "No web browser found on this device.") },
                    onMap = { openApp(mapIntent(it), "No map app found on this device.") }
                )

                null -> Text(
                    "Use your location or pick a destination to see hotels, guest houses and other places to stay nearby.",
                    fontSize = 13.sp, color = TextMuted
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultSection(
    r: NearbyHotelsResponse,
    onCall: (String) -> Unit,
    onWeb: (String) -> Unit,
    onMap: (HotelDto) -> Unit
) {
    val hotels = r.hotels.orEmpty()
    val where = r.origin.destination
        ?: "your location (${"%.4f".format(r.origin.lat)}, ${"%.4f".format(r.origin.lon)})"

    Text("Places to stay near $where", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
    if (hotels.isNotEmpty()) {
        val total = r.total_in_radius ?: r.count
        Text(
            "Showing ${hotels.size} of $total within ${"%.0f".format(r.radius_km)} km, nearest first",
            fontSize = 12.sp, color = TextMuted
        )
    }
    Spacer(Modifier.height(10.dp))

    if (hotels.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF4E0))
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                r.message
                    ?: "No place to stay is mapped within ${"%.0f".format(r.radius_km)} km. " +
                    "The map data may be incomplete. Try a larger distance or another destination.",
                fontSize = 13.sp, color = Color(0xFF7A4A00)
            )
        }
        r.nearest_overall?.let {
            Spacer(Modifier.height(12.dp))
            Text("Nearest mapped place to stay", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            HotelCard(it, onCall, onWeb, onMap)
        }
    } else {
        hotels.forEach {
            HotelCard(it, onCall, onWeb, onMap)
            Spacer(Modifier.height(10.dp))
        }
    }

    Spacer(Modifier.height(8.dp))
    Text(
        "No prices are shown because OpenStreetMap has none. Contact a place to ask about rooms and rates.",
        fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(vertical = 1.dp)
    )
    r.notes.orEmpty().forEach {
        Text("•  $it", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(vertical = 1.dp))
    }
    r.attribution?.let { Text(it, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp)) }
}

private fun typeStyle(type: String?): Pair<String, Color> = when (type) {
    "hotel" -> "Hotel" to Color(0xFF1565C0)
    "guest_house" -> "Guest house" to Color(0xFF00796B)
    "hostel" -> "Hostel" to Color(0xFF6A1B9A)
    "motel" -> "Motel" to Color(0xFF455A64)
    "chalet" -> "Chalet" to Color(0xFF8D6E63)
    "apartment" -> "Apartment" to Color(0xFF3949AB)
    "alpine_hut" -> "Mountain hut" to Color(0xFFE65100)
    "camp_site" -> "Camp site" to Color(0xFF2E7D32)
    null -> "Place to stay" to TextMuted
    else -> type.replace('_', ' ').replaceFirstChar { it.uppercase() } to TextMuted
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotelCard(
    h: HotelDto,
    onCall: (String) -> Unit,
    onWeb: (String) -> Unit,
    onMap: (HotelDto) -> Unit
) {
    val (label, color) = typeStyle(h.type)
    val phoneText = h.phone?.takeIf { it.isNotBlank() }
    // some entries list several numbers separated by ; or , so dial the first one
    val dialNumber = phoneText?.split(';', ',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    val site = h.website?.takeIf { it.isNotBlank() }
    val stars = h.stars?.takeIf { it in 1..5 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(h.name ?: "Unnamed place", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InfoTag(label, color)
                        if (stars != null) InfoTag("$stars-star", Amber)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${"%.1f".format(h.distance_km)} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
                    Text("straight line", fontSize = 10.sp, color = TextMuted)
                }
            }
            if (phoneText != null) {
                Spacer(Modifier.height(8.dp))
                Text(phoneText, fontSize = 12.sp, color = Color(0xFF444444))
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dialNumber != null) {
                    OutlinedButton(onClick = { onCall(dialNumber) }) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                }
                if (site != null) {
                    OutlinedButton(onClick = { onWeb(site) }) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Website")
                    }
                }
                OutlinedButton(onClick = { onMap(h) }) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Map")
                }
            }
        }
    }
}