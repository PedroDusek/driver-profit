package com.driverprofit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.driverprofit.core.navigation.DriverProfitNavHost
import com.driverprofit.core.ui.theme.DriverProfitTheme

/**
 * Única Activity do aplicativo (PRD §3). Toda navegação acontece dentro do
 * Compose, via [DriverProfitNavHost].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DriverProfitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DriverProfitNavHost()
                }
            }
        }
    }
}
