package com.example.trailmind.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.TripDetailDto
import com.example.trailmind.data.remote.dto.TripSummaryDto
import com.example.trailmind.data.repository.TripRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyTripsViewModel : ViewModel() {
    private val repo = TripRepository()

    private val _trips = MutableStateFlow<Resource<List<TripSummaryDto>>?>(null)
    val trips: StateFlow<Resource<List<TripSummaryDto>>?> = _trips

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load() {
        _trips.value = Resource.Loading
        viewModelScope.launch { _trips.value = repo.myTrips() }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val r = repo.deleteTrip(id)) {
                is Resource.Success -> {
                    val current = _trips.value as? Resource.Success<List<TripSummaryDto>>
                    if (current != null) {
                        _trips.value = Resource.Success(current.data.filterNot { it.id == id })
                    }
                }
                is Resource.Error -> _message.value = r.message
                else -> {}
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

class SavedTripViewModel : ViewModel() {
    private val repo = TripRepository()

    private val _trip = MutableStateFlow<Resource<TripDetailDto>?>(null)
    val trip: StateFlow<Resource<TripDetailDto>?> = _trip

    fun load(id: String) {
        if (_trip.value is Resource.Success || _trip.value is Resource.Loading) return
        _trip.value = Resource.Loading
        viewModelScope.launch {
            val r = repo.trip(id)
            _trip.value =
                if (r is Resource.Success && !isComplete(r.data))
                    Resource.Error("This trip was saved in an old format and cannot be opened.")
                else r
        }
    }

    fun retry(id: String) {
        _trip.value = null
        load(id)
    }

    // trips saved by hand (e.g. from Postman) may lack fields the screen needs
    private fun isComplete(t: TripDetailDto): Boolean = try {
        val d = t.itinerary
        val probe = d.days.size + d.safety_notes.size + d.route.stops.size + d.route.leg_km.size +
            d.warnings.size + d.title.length + d.summary.length + d.source.length +
            d.budget.total_npr.toInt() +
            d.days.sumOf { it.activities.size + it.title.length + it.location.length }
        probe >= 0
    } catch (e: NullPointerException) {
        false
    }
}