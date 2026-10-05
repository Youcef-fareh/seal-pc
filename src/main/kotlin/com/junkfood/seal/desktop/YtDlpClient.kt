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
    val formats: List<VideoFormat> = emptyList(),
)

@Serializable
data class VideoFormat(
    val height: Int? = null,
    val vcodec: String? = null,
) {
    fun hasVideo(): Boolean = !vcodec.isNullOrBlank() && vcodec != "none"
}

class YtDlpClient(private val executable: String) {
    private val json = Json { ignoreUnknownKeys = true }

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
            val format =
                if (maxHeight == null) {
                    "bv*+ba/b"
                } else {
                    "bestvideo[height<=$maxHeight]+bestaudio/best[height<=$maxHeight]"
                }
            args += listOf("--format", format)
            args += listOf("--merge-output-format", "mkv")
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
                ProcessBuilder(listOf(executable) + arguments)
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
