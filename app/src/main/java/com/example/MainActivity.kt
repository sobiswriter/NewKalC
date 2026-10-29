package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.example.ui.FoodCalorieScreen
import com.example.ui.FoodCalorieViewModel
import com.example.ui.theme.FoodCalorieAITheme

class MainActivity : ComponentActivity() {

    private val viewModel: FoodCalorieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
            FoodCalorieAITheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FoodCalorieScreen(viewModel = viewModel)
                }
            }
        }
    }
}
