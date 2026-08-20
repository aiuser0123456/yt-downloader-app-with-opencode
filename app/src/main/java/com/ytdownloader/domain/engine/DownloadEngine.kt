package com.ytdownloader.domain.engine

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLListener
import com.ytdownloader.data.model.DownloadStatus
import com.ytdownloader.data.model.DownloadTask
import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class DownloadEngine(
    private val downloadDir: File
) {
    private val _downloadState = MutableStateFlow<DownloadTask?>(null)
    val downloadState: StateFlow<DownloadTask?> = _downloadState

    private var currentProcess: Process? = null

    suspend fun startDownload(
        videoInfo: VideoInfo,
        videoFormat: FormatOption,
        audioFormat: FormatOption
    ) {
        val safeTitle = videoInfo.title.replace(Regex("[^a-zA-Z0-9 ._-]"), "_")
            .take(100)
        val outputFileName = "$safeTitle.mp4"

        val task = DownloadTask(
            id = videoInfo.id,
            videoInfo = videoInfo,
            selectedVideoFormat = videoFormat,
            selectedAudioFormat = audioFormat,
            status = DownloadStatus.QUEUED,
            finalFileName = outputFileName
        )
        _downloadState.value = task

        downloadDir.mkdirs()

        val outputFile = File(downloadDir, outputFileName)

        try {
            _downloadState.value = task.copy(status = DownloadStatus.DOWNLOADING_VIDEO)

            val formatSpec = "${videoFormat.formatId}+${audioFormat.formatId}/best"

            val request = YoutubeDLRequest(videoInfo.url)
            request.addOption("-f", formatSpec)
            request.addOption("--merge-output-format", "mp4")
            request.addOption("-o", outputFile.absolutePath)
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")
            request.addOption("--newline")

            _downloadState.value = task.copy(status = DownloadStatus.DOWNLOADING_VIDEO)

            val success = executeDownload(request, outputFile)

            if (success) {
                _downloadState.value = _downloadState.value?.copy(
                    status = DownloadStatus.COMPLETED,
                    overallProgress = 1f,
                    outputPath = outputFile.absolutePath
                )
            } else {
                throw RuntimeException("Download failed")
            }

        } catch (e: Exception) {
            outputFile.delete()
            _downloadState.value = _downloadState.value?.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message ?: "Download failed"
            )
        }
    }

    private suspend fun executeDownload(
        request: YoutubeDLRequest,
        outputFile: File
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val listener = object : YoutubeDLListener {
            override fun onProgressUpdate(progress: Float, eta: Int) {
                val currentTask = _downloadState.value ?: return
                _downloadState.value = currentTask.copy(
                    overallProgress = progress,
                    audioEta = eta
                )
            }

            override fun onDebugUpdate(line: String) {
                parseProgress(line)
            }

            override fun onStart() {}

            override fun onEnd() {}
        }

        YoutubeDL.getInstance().enqueue(request, listener)

        continuation.invokeOnCancellation {
            YoutubeDL.getInstance().destroy()
        }
    }

    private fun parseProgress(line: String) {
        val percentRegex = Regex("""(\d+\.?\d*)%""")
        val match = percentRegex.find(line) ?: return

        val progress = match.groupValues[1].toFloatOrNull() ?: return
        val currentTask = _downloadState.value ?: return

        _downloadState.value = currentTask.copy(
            overallProgress = progress / 100f
        )

        val speedRegex = Regex("""at\s+(\d+\.?\d*\s*[KMG]iB/s)""")
        val speedMatch = speedRegex.find(line)
        if (speedMatch != null) {
            _downloadState.value = currentTask.copy(
                videoSpeed = speedMatch.groupValues[1]
            )
        }
    }

    fun cancelDownload() {
        YoutubeDL.getInstance().destroy()
        _downloadState.value = null
    }
}
