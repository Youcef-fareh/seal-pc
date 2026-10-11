package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class DownloadHistoryEntry(
    val id: String,
    val url: String,
    val title: String,
    val sourcePlatform: String? = null,
    val outputDirectory: String,
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
    val status: QueueStatus,
    val message: String,
    val completedAtMillis: Long,
) {
    fun toQueueItem(): DownloadQueueItem =
        DownloadQueueItem(
            id = id,
            url = url,
            title = title,
            sourcePlatform = sourcePlatform,
            outputDirectory = File(outputDirectory),
            maxHeight = maxHeight,
            audioFormat = audioFormat,
            audioQuality = audioQuality,
            qualityLabel = qualityLabel,
            estimatedSizeBytes = estimatedSizeBytes,
            formatSelector = formatSelector,
            outputTemplate = outputTemplate,
            rateLimit = rateLimit,
            proxy = proxy,
            writeThumbnail = writeThumbnail,
            embedThumbnail = embedThumbnail,
            embedMetadata = embedMetadata,
            embedChapters = embedChapters,
            aria2cExecutable = aria2cExecutable,
            cookiesFile = cookiesFile,
            sponsorBlockCategories = sponsorBlockCategories,
            customArguments = customArguments,
            subtitleSource = subtitleSource,
            subtitleLanguages = subtitleLanguages,
            embedSubtitles = embedSubtitles,
            subtitleFormat = subtitleFormat,
            status = status,
            progress = if (status == QueueStatus.COMPLETE) 1f else 0f,
            message = message,
        )
}

internal fun DownloadQueueItem.toHistoryEntry(
    terminalStatus: QueueStatus,
    terminalMessage: String,
    completedAtMillis: Long = System.currentTimeMillis(),
): DownloadHistoryEntry {
    require(terminalStatus in setOf(QueueStatus.COMPLETE, QueueStatus.FAILED, QueueStatus.CANCELLED))
    return DownloadHistoryEntry(
        id = id,
        url = url,
        title = title,
        sourcePlatform = sourcePlatform,
        outputDirectory = outputDirectory.absolutePath,
        maxHeight = maxHeight,
        audioFormat = audioFormat,
        audioQuality = audioQuality,
        qualityLabel = qualityLabel,
        estimatedSizeBytes = estimatedSizeBytes,
        formatSelector = formatSelector,
        outputTemplate = outputTemplate,
        rateLimit = rateLimit,
        proxy = proxy,
        writeThumbnail = writeThumbnail,
        embedThumbnail = embedThumbnail,
        embedMetadata = embedMetadata,
        embedChapters = embedChapters,
        aria2cExecutable = aria2cExecutable,
        cookiesFile = cookiesFile,
        sponsorBlockCategories = sponsorBlockCategories,
        customArguments = customArguments,
        subtitleSource = subtitleSource,
        subtitleLanguages = subtitleLanguages,
        embedSubtitles = embedSubtitles,
        subtitleFormat = subtitleFormat,
        status = terminalStatus,
        message = terminalMessage,
        completedAtMillis = completedAtMillis,
    )
}

internal class DownloadHistoryStore(private val file: File = File(appDataDirectory(), "download-history.json")) {
    private val json = Json { prettyPrint = true }

    @Synchronized
    fun load(): List<DownloadHistoryEntry> {
        if (!file.exists()) return emptyList()
        val entries = json.decodeFromString<List<DownloadHistoryEntry>>(file.readText())
        require(entries.all { it.status in TERMINAL_STATUSES }) {
            "Download history contains an unfinished queue item."
        }
        return entries
    }

    @Synchronized
    fun save(entries: List<DownloadHistoryEntry>) {
        val directory = checkNotNull(file.absoluteFile.parentFile)
        check(directory.mkdirs() || directory.isDirectory) {
            "Could not create the download history folder."
        }
        val temporaryFile = Files.createTempFile(directory.toPath(), "download-history-", ".tmp")
        try {
            Files.writeString(temporaryFile, json.encodeToString(entries))
            try {
                Files.move(
                    temporaryFile,
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporaryFile, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }

    @Synchronized
    fun append(entry: DownloadHistoryEntry): List<DownloadHistoryEntry> {
        val updated = (listOf(entry) + load()).take(MAX_ENTRIES)
        save(updated)
        return updated
    }

    private companion object {
        val TERMINAL_STATUSES = setOf(QueueStatus.COMPLETE, QueueStatus.FAILED, QueueStatus.CANCELLED)
        const val MAX_ENTRIES = 500
    }
}

internal object DiagnosticLog {
    val file: File by lazy { File(File(appDataDirectory(), "logs"), "seal-desktop.log") }
    private const val MAX_LOG_BYTES = 2L * 1024 * 1024

    @Synchronized
    fun append(event: String, details: String? = null) {
        val parent = checkNotNull(file.absoluteFile.parentFile)
        check(parent.mkdirs() || parent.isDirectory) { "Could not create the diagnostics log folder." }
        val text = buildString {
            append(Instant.now())
            append(" [")
            append(event.replace('\n', ' '))
            append(']')
            if (!details.isNullOrBlank()) {
                append(' ')
                append(details.trimEnd())
            }
            append(System.lineSeparator())
        }
        if (file.exists() && file.length() + text.toByteArray(Charsets.UTF_8).size > MAX_LOG_BYTES) {
            Files.move(
                file.toPath(),
                File(parent, "seal-desktop.log.1").toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        Files.writeString(
            file.toPath(),
            text,
            Charsets.UTF_8,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND,
        )
    }
}

internal fun appDataDirectory(): File {
    val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
    return File(localAppData ?: File(System.getProperty("user.home"), "AppData/Local").absolutePath, "SealDesktop")
}
