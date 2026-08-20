package com.ytdownloader.domain.merger

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

            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "ffmpeg",
                    "-i", videoFile.absolutePath,
                    "-i", audioFile.absolutePath,
                    "-c:v", "copy",
                    "-c:a", "copy",
                    "-map", "0:v:0",
                    "-map", "1:a:0",
                    "-y",
                    outputFile.absolutePath
                )
            )

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
}
