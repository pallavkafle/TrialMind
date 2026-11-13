package com.example.trailmind

import com.example.trailmind.ui.components.OfflineOverlay
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.trailmind.ui.theme.TrailMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // white status bar icons (the blue top bars); Home asks for dark icons itself
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            // dark navigation icons: the bottom of every screen is light
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            TrailMindTheme {
                OfflineOverlay { AppNavigator() }
            }
        }
    }
}