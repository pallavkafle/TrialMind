package com.example.trailmind.ui.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.NearestFacilitiesResponse
import com.example.trailmind.data.repository.HealthRepository
import com.example.trailmind.data.repository.TripRepository
import com.example.trailmind.util.LocationHelper
import com.example.trailmind.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmergencyViewModel : ViewModel() {
    private val tripRepo = TripRepository()
    private val healthRepo = HealthRepository()

    private val _destinations = MutableStateFlow<Resource<List<DestinationDto>>?>(null)
    val destinations: StateFlow<Resource<List<DestinationDto>>?> = _destinations

    private val _result = MutableStateFlow<Resource<NearestFacilitiesResponse>?>(null)
    val result: StateFlow<Resource<NearestFacilitiesResponse>?> = _result

    // null = all types, otherwise hospital / clinic / doctors / rescue
    private val _type = MutableStateFlow<String?>(null)
    val type: StateFlow<String?> = _type

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating

    // where the last search started, so changing the type filter can repeat it
    private sealed interface Source {
        data class Place(val name: String) : Source
        data class Gps(val lat: Double, val lon: Double) : Source
    }

    private var source: Source? = null
    private var job: Job? = null

    fun loadDestinations() {
        val current = _destinations.value
        if (current is Resource.Success || current is Resource.Loading) return
        _destinations.value = Resource.Loading
        viewModelScope.launch { _destinations.value = tripRepo.destinations() }
    }

    fun searchPlace(name: String) {
        source = Source.Place(name)
        _locating.value = false
        load()
    }

    fun searchMyLocation(appContext: Context) {
        if (_locating.value) return
        job?.cancel()
        _locating.value = true
        _result.value = Resource.Loading
        job = viewModelScope.launch {
            val loc = try {
                LocationHelper.getLocation(appContext)
            } finally {
                _locating.value = false
            }
            if (loc == null) {
                _result.value = Resource.Error(
                    "Could not get your location. Turn on location on your phone and try again, or pick a destination."
                )
            } else {
                source = Source.Gps(loc.latitude, loc.longitude)
                _result.value = healthRepo.nearest(null, loc.latitude, loc.longitude, _type.value)
            }
        }
    }

    fun setType(newType: String?) {
        _type.value = newType
        if (!_locating.value && source != null) load()
    }

    private fun load() {
        val s = source ?: return
        val t = _type.value
        job?.cancel()
        _result.value = Resource.Loading
        job = viewModelScope.launch {
            _result.value = when (s) {
                is Source.Place -> healthRepo.nearest(s.name, null, null, t)
                is Source.Gps -> healthRepo.nearest(null, s.lat, s.lon, t)
            }
        }
    }
}