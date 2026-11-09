package com.example.trailmind.ui.emergency

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
import com.example.trailmind.data.remote.dto.FacilityDto
import com.example.trailmind.data.remote.dto.NearestFacilitiesResponse
import com.example.trailmind.ui.components.*
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.util.LocationHelper
import com.example.trailmind.util.Resource
import kotlinx.coroutines.launch

private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)
private val EmergencyRed = Color(0xFFC62828)

// kept in the app so they show even with no internet
private val emergencyNumbers = listOf(
    "Police" to "100",
    "Ambulance" to "102",
    "Fire brigade" to "101",
    "Tourist police" to "1144"
)

private val typeFilters: List<Pair<String, String?>> = listOf(
    "All" to null,
    "Hospitals" to "hospital",
    "Clinics" to "clinic",
    "Doctors" to "doctors",
    "Rescue" to "rescue"
)

// opens the dialer with the number filled in; it never places the call by itself
private fun dialIntent(number: String) =
    Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.filter { it.isDigit() || it == '+' }))

private fun mapIntent(f: FacilityDto) =
    Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:${f.lat},${f.lon}?q=${f.lat},${f.lon}(${Uri.encode(f.name)})")
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmergencyScreen(
    onBack: () -> Unit,
    viewModel: EmergencyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val destinations by viewModel.destinations.collectAsState()
    val result by viewModel.result.collectAsState()
    val type by viewModel.type.collectAsState()
    val locating by viewModel.locating.collectAsState()

    var picked by rememberSaveable { mutableStateOf("") }
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

    fun openApp(intent: Intent, failMessage: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(failMessage) }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadDestinations() }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Emergency", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmergencyRed,
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

            SectionCard(title = "Emergency numbers", icon = Icons.Default.Call) {
                Text("Tap Call to open your phone's dialer.", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(6.dp))
                emergencyNumbers.forEach { (service, number) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(service, fontSize = 14.sp, color = TextDark)
                            Text(number, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
                        }
                        OutlinedButton(
                            onClick = { openApp(dialIntent(number), "No phone app found on this device.") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call")
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Find nearby help", icon = Icons.Default.LocalHospital) {
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
                        fontSize = 12.sp, color = EmergencyRed
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
                        TextButton(onClick = { viewModel.loadDestinations() }) { Text("Retry") }
                    }
                    else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                Spacer(Modifier.height(16.dp))
                Text("Show", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
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
                    onCall = { number -> openApp(dialIntent(number), "No phone app found on this device.") },
                    onMap = { f -> openApp(mapIntent(f), "No map app found on this device.") }
                )

                null -> Text(
                    "Use your location or pick a destination to see the nearest hospitals, clinics and rescue posts.",
                    fontSize = 13.sp, color = TextMuted
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultSection(
    r: NearestFacilitiesResponse,
    onCall: (String) -> Unit,
    onMap: (FacilityDto) -> Unit
) {
    val where = r.origin.destination ?: "your location"
    Text("Nearest to $where", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
    Text("Straight-line distances, searched within ${r.radius_km.toInt()} km", fontSize = 12.sp, color = TextMuted)
    Spacer(Modifier.height(10.dp))

    if (r.facilities.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF4E0))
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFB26A00), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                r.message ?: "No mapped facility nearby. Call an emergency number.",
                fontSize = 13.sp, color = Color(0xFF7A4A00)
            )
        }
        r.nearest_overall?.let {
            Spacer(Modifier.height(12.dp))
            Text("Nearest mapped facility", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            FacilityCard(it, onCall, onMap)
        }
    } else {
        r.facilities.forEach {
            FacilityCard(it, onCall, onMap)
            Spacer(Modifier.height(10.dp))
        }
    }

    Spacer(Modifier.height(8.dp))
    r.notes.forEach {
        Text("•  $it", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(vertical = 1.dp))
    }
    Text(r.attribution, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
}

private fun typeStyle(type: String): Pair<String, Color> = when (type) {
    "hospital" -> "Hospital" to Color(0xFFC62828)
    "clinic" -> "Clinic / health post" to Color(0xFF1565C0)
    "doctors" -> "Doctor" to Color(0xFF00796B)
    "rescue" -> "Rescue / ambulance" to Color(0xFFE65100)
    else -> type.replaceFirstChar { it.uppercase() } to TextMuted
}

@Composable
private fun FacilityCard(
    f: FacilityDto,
    onCall: (String) -> Unit,
    onMap: (FacilityDto) -> Unit
) {
    val (label, color) = typeStyle(f.type)
    val phoneText = f.phone?.takeIf { it.isNotBlank() }
    // some entries list several numbers separated by ; or , so dial the first one
    val dialNumber = phoneText?.split(';', ',')?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${"%.1f".format(f.distance_km)} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
                    Text("straight line", fontSize = 10.sp, color = TextMuted)
                }
            }
            if (phoneText != null) {
                Spacer(Modifier.height(8.dp))
                Text(phoneText, fontSize = 12.sp, color = Color(0xFF444444))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (dialNumber != null) {
                    OutlinedButton(onClick = { onCall(dialNumber) }) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                }
                OutlinedButton(onClick = { onMap(f) }) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Map")
                }
            }
        }
    }
}