package com.example.trailmind.ui.permits

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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.PermitCheckRequest
import com.example.trailmind.data.remote.dto.PermitCheckResponse
import com.example.trailmind.data.remote.dto.PermitItemDto
import com.example.trailmind.ui.components.*
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.util.Resource

private val TextMuted = Color(0xFF6B7280)
private val TextDark = Color(0xFF1A1A2E)

private val nationalities = listOf("Domestic", "SAARC", "Foreign")
private val months = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private fun npr(v: Double) = "NPR ${"%,.0f".format(v)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermitsScreen(onBack: () -> Unit, viewModel: PermitsViewModel = viewModel()) {
    val selected = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }
    var nationality by rememberSaveable { mutableStateOf("Foreign") }
    var days by rememberSaveable { mutableStateOf("7") }
    var group by rememberSaveable { mutableStateOf("2") }
    var month by rememberSaveable { mutableStateOf("October") }
    var localError by remember { mutableStateOf<String?>(null) }

    val destinations by viewModel.destinations.collectAsState()
    val result by viewModel.result.collectAsState()
    val isLoading = result is Resource.Loading

    LaunchedEffect(Unit) { viewModel.loadDestinations() }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Permits & fees", fontWeight = FontWeight.SemiBold) },
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
            SectionCard(title = "Where to?", icon = Icons.Default.Flag) {
                DestinationChips(destinations, selected, 10) { viewModel.loadDestinations() }
            }

            Spacer(Modifier.height(14.dp))

            SectionCard(title = "Trip details", icon = Icons.Default.CalendarMonth) {
                Dropdown("Nationality", nationality, nationalities, Icons.Default.Public) { nationality = it }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField("Days", days, Icons.Default.Schedule, Modifier.weight(1f)) { days = it }
                    NumberField("Group size", group, Icons.Default.Groups, Modifier.weight(1f)) { group = it }
                }
                Spacer(Modifier.height(12.dp))
                Dropdown("Travel month", month, months, Icons.Default.CalendarMonth) { month = it }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val d = days.toIntOrNull() ?: 0
                    val g = group.toIntOrNull() ?: 0
                    localError = when {
                        selected.isEmpty() -> "Pick at least one destination."
                        d < 1 || d > 30 -> "Days must be between 1 and 30."
                        g < 1 || g > 20 -> "Group size must be between 1 and 20."
                        else -> null
                    }
                    if (localError == null) {
                        viewModel.check(
                            PermitCheckRequest(
                                destinations = selected.toList(),
                                duration_days = d,
                                group_size = g,
                                travel_month = month,
                                nationality_category = nationality
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
                    Icon(Icons.Default.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Check permits", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            localError?.let { ErrorBox(it) }
            when (val s = result) {
                is Resource.Error -> ErrorBox(s.message)
                is Resource.Success -> PermitResult(s.data)
                else -> {}
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermitResult(r: PermitCheckResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Estimated permits & fees", fontSize = 12.sp, color = TextMuted)
            Text(npr(r.total_npr), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
            Text(
                "${npr(r.total_per_person_npr)} per person · ${r.nationality_category} · ${r.group_size} people",
                fontSize = 13.sp, color = TextMuted
            )
            Spacer(Modifier.height(14.dp))
            if (r.permits.isEmpty()) {
                Text("No permits found in our data for these destinations.", fontSize = 13.sp, color = TextMuted)
            }
            r.permits.forEach { PermitRow(it) }
        }
    }

    r.destinations.filter { it.notes.isNotEmpty() }.forEach { d ->
        Spacer(Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(d.destination, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                d.notes.forEach {
                    Text("•  $it", fontSize = 12.sp, color = Color(0xFF444444), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }

    if (r.needs_verification.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
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
                "Check before you travel: ${r.needs_verification.joinToString("; ")}",
                fontSize = 12.sp, color = Color(0xFF7A4A00)
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    r.advisories.forEach {
        Text("•  $it", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(vertical = 2.dp))
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "Fee data researched ${r.as_of}. Fees change, so confirm with the authority before you go.",
        fontSize = 11.sp, color = TextMuted
    )
}

@Composable
private fun PermitRow(p: PermitItemDto) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                p.fee_per_person_npr?.let { npr(it) } ?: "Not found",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TrailBlue
            )
        }
        if (!p.authority.isNullOrBlank()) {
            Text(p.authority, fontSize = 11.sp, color = TextMuted)
        }
        Spacer(Modifier.height(6.dp))
        ConfidencePill(p.confidence)
        if (!p.note.isNullOrBlank()) {
            Text(p.note, fontSize = 12.sp, color = Color(0xFF555555), modifier = Modifier.padding(top = 6.dp))
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}

@Composable
private fun ConfidencePill(confidence: String) {
    val (label, color) = when (confidence) {
        "high" -> "High confidence" to Color(0xFF2E7D32)
        "medium" -> "Medium confidence" to Color(0xFFB26A00)
        else -> "Low confidence" to Color(0xFFD32F2F)
    }
    Text(
        label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}