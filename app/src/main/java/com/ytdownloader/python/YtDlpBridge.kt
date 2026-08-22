package com.ytdownloader.python

import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YtDlpBridge {

    suspend fun fetchVideoInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        try {
            val streamInfo = YoutubeDL.getInstance().getInfo(url)

            VideoInfo(
                url = url,
                id = streamInfo.id ?: "",
                title = streamInfo.title ?: "",
                description = streamInfo.description ?: "",
                thumbnailUrl = streamInfo.thumbnail ?: "",
                durationSeconds = streamInfo.duration.toInt(),
                viewCount = streamInfo.viewCount,
                likeCount = streamInfo.likeCount,
                uploadDate = streamInfo.uploadDate ?: "",
                uploader = streamInfo.uploader ?: streamInfo.channel ?: "",
                channel = streamInfo.channel ?: streamInfo.uploader ?: ""
            )
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun fetchFormats(url: String): Pair<List<FormatOption>, List<FormatOption>> =
        withContext(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest(url)
                request.addOption("-j")
                request.addOption("--no-playlist")

                val result = YoutubeDL.getInstance().execute(request)
                val json = result.out

                parseFormats(json)
            } catch (e: Exception) {
                throw e
            }
        }

    suspend fun testBinary(): String = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            request.addOption("--dump-json")
            request.addOption("--no-playlist")
            request.addOption("--skip-download")

            YoutubeDL.getInstance().execute(request)
            "OK - library initialized"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun parseFormats(json: String): Pair<List<FormatOption>, List<FormatOption>> {
        val obj = org.json.JSONObject(json)
        val formatsArray = obj.getJSONArray("formats")

        val videoFormats = mutableListOf<FormatOption>()
        val audioFormats = mutableListOf<FormatOption>()

        for (i in 0 until formatsArray.length()) {
            val f = formatsArray.getJSONObject(i)
            val hasVideo = f.optString("vcodec", "none") != "none"
            val hasAudio = f.optString("acodec", "none") != "none"
            val filesize = f.optLong("filesize", f.optLong("filesize_approx", 0))

            if (!hasVideo && !hasAudio) continue

            val option = FormatOption(
                formatId = f.optString("format_id", ""),
                extension = f.optString("ext", ""),
                width = f.optInt("width", 0),
                height = f.optInt("height", 0),
                fps = f.optInt("fps", 0),
                vcodec = f.optString("vcodec", ""),
                acodec = f.optString("acodec", ""),
                filesize = filesize,
                tbr = f.optDouble("tbr", 0.0),
                abr = f.optDouble("abr", 0.0),
                vbr = f.optDouble("vbr", 0.0),
                formatNote = f.optString("format_note", ""),
                audioOnly = !hasVideo && hasAudio
            )

            if (option.audioOnly) {
                audioFormats.add(option)
            } else if (hasVideo && option.height > 0) {
                videoFormats.add(option)
            }
        }

        val sortedVideo = videoFormats
            .sortedByDescending { it.height }
            .distinctBy { it.height }
            .take(8)

        val sortedAudio = audioFormats
            .filter { it.abr > 0 }
            .sortedByDescending { it.abr }
            .distinctBy { it.abr }
            .take(6)

        return sortedVideo to sortedAudio
    }
}
