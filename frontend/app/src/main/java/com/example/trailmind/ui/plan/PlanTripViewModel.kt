package com.example.trailmind.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.ItineraryRequest
import com.example.trailmind.data.remote.dto.ItineraryResponse
import com.example.trailmind.data.remote.dto.SaveTripRequest
import com.example.trailmind.data.remote.dto.SavedTripDto
import com.example.trailmind.data.repository.TripRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// shared by the Plan Trip screens and the itinerary screen (one instance for the Activity)
class PlanTripViewModel : ViewModel() {

    private val repo = TripRepository()

    private val _destinations = MutableStateFlow<Resource<List<DestinationDto>>?>(null)
    val destinations: StateFlow<Resource<List<DestinationDto>>?> = _destinations

    private val _itinerary = MutableStateFlow<Resource<ItineraryResponse>?>(null)
    val itinerary: StateFlow<Resource<ItineraryResponse>?> = _itinerary

    private val _save = MutableStateFlow<Resource<SavedTripDto>?>(null)
    val saveState: StateFlow<Resource<SavedTripDto>?> = _save

    var lastRequest: ItineraryRequest? = null
        private set

    fun loadDestinations() {
        val current = _destinations.value
        if (current is Resource.Success || current is Resource.Loading) return
        _destinations.value = Resource.Loading
        viewModelScope.launch { _destinations.value = repo.destinations() }
    }

    fun generate(request: ItineraryRequest) {
        if (_itinerary.value is Resource.Loading) return
        lastRequest = request
        _itinerary.value = Resource.Loading
        viewModelScope.launch { _itinerary.value = repo.itinerary(request) }
    }

    fun clearItinerary() {
        if (_itinerary.value !is Resource.Loading) _itinerary.value = null
    }

    fun saveTrip(request: ItineraryRequest, itinerary: ItineraryResponse) {
        if (_save.value is Resource.Loading) return
        _save.value = Resource.Loading
        viewModelScope.launch {
            _save.value = repo.saveTrip(SaveTripRequest(itinerary.title, request, itinerary))
        }
    }

    fun resetSave() {
        _save.value = null
    }
}