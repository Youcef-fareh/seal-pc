package com.junkfood.seal.desktop

import java.io.File
import kotlinx.serialization.Serializable

@Serializable
internal enum class QueueStatus {
    WAITING,
    DOWNLOADING,
    PAUSED,
    COMPLETE,
    FAILED,
    CANCELLED,
}

internal data class DownloadQueueItem(
    val id: String,
    val url: String,
    val title: String,
    val sourcePlatform: String? = null,
    val outputDirectory: File,
    val maxHeight: Int?,
    val audioFormat: String?,
    val audioQuality: String,
    val qualityLabel: String,
    val estimatedSizeBytes: Long?,
    val formatSelector: String? = null,
    val outputTemplate: String = "%(title).200B [%(id)s].%(ext)s",
    val rateLimit: String? = null,
    val proxy: String? = null,
    val writeThumbnail: Boolean = false,
    val embedThumbnail: Boolean = false,
    val embedMetadata: Boolean = false,
    val embedChapters: Boolean = false,
    val aria2cExecutable: String? = null,
    val cookiesFile: String? = null,
    val sponsorBlockCategories: List<String> = emptyList(),
    val customArguments: String = "",
    val subtitleSource: String = "off",
    val subtitleLanguages: String = "en",
    val embedSubtitles: Boolean = false,
    val subtitleFormat: String = "srt",
    val status: QueueStatus = QueueStatus.WAITING,
    val progress: Float = 0f,
    val message: String = "",
)
