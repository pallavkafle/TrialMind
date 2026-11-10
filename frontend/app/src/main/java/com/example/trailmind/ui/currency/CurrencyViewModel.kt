package com.example.trailmind.ui.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.CurrencyRatesDto
import com.example.trailmind.data.repository.CurrencyRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CurrencyViewModel : ViewModel() {
    private val repo = CurrencyRepository()

    private val _state = MutableStateFlow<Resource<CurrencyRatesDto>?>(null)
    val state: StateFlow<Resource<CurrencyRatesDto>?> = _state

    fun load(force: Boolean = false) {
        val current = _state.value
        if (current is Resource.Loading) return
        if (!force && current is Resource.Success) return
        _state.value = Resource.Loading
        viewModelScope.launch { _state.value = repo.rates() }
    }
}