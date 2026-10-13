package com.example.bangtobeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.bangtobeat.theme.BangToBeatTheme
import com.example.bangtobeat.theme.StudioCarbon

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            BangToBeatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioCarbon
                ) {
                    MainNavigation()
                }
            }
        }
    }
}
