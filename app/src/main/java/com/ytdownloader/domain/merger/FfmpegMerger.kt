package com.ytdownloader.domain.merger

import com.ytdownloader.python.BinaryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object FfmpegMerger {

    suspend fun merge(
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists() || !audioFile.exists()) {
                return@withContext false
            }

            val command = listOf(
                BinaryManager.getFfmpegPath(),
                "-i", videoFile.absolutePath,
                "-i", audioFile.absolutePath,
                "-c:v", "copy",
                "-c:a", "copy",
                "-map", "0:v:0",
                "-map", "1:a:0",
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

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getMediaInfo(file: File): MediaInfo = withContext(Dispatchers.IO) {
        val command = listOf(
            BinaryManager.getFfmpegPath(),
            "-i", file.absolutePath,
            "-f", "null",
            "-"
        )

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.errorStream))
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        process.waitFor()

        parseMediaInfo(output.toString())
    }

    private fun parseMediaInfo(output: String): MediaInfo {
        val durationRegex = Regex("""Duration:\s+(\d{2}):(\d{2}):(\d{2})\.(\d{2})""")
        val durationMatch = durationRegex.find(output)

        val durationMs = if (durationMatch != null) {
            val (h, m, s, cs) = durationMatch.destructured
            (h.toLong() * 3600 + m.toLong() * 60 + s.toLong()) * 1000 + cs.toLong() * 10
        } else {
            0L
        }

        val videoCodecRegex = Regex("""Video:\s+(\w+)""")
        val videoCodecMatch = videoCodecRegex.find(output)
        val videoCodec = videoCodecMatch?.groupValues?.get(1) ?: "unknown"

        val audioCodecRegex = Regex("""Audio:\s+(\w+)""")
        val audioCodecMatch = audioCodecRegex.find(output)
        val audioCodec = audioCodecMatch?.groupValues?.get(1) ?: "unknown"

        return MediaInfo(
            durationMs = durationMs,
            videoCodec = videoCodec,
            audioCodec = audioCodec
        )
    }

    data class MediaInfo(
        val durationMs: Long,
        val videoCodec: String,
        val audioCodec: String
    ) {
        val formattedDuration: String
            get() {
                val totalSeconds = durationMs / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                return if (hours > 0) {
                    "%d:%02d:%02d".format(hours, minutes, seconds)
                } else {
                    "%d:%02d".format(minutes, seconds)
                }
            }
    }
}
