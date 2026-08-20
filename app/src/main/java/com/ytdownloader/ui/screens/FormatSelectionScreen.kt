package com.ytdownloader.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.ytdownloader.ui.components.FormatCard
import com.ytdownloader.ui.components.VideoInfoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionScreen(
    videoInfo: VideoInfo,
    videoFormats: List<FormatOption>,
    audioFormats: List<FormatOption>,
    onBack: () -> Unit,
    onDownload: (FormatOption, FormatOption) -> Unit
) {
    var selectedVideoFormat by remember { mutableStateOf<FormatOption?>(null) }
    var selectedAudioFormat by remember { mutableStateOf<FormatOption?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    val canDownload = selectedVideoFormat != null && selectedAudioFormat != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Quality") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AnimatedVisibility(
                        visible = !canDownload,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "Select at least one video and one audio quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = { showDownloadDialog = true },
                        enabled = canDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                VideoInfoCard(videoInfo = videoInfo)
            }

            item {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(videoFormats) { format ->
                FormatCard(
                    format = format,
                    isSelected = format == selectedVideoFormat,
                    onClick = { selectedVideoFormat = format }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                Text(
                    text = "Audio Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(audioFormats) { format ->
                FormatCard(
                    format = format,
                    isSelected = format == selectedAudioFormat,
                    onClick = { selectedAudioFormat = format }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showDownloadDialog && canDownload) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("Confirm Download") },
            text = {
                Column {
                    Text("Video: ${selectedVideoFormat?.label}")
                    Text("Audio: ${selectedAudioFormat?.label}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The video and audio will be downloaded separately and merged into a single file.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        selectedVideoFormat?.let { video ->
                            selectedAudioFormat?.let { audio ->
                                onDownload(video, audio)
                            }
                        }
                    }
                ) {
                    Text("Start Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
