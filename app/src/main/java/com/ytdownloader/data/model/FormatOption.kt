package com.ytdownloader.data.model

data class FormatOption(
    val formatId: String,
    val extension: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val vcodec: String,
    val acodec: String,
    val filesize: Long,
    val tbr: Double,
    val abr: Double,
    val vbr: Double,
    val formatNote: String,
    val audioOnly: Boolean
) {
    val label: String
        get() = if (audioOnly) {
            val bitrate = if (abr > 0) "${abr.toInt()}kbps" else ""
            val codec = acodec.split(".").firstOrNull() ?: acodec
            "$bitrate $codec".trim()
        } else {
            val res = "${height}p"
            val fpsStr = if (fps > 30) " ${fps}fps" else ""
            val codec = vcodec.split(".").firstOrNull() ?: vcodec
            "$res$fpsStr $codec".trim()
        }

    val resolutionLabel: String
        get() = if (audioOnly) "Audio" else "${height}p"

    val codecLabel: String
        get() = if (audioOnly) {
            acodec.split(".").firstOrNull() ?: acodec
        } else {
            vcodec.split(".").firstOrNull() ?: vcodec
        }

    val formattedSize: String
        get() = when {
            filesize <= 0 -> "Unknown"
            filesize < 1_024 * 1_024 -> "${filesize / 1_024} KB"
            filesize < 1_024 * 1_024 * 1_024 -> "%.1f MB".format(filesize / (1_024.0 * 1_024))
            else -> "%.2f GB".format(filesize / (1_024.0 * 1_024 * 1_024))
        }

    val qualityDescription: String
        get() = when {
            audioOnly -> {
                val bitrate = if (abr > 0) "${abr.toInt()} kbps" else "Unknown bitrate"
                val codec = acodec.split(".").firstOrNull() ?: acodec
                "$bitrate - $codec"
            }
            height >= 2160 -> "4K Ultra HD"
            height >= 1440 -> "2K QHD"
            height >= 1080 -> "Full HD"
            height >= 720 -> "HD"
            height >= 480 -> "SD"
            height >= 360 -> "Low"
            else -> "Very Low"
        }

    fun toFormatSpec(): String = formatId
}
