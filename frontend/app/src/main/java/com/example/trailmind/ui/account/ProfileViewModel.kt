package com.example.trailmind.ui.account

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trailmind.data.local.TokenStore
import com.example.trailmind.data.remote.dto.UserDto
import com.example.trailmind.data.repository.AccountRepository
import com.example.trailmind.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val tokenStore = TokenStore.get(app)
    private val repo = AccountRepository()

    private val _user = MutableStateFlow(tokenStore.getUser())
    val user: StateFlow<UserDto?> = _user

    private val _save = MutableStateFlow<Resource<UserDto>?>(null)
    val save: StateFlow<Resource<UserDto>?> = _save

    fun save(name: String, nationality: String) {
        if (_save.value is Resource.Loading) return
        _save.value = Resource.Loading
        viewModelScope.launch {
            val r = repo.updateProfile(name.trim(), nationality)
            if (r is Resource.Success) {
                tokenStore.saveUser(r.data)     // the drawer and the default prices read it from here
                _user.value = r.data
            }
            _save.value = r
        }
    }

    fun clearSave() {
        if (_save.value !is Resource.Loading) _save.value = null
    }
}