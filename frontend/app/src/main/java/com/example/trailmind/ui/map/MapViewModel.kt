package com.example.trailmind.ui.map

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.repository.MapRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// a point drawn on the map; kind is hospital, clinic, doctors, rescue or hotel
data class MapPoint(val lat: Double, val lon: Double, val title: String, val snippet: String, val kind: String)

// what the map is showing: every destination, the route of one trip, or one place in focus
sealed interface MapMode {
    data object All : MapMode
    data class Route(val stops: List<String>) : MapMode
    data class Focus(val name: String) : MapMode
}

// screens call these to open the map without needing the navigation controller
class MapActions(
    val showAll: () -> Unit,
    val showRoute: (List<String>) -> Unit,
    val focus: (String) -> Unit
)

val LocalMapActions = compositionLocalOf { MapActions(showAll = {}, showRoute = {}, focus = {}) }

private fun facilityLabel(type: String): String = when (type) {
    "hospital" -> "Hospital"
    "clinic" -> "Clinic / health post"
    "doctors" -> "Doctor"
    "rescue" -> "Rescue / ambulance"
    else -> type.replaceFirstChar { it.uppercase() }
}

private fun hotelLabel(type: String?): String = when (type) {
    "hotel" -> "Hotel"
    "guest_house" -> "Guest house"
    "hostel" -> "Hostel"
    "motel" -> "Motel"
    "chalet" -> "Chalet"
    "apartment" -> "Apartment"
    "alpine_hut" -> "Mountain hut"
    "camp_site" -> "Camp site"
    else -> "Place to stay"
}

class MapViewModel : ViewModel() {
    private val repo = MapRepository()

    private val _mode = MutableStateFlow<MapMode>(MapMode.All)
    val mode: StateFlow<MapMode> = _mode

    private val _showHospitals = MutableStateFlow(false)
    val showHospitals: StateFlow<Boolean> = _showHospitals
    private val _showHotels = MutableStateFlow(false)
    val showHotels: StateFlow<Boolean> = _showHotels

    private val _hospitals = MutableStateFlow<List<MapPoint>>(emptyList())
    val hospitals: StateFlow<List<MapPoint>> = _hospitals
    private val _hotels = MutableStateFlow<List<MapPoint>>(emptyList())
    val hotels: StateFlow<List<MapPoint>> = _hotels

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun showAll() {
        _mode.value = MapMode.All
    }

    fun showRoute(stops: List<String>) {
        _mode.value = MapMode.Route(stops)
    }

    fun focus(name: String) {
        _mode.value = MapMode.Focus(name)
    }

    fun toggleHospitals(lat: Double, lon: Double) {
        val on = !_showHospitals.value
        _showHospitals.value = on
        if (on) loadHospitals(lat, lon) else _hospitals.value = emptyList()
    }

    fun toggleHotels(lat: Double, lon: Double) {
        val on = !_showHotels.value
        _showHotels.value = on
        if (on) loadHotels(lat, lon) else _hotels.value = emptyList()
    }

    // reload the switched-on layers around a new spot (the middle of the map)
    fun searchArea(lat: Double, lon: Double) {
        if (_showHospitals.value) loadHospitals(lat, lon)
        if (_showHotels.value) loadHotels(lat, lon)
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun loadHospitals(lat: Double, lon: Double) {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repo.facilities(lat, lon)) {
                is Resource.Success -> {
                    _hospitals.value = r.data.facilities.map {
                        MapPoint(
                            it.lat, it.lon, it.name,
                            "${facilityLabel(it.type)} · ${"%.1f".format(it.distance_km)} km from the search spot",
                            it.type
                        )
                    }
                    if (r.data.facilities.isEmpty()) {
                        _message.value =
                            "No hospitals or clinics are mapped within 25 km of this spot. The map data may be incomplete."
                    }
                }
                is Resource.Error -> _message.value = r.message
                else -> {}
            }
            _loading.value = false
        }
    }

    private fun loadHotels(lat: Double, lon: Double) {
        viewModelScope.launch {
            _loading.value = true
            when (val r = repo.hotels(lat, lon)) {
                is Resource.Success -> {
                    val list = r.data.hotels.orEmpty()
                    _hotels.value = list.map {
                        val phone = it.phone?.takeIf { p -> p.isNotBlank() }?.let { p -> " · $p" } ?: ""
                        MapPoint(
                            it.lat, it.lon, it.name ?: "Place to stay",
                            "${hotelLabel(it.type)} · ${"%.1f".format(it.distance_km)} km from the search spot$phone",
                            "hotel"
                        )
                    }
                    if (list.isEmpty()) {
                        _message.value =
                            "No places to stay are mapped within 15 km of this spot. The map data may be incomplete."
                    }
                }
                is Resource.Error -> _message.value = r.message
                else -> {}
            }
            _loading.value = false
        }
    }
}