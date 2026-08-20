package com.ytdownloader.python

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object BinaryManager {
    private var initialized = false
    private lateinit var binariesDir: File
    private lateinit var nativeLibDir: String

    fun initialize(context: Context) {
        if (initialized) return

        nativeLibDir = context.applicationInfo.nativeLibraryDir
        binariesDir = File(context.filesDir, "binaries")
        binariesDir.mkdirs()

        prepareBinaries(context)

        initialized = true
    }

    private fun prepareBinaries(context: Context) {
        val ytdlpSource = File(nativeLibDir, "libytdlp.so")
        val ffmpegSource = File(nativeLibDir, "libffmpeg.so")

        val ytdlpTarget = File(binariesDir, "yt-dlp")
        val ffmpegTarget = File(binariesDir, "ffmpeg")

        if (ytdlpSource.exists() && !ytdlpTarget.exists()) {
            copyAndMakeExecutable(ytdlpSource, ytdlpTarget)
        }

        if (ffmpegSource.exists() && !ffmpegTarget.exists()) {
            copyAndMakeExecutable(ffmpegSource, ffmpegTarget)
        }
    }

    private fun copyAndMakeExecutable(source: File, target: File) {
        try {
            source.inputStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            target.setExecutable(true, false)
            target.setReadable(true, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getYtDlpPath(): String {
        return File(binariesDir, "yt-dlp").absolutePath
    }

    fun getFfmpegPath(): String {
        return File(binariesDir, "ffmpeg").absolutePath
    }

    fun getBinariesDir(): File = binariesDir

    fun isInitialized(): Boolean = initialized

    fun verifyBinaries(): Pair<Boolean, Boolean> {
        val ytdlp = File(binariesDir, "yt-dlp")
        val ffmpeg = File(binariesDir, "ffmpeg")
        return Pair(ytdlp.exists() && ytdlp.canExecute(), ffmpeg.exists() && ffmpeg.canExecute())
    }
}
