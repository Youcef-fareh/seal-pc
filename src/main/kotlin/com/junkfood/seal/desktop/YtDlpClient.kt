package com.junkfood.seal.desktop

import java.io.File
import java.net.URI
import java.util.ArrayDeque
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class VideoDetails(
    val title: String = "",
    val uploader: String? = null,
    val duration: Double? = null,
    val formats: List<VideoFormat> = emptyList(),
)

@Serializable
data class VideoFormat(
    val height: Int? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val ext: String? = null,
    val filesize: Long? = null,
    val filesize_approx: Long? = null,
    val abr: Double? = null,
) {
    fun hasVideo(): Boolean = !vcodec.isNullOrBlank() && vcodec != "none"

    fun hasAudio(): Boolean = !acodec.isNullOrBlank() && acodec != "none"

    fun estimatedSizeBytes(): Long? = (filesize ?: filesize_approx)?.takeIf { it > 0 }
}

class YtDlpClient(private val executable: String) {
    private val json = Json { ignoreUnknownKeys = true }
    private val ytDlpExecutable = resolveYtDlpExecutable(executable)
    private val ffmpegLocationArguments = bundledFfmpegLocationArguments()
    private val jsRuntimeArguments = bundledDenoArguments()

    fun version(): String = run(listOf("--version")).trim()

    fun fetchVideoDetails(url: String): VideoDetails {
        requireYouTubeUrl(url)
        val output =
            run(
                listOf(
                    "--quiet",
                    "--no-warnings",
                    "--no-playlist",
                    "--socket-timeout",
                    "30",
                    "--dump-single-json",
                    "--skip-download",
                    url,
                ),
            )
        return json.decodeFromString(output)
    }

    fun download(
        url: String,
        outputDirectory: File,
        maxHeight: Int?,
        audioFormat: String?,
        audioQuality: String,
        control: DownloadControl = DownloadControl(),
        onProgress: (String) -> Unit,
    ) {
        requireYouTubeUrl(url)
        require(outputDirectory.isDirectory) { "Choose an existing download folder." }

        val args =
            mutableListOf(
                "--newline",
                "--no-playlist",
                "--ignore-config",
                "--no-ignore-errors",
                "--socket-timeout",
                "30",
                "--continue",
                "--paths",
                outputDirectory.absolutePath,
                "-o",
                "%(title).200B [%(id)s].%(ext)s",
            )

        if (audioFormat != null) {
            args += listOf("--extract-audio", "--audio-format", audioFormat)
            if (audioQuality.isNotBlank()) {
                args += listOf("--audio-quality", audioQuality)
            }
        } else {
            args += listOf("--format", videoFormatSelector(maxHeight))
        }

        args += url
        run(args, onProgress, captureOutput = false, control = control)
    }

    private fun run(
        arguments: List<String>,
        onLine: (String) -> Unit = {},
        captureOutput: Boolean = true,
        control: DownloadControl? = null,
    ): String {
        val process =
            try {
                ProcessBuilder(listOf(ytDlpExecutable) + jsRuntimeArguments + ffmpegLocationArguments + arguments)
                    .redirectErrorStream(true)
                    .start()
            } catch (exception: Exception) {
                throw IllegalStateException(
                    "Could not start yt-dlp. Install yt-dlp or select its executable in the app.",
                    exception,
                )
            }

        val lines = Collections.synchronizedList(mutableListOf<String>())
        val recentLines = ArrayDeque<String>()
        control?.attach(process)
        val reader =
            thread(name = "yt-dlp-output-reader", isDaemon = true) {
                process.inputStream.bufferedReader().useLines { outputLines ->
                    outputLines.forEach { line ->
                        synchronized(lines) {
                            if (captureOutput) lines += line
                            recentLines.addLast(line)
                            if (recentLines.size > 8) recentLines.removeFirst()
                        }
                        onLine(line)
                    }
                }
            }

        try {
            process.waitFor()
        } finally {
            control?.detach(process)
        }
        reader.join()

        if (control?.wasCancelled == true) throw DownloadCancelledException()
        if (control?.wasPaused == true) throw DownloadPausedException()

        if (process.exitValue() != 0) {
            val details = synchronized(lines) { recentLines.joinToString("\n") }
            throw IllegalStateException(details.ifBlank { "yt-dlp exited with code ${process.exitValue()}." })
        }

        return synchronized(lines) { lines.joinToString("\n") }
    }
}

internal fun resolveYtDlpExecutable(
    configuredExecutable: String,
    resourcesDirectory: String? = System.getProperty("compose.application.resources.dir"),
): String {
    val configured = configuredExecutable.trim().ifBlank { "yt-dlp" }
    if (!configured.equals("yt-dlp", ignoreCase = true)) return configured
    val bundledExecutable = resourcesDirectory?.let { File(it, "yt-dlp.exe") }
    return if (bundledExecutable?.isFile == true) bundledExecutable.absolutePath else configured
}

internal fun bundledFfmpegLocationArguments(
    resourcesDirectory: String? = System.getProperty("compose.application.resources.dir"),
): List<String> {
    val ffmpegDirectory = resourcesDirectory?.let { File(it, "ffmpeg") } ?: return emptyList()
    return if (File(ffmpegDirectory, "ffmpeg.exe").isFile) {
        listOf("--ffmpeg-location", ffmpegDirectory.absolutePath)
    } else {
        emptyList()
    }
}

internal fun bundledDenoArguments(
    resourcesDirectory: String? = System.getProperty("compose.application.resources.dir"),
): List<String> {
    val denoExecutable = resourcesDirectory?.let { File(it, "deno.exe") } ?: return emptyList()
    return if (denoExecutable.isFile) {
        listOf("--js-runtimes", "deno:${denoExecutable.absolutePath}")
    } else {
        emptyList()
    }
}

internal fun estimateDownloadSizeBytes(
    video: VideoDetails,
    maxHeight: Int?,
    audioFormat: String?,
    audioQuality: String,
): Long? {
    if (audioFormat != null) {
        val targetBitrateKbps = Regex("""^(\d+)K$""").matchEntire(audioQuality)?.groupValues?.get(1)?.toLongOrNull()
        if (targetBitrateKbps != null && video.duration != null && video.duration.isFinite() && video.duration > 0) {
            return (video.duration * targetBitrateKbps * 1000 / 8).toLong()
        }
        return video.formats
            .filter { it.hasAudio() && !it.hasVideo() }
            .maxByOrNull { it.abr ?: it.estimatedSizeBytes()?.toDouble() ?: 0.0 }
            ?.estimatedSizeBytes()
    }

    val formats = video.formats
    val videoFormats = formats.filter { it.hasVideo() && (maxHeight == null || (it.height ?: 0) <= maxHeight) }
    val audioFormats = formats.filter { it.hasAudio() && !it.hasVideo() }
    val mp4Videos = videoFormats.filter { it.ext == "mp4" }
    val m4aAudios = audioFormats.filter { it.ext == "m4a" }

    fun combinedSize(videoFormat: VideoFormat, audioFormat: VideoFormat): Long? {
        val videoSize = videoFormat.estimatedSizeBytes() ?: return null
        val audioSize = audioFormat.estimatedSizeBytes() ?: return null
        return videoSize + audioSize
    }

    val preferredVideo = mp4Videos.maxByOrNull { it.height ?: 0 }
    val preferredAudio = m4aAudios.maxByOrNull { it.abr ?: it.estimatedSizeBytes()?.toDouble() ?: 0.0 }
    if (preferredVideo != null && preferredAudio != null) return combinedSize(preferredVideo, preferredAudio)

    val fallbackVideo = videoFormats.maxByOrNull { it.height ?: 0 }
    val fallbackAudio = audioFormats.maxByOrNull { it.abr ?: it.estimatedSizeBytes()?.toDouble() ?: 0.0 }
    if (fallbackVideo != null && fallbackAudio != null) return combinedSize(fallbackVideo, fallbackAudio)

    val muxedFormats =
        formats.filter { it.hasVideo() && it.hasAudio() && (maxHeight == null || (it.height ?: 0) <= maxHeight) }
    val mp4MuxedFormat = muxedFormats.filter { it.ext == "mp4" }.maxByOrNull { it.height ?: 0 }
    if (mp4MuxedFormat != null) return mp4MuxedFormat.estimatedSizeBytes()
    return muxedFormats.maxByOrNull { it.height ?: 0 }?.estimatedSizeBytes()
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    for (unit in units) {
        size /= 1024
        if (size < 1024 || unit == units.last()) {
            return "${"%.1f".format(java.util.Locale.ROOT, size)} $unit"
        }
    }
    return "$bytes B"
}

internal fun videoFormatSelector(maxHeight: Int?): String {
    val heightFilter = maxHeight?.let { "[height<=$it]" }.orEmpty()
    return "(bestvideo[ext=mp4]$heightFilter+bestaudio[ext=m4a])/" +
        "(bestvideo$heightFilter+bestaudio)/" +
        "best[ext=mp4]$heightFilter/best$heightFilter"
}

class DownloadControl {
    private val process = AtomicReference<Process?>()
    private val paused = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)

    val wasPaused: Boolean
        get() = paused.get()

    val wasCancelled: Boolean
        get() = cancelled.get()

    fun pause() {
        if (cancelled.get()) return
        paused.set(true)
        process.get()?.destroy()
    }

    fun cancel() {
        cancelled.set(true)
        paused.set(false)
        process.get()?.destroy()
    }

    internal fun attach(runningProcess: Process) {
        process.set(runningProcess)
        if (paused.get() || cancelled.get()) runningProcess.destroy()
    }

    internal fun detach(runningProcess: Process) {
        process.compareAndSet(runningProcess, null)
    }
}

class DownloadPausedException : Exception()

class DownloadCancelledException : Exception()

internal fun requireYouTubeUrl(url: String) {
    val uri =
        try {
            URI(url.trim())
        } catch (exception: Exception) {
            throw IllegalArgumentException("Enter a valid YouTube video URL.", exception)
        }
    val host = uri.host?.lowercase().orEmpty()
    val isYouTubeHost = host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be"
    require(uri.scheme in setOf("http", "https") && isYouTubeHost) {
        "Enter a YouTube video URL (youtube.com or youtu.be)."
    }
}
