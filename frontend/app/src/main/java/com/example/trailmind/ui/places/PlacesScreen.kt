package com.example.trailmind.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.InfoTag
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.home.TrailBlueFaint
import com.example.trailmind.util.Resource

private val TextMuted: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textMuted
private val TextDark: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textDark

private val categories = listOf("All", "Cultural", "Trekking", "Hiking", "Homestay", "Wildlife")

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Cultural" -> Icons.Default.AccountBalance
    "Trekking" -> Icons.Default.Terrain
    "Hiking" -> Icons.Default.Hiking
    "Homestay" -> Icons.Default.Home
    "Wildlife" -> Icons.Default.Pets
    else -> Icons.Default.Place
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlacesScreen(
    initialCategory: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: PlacesViewModel
) {
    var category by rememberSaveable {
        mutableStateOf(if (initialCategory in categories) initialCategory else "All")
    }
    var query by rememberSaveable { mutableStateOf("") }
    val state by viewModel.destinations.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    val title = if (initialCategory != "All" && initialCategory in categories) initialCategory else "Places"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
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
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search places") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TrailBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrailBlue,
                        focusedLabelColor = TrailBlue,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TrailBlueFaint,
                                selectedLabelColor = TrailBlue
                            )
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val s = state) {
                    null, is Resource.Loading ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    is Resource.Error -> Column(Modifier.padding(20.dp)) {
                        ErrorBox(s.message)
                        TextButton(onClick = { viewModel.load() }) { Text("Retry") }
                    }

                    is Resource.Success -> {
                        val q = query.trim()
                        val items = s.data
                            .filter { category == "All" || it.category == category }
                            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
                            .sortedBy { it.name }

                        if (items.isEmpty()) {
                            Text(
                                "No places match your search.",
                                fontSize = 13.sp, color = TextMuted,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items, key = { it.name }) { dest ->
                                    PlaceCard(dest) { onOpen(dest.name) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceCard(d: DestinationDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(TrailBlueFaint),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(d.category), contentDescription = null, tint = TrailBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(3.dp))
                Text("${d.category} · ${"%,d".format(d.altitude_m)} m", fontSize = 12.sp, color = TextMuted)
                if (d.is_restricted || d.has_porter) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (d.is_restricted) InfoTag("Restricted area", com.example.trailmind.ui.theme.Pal.red)
                        if (d.has_porter) InfoTag("Porter available", com.example.trailmind.ui.theme.Pal.green)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF999999))
        }
    }
}