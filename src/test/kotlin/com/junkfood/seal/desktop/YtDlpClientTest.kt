package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class YtDlpClientTest {
    @Test
    fun acceptsAnyValidHttpOrHttpsHostForYtDlp() {
        requireHttpUrl("https://www.youtube.com/watch?v=abc123")
        requireHttpUrl("https://vimeo.com/123456")
        requireHttpUrl("http://media.example.org/video?id=1")
        requireHttpUrl("HTTP://MEDIA.EXAMPLE.ORG/video")
        assertEquals(false, isValidHttpUrl("file://media.example.org/video"))
        assertEquals(false, isValidHttpUrl("https:///missing-host"))
        assertEquals(false, isValidHttpUrl("https://not a url"))
        assertFailsWith<IllegalArgumentException> { requireHttpUrl("ftp://media.example.org/video") }
    }

    @Test
    fun mapsSubtitleOptionsForSourcesLanguagesFormatsAndMediaTypes() {
        assertEquals(
            listOf("--write-auto-subs", "--sub-langs", "ar,fr", "--embed-subs", "--convert-subs", "srt"),
            subtitleArguments("auto", "ar, fr", embed = true, format = "srt", audioOnly = false),
        )
        assertEquals(
            listOf("--write-subs", "--sub-langs", "en", "--convert-subs", "vtt"),
            subtitleArguments("manual", "en", embed = false, format = "vtt", audioOnly = false),
        )
        assertEquals(
            listOf("--write-subs", "--write-auto-subs", "--sub-langs", "all", "--convert-subs", "vtt"),
            subtitleArguments("both", "all", embed = true, format = "vtt", audioOnly = true),
        )
        assertEquals(emptyList(), subtitleArguments("off", "invalid value", embed = true, format = "bad", audioOnly = false))
        assertFailsWith<IllegalArgumentException> {
            subtitleArguments("auto", "all,en", embed = false, format = "srt", audioOnly = false)
        }
        assertFailsWith<IllegalArgumentException> {
            subtitleArguments("auto", "en/ar", embed = false, format = "srt", audioOnly = false)
        }
    }

    @Test
    fun subtitleOnlyDownloadsBothCaptionSourcesWithoutDownloadingMedia() {
        assertEquals(
            listOf(
                "--skip-download",
                "--write-subs",
                "--write-auto-subs",
                "--sub-langs",
                "ar,fr",
                "--convert-subs",
                "vtt",
                "--sleep-requests",
                "1",
                "--sleep-subtitles",
                "5",
                "--retry-sleep",
                "http:exp=2:60",
            ),
            subtitleOnlyDownloadArguments(source = "both", languages = "ar, fr", format = "vtt"),
        )
    }

    @Test
    fun subtitleOnlyHonorsTheChosenCaptionSource() {
        assertEquals(
            listOf(
                "--skip-download",
                "--write-subs",
                "--sub-langs",
                "en",
                "--convert-subs",
                "srt",
                "--sleep-requests",
                "1",
                "--sleep-subtitles",
                "5",
                "--retry-sleep",
                "http:exp=2:60",
            ),
            subtitleOnlyDownloadArguments(source = "manual", languages = "en", format = "srt"),
        )
        assertEquals(
            listOf(
                "--skip-download",
                "--write-auto-subs",
                "--sub-langs",
                "ar",
                "--convert-subs",
                "srt",
                "--sleep-requests",
                "1",
                "--sleep-subtitles",
                "5",
                "--retry-sleep",
                "http:exp=2:60",
            ),
            subtitleOnlyDownloadArguments(source = "auto", languages = "ar", format = "srt"),
        )
    }

    @Test
    fun skipsPlatformTranslationsWhenAutoTranslatedCaptionsAreDisabled() {
        assertEquals(
            listOf(
                "--write-auto-subs",
                "--extractor-args",
                "youtube:skip=translated_subs",
                "--sub-langs",
                "en",
                "--convert-subs",
                "srt",
            ),
            subtitleArguments(
                source = "auto",
                languages = "en",
                embed = false,
                format = "srt",
                audioOnly = false,
                autoTranslatedSubtitles = false,
            ),
        )
        assertEquals(
            listOf(
                "--skip-download",
                "--write-subs",
                "--write-auto-subs",
                "--extractor-args",
                "youtube:skip=translated_subs",
                "--sub-langs",
                "en",
                "--convert-subs",
                "srt",
                "--sleep-requests",
                "1",
                "--sleep-subtitles",
                "5",
                "--retry-sleep",
                "http:exp=2:60",
            ),
            subtitleOnlyDownloadArguments(
                source = "both",
                languages = "en",
                format = "srt",
                autoTranslatedSubtitles = false,
            ),
        )
        // Uploaded-only downloads never need the translated-captions skip.
        assertEquals(
            listOf("--write-subs", "--sub-langs", "en", "--convert-subs", "srt"),
            subtitleArguments(
                source = "manual",
                languages = "en",
                embed = false,
                format = "srt",
                audioOnly = false,
                autoTranslatedSubtitles = false,
            ),
        )
    }

    @Test
    fun retriesSubtitleFailuresWithoutSuppressingUnrelatedDownloadErrors() {
        val subtitleError = "ERROR: Unable to download video subtitles for 'ar': HTTP Error 429"
        assertEquals(true, isSubtitleDownloadFailure(subtitleError))
        assertEquals(false, isSubtitleDownloadFailure("ERROR: Unable to download video: HTTP Error 429"))
        assertEquals(emptyList(), subtitleThrottleArguments("off"))
        assertEquals(
            listOf(
                "--sleep-requests",
                "1",
                "--sleep-subtitles",
                "5",
                "--retry-sleep",
                "http:exp=2:60",
            ),
            subtitleThrottleArguments("auto"),
        )

        assertEquals(
            listOf("--ignore-config", "--socket-timeout", "30", "https://youtu.be/abc123"),
            withoutSubtitleArguments(
                listOf(
                    "--ignore-config",
                    "--write-auto-subs",
                    "--sub-langs",
                    "ar",
                    "--embed-subs",
                    "--convert-subs",
                    "srt",
                    "--sleep-requests",
                    "1",
                    "--sleep-subtitles",
                    "5",
                    "--retry-sleep",
                    "http:exp=2:60",
                    "--socket-timeout",
                    "30",
                    "https://youtu.be/abc123",
                ),
            ),
        )
    }

    @Test
    fun parsesUrlsFromDroppedTextAndUriLists() {
        assertEquals("https://youtu.be/abc123", parseDroppedUrl("# shared link\r\n https://youtu.be/abc123 \r\n"))
        assertEquals(
            "https://example.org/video",
            parseDroppedUrl("file:///C:/video.mp4\nhttps://example.org/video"),
        )
        assertEquals(null, parseDroppedUrl("# no URL here\nnot a URL"))
    }

    @Test
    fun preventsAdvancedSubtitleOptionsFromConflictingWithDedicatedControls() {
        val args = parseCustomYtDlpArguments("--sub-langs\nen")
        assertFailsWith<IllegalArgumentException> { validateSubtitleCustomArguments("auto", args) }
        validateSubtitleCustomArguments("off", args)
    }

    @Test
    fun explainsUnsupportedExtractorErrorsWithoutHidingBackendDetails() {
        val backendError = "ERROR: Unsupported URL: https://example.org/video"
        val friendly = userFriendlyYtDlpError(backendError)
        assertEquals(true, friendly.startsWith("This URL is invalid or this site is not supported by yt-dlp."))
        assertEquals(true, friendly.contains(backendError))
        assertEquals("ERROR: Network timeout", userFriendlyYtDlpError("ERROR: Network timeout"))
    }

    @Test
    fun videoFormatRequiresVideoCodec() {
        assertEquals(true, VideoFormat(vcodec = "avc1").hasVideo())
        assertEquals(false, VideoFormat(vcodec = "none").hasVideo())
        assertEquals(false, VideoFormat(vcodec = null).hasVideo())
    }

    @Test
    fun comparesReleaseVersionsNumerically() {
        assertEquals(true, isNewerVersion("1.0.9", "1.0.10"))
        assertEquals(true, isNewerVersion("1.0.0", "1.1"))
        assertEquals(false, isNewerVersion("1.0.0", "1.0"))
        assertEquals(false, isNewerVersion("2.0", "1.99.99"))
        assertEquals(false, isNewerVersion("invalid", "1.0.1"))
    }

    @Test
    fun prefersMp4VideoWithM4aAudioAndKeepsHeightLimit() {
        assertEquals(
            "(bestvideo[ext=mp4][height<=720]+bestaudio[ext=m4a])/(bestvideo[height<=720]+bestaudio)/best[ext=mp4][height<=720]/best[height<=720]",
            videoFormatSelector(720),
        )
        assertEquals(
            "(bestvideo[ext=mp4]+bestaudio[ext=m4a])/(bestvideo+bestaudio)/best[ext=mp4]/best",
            videoFormatSelector(null),
        )
    }

    @Test
    fun usesBundledFfmpegWhenAvailable() {
        val resources = Files.createTempDirectory("seal-resources").toFile()
        val ffmpegDirectory = File(resources, "ffmpeg").apply { mkdirs() }
        File(ffmpegDirectory, "ffmpeg.exe").createNewFile()

        assertEquals(
            listOf("--ffmpeg-location", ffmpegDirectory.absolutePath),
            bundledFfmpegLocationArguments(resources.absolutePath),
        )
        assertEquals(emptyList(), bundledFfmpegLocationArguments(null))

        resources.deleteRecursively()
    }

    @Test
    fun prefersBundledYtDlpForDefaultCommandButPreservesCustomExecutable() {
        val resources = Files.createTempDirectory("seal-resources").toFile()
        val bundledYtDlp = File(resources, "yt-dlp.exe").apply { createNewFile() }

        assertEquals(
            bundledYtDlp.absolutePath,
            resolveYtDlpExecutable("yt-dlp", resources.absolutePath),
        )
        assertEquals("C:\\tools\\yt-dlp.exe", resolveYtDlpExecutable("C:\\tools\\yt-dlp.exe", resources.absolutePath))
        assertEquals("yt-dlp", resolveYtDlpExecutable("yt-dlp", null))

        resources.deleteRecursively()
    }

    @Test
    fun usesBundledDenoForYouTubeJavaScriptChallenges() {
        val resources = Files.createTempDirectory("seal-resources").toFile()
        File(resources, "deno.exe").createNewFile()

        assertEquals(
            listOf("--js-runtimes", "deno:${File(resources, "deno.exe").absolutePath}"),
            bundledDenoArguments(resources.absolutePath),
        )
        assertEquals(emptyList(), bundledDenoArguments(null))

        resources.deleteRecursively()
    }

    @Test
    fun estimatesVideoSizeFromSelectedStreamsAndRespectsHeightLimit() {
        val video =
            VideoDetails(
                duration = 120.0,
                formats =
                    listOf(
                        VideoFormat(height = 1080, vcodec = "avc1", ext = "mp4", filesize = 10_000),
                        VideoFormat(height = 720, vcodec = "avc1", ext = "mp4", filesize_approx = 5_000),
                        VideoFormat(vcodec = "none", acodec = "mp4a", ext = "m4a", filesize = 1_000),
                    ),
            )

        assertEquals(11_000L, estimateDownloadSizeBytes(video, null, null, "0"))
        assertEquals(6_000L, estimateDownloadSizeBytes(video, 720, null, "0"))
    }

    @Test
    fun estimatesTargetAudioSizeAndFormatsByteCounts() {
        val video = VideoDetails(duration = 60.0)

        assertEquals(2_400_000L, estimateDownloadSizeBytes(video, null, "mp3", "320K"))
        assertEquals("1.0 MB", formatFileSize(1_048_576))
        assertNull(estimateDownloadSizeBytes(VideoDetails(), null, null, "0"))
        assertNull(estimateDownloadSizeBytes(VideoDetails(duration = Double.NaN), null, "mp3", "320K"))
    }

    @Test
    fun doesNotSubstituteAnotherFormatWhenSelectedFormatSizeIsUnavailable() {
        val video =
            VideoDetails(
                formats =
                    listOf(
                        VideoFormat(height = 720, vcodec = "avc1", ext = "mp4"),
                        VideoFormat(vcodec = "none", acodec = "mp4a", ext = "m4a"),
                        VideoFormat(height = 480, vcodec = "vp9", ext = "webm", filesize = 20_000),
                        VideoFormat(vcodec = "none", acodec = "opus", ext = "webm", filesize = 2_000),
                    ),
            )

        assertNull(estimateDownloadSizeBytes(video, null, null, "0"))
    }

    @Test
    fun supportsExplicitVideoFormatSelectorsSafely() {
        assertEquals(
            "22",
            videoFormatSelector(VideoFormat(format_id = "22", vcodec = "avc1", acodec = "mp4a")),
        )
        assertEquals(
            "137+bestaudio/best",
            videoFormatSelector(VideoFormat(format_id = "137", vcodec = "avc1", acodec = "none")),
        )
        assertNull(videoFormatSelector(VideoFormat(format_id = "137;deliberate", vcodec = "avc1")))
    }

    @Test
    fun validatesOutputTemplatesAndProxyAddresses() {
        assertEquals(
            true,
            isValidOutputTemplate("%(uploader)s/%(upload_date)s - %(title).200B [%(id)s].%(ext)s"),
        )
        assertEquals(false, isValidOutputTemplate("../%(title)s.%(ext)s"))
        assertEquals(false, isValidOutputTemplate("%(unexpected)s.%(ext)s"))
        assertEquals(false, isValidOutputTemplate("%(title)\\%(id)s.%(ext)s"))
        assertEquals(true, isValidProxyUrl("socks5://127.0.0.1:1080"))
        assertEquals(false, isValidProxyUrl("socks5://user:secret@127.0.0.1:1080"))
        assertEquals(false, isValidProxyUrl("file://localhost/path"))
    }

    @Test
    fun recognizesPlaylistUrlsAndBuildsSafeVideoLinks() {
        assertEquals(true, isYouTubePlaylistUrl("https://www.youtube.com/playlist?list=PL123456"))
        assertEquals(true, isYouTubePlaylistUrl("https://www.youtube.com/watch?v=abc123&list=PL123456"))
        assertEquals(true, isYouTubePlaylistUrl("https://youtu.be/abc123?list=PL123456"))
        assertEquals(false, isYouTubePlaylistUrl("https://youtu.be/abc123"))
        assertEquals(true, isYouTubeVideoUrl("https://www.youtube.com/watch?v=abc123"))
        assertEquals(true, isYouTubeVideoUrl("https://www.youtube.com/shorts/abc123"))
        assertEquals(false, isYouTubeVideoUrl("https://www.youtube.com/playlist?list=PL123456"))
        assertEquals("https://www.youtube.com/watch?v=abc123", playlistEntryUrl(PlaylistEntry(id = "abc123")))
        assertEquals(
            "https://www.youtube.com/watch?v=abc123",
            playlistEntryUrl(PlaylistEntry(url = "abc123")),
        )
        assertNull(playlistEntryUrl(PlaylistEntry(id = "bad/id")))
        assertEquals(
            "https://youtu.be/abc123",
            playlistEntryUrl(PlaylistEntry(id = "other123", webpage_url = "https://youtu.be/abc123")),
        )
    }

    @Test
    fun validatesSponsorBlockCategoriesAndCustomArgumentTemplates() {
        assertEquals(true, isValidSponsorBlockCategories(listOf("sponsor", "intro")))
        assertEquals(false, isValidSponsorBlockCategories(listOf("sponsor", "--exec")))
        assertEquals(false, isValidSponsorBlockCategories(listOf("sponsor", "sponsor")))
        assertEquals(
            listOf("--format", "bestvideo+bestaudio/best", "--write-subs"),
            parseCustomYtDlpArguments("--format\nbestvideo+bestaudio/best\n--write-subs"),
        )
        assertEquals(
            listOf("--playlist-items=1-10"),
            parseCustomYtDlpArguments("--playlist-items=1-10"),
        )
        assertFailsWith<IllegalArgumentException> { parseCustomYtDlpArguments("--exec\nwhoami") }
        assertFailsWith<IllegalArgumentException> { parseCustomYtDlpArguments("--format") }
        assertFailsWith<IllegalArgumentException> { parseCustomYtDlpArguments("--format\n--cookies-from-browser") }
    }

    @Test
    fun acceptsOnlyNetscapeCookieFilesAndExistingAria2Executables() {
        val directory = Files.createTempDirectory("seal-download-options").toFile()
        try {
            val cookieFile = File(directory, "cookies.txt").apply {
                writeText("# Netscape HTTP Cookie File\n.youtube.com\tTRUE\t/\tTRUE\t0\tSID\tsecret\n")
            }
            val otherFile = File(directory, "other.txt").apply { writeText("not a cookie file") }
            val aria2c = File(directory, "aria2c.exe").apply { writeText("binary placeholder") }

            assertEquals(true, isNetscapeCookieFile(cookieFile))
            assertEquals(false, isNetscapeCookieFile(otherFile))
            assertEquals(true, isValidAria2cExecutable(aria2c.absolutePath))
            assertEquals(false, isValidAria2cExecutable(File(directory, "missing.exe").absolutePath))
        } finally {
            directory.deleteRecursively()
        }
    }
}
