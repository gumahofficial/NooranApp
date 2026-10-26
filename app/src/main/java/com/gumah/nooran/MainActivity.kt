package com.gumah.nooran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.gumah.nooran.ai.NooranAiService
import com.gumah.nooran.data.NooranRepository
import com.gumah.nooran.ui.MainApp
import com.gumah.nooran.ui.theme.NooranTheme
import com.gumah.nooran.util.configureAppCheck
import com.gumah.nooran.viewmodel.NooranViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check initialization with launch intent for Firebase AI Logic
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val repository = NooranRepository(applicationContext)
        val aiService = NooranAiService()
        val viewModel = NooranViewModel(repository, aiService)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            NooranTheme(darkTheme = isDarkMode) {
                MainApp(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = it }
                )
            }
        }
    }
}
