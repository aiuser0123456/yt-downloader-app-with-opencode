package com.ytdownloader.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ytdownloader.data.model.DownloadHistory
import com.ytdownloader.data.model.DownloadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "yt_downloader_prefs"
        private const val KEY_CUSTOM_DIR = "custom_download_dir"
        private const val DEFAULT_DIR_NAME = "yt-downloader"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getDefaultDownloadDir(): File {
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        return File(downloads, DEFAULT_DIR_NAME)
    }

    fun getCustomDownloadDir(): File? {
        val uriString = prefs.getString(KEY_CUSTOM_DIR, null)
        return if (uriString != null) {
            try {
                val uri = Uri.parse(uriString)
                val docFile = android.provider.DocumentsContract
                    .buildDocumentUriTreeId("com.android.externalstorage.documents", uri.lastPathSegment ?: "")
                File(uri.lastPathSegment ?: "")
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun getDownloadDir(): File {
        return getCustomDownloadDir() ?: getDefaultDownloadDir()
    }

    fun setCustomDownloadDir(uri: Uri) {
        prefs.edit().putString(KEY_CUSTOM_DIR, uri.toString()).apply()
    }

    fun clearCustomDownloadDir() {
        prefs.edit().remove(KEY_CUSTOM_DIR).apply()
    }

    suspend fun saveDownloadHistory(task: DownloadTask) = withContext(Dispatchers.IO) {
        val historyFile = File(context.filesDir, "download_history.txt")
        val entry = buildString {
            append(task.id)
            append("|")
            append(task.videoInfo.title)
            append("|")
            append(task.videoInfo.thumbnailUrl)
            append("|")
            append(task.outputPath)
            append("|")
            append(File(task.outputPath).length())
            append("|")
            append(task.timestamp)
            append("|")
            append(task.selectedVideoFormat.label)
            append("|")
            append(task.selectedAudioFormat.label)
            append("\n")
        }
        historyFile.appendText(entry)
    }

    suspend fun getDownloadHistory(): List<DownloadHistory> = withContext(Dispatchers.IO) {
        val historyFile = File(context.filesDir, "download_history.txt")
        if (!historyFile.exists()) return@withContext emptyList()

        historyFile.readLines().mapNotNull { line ->
            try {
                val parts = line.split("|")
                if (parts.size >= 8) {
                    DownloadHistory(
                        id = parts[0],
                        title = parts[1],
                        thumbnailUrl = parts[2],
                        filePath = parts[3],
                        fileSize = parts[4].toLongOrNull() ?: 0L,
                        downloadDate = parts[5].toLongOrNull() ?: 0L,
                        videoQuality = parts[6],
                        audioQuality = parts[7]
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.downloadDate }
    }

    suspend fun deleteHistoryItem(id: String) = withContext(Dispatchers.IO) {
        val historyFile = File(context.filesDir, "download_history.txt")
        if (!historyFile.exists()) return@withContext

        val lines = historyFile.readLines()
        val filtered = lines.filter { !it.startsWith(id) }
        historyFile.writeText(filtered.joinToString("\n"))
    }

    fun getMimeType(format: String): String = when (format.lowercase()) {
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/opus"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        else -> "video/mp4"
    }
}
