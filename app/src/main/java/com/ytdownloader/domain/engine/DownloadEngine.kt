package com.ytdownloader.domain.engine

import com.ytdownloader.data.model.DownloadStatus
import com.ytdownloader.data.model.DownloadTask
import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.ytdownloader.python.BinaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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

        val videoFile = File(downloadDir, "${videoInfo.id}_video.${videoFormat.extension}")
        val audioFile = File(downloadDir, "${videoInfo.id}_audio.${audioFormat.extension}")
        val outputFile = File(downloadDir, outputFileName)

        try {
            _downloadState.value = task.copy(status = DownloadStatus.DOWNLOADING_VIDEO)

            coroutineScope {
                val videoJob = async(Dispatchers.IO) {
                    downloadStream(
                        url = videoInfo.url,
                        formatSpec = videoFormat.formatId,
                        outputFile = videoFile,
                        isVideo = true
                    )
                }

                val audioJob = async(Dispatchers.IO) {
                    downloadStream(
                        url = videoInfo.url,
                        formatSpec = audioFormat.formatId,
                        outputFile = audioFile,
                        isVideo = false
                    )
                }

                videoJob.await()
                audioJob.await()
            }

            _downloadState.value = _downloadState.value?.copy(
                status = DownloadStatus.MERGING,
                videoProgress = 1f,
                audioProgress = 1f,
                overallProgress = 0.9f
            )

            val mergeSuccess = mergeFiles(videoFile, audioFile, outputFile)

            if (!mergeSuccess) {
                throw RuntimeException("FFmpeg merge failed")
            }

            videoFile.delete()
            audioFile.delete()

            _downloadState.value = _downloadState.value?.copy(
                status = DownloadStatus.COMPLETED,
                overallProgress = 1f,
                outputPath = outputFile.absolutePath
            )

        } catch (e: Exception) {
            videoFile.delete()
            audioFile.delete()
            _downloadState.value = _downloadState.value?.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message ?: "Download failed"
            )
        }
    }

    private fun ensureExecutePermission(path: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "755", path)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun downloadStream(
        url: String,
        formatSpec: String,
        outputFile: File,
        isVideo: Boolean
    ) {
        val ytdlpPath = BinaryManager.getYtDlpPath()

        ensureExecutePermission(ytdlpPath)

        val command = listOf(
            ytdlpPath,
            "-f", formatSpec,
            "--no-playlist",
            "--no-warnings",
            "--no-check-certificates",
            "-o", outputFile.absolutePath,
            url
        )

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        currentProcess = process

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            line?.let { parseProgress(it, isVideo) }
        }

        val exitCode = process.waitFor()
        currentProcess = null

        if (exitCode != 0 && !outputFile.exists()) {
            throw RuntimeException("Download failed with exit code $exitCode")
        }
    }

    private fun parseProgress(line: String, isVideo: Boolean) {
        val percentRegex = Regex("""(\d+\.?\d*)%""")
        val match = percentRegex.find(line)

        if (match != null) {
            val progress = match.groupValues[1].toFloatOrNull() ?: 0f

            val currentTask = _downloadState.value ?: return

            if (isVideo) {
                _downloadState.value = currentTask.copy(
                    videoProgress = progress / 100f,
                    overallProgress = (progress / 100f) * 0.5f
                )
            } else {
                _downloadState.value = currentTask.copy(
                    audioProgress = progress / 100f,
                    overallProgress = 0.5f + (progress / 100f) * 0.5f
                )
            }
        }

        val speedRegex = Regex("""at\s+(\d+\.?\d*\s*[KMG]iB/s)""")
        val speedMatch = speedRegex.find(line)
        if (speedMatch != null) {
            val speed = speedMatch.groupValues[1]
            val currentTask = _downloadState.value ?: return
            if (isVideo) {
                _downloadState.value = currentTask.copy(videoSpeed = speed)
            } else {
                _downloadState.value = currentTask.copy(audioSpeed = speed)
            }
        }

        val etaRegex = Regex("""ETA\s+(\d+:\d+)""")
        val etaMatch = etaRegex.find(line)
        if (etaMatch != null) {
            val eta = etaMatch.groupValues[1]
            val etaSeconds = parseEtaToSeconds(eta)
            val currentTask = _downloadState.value ?: return
            if (isVideo) {
                _downloadState.value = currentTask.copy(videoEta = etaSeconds)
            } else {
                _downloadState.value = currentTask.copy(audioEta = etaSeconds)
            }
        }
    }

    private fun parseEtaToSeconds(eta: String): Int {
        val parts = eta.split(":")
        return when (parts.size) {
            2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
            else -> 0
        }
    }

    private suspend fun mergeFiles(videoFile: File, audioFile: File, outputFile: File): Boolean =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            val ffmpegPath = BinaryManager.getFfmpegPath()

            if (!File(ffmpegPath).exists()) {
                return@withContext false
            }

            ensureExecutePermission(ffmpegPath)

            val command = listOf(
                ffmpegPath,
                "-i", videoFile.absolutePath,
                "-i", audioFile.absolutePath,
                "-c", "copy",
                "-y",
                outputFile.absolutePath
            )

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            while (reader.readLine() != null) {
            }

            val exitCode = process.waitFor()
            exitCode == 0 && outputFile.exists() && outputFile.length() > 0
        }

    fun cancelDownload() {
        currentProcess?.destroy()
        currentProcess = null
        _downloadState.value = null
    }
}
