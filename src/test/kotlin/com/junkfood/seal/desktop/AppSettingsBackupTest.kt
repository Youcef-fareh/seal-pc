package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppSettingsBackupTest {
    @Test
    fun writesAndReadsSettingsBackup() {
        val directory = Files.createTempDirectory("seal-settings").toFile()
        try {
            val file = File(directory, "settings.json")
            val backup = sampleBackup()
            backup.write(file)

            assertEquals(backup, AppSettingsBackup.read(file))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsUnsupportedOrUnsafeSettings() {
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(outputTemplate = "../%(title)s").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(proxy = "socks5://user:secret@localhost:1080").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(downloadConcurrency = 12).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(sponsorBlockCategories = listOf("--exec")).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(customArguments = "--exec\nwhoami").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(subtitleSource = "unknown").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(subtitleLanguages = "en/ar").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            sampleBackup().copy(customArguments = "--write-auto-subs").validate()
        }
    }

    private fun sampleBackup() =
        AppSettingsBackup(
            language = "en",
            theme = "system",
            executable = "yt-dlp",
            downloadDirectory = "C:\\Downloads",
            audioOnly = false,
            audioFormat = "mp3",
            audioQuality = "Best",
            outputTemplate = "%(title)s [%(id)s].%(ext)s",
            downloadConcurrency = 2,
            rateLimit = "2M",
            proxy = "",
            writeThumbnail = true,
            embedThumbnail = false,
            embedMetadata = true,
            embedChapters = true,
            clipboardMonitoring = false,
            aria2cExecutable = "C:\\tools\\aria2c.exe",
            sponsorBlockCategories = listOf("sponsor", "intro"),
            customArguments = "--format\nbestvideo+bestaudio/best",
            subtitleSource = "auto",
            subtitleLanguages = "ar,fr",
            embedSubtitles = true,
            subtitleFormat = "vtt",
        )
}
