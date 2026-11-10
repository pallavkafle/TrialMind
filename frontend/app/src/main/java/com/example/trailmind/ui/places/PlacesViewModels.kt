package com.example.trailmind.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.ReviewDto
import com.example.trailmind.data.remote.dto.ReviewListResponse
import com.example.trailmind.data.repository.ReviewRepository
import com.example.trailmind.data.repository.TripRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// one instance for the whole app, shared by the list and detail screens
class PlacesViewModel : ViewModel() {
    private val repo = TripRepository()

    private val _destinations = MutableStateFlow<Resource<List<DestinationDto>>?>(null)
    val destinations: StateFlow<Resource<List<DestinationDto>>?> = _destinations

    fun load() {
        val current = _destinations.value
        if (current is Resource.Success || current is Resource.Loading) return
        _destinations.value = Resource.Loading
        viewModelScope.launch { _destinations.value = repo.destinations() }
    }
}

// reviews of one destination (one instance per detail screen)
class PlaceDetailViewModel : ViewModel() {
    private val repo = ReviewRepository()

    private val _state = MutableStateFlow<Resource<ReviewListResponse>?>(null)
    val state: StateFlow<Resource<ReviewListResponse>?> = _state

    // ids of the reviews written by the logged-in user, so we can show a delete icon on them
    private val _mine = MutableStateFlow<Set<String>>(emptySet())
    val mine: StateFlow<Set<String>> = _mine

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load(destination: String) {
        if (_state.value !is Resource.Success) _state.value = Resource.Loading
        viewModelScope.launch {
            val list = async { repo.reviews(destination) }
            val own = async { repo.mine() }
            _state.value = list.await()
            val m = own.await()
            if (m is Resource.Success) _mine.value = m.data.map { it.id }.toSet()
        }
    }

    fun delete(id: String, destination: String) {
        viewModelScope.launch {
            when (val r = repo.delete(id)) {
                is Resource.Success -> load(destination)
                is Resource.Error -> _message.value = r.message
                else -> {}
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

class WriteReviewViewModel : ViewModel() {
    private val repo = ReviewRepository()

    private val _state = MutableStateFlow<Resource<ReviewDto>?>(null)
    val state: StateFlow<Resource<ReviewDto>?> = _state

    fun submit(destination: String, rating: Int, text: String) {
        if (_state.value is Resource.Loading) return
        _state.value = Resource.Loading
        viewModelScope.launch { _state.value = repo.create(destination, rating, text) }
    }
}