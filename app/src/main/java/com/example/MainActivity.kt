package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.LogoDatabase
import com.example.data.LogoRepository
import com.example.viewmodel.LogoViewModel
import com.example.viewmodel.AppScreen
import com.example.ui.MainDashboardScreen
import com.example.ui.LogoEditorScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LogoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository layers
        val database = LogoDatabase.getDatabase(applicationContext)
        val repository = LogoRepository(database.logoDao())
        
        // 2. Instantiate ViewModel using simple Factory pattern
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LogoViewModel(repository) as T
            }
        })[LogoViewModel::class.java]

        // 3. Configure back gesture handler for editor screens
        onBackPressedDispatcher.addCallback(this) {
            val current = viewModel.currentScreen.value
            if (current is AppScreen.Editor) {
                viewModel.navigateToDashboard()
            } else {
                finish()
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val screenState by viewModel.currentScreen.collectAsState()

                    // Cross-fade animations for fluent navigation
                    when (val screen = screenState) {
                        is AppScreen.Dashboard -> {
                            MainDashboardScreen(
                                viewModel = viewModel,
                                onNavigateToEditor = { id -> viewModel.loadLogo(id) }
                            )
                        }
                        is AppScreen.Editor -> {
                            LogoEditorScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateToDashboard() }
                            )
                        }
                    }
                }
            }
        }
    }
}
