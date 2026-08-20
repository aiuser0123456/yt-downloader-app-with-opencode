package com.ytdownloader.data.model

data class DownloadHistory(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val filePath: String,
    val fileSize: Long,
    val downloadDate: Long,
    val videoQuality: String,
    val audioQuality: String
) {
    val formattedSize: String
        get() = when {
            fileSize < 1_024 * 1_024 -> "${fileSize / 1_024} KB"
            fileSize < 1_024 * 1_024 * 1_024 -> "%.1f MB".format(fileSize / (1_024.0 * 1_024))
            else -> "%.2f GB".format(fileSize / (1_024.0 * 1_024 * 1_024))
        }
}
