package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class YtDlpClientTest {
    @Test
    fun acceptsYouTubeVideoHosts() {
        requireYouTubeUrl("https://www.youtube.com/watch?v=abc123")
        requireYouTubeUrl("https://youtu.be/abc123")
    }

    @Test
    fun rejectsNonYouTubeHostsAndInvalidSchemes() {
        assertFailsWith<IllegalArgumentException> {
            requireYouTubeUrl("https://youtube.com.example.org/watch?v=abc123")
        }
        assertFailsWith<IllegalArgumentException> {
            requireYouTubeUrl("file://youtube.com/video")
        }
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
}
