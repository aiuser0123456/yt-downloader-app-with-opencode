package com.ytdownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ytdownloader.ui.screens.*
import com.ytdownloader.ui.theme.YTDownloaderTheme
import com.ytdownloader.viewmodel.DownloadViewModel
import com.ytdownloader.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = when {
            intent?.action == Intent.ACTION_SEND ->
                intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        setContent {
            YTDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YTDownloaderNavHost(initialUrl = sharedUrl)
                }
            }
        }
    }
}

@Composable
fun YTDownloaderNavHost(initialUrl: String? = null) {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val downloadViewModel: DownloadViewModel = viewModel()

    LaunchedEffect(initialUrl) {
        if (initialUrl != null) {
            homeViewModel.updateUrl(initialUrl)
            homeViewModel.fetchVideo()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val uiState by homeViewModel.uiState.collectAsState()
            val url by homeViewModel.url.collectAsState()

            HomeScreen(
                uiState = uiState,
                url = url,
                onUrlChange = { homeViewModel.updateUrl(it) },
                onFetch = { homeViewModel.fetchVideo() },
                onNavigateToFormats = {
                    navController.navigate("formats")
                },
                onNavigateToDiagnostics = {
                    navController.navigate("diagnostics")
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                }
            )
        }

        composable("formats") {
            val uiState = homeViewModel.uiState.collectAsState().value

            when (val state = uiState) {
                is HomeUiState.Success -> {
                    FormatSelectionScreen(
                        videoInfo = state.videoInfo,
                        videoFormats = state.videoFormats,
                        audioFormats = state.audioFormats,
                        onBack = {
                            homeViewModel.reset()
                            navController.popBackStack()
                        },
                        onDownload = { videoFormat, audioFormat ->
                            downloadViewModel.startDownload(
                                videoInfo = state.videoInfo,
                                videoFormat = videoFormat,
                                audioFormat = audioFormat
                            )
                            navController.navigate("download")
                        }
                    )
                }
                else -> {
                    navController.popBackStack()
                }
            }
        }

        composable("download") {
            val downloadState by downloadViewModel.downloadState.collectAsState()

            DownloadProgressScreen(
                downloadTask = downloadState,
                onBack = {
                    downloadViewModel.cancelDownload()
                    navController.popBackStack()
                },
                onDone = {
                    downloadViewModel.resetDownload()
                    homeViewModel.reset()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("history") {
            val history by downloadViewModel.history.collectAsState()

            LaunchedEffect(Unit) {
                downloadViewModel.loadHistory()
            }

            DownloadHistoryScreen(
                history = history,
                onDeleteItem = { downloadViewModel.deleteHistoryItem(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable("diagnostics") {
            DiagnosticScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
