package com.example.trailmind.ui.map

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.ui.home.TrailBlue
import com.example.trailmind.ui.places.PlacesViewModel
import com.example.trailmind.util.Resource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private val NEPAL_BOX = BoundingBox(30.6, 88.3, 26.2, 79.9)   // north, east, south, west

private val ROUTE_LINE = 0xFFC62828.toInt()
private val START_GREEN = 0xFF2E7D32.toInt()
private val END_RED = 0xFFC62828.toInt()
private val STOP_BLUE = 0xFF1F5C8B.toInt()
private val HOTEL_PURPLE = 0xFF6A1B9A.toInt()

private fun categoryColor(category: String): Int = when (category) {
    "Cultural" -> 0xFFC62828
    "Trekking" -> 0xFF2E7D32
    "Hiking" -> 0xFF66BB6A
    "Homestay" -> 0xFFEF6C00
    "Wildlife" -> 0xFF6A1B9A
    else -> 0xFF1F5C8B
}.toInt()

private fun facilityColor(kind: String): Int = when (kind) {
    "hospital" -> 0xFFD50000
    "clinic" -> 0xFF1565C0
    "doctors" -> 0xFF00796B
    "rescue" -> 0xFFE65100
    else -> 0xFF455A64
}.toInt()

// one marker on the map; squares are hospitals and hotels, circles are destinations
private data class MapItem(
    val lat: Double,
    val lon: Double,
    val title: String,
    val snippet: String,
    val color: Int,
    val sizeDp: Int,
    val square: Boolean
)

// plain holder for things the map needs between redraws (not Compose state)
private class MapHolder {
    var view: MapView? = null
    var drawKey: Int? = null
    var cameraKey: Any? = null
}

private tailrec fun Context.lifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.lifecycleOwner()
    else -> null
}

private fun pin(ctx: Context, color: Int, sizeDp: Int, square: Boolean): Drawable {
    val density = ctx.resources.displayMetrics.density
    val px = (sizeDp * density).toInt()
    return GradientDrawable().apply {
        shape = if (square) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
        if (square) cornerRadius = 4 * density
        setColor(color)
        setStroke((2 * density).toInt(), android.graphics.Color.WHITE)
        setSize(px, px)
    }
}

private fun redraw(mv: MapView, items: List<MapItem>, line: List<GeoPoint>, focusTitle: String?) {
    mv.overlays.removeAll { it !is CopyrightOverlay }
    if (line.size >= 2) {
        mv.overlays.add(Polyline().apply {
            setPoints(line)
            outlinePaint.color = ROUTE_LINE
            outlinePaint.strokeWidth = 8f
        })
    }
    items.forEach { item ->
        val marker = Marker(mv).apply {
            position = GeoPoint(item.lat, item.lon)
            title = item.title
            snippet = item.snippet
            icon = pin(mv.context, item.color, item.sizeDp, item.square)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        }
        mv.overlays.add(marker)
        if (item.title == focusTitle) mv.post { marker.showInfoWindow() }
    }
    mv.invalidate()
}

private fun moveCamera(mv: MapView, mode: MapMode, line: List<GeoPoint>, byName: Map<String, DestinationDto>) {
    val move: () -> Unit = {
        when (mode) {
            is MapMode.All -> mv.zoomToBoundingBox(NEPAL_BOX, false, 40)
            is MapMode.Focus -> {
                val d = byName[mode.name]
                if (d == null) {
                    mv.zoomToBoundingBox(NEPAL_BOX, false, 40)
                } else {
                    mv.controller.setZoom(11.0)
                    mv.controller.setCenter(GeoPoint(d.latitude, d.longitude))
                }
            }
            is MapMode.Route -> when {
                line.size >= 2 ->
                    mv.zoomToBoundingBox(BoundingBox.fromGeoPoints(line).increaseByScale(1.3f), false, 80)
                line.size == 1 -> {
                    mv.controller.setZoom(11.0)
                    mv.controller.setCenter(line[0])
                }
                else -> mv.zoomToBoundingBox(NEPAL_BOX, false, 40)
            }
        }
    }
    if (mv.isLayoutOccurred) move() else mv.addOnFirstLayoutListener { _, _, _, _, _ -> move() }
}

private fun centerOf(holder: MapHolder): Pair<Double, Double> {
    val c = holder.view?.mapCenter
    return if (c != null) c.latitude to c.longitude else 28.0 to 84.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    mapVm: MapViewModel,
    placesVm: PlacesViewModel
) {
    val context = LocalContext.current
    val places by placesVm.destinations.collectAsState()
    val mode by mapVm.mode.collectAsState()
    val showHospitals by mapVm.showHospitals.collectAsState()
    val showHotels by mapVm.showHotels.collectAsState()
    val hospitals by mapVm.hospitals.collectAsState()
    val hotels by mapVm.hotels.collectAsState()
    val loading by mapVm.loading.collectAsState()
    val message by mapVm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val holder = remember { MapHolder() }

    LaunchedEffect(Unit) { placesVm.load() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            mapVm.clearMessage()
        }
    }

    // keep the map's own life cycle in step with the screen
    val owner = remember(context) { context.lifecycleOwner() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.view?.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.view?.onPause()
                else -> {}
            }
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            holder.view?.onPause()
            holder.view?.onDetach()
            holder.view = null
        }
    }

    val destinations: List<DestinationDto> = when (val p = places) {
        is Resource.Success -> p.data
        else -> emptyList()
    }
    val byName = destinations.associateBy { it.name }
    val m = mode

    val items = ArrayList<MapItem>()
    val line = ArrayList<GeoPoint>()
    when (m) {
        is MapMode.Route -> {
            val stops = m.stops.mapNotNull { byName[it] }
            stops.forEachIndexed { i, d ->
                val color = when (i) {
                    0 -> START_GREEN
                    stops.lastIndex -> END_RED
                    else -> STOP_BLUE
                }
                items.add(
                    MapItem(
                        d.latitude, d.longitude, "${i + 1}. ${d.name}",
                        "${d.category} · ${"%,d".format(d.altitude_m)} m", color, 22, false
                    )
                )
                line.add(GeoPoint(d.latitude, d.longitude))
            }
        }
        else -> destinations.forEach { d ->
            items.add(
                MapItem(
                    d.latitude, d.longitude, d.name,
                    "${d.category} · ${"%,d".format(d.altitude_m)} m", categoryColor(d.category), 18, false
                )
            )
        }
    }
    hospitals.forEach { items.add(MapItem(it.lat, it.lon, it.title, it.snippet, facilityColor(it.kind), 16, true)) }
    hotels.forEach { items.add(MapItem(it.lat, it.lon, it.title, it.snippet, HOTEL_PURPLE, 16, true)) }
    val focusTitle = (m as? MapMode.Focus)?.name

    val title = when (m) {
        is MapMode.Route -> "Trip route"
        is MapMode.Focus -> m.name
        else -> "Map"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1) },
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

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 4.0
                        maxZoomLevel = 18.0
                        isTilesScaledToDpi = true
                        overlays.add(CopyrightOverlay(ctx))
                        controller.setZoom(7.0)
                        controller.setCenter(GeoPoint(28.3, 84.2))
                        holder.view = this
                    }
                },
                update = { mv ->
                    val drawKey = listOf(items, line, focusTitle).hashCode()
                    if (holder.drawKey != drawKey) {
                        holder.drawKey = drawKey
                        redraw(mv, items, line, focusTitle)
                    }
                    val cameraKey = Pair(m, destinations.isNotEmpty())
                    if (holder.cameraKey != cameraKey && destinations.isNotEmpty()) {
                        holder.cameraKey = cameraKey
                        moveCamera(mv, m, line, byName)
                    }
                }
            )

            Column(Modifier.align(Alignment.TopStart).padding(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showHospitals,
                            onClick = {
                                val c = centerOf(holder)
                                mapVm.toggleHospitals(c.first, c.second)
                            },
                            label = { Text("Hospitals", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showHotels,
                            onClick = {
                                val c = centerOf(holder)
                                mapVm.toggleHotels(c.first, c.second)
                            },
                            label = { Text("Hotels", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                if (loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showHospitals || showHotels) {
                    Button(
                        onClick = {
                            val c = centerOf(holder)
                            mapVm.searchArea(c.first, c.second)
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = TrailBlue)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Search this area", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Text(
                            "Squares: hospitals and hotels. Circles: destinations. Tap a marker for details.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else if (m is MapMode.Route) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Text(
                            "Route lines are straight lines between stops; real road distance is longer.",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}