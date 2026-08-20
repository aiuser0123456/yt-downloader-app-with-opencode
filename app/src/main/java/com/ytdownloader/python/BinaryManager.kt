package com.ytdownloader.python

import android.content.Context
import java.io.File

object BinaryManager {
    private var initialized = false
    private lateinit var nativeLibDir: String

    fun initialize(context: Context) {
        if (initialized) return

        nativeLibDir = context.applicationInfo.nativeLibraryDir

        initialized = true
    }

    fun getYtDlpPath(): String {
        return File(nativeLibDir, "libytdlp.so").absolutePath
    }

    fun getFfmpegPath(): String {
        return File(nativeLibDir, "libffmpeg.so").absolutePath
    }

    fun isInitialized(): Boolean = initialized

    fun verifyBinaries(): Pair<Boolean, Boolean> {
        val ytdlp = File(getYtDlpPath())
        val ffmpeg = File(getFfmpegPath())
        return Pair(ytdlp.exists(), ffmpeg.exists())
    }
}
