package com.junkfood.seal.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
