package com.example.trailmind.ui.account

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.repository.AccountRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

val LocalFavorites = staticCompositionLocalOf<FavoritesViewModel> {
    error("FavoritesViewModel is not provided")
}

// the saved places of the logged-in user, shared by every heart in the app
class FavoritesViewModel : ViewModel() {
    private val repo = AccountRepository()

    private val _list = MutableStateFlow<List<String>>(emptyList())
    val list: StateFlow<List<String>> = _list          // newest first

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var loading = false

    fun ensureLoaded() {
        if (_loaded.value || loading) return
        refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        _error.value = null
        viewModelScope.launch {
            when (val r = repo.favorites()) {
                is Resource.Success -> {
                    _list.value = r.data
                    _loaded.value = true
                }
                is Resource.Error -> _error.value = r.message
                else -> {}
            }
            loading = false
        }
    }

    // flips the heart at once, then asks the server; goes back if the server could not be reached
    fun toggle(name: String) {
        val before = _list.value
        _list.value = if (name in before) before - name else listOf(name) + before
        viewModelScope.launch {
            when (val r = repo.toggle(name)) {
                is Resource.Success -> {
                    val cur = _list.value
                    _list.value = if (r.data.favorite) {
                        if (name in cur) cur else listOf(name) + cur
                    } else {
                        cur - name
                    }
                }
                is Resource.Error -> {
                    _list.value = before
                    _message.value = r.message
                }
                else -> {}
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    // called on logout, so the next account never sees this one's saved places
    fun reset() {
        _list.value = emptyList()
        _loaded.value = false
        _error.value = null
        loading = false
    }
}