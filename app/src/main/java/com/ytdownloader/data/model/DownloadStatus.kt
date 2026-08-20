package com.ytdownloader.data.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING_VIDEO,
    DOWNLOADING_AUDIO,
    MERGING,
    SAVING,
    COMPLETED,
    FAILED
}
