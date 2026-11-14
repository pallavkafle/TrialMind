package com.example.trailmind.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.remote.dto.ParsedTripDto
import com.example.trailmind.data.repository.ChatRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatTripViewModel : ViewModel() {
    private val repo = ChatRepository()

    private val _state = MutableStateFlow<Resource<ParsedTripDto>?>(null)
    val state: StateFlow<Resource<ParsedTripDto>?> = _state

    // goes up on every successful reading, so the screen fills its form once per reading
    var seq = 0
        private set

    fun parse(text: String) {
        if (_state.value is Resource.Loading) return
        _state.value = Resource.Loading
        viewModelScope.launch {
            val result = repo.parse(text)
            if (result is Resource.Success) seq++
            _state.value = result
        }
    }

    // back to the text box
    fun reset() {
        if (_state.value !is Resource.Loading) _state.value = null
    }
}