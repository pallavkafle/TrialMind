package com.example.trailmind.ui.account

import androidx.compose.runtime.compositionLocalOf

// screens call these to open other screens without needing the navigation controller
class AppActions(
    val openPlace: (String) -> Unit,
    val openPlaces: () -> Unit,
    val openProfile: () -> Unit,
    val openSaved: () -> Unit,
    val openSettings: () -> Unit
)

val LocalAppActions = compositionLocalOf { AppActions({}, {}, {}, {}, {}) }