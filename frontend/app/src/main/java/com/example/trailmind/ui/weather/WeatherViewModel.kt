package com.example.trailmind.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.WeatherDto
import com.example.trailmind.data.repository.WeatherRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {
    private val repo = WeatherRepository()

    private val _state = MutableStateFlow<Resource<WeatherDto>?>(null)
    val state: StateFlow<Resource<WeatherDto>?> = _state

    private var key: String? = null

    fun load(lat: Double, lon: Double, force: Boolean = false) {
        val k = "$lat,$lon"
        val current = _state.value
        if (current is Resource.Loading && k == key) return
        if (!force && current is Resource.Success && k == key) return
        key = k
        _state.value = Resource.Loading
        viewModelScope.launch { _state.value = repo.forecast(lat, lon) }
    }
}