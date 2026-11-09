package com.example.trailmind.ui.permits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.DestinationDto
import com.example.trailmind.data.remote.dto.PermitCheckRequest
import com.example.trailmind.data.remote.dto.PermitCheckResponse
import com.example.trailmind.data.repository.PermitRepository
import com.example.trailmind.data.repository.TripRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PermitsViewModel : ViewModel() {
    private val tripRepo = TripRepository()
    private val permitRepo = PermitRepository()

    private val _destinations = MutableStateFlow<Resource<List<DestinationDto>>?>(null)
    val destinations: StateFlow<Resource<List<DestinationDto>>?> = _destinations

    private val _result = MutableStateFlow<Resource<PermitCheckResponse>?>(null)
    val result: StateFlow<Resource<PermitCheckResponse>?> = _result

    fun loadDestinations() {
        val current = _destinations.value
        if (current is Resource.Success || current is Resource.Loading) return
        _destinations.value = Resource.Loading
        viewModelScope.launch { _destinations.value = tripRepo.destinations() }
    }

    fun check(request: PermitCheckRequest) {
        if (_result.value is Resource.Loading) return
        _result.value = Resource.Loading
        viewModelScope.launch { _result.value = permitRepo.check(request) }
    }
}