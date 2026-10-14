package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LoginScreen
import com.example.ui.components.PdfPreviewDialog
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DocumentArchiveScreen
import com.example.ui.screens.MainFormScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SpklViewModel
import kotlinx.coroutines.flow.collectLatest

enum class AppScreen {
    MAIN_FORM,
    DOCUMENT_ARCHIVE,
    CLOUD_SYNC,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SpklViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpklAppRoot(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SpklAppRoot(viewModel: SpklViewModel) {
    val context = LocalContext.current
    val isLoginEnabled by viewModel.isLoginEnabled.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentPin by viewModel.userPin.collectAsState()
    val previewFile by viewModel.previewPdfFile.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_FORM) }

    // Collect Toast notifications
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Login Gate
    if (isLoginEnabled && !isLoggedIn) {
        LoginScreen(
            correctPin = currentPin,
            onLoginSuccess = { viewModel.login() }
        )
        return
    }

    // Screen Router
    when (currentScreen) {
        AppScreen.MAIN_FORM -> {
            MainFormScreen(
                viewModel = viewModel,
                onNavigateToArchive = { currentScreen = AppScreen.DOCUMENT_ARCHIVE },
                onNavigateToCloud = { currentScreen = AppScreen.CLOUD_SYNC },
                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
            )
        }

        AppScreen.DOCUMENT_ARCHIVE -> {
            BackHandler { currentScreen = AppScreen.MAIN_FORM }
            DocumentArchiveScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MAIN_FORM }
            )
        }

        AppScreen.CLOUD_SYNC -> {
            BackHandler { currentScreen = AppScreen.MAIN_FORM }
            CloudSyncScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MAIN_FORM }
            )
        }

        AppScreen.SETTINGS -> {
            BackHandler { currentScreen = AppScreen.MAIN_FORM }
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.MAIN_FORM }
            )
        }
    }

    // Document Preview Overlay
    if (previewFile != null) {
        PdfPreviewDialog(
            pdfFile = previewFile!!,
            onDismiss = { viewModel.closePreview() },
            onPrint = {
                viewModel.printDocument(context, previewFile)
            },
            onSaveToDownloads = {
                viewModel.saveToDownloads(context, previewFile)
            },
            onShare = {
                viewModel.shareDocument(context, previewFile)
            },
            onExportCloud = {
                viewModel.shareDocument(context, previewFile)
            }
        )
    }
}
