package com.example.trailmind.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trailmind.R
import com.example.trailmind.ui.theme.TrailBlue

// Pure picture. AppNavigation decides when to leave (after the login check and a short minimum time).
@Composable
fun SplashScreen() {
    var showText by remember { mutableStateOf(false) }
    val textAlpha by animateFloatAsState(
        targetValue = if (showText) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "splashText"
    )
    LaunchedEffect(Unit) { showText = true }

    Box(
        modifier = Modifier.fillMaxSize().background(TrailBlue),
        contentAlignment = Alignment.Center
    ) {
        // same picture, size and position as the system splash, so the hand-over looks like one screen
        Image(
            painter = painterResource(id = R.drawable.ic_splash_compass),
            contentDescription = "TrailMind logo",
            modifier = Modifier.size(288.dp)
        )
        Column(
            modifier = Modifier.offset(y = 140.dp).alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TrailMind", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}