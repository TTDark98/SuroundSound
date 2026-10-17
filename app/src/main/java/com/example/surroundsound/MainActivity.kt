package com.example.surroundsound

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.surroundsound.navigation.MainNavigation
import com.example.surroundsound.ui.theme.ThemePreference
import com.example.surroundsound.ui.theme.studioTokens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The studio owns its own theming: MainScreen re-applies these bar styles whenever the
        // mood changes. Starting from the saved mood keeps the first frame from flashing.
        val saved = studioTokens(ThemePreference.load(this))
        val barStyle = if (saved.dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)

        setContent {
            MainNavigation()
        }
    }
}
