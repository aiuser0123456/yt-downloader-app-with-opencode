package com.ytdownloader.python

import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object YtDlpBridge {

    suspend fun fetchVideoInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url)
        request.addOption("--dump-json")
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")

        val result = YoutubeDL.getInstance().execute(request)
        val json = result.out

        parseVideoInfo(json, url)
    }

    suspend fun fetchFormats(url: String): Pair<List<FormatOption>, List<FormatOption>> =
        withContext(Dispatchers.IO) {
            val request = YoutubeDLRequest(url)
            request.addOption("--dump-json")
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")

            val result = YoutubeDL.getInstance().execute(request)
            val json = result.out

            parseFormats(json)
        }

    suspend fun testBinary(): String = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest("--version")
            val result = YoutubeDL.getInstance().execute(request)
            result.out.trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun parseVideoInfo(json: String, url: String): VideoInfo {
        val obj = JSONObject(json)
        return VideoInfo(
            url = url,
            id = obj.optString("id", ""),
            title = obj.optString("title", ""),
            description = obj.optString("description", ""),
            thumbnailUrl = obj.optString("thumbnail", ""),
            durationSeconds = obj.optInt("duration", 0),
            viewCount = obj.optLong("view_count", 0),
            likeCount = obj.optLong("like_count", 0),
            uploadDate = obj.optString("upload_date", ""),
            uploader = obj.optString("uploader", obj.optString("channel", "")),
            channel = obj.optString("channel", obj.optString("uploader", ""))
        )
    }

    private fun parseFormats(json: String): Pair<List<FormatOption>, List<FormatOption>> {
        val obj = JSONObject(json)
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
