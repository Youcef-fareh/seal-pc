package com.junkfood.seal.desktop

import java.io.File
import java.nio.file.Files
import java.time.Duration

internal data class StorageSummary(
    val bytes: Long,
    val oldPartialFiles: List<File>,
)

internal fun scanDownloadStorage(
    directories: List<File>,
    nowMillis: Long = System.currentTimeMillis(),
    partialFileAge: Duration = Duration.ofDays(30),
): StorageSummary {
    val distinctRoots = directories.map { it.canonicalFile }.distinct()
    val roots =
        distinctRoots.filter { candidate ->
            distinctRoots.none { parent ->
                parent != candidate && candidate.toPath().startsWith(parent.toPath())
            }
        }
    val cutoff = nowMillis - partialFileAge.toMillis()
    var totalBytes = 0L
    val oldPartialFiles = mutableListOf<File>()

    roots.forEach { root ->
        if (!root.exists()) return@forEach
        require(root.isDirectory) { "A download storage path is not a directory: ${root.path}" }
        root.walkTopDown().forEach { file ->
            if (file.isFile) {
                val length = file.length()
                totalBytes =
                    if (Long.MAX_VALUE - totalBytes < length) Long.MAX_VALUE
                    else totalBytes + length
                if (file.name.endsWith(".part", ignoreCase = true) && file.lastModified() < cutoff) {
                    oldPartialFiles += file
                }
            }
        }
    }
    return StorageSummary(totalBytes, oldPartialFiles)
}

internal fun deleteOldPartialFiles(
    files: List<File>,
    allowedRoots: List<File>,
    nowMillis: Long = System.currentTimeMillis(),
    partialFileAge: Duration = Duration.ofDays(30),
): Int {
    val rootPaths = allowedRoots.map { it.canonicalFile.toPath() }
    val cutoff = nowMillis - partialFileAge.toMillis()
    var deleted = 0
    files.forEach { file ->
        if (!file.exists()) return@forEach
        val candidate = file.canonicalFile.toPath()
        require(rootPaths.any { candidate.startsWith(it) && candidate != it }) {
            "Refusing to delete a file outside the selected download folder."
        }
        require(
            file.name.endsWith(".part", ignoreCase = true) &&
                file.isFile && file.lastModified() < cutoff,
        ) {
            "Refusing to delete a recent or non-partial file."
        }
        if (Files.deleteIfExists(candidate)) deleted++
    }
    return deleted
}
