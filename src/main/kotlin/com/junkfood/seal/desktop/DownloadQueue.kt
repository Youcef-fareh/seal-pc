package com.junkfood.seal.desktop

import java.io.File

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
    val outputDirectory: File,
    val maxHeight: Int?,
    val audioFormat: String?,
    val audioQuality: String,
    val status: QueueStatus = QueueStatus.WAITING,
    val progress: Float = 0f,
    val message: String = "",
)
