package com.ytdownloader.data.model

data class DownloadTask(
    val id: String,
    val videoInfo: VideoInfo,
    val selectedVideoFormat: FormatOption,
    val selectedAudioFormat: FormatOption,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val videoProgress: Float = 0f,
    val audioProgress: Float = 0f,
    val overallProgress: Float = 0f,
    val videoSpeed: String = "",
    val audioSpeed: String = "",
    val videoEta: Int = 0,
    val audioEta: Int = 0,
    val outputPath: String = "",
    val finalFileName: String = "",
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val videoProgressPercent: Int get() = (videoProgress * 100).toInt()
    val audioProgressPercent: Int get() = (audioProgress * 100).toInt()
    val overallProgressPercent: Int get() = (overallProgress * 100).toInt()

    val statusText: String
        get() = when (status) {
            DownloadStatus.QUEUED -> "Queued"
            DownloadStatus.DOWNLOADING_VIDEO -> "Downloading video... $videoProgressPercent%"
            DownloadStatus.DOWNLOADING_AUDIO -> "Downloading audio... $audioProgressPercent%"
            DownloadStatus.MERGING -> "Merging video and audio..."
            DownloadStatus.SAVING -> "Saving to downloads..."
            DownloadStatus.COMPLETED -> "Completed"
            DownloadStatus.FAILED -> "Failed: ${errorMessage ?: "Unknown error"}"
        }
}
