package com.ytdownloader.data.model

data class VideoInfo(
    val url: String,
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val durationSeconds: Int,
    val viewCount: Long,
    val likeCount: Long,
    val uploadDate: String,
    val uploader: String,
    val channel: String
) {
    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val mins = (durationSeconds % 3600) / 60
            val secs = durationSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, mins, secs)
            } else {
                "%d:%02d".format(mins, secs)
            }
        }

    val formattedViews: String
        get() = when {
            viewCount >= 1_000_000_000 -> "%.1fB".format(viewCount / 1_000_000_000.0)
            viewCount >= 1_000_000 -> "%.1fM".format(viewCount / 1_000_000.0)
            viewCount >= 1_000 -> "%.1fK".format(viewCount / 1_000.0)
            else -> viewCount.toString()
        }

    val formattedLikes: String
        get() = when {
            likeCount >= 1_000_000 -> "%.1fM".format(likeCount / 1_000_000.0)
            likeCount >= 1_000 -> "%.1fK".format(likeCount / 1_000.0)
            else -> likeCount.toString()
        }

    val formattedUploadDate: String
        get() = if (uploadDate.length == 8) {
            "${uploadDate.substring(0, 4)}-${uploadDate.substring(4, 6)}-${uploadDate.substring(6, 8)}"
        } else {
            uploadDate
        }
}
