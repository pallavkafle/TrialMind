package com.example.trailmind.ui.hotels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.NearbyHotelsResponse
import com.example.trailmind.data.repository.HotelRepository
import com.example.trailmind.util.LocationHelper
import com.example.trailmind.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HotelsViewModel : ViewModel() {
    private val repo = HotelRepository()

    private val _result = MutableStateFlow<Resource<NearbyHotelsResponse>?>(null)
    val result: StateFlow<Resource<NearbyHotelsResponse>?> = _result

    // null = every kind of lodging, otherwise hotel / guest_house / hostel / alpine_hut / camp_site
    private val _type = MutableStateFlow<String?>(null)
    val type: StateFlow<String?> = _type

    private val _radius = MutableStateFlow(10)
    val radius: StateFlow<Int> = _radius

    private val _locating = MutableStateFlow(false)
    val locating: StateFlow<Boolean> = _locating

    // where the last search started, so changing a filter can repeat it
    private sealed interface Source {
        data class Place(val name: String) : Source
        data class Gps(val lat: Double, val lon: Double) : Source
    }

    private var source: Source? = null
    private var job: Job? = null

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
                _result.value = repo.nearby(null, loc.latitude, loc.longitude, _type.value, _radius.value)
            }
        }
    }

    fun setType(newType: String?) {
        _type.value = newType
        if (!_locating.value && source != null) load()
    }

    fun setRadius(km: Int) {
        _radius.value = km
        if (!_locating.value && source != null) load()
    }

    private fun load() {
        val s = source ?: return
        val t = _type.value
        val r = _radius.value
        job?.cancel()
        _result.value = Resource.Loading
        job = viewModelScope.launch {
            _result.value = when (s) {
                is Source.Place -> repo.nearby(s.name, null, null, t, r)
                is Source.Gps -> repo.nearby(null, s.lat, s.lon, t, r)
            }
        }
    }
}