package com.example.trailmind.ui.places

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.ReviewDto
import com.example.trailmind.data.remote.dto.ReviewSummaryDto
import com.example.trailmind.ui.components.ErrorBox
import com.example.trailmind.ui.components.InfoTag
import com.example.trailmind.ui.components.RatingStars
import com.example.trailmind.ui.components.SectionCard
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.weather.WeatherCard
import com.example.trailmind.ui.weather.WeatherViewModel
import com.example.trailmind.util.Resource
import com.example.trailmind.util.formatIsoDate

private val TextMuted: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textMuted
private val TextDark: Color @Composable get() = com.example.trailmind.ui.theme.Pal.textDark
private val Red: Color @Composable get() = com.example.trailmind.ui.theme.Pal.red
private val Green: Color @Composable get() = com.example.trailmind.ui.theme.Pal.green
private val Amber: Color @Composable get() = com.example.trailmind.ui.theme.Pal.amber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    name: String,
    onBack: () -> Unit,
    onWriteReview: () -> Unit,
    onFindHotels: () -> Unit,
    placesVm: PlacesViewModel,
    viewModel: PlaceDetailViewModel = viewModel(),
    weatherVm: WeatherViewModel = viewModel()
) {
    val places by placesVm.destinations.collectAsState()
    val state by viewModel.state.collectAsState()
    val mine by viewModel.mine.collectAsState()
    val message by viewModel.message.collectAsState()
    val weather by weatherVm.state.collectAsState()
    val mapActions = LocalMapActions.current
    val snackbar = remember { SnackbarHostState() }
    var toDelete by remember { mutableStateOf<ReviewDto?>(null) }

    // runs again each time we come back from the write-review screen, so a new review shows up
    LaunchedEffect(name) {
        placesVm.load()
        viewModel.load(name)
    }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val dest: DestinationDto? = when (val p = places) {
        is Resource.Success -> p.data.firstOrNull { it.name == name }
        else -> null
    }

    // the forecast needs the destination's coordinates, so it starts once the destination is known
    LaunchedEffect(dest) {
        dest?.let { weatherVm.load(it.latitude, it.longitude) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = onWriteReview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Write a review", fontWeight = FontWeight.SemiBold)
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
            HeaderCard(name, dest)
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onFindHotels,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TrailBlue)
            ) {
                Icon(Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Places to stay nearby", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { mapActions.focus(name) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TrailBlue)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View on map", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))

            if (dest != null) {
                WeatherCard(
                    state = weather,
                    highestM = dest.altitude_m,
                    onRetry = { weatherVm.load(dest.latitude, dest.longitude, force = true) }
                )
                Spacer(Modifier.height(14.dp))
            }

            when (val s = state) {
                null, is Resource.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is Resource.Error -> {
                    ErrorBox(s.message)
                    TextButton(onClick = { viewModel.load(name) }) { Text("Retry") }
                }

                is Resource.Success -> {
                    val r = s.data
                    SectionCard(title = "Visitor reviews", icon = Icons.Default.Star) {
                        SummaryContent(r.summary)
                    }
                    Spacer(Modifier.height(14.dp))
                    r.data.orEmpty().forEach { review ->
                        ReviewCard(
                            review = review,
                            canDelete = review.id in mine,
                            onDelete = { toDelete = review }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    toDelete?.let { review ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete your review?") },
            text = { Text("It will be removed from this place's reviews.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(review.id, name)
                    toDelete = null
                }) { Text("Delete", color = com.example.trailmind.ui.theme.Pal.red) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderCard(name: String, d: DestinationDto?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            if (d != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${d.category} · ${"%,d".format(d.altitude_m)} m above sea level",
                    fontSize = 13.sp, color = TextMuted
                )
                if (d.is_restricted || d.has_porter) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (d.is_restricted) InfoTag("Restricted area: special permit needed", Red)
                        if (d.has_porter) InfoTag("Porter available", Green)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(sm: ReviewSummaryDto?) {
    if (sm == null || sm.count == 0) {
        Text("No reviews yet. Be the first to share how it was.", fontSize = 13.sp, color = TextMuted)
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            sm.avgRating?.let { "%.1f".format(it) } ?: "-",
            fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TrailBlue
        )
        Spacer(Modifier.width(12.dp))
        Column {
            RatingStars(sm.avgRating ?: 0.0, size = 18.dp)
            Text(
                if (sm.count == 1) "1 review" else "${sm.count} reviews",
                fontSize = 12.sp, color = TextMuted
            )
        }
    }

    sm.positivePct?.let {
        Spacer(Modifier.height(8.dp))
        Text("${"%.0f".format(it)}% of checked reviews are positive", fontSize = 13.sp, color = TextDark)
    }

    if (sm.warningCount > 0) {
        Spacer(Modifier.height(10.dp))
        NoticeBox(
            text = (if (sm.warningCount == 1) "1 review mentions" else "${sm.warningCount} reviews mention") +
                " safety hazards such as landslides or floods. Read them before you go.",
            background = com.example.trailmind.ui.theme.Pal.errorBg,
            tint = Red
        )
    } else if (sm.cautionCount > 0) {
        Spacer(Modifier.height(10.dp))
        NoticeBox(
            text = (if (sm.cautionCount == 1) "1 review is" else "${sm.cautionCount} reviews are") +
                " strongly negative. Read them before you go.",
            background = com.example.trailmind.ui.theme.Pal.amberBg,
            tint = Amber
        )
    }
}

@Composable
private fun NoticeBox(text: String, background: Color, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = tint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCard(review: ReviewDto, canDelete: Boolean, onDelete: () -> Unit) {
    val s = review.sentiment
    val label = s?.label
    val flag = s?.safetyFlag
    val keywords = s?.matchedKeywords.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(review.author ?: "Traveller", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    val date = formatIsoDate(review.createdAt)
                    if (date.isNotEmpty()) Text(date, fontSize = 11.sp, color = TextMuted)
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete my review", tint = Color(0xFF9CA3AF))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            RatingStars(review.rating.toDouble())
            Spacer(Modifier.height(8.dp))
            Text(review.text, fontSize = 13.sp, color = com.example.trailmind.ui.theme.Pal.textBody)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (label) {
                    null -> InfoTag("Analysis unavailable", TextMuted)
                    "POSITIVE" -> InfoTag("Positive", Green)
                    else -> InfoTag("Negative", Red)
                }
                when (flag) {
                    "WARNING" -> InfoTag(
                        if (keywords.isEmpty()) "Safety warning" else "Safety warning: ${keywords.joinToString(", ")}",
                        Red
                    )
                    "CAUTION" -> InfoTag("Caution: strongly negative", Amber)
                    else -> {}
                }
            }
        }
    }
}