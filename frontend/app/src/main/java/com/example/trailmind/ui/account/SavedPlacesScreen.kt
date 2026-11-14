package com.example.trailmind.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.places.PlacesViewModel
import com.example.trailmind.ui.theme.Pal
import com.example.trailmind.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    onBack: () -> Unit,
    placesVm: PlacesViewModel
) {
    val fav = LocalFavorites.current
    val actions = LocalAppActions.current
    val list by fav.list.collectAsState()
    val loaded by fav.loaded.collectAsState()
    val error by fav.error.collectAsState()
    val places by placesVm.destinations.collectAsState()

    LaunchedEffect(Unit) {
        fav.ensureLoaded()
        placesVm.load()
    }

    val byName: Map<String, DestinationDto> = when (val p = places) {
        is Resource.Success -> p.data.associateBy { it.name }
        else -> emptyMap()
    }
    val err = error

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Saved Places", fontWeight = FontWeight.SemiBold) },
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !loaded && err != null -> Column(Modifier.padding(20.dp)) {
                    ErrorBox(err)
                    TextButton(onClick = { fav.refresh() }) { Text("Retry") }
                }
                !loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                list.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No saved places yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Pal.textDark)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap the heart on any place to save it here.",
                        fontSize = 13.sp, color = Pal.textMuted
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it }) { name ->
                        SavedCard(name, byName[name]) { actions.openPlace(name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedCard(name: String, d: DestinationDto?, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Pal.textDark)
                if (d != null) {
                    Text(
                        "${d.category} · ${"%,d".format(d.altitude_m)} m",
                        fontSize = 12.sp, color = Pal.textMuted
                    )
                }
            }
            FavoriteHeart(name)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF999999))
        }
    }
}