package com.ytdownloader.domain.engine

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.ytdownloader.data.model.DownloadStatus
import com.ytdownloader.data.model.DownloadTask
import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class DownloadEngine(
    private val downloadDir: File
) {
    private val _downloadState = MutableStateFlow<DownloadTask?>(null)
    val downloadState: StateFlow<DownloadTask?> = _downloadState

    private var currentProcessId: String? = null

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
        currentProcessId = "yt_download_${videoInfo.id}"

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

            _downloadState.value = task.copy(
                status = DownloadStatus.DOWNLOADING_VIDEO,
                overallProgress = 0.1f
            )

            withContext(Dispatchers.IO) {
                YoutubeDL.getInstance().execute(request, currentProcessId) { progress, _, _ ->
                    _downloadState.value = _downloadState.value?.copy(
                        status = DownloadStatus.DOWNLOADING_VIDEO,
                        overallProgress = 0.1f + (progress / 100f) * 0.9f,
                        videoProgress = progress / 100f
                    )
                }
            }

            if (outputFile.exists() && outputFile.length() > 0) {
                _downloadState.value = _downloadState.value?.copy(
                    status = DownloadStatus.COMPLETED,
                    overallProgress = 1f,
                    outputPath = outputFile.absolutePath
                )
            } else {
                throw RuntimeException("Download failed - output file not created")
            }

        } catch (e: Exception) {
            outputFile.delete()
            _downloadState.value = _downloadState.value?.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message ?: "Download failed"
            )
        }
    }

    fun cancelDownload() {
        currentProcessId?.let { pid ->
            try {
                YoutubeDL.getInstance().destroyProcessById(pid)
            } catch (_: Exception) {}
        }
        currentProcessId = null
        _downloadState.value = null
    }
}
