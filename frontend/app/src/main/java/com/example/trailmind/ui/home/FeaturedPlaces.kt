package com.example.trailmind.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.FeaturedDataDto
import com.example.trailmind.data.remote.dto.FeaturedPlaceDto
import com.example.trailmind.data.repository.AccountRepository
import com.example.trailmind.ui.account.FavoriteHeart
import com.example.trailmind.ui.account.LocalAppActions
import com.example.trailmind.ui.theme.Pal
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeaturedViewModel : ViewModel() {
    private val repo = AccountRepository()

    private val _state = MutableStateFlow<Resource<FeaturedDataDto>?>(null)
    val state: StateFlow<Resource<FeaturedDataDto>?> = _state

    // shows what we already have while the fresh answer loads
    fun load() {
        if (_state.value !is Resource.Success) _state.value = Resource.Loading
        viewModelScope.launch {
            val r = repo.featured()
            if (r is Resource.Success || _state.value !is Resource.Success) _state.value = r
        }
    }
}

private fun featuredIcon(category: String?): ImageVector = when (category) {
    "Cultural" -> Icons.Default.AccountBalance
    "Trekking" -> Icons.Default.Terrain
    "Hiking" -> Icons.Default.Hiking
    "Homestay" -> Icons.Default.Home
    "Wildlife" -> Icons.Default.Pets
    else -> Icons.Default.Landscape
}

@Composable
fun FeaturedPlacesSection(vm: FeaturedViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val actions = LocalAppActions.current
    LaunchedEffect(Unit) { vm.load() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Popular Places", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Pal.textDark)
            TextButton(onClick = { actions.openPlaces() }) {
                Text("See all", color = TrailBlue, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(6.dp))

        when (val s = state) {
            null, is Resource.Loading -> Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp) }

            is Resource.Error -> Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Popular places could not be loaded.", fontSize = 13.sp, color = Pal.textMuted)
                TextButton(onClick = { vm.load() }) { Text("Retry") }
            }

            is Resource.Success -> {
                val places = s.data.places.orEmpty()
                if (places.isEmpty()) {
                    Text(
                        "No places to show yet.",
                        fontSize = 13.sp, color = Pal.textMuted,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(places, key = { it.name }) { p ->
                            FeaturedCard(p) { actions.openPlace(p.name) }
                        }
                    }
                    val basis = places.firstNotNullOfOrNull { it.typical?.basis?.takeIf { b -> b.isNotBlank() } }
                    if (basis != null) {
                        Text(
                            "Prices are typical per person: $basis",
                            fontSize = 11.sp, color = Pal.textMuted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(p: FeaturedPlaceDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(180.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.verticalGradient(listOf(TrailBlueMid, TrailBlueDark))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    featuredIcon(p.category), contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(40.dp)
                )
                Box(Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                    FavoriteHeart(p.name, tint = Color.White)
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Pal.textDark,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    listOfNotNull(p.category, p.altitude_m?.let { "${"%,d".format(it)} m" }).joinToString(" · "),
                    fontSize = 11.sp, color = Pal.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                val rating = p.avg_rating
                val count = p.review_count ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rating != null && count > 0) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${"%.1f".format(rating)} ($count)", fontSize = 12.sp, color = Pal.textBody)
                    } else {
                        Text("No reviews yet", fontSize = 12.sp, color = Pal.textMuted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val t = p.typical
                val price = t?.per_person_npr
                if (price != null) {
                    Text("from NPR ${"%,.0f".format(price)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TrailBlue)
                    Text(
                        "per person" + (t.days?.let { " · $it days" } ?: ""),
                        fontSize = 11.sp, color = Pal.textMuted
                    )
                } else {
                    Text("Price not available", fontSize = 11.sp, color = Pal.textMuted)
                }
            }
        }
    }
}