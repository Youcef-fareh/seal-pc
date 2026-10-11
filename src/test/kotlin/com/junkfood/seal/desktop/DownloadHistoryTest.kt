package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DownloadHistoryTest {
    @Test
    fun persistsAndLoadsTerminalHistoryEntries() {
        val directory = Files.createTempDirectory("seal-history").toFile()
        try {
            val store = DownloadHistoryStore(File(directory, "history.json"))
            val entries =
                listOf(
                    DownloadHistoryEntry(
                        id = "item-1",
                        url = "https://youtu.be/video",
                        title = "Example",
                        outputDirectory = directory.absolutePath,
                        maxHeight = 720,
                        audioFormat = null,
                        audioQuality = "0",
                        qualityLabel = "720 p",
                        estimatedSizeBytes = 42_000,
                        status = QueueStatus.FAILED,
                        message = "Network error",
                        completedAtMillis = 1234L,
                        aria2cExecutable = "C:\\tools\\aria2c.exe",
                        cookiesFile = "C:\\private\\cookies.txt",
                        sponsorBlockCategories = listOf("sponsor", "intro"),
                        customArguments = "--write-subs",
                        subtitleSource = "auto",
                        subtitleLanguages = "ar,fr",
                        embedSubtitles = true,
                        subtitleFormat = "vtt",
                    ),
                )

            store.save(entries)

            assertEquals(entries, store.load())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun appendsHistoryWithoutOverwritingEarlierResults() {
        val directory = Files.createTempDirectory("seal-history").toFile()
        try {
            val store = DownloadHistoryStore(File(directory, "history.json"))
            val first = historyEntry("first", QueueStatus.FAILED, 100L)
            val second = historyEntry("second", QueueStatus.COMPLETE, 200L)

            store.append(first)
            store.append(second)

            assertEquals(listOf(second, first), store.load())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun convertsFailedHistoryToAnEquivalentRetryableQueueItem() {
        val entry =
            DownloadHistoryEntry(
                id = "item-1",
                url = "https://youtu.be/video",
                title = "Example",
                outputDirectory = "C:\\Downloads",
                maxHeight = null,
                audioFormat = "mp3",
                audioQuality = "192K",
                qualityLabel = "mp3 · 192 kbps",
                estimatedSizeBytes = null,
                status = QueueStatus.FAILED,
                message = "Network error",
                completedAtMillis = 1234L,
                cookiesFile = "C:\\private\\cookies.txt",
                sponsorBlockCategories = listOf("sponsor"),
                customArguments = "--write-subs",
                subtitleSource = "both",
                subtitleLanguages = "en",
                embedSubtitles = false,
                subtitleFormat = "srt",
            )

        val queueItem = entry.toQueueItem()

        assertEquals("https://youtu.be/video", queueItem.url)
        assertEquals("C:\\Downloads", queueItem.outputDirectory.path)
        assertEquals("mp3", queueItem.audioFormat)
        assertEquals("192K", queueItem.audioQuality)
        assertEquals(QueueStatus.FAILED, queueItem.status)
        assertEquals("C:\\private\\cookies.txt", queueItem.cookiesFile)
        assertEquals(listOf("sponsor"), queueItem.sponsorBlockCategories)
        assertEquals("--write-subs", queueItem.customArguments)
        assertEquals("both", queueItem.subtitleSource)
        assertEquals("en", queueItem.subtitleLanguages)
        assertEquals(false, queueItem.embedSubtitles)
        assertEquals("srt", queueItem.subtitleFormat)
    }

    @Test
    fun rejectsUnfinishedQueueItemsInHistory() {
        val directory = Files.createTempDirectory("seal-history").toFile()
        try {
            val store = DownloadHistoryStore(File(directory, "history.json"))
            store.save(
                listOf(
                    DownloadHistoryEntry(
                        id = "item-1",
                        url = "https://youtu.be/video",
                        title = "Example",
                        outputDirectory = directory.absolutePath,
                        maxHeight = null,
                        audioFormat = null,
                        audioQuality = "0",
                        qualityLabel = "Best",
                        estimatedSizeBytes = null,
                        status = QueueStatus.WAITING,
                        message = "",
                        completedAtMillis = 1234L,
                    ),
                ),
            )

            assertFailsWith<IllegalArgumentException> { store.load() }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun historyEntry(id: String, status: QueueStatus, time: Long) =
        DownloadHistoryEntry(
            id = id,
            url = "https://youtu.be/$id",
            title = id,
            outputDirectory = "C:\\Downloads",
            maxHeight = null,
            audioFormat = null,
            audioQuality = "0",
            qualityLabel = "Best",
            estimatedSizeBytes = null,
            status = status,
            message = "",
            completedAtMillis = time,
        )
}
