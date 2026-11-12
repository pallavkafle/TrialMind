package com.example.trailmind.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// the sun/moon button on the Home header
@Composable
fun ThemeToggleButton(modifier: Modifier = Modifier) {
    val dark = isAppDark()
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Pal.blueFaint)
            .clickable { ThemePrefs.setDark(context, !dark) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (dark) "Switch to light mode" else "Switch to dark mode",
            tint = Pal.blue,
            modifier = Modifier.size(22.dp)
        )
    }
}