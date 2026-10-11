package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StorageManagerTest {
    @Test
    fun scansFolderSizesAndFindsOnlyOldPartialFiles() {
        val root = Files.createTempDirectory("seal-storage").toFile()
        try {
            val regular = File(root, "video.mp4").apply { writeBytes(ByteArray(20)) }
            val oldPartial = File(root, "old.part").apply {
                writeBytes(ByteArray(10))
                setLastModified(1_000L)
            }
            File(root, "recent.part").writeText("keep")
            val result = scanDownloadStorage(listOf(root), nowMillis = 4_000_000_000_000L)

            assertEquals(34L, result.bytes)
            assertEquals(listOf(oldPartial), result.oldPartialFiles)
            assertEquals(regular.length() + oldPartial.length() + File(root, "recent.part").length(), result.bytes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun cleanupDeletesOnlyContainedPartialFiles() {
        val root = Files.createTempDirectory("seal-storage").toFile()
        val outside = Files.createTempDirectory("seal-outside").toFile()
        try {
            val part = File(root, "download.part").apply {
                writeText("partial")
                setLastModified(1_000L)
            }
            assertEquals(1, deleteOldPartialFiles(listOf(part), listOf(root), nowMillis = 4_000_000_000_000L))
            assertEquals(false, part.exists())
            assertFailsWith<IllegalArgumentException> {
                val outsidePart = File(outside, "outside.part").apply {
                    createNewFile()
                    setLastModified(1_000L)
                }
                deleteOldPartialFiles(listOf(outsidePart), listOf(root), nowMillis = 4_000_000_000_000L)
            }
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }
}
