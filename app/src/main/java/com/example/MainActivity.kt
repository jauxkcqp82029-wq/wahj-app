package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ThemeMode
import com.example.ui.navigation.WahjNavHost
import com.example.ui.theme.WahjTheme
import com.example.ui.viewmodel.WahjViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as WahjApplication

        setContent {
            val viewModel: WahjViewModel = viewModel(
                factory = WahjViewModel.Factory(applicationContext, app.repository)
            )
            val prefs by viewModel.sessionPrefs.collectAsStateWithLifecycle()
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                WahjTheme(darkTheme = darkTheme) {
                    WahjNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
