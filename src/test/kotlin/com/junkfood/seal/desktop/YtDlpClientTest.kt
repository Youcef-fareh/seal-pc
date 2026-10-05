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
}
