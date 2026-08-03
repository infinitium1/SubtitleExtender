package com.subtitleextender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.subtitleextender.ui.main.MainScreen
import com.subtitleextender.ui.main.MainViewModel
import com.subtitleextender.ui.theme.SubtitleExtenderTheme

/**
 * The application's single entry point. Hosts the Compose UI tree and wires
 * up [MainViewModel] via its manual factory - see
 * [MainViewModel.Companion.provideFactory].
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubtitleExtenderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
