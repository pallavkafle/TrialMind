package com.example.trailmind.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.trailmind.ui.theme.Pal

// the heart that saves or un-saves a place; tint is the colour of the empty heart
@Composable
fun FavoriteHeart(name: String, modifier: Modifier = Modifier, tint: Color? = null) {
    val vm = LocalFavorites.current
    val list by vm.list.collectAsState()
    LaunchedEffect(Unit) { vm.ensureLoaded() }
    val on = name in list
    IconButton(onClick = { vm.toggle(name) }, modifier = modifier.size(40.dp)) {
        Icon(
            imageVector = if (on) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (on) "Remove from saved places" else "Save this place",
            tint = if (on) Color(0xFFE53935) else (tint ?: Pal.textMuted),
            modifier = Modifier.size(22.dp)
        )
    }
}

// shows a short message when a heart could not be saved (for example no internet)
@Composable
fun FavoritesToaster() {
    val vm = LocalFavorites.current
    val message by vm.message.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearMessage()
        }
    }
}