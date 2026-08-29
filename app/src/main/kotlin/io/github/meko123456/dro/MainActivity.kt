package io.github.meko123456.dro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.meko123456.dro.ui.home.HomeScreen
import io.github.meko123456.dro.ui.theme.DroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DroTheme {
                HomeScreen()
            }
        }
    }
}
