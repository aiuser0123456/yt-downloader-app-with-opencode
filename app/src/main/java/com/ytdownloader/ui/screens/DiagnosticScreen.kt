package com.ytdownloader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ytdownloader.python.BinaryManager
import com.ytdownloader.python.YtDlpBridge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var binaryStatus by remember { mutableStateOf("Checking...") }
    var ytdlpVersion by remember { mutableStateOf("") }
    var ffmpegStatus by remember { mutableStateOf("Checking...") }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (ytdlpExists, ffmpegExists) = BinaryManager.verifyBinaries()
        binaryStatus = if (ytdlpExists) "Found" else "Missing"
        ffmpegStatus = if (ffmpegExists) "Found" else "Missing"

        if (ytdlpExists) {
            isTesting = true
            try {
                ytdlpVersion = YtDlpBridge.testBinary()
            } catch (e: Exception) {
                ytdlpVersion = "Error: ${e.message}"
            }
            isTesting = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Binary Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DiagnosticRow(
                        label = "yt-dlp",
                        status = binaryStatus,
                        isOk = binaryStatus == "Found"
                    )

                    if (ytdlpVersion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version: $ytdlpVersion",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticRow(
                        label = "ffmpeg",
                        status = ffmpegStatus,
                        isOk = ffmpegStatus == "Found"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Binary Paths",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "yt-dlp: ${BinaryManager.getYtDlpPath()}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ffmpeg: ${BinaryManager.getFfmpegPath()}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Binaries dir: ${BinaryManager.getBinariesDir()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (binaryStatus != "Found" || ffmpegStatus != "Found") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Missing Binaries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = """
                                Please ensure these files exist in app/src/main/jniLibs/arm64-v8a/:
                                
                                • libytdlp.so (yt-dlp binary)
                                • libffmpeg.so (ffmpeg binary)
                                
                                Download from:
                                • yt-dlp: https://github.com/yt-dlp/yt-dlp/releases
                                • ffmpeg: https://github.com/nicoverbruggen/ffmpeg-binary-android/releases
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(
    label: String,
    status: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
