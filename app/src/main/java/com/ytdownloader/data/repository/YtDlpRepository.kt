package com.ytdownloader.data.repository

import com.ytdownloader.data.model.FormatOption
import com.ytdownloader.data.model.VideoInfo
import com.ytdownloader.python.YtDlpBridge

class YtDlpRepository {

    suspend fun fetchVideoInfo(url: String): VideoInfo {
        return YtDlpBridge.fetchVideoInfo(url)
    }

    suspend fun fetchFormats(url: String): Pair<List<FormatOption>, List<FormatOption>> {
        return YtDlpBridge.fetchFormats(url)
    }
}
