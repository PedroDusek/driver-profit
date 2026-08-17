package com.driverpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.driverpro.core.navigation.DriverProNavHost
import com.driverpro.core.ui.theme.DriverProTheme

/**
 * Única Activity do aplicativo (PRD §3). Toda navegação acontece dentro do
 * Compose, via [DriverProNavHost].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DriverProTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DriverProNavHost()
                }
            }
        }
    }
}
