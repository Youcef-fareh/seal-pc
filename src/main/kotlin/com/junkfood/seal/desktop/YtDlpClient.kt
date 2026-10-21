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
    val extractor: String? = null,
    val extractor_key: String? = null,
    val duration: Double? = null,
    val formats: List<VideoFormat> = emptyList(),
)

@Serializable
data class PlaylistDetails(
    val title: String = "",
    val uploader: String? = null,
    val entries: List<PlaylistEntry?> = emptyList(),
)

@Serializable
data class PlaylistEntry(
    val id: String? = null,
    val title: String? = null,
    val url: String? = null,
    val webpage_url: String? = null,
    val duration: Double? = null,
)

@Serializable
data class VideoFormat(
    val format_id: String? = null,
    val height: Int? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val ext: String? = null,
    val fps: Double? = null,
    val tbr: Double? = null,
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
        val validatedUrl = url.trim().also(::requireHttpUrl)
        val output =
            run(
                listOf(
                    "--quiet",
                    "--no-warnings",
                    "--no-playlist",
                    "--ignore-config",
                    "--socket-timeout",
                    "30",
                    "--dump-single-json",
                    "--skip-download",
                    validatedUrl,
                ),
            )
        return json.decodeFromString(output)
    }

    fun fetchPlaylistDetails(url: String): PlaylistDetails {
        val validatedUrl = url.trim().also(::requireHttpUrl)
        require(isYouTubePlaylistUrl(validatedUrl)) { "Enter a YouTube playlist URL." }
        val output =
            run(
                listOf(
                    "--quiet",
                    "--no-warnings",
                    "--yes-playlist",
                    "--flat-playlist",
                    "--ignore-config",
                    "--dump-single-json",
                    "--skip-download",
                    "--socket-timeout",
                    "30",
                    validatedUrl,
                ),
            )
        val playlist = json.decodeFromString<PlaylistDetails>(output)
        require(playlist.entries.isNotEmpty()) { "The playlist contains no available videos." }
        return playlist
    }

    fun download(
        url: String,
        outputDirectory: File,
        maxHeight: Int?,
        audioFormat: String?,
        audioQuality: String,
        control: DownloadControl = DownloadControl(),
        formatSelector: String? = null,
        outputTemplate: String = "%(title).200B [%(id)s].%(ext)s",
        rateLimit: String? = null,
        proxy: String? = null,
        writeThumbnail: Boolean = false,
        embedThumbnail: Boolean = false,
        embedMetadata: Boolean = false,
        embedChapters: Boolean = false,
        aria2cExecutable: String? = null,
        cookiesFile: String? = null,
        sponsorBlockCategories: List<String> = emptyList(),
        customArguments: String = "",
        subtitleSource: String = "off",
        subtitleLanguages: String = "en",
        embedSubtitles: Boolean = false,
        subtitleFormat: String = "srt",
        subtitlesOnly: Boolean = false,
        autoTranslatedSubtitles: Boolean = true,
        onProgress: (String) -> Unit,
    ) {
        val validatedUrl = url.trim().also(::requireHttpUrl)
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory) { "Could not create the download folder." }
        require(isValidOutputTemplate(outputTemplate)) { "The output template contains unsupported placeholders." }

        val args =
            mutableListOf(
                "--newline",
                "--no-playlist",
                "--ignore-config",
                "--no-ignore-errors",
                "--socket-timeout",
                "30",
                "--retries",
                "10",
                "--fragment-retries",
                "infinite",
                "--continue",
                "--paths",
                outputDirectory.absolutePath,
                "-o",
                outputTemplate,
            )
        rateLimit?.takeIf { it.isNotBlank() }?.let {
            require(Regex("""^\d+(?:\.\d+)?[KMG]?$""", RegexOption.IGNORE_CASE).matches(it)) {
                "Enter a valid download speed limit, such as 1M or 500K."
            }
            args += listOf("--limit-rate", it)
        }
        proxy?.takeIf { it.isNotBlank() }?.let {
            require(isValidProxyUrl(it)) {
                "Enter a valid HTTP or SOCKS proxy URL."
            }
            args += listOf("--proxy", it)
        }
        cookiesFile?.takeIf { it.isNotBlank() }?.let {
            require(isNetscapeCookieFile(File(it))) {
                "Select a readable Netscape-format cookie file."
            }
            args += listOf("--cookies", File(it).absolutePath)
        }
        val aria2cPath =
            aria2cExecutable?.trim()?.takeIf { it.isNotBlank() }?.also {
                require(isValidAria2cExecutable(it)) { "Select an existing aria2c executable." }
                args += listOf("--downloader", "aria2c", "--downloader-args", "aria2c:-x16 -k1M")
            }
        if (!subtitlesOnly && sponsorBlockCategories.isNotEmpty()) {
            require(isValidSponsorBlockCategories(sponsorBlockCategories)) {
                "Select supported SponsorBlock categories."
            }
            args += listOf("--sponsorblock-remove", sponsorBlockCategories.joinToString(","))
        }
        if (subtitlesOnly) {
            require(audioFormat == null) { "Subtitle-only downloads cannot use an audio format." }
        } else {
            if (writeThumbnail) args += "--write-thumbnail"
            if (embedThumbnail && audioFormat != null) args += "--embed-thumbnail"
            if (embedMetadata) args += "--embed-metadata"
            if (embedChapters) args += "--embed-chapters"

            if (audioFormat != null) {
                args += listOf("--extract-audio", "--audio-format", audioFormat)
                if (audioQuality.isNotBlank()) {
                    args += listOf("--audio-quality", audioQuality)
                }
            } else {
                args += listOf("--format", formatSelector ?: videoFormatSelector(maxHeight))
            }
        }

        val parsedCustomArguments = parseCustomYtDlpArguments(customArguments)
        // Subtitles-only still falls back to both sources when the user left subtitles off.
        val effectiveSubtitleSource =
            if (subtitlesOnly && subtitleSource == "off") "both" else subtitleSource
        validateSubtitleCustomArguments(effectiveSubtitleSource, parsedCustomArguments)
        if (subtitlesOnly) {
            args +=
                subtitleOnlyDownloadArguments(
                    source = effectiveSubtitleSource,
                    languages = subtitleLanguages,
                    format = subtitleFormat,
                    autoTranslatedSubtitles = autoTranslatedSubtitles,
                )
        } else {
            args +=
                subtitleArguments(
                    source = effectiveSubtitleSource,
                    languages = subtitleLanguages,
                    embed = embedSubtitles,
                    format = subtitleFormat,
                    audioOnly = audioFormat != null,
                    autoTranslatedSubtitles = autoTranslatedSubtitles,
                )
            args += subtitleThrottleArguments(effectiveSubtitleSource)
        }
        args += parsedCustomArguments

        args += validatedUrl
        try {
            run(
                args,
                onProgress,
                captureOutput = false,
                control = control,
                extraPathDirectories = aria2cPath?.let { listOf(File(it).absoluteFile.parent) }.orEmpty(),
            )
        } catch (exception: IllegalStateException) {
            if (effectiveSubtitleSource == "off" || subtitlesOnly ||
                !isSubtitleDownloadFailure(exception.message.orEmpty())
            ) {
                throw exception
            }
            onProgress("WARNING: Subtitle download failed; retrying the video without subtitles.")
            run(
                withoutSubtitleArguments(args),
                onProgress,
                captureOutput = false,
                control = control,
                extraPathDirectories = aria2cPath?.let { listOf(File(it).absoluteFile.parent) }.orEmpty(),
            )
        }
    }

    private fun run(
        arguments: List<String>,
        onLine: (String) -> Unit = {},
        captureOutput: Boolean = true,
        control: DownloadControl? = null,
        extraPathDirectories: List<String> = emptyList(),
    ): String {
        val process =
            try {
                ProcessBuilder(listOf(ytDlpExecutable) + jsRuntimeArguments + ffmpegLocationArguments + arguments)
                    .apply {
                        if (extraPathDirectories.isNotEmpty()) {
                            val environment = environment()
                            environment["PATH"] =
                                (extraPathDirectories + environment["PATH"].orEmpty())
                                    .joinToString(File.pathSeparator)
                        }
                    }
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

private val sponsorBlockCategories =
    setOf(
        "sponsor",
        "intro",
        "outro",
        "selfpromo",
        "preview",
        "filler",
        "interaction",
        "music_offtopic",
        "poi_highlight",
        "exclusive_access",
        "chapter",
    )

internal fun isValidSponsorBlockCategories(categories: List<String>): Boolean =
    categories.isNotEmpty() && categories.distinct().size == categories.size &&
        categories.all { it in sponsorBlockCategories }

internal fun parseCustomYtDlpArguments(template: String): List<String> {
    val valueOptions =
        setOf(
            "--format",
            "--merge-output-format",
            "--recode-video",
            "--audio-format",
            "--audio-quality",
            "--sub-langs",
            "--match-title",
            "--reject-title",
            "--date",
            "--dateafter",
            "--datebefore",
            "--min-filesize",
            "--max-filesize",
            "--playlist-items",
            "--extractor-args",
            "--compat-options",
        )
    val flagOptions = setOf("--write-subs", "--write-auto-subs", "--embed-subs")
    val lines = template.lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith('#') }.toList()
    require(template.length <= 7000) { "Custom templates may contain at most 7000 characters." }
    require(lines.size <= 32) { "Custom templates may contain at most 32 arguments." }
    require(lines.all { it.length <= 1000 && it.none(Char::isISOControl) }) {
        "Custom template arguments contain unsupported control characters or exceed 1000 characters."
    }

    val result = mutableListOf<String>()
    var expectsValue = false
    lines.forEach { argument ->
        if (expectsValue) {
            require(!argument.startsWith("--") && argument.isNotBlank()) {
                "Each custom option that takes a value must be followed by its value on the next line."
            }
            result += argument
            expectsValue = false
            return@forEach
        }
        val option = argument.substringBefore('=')
        when {
            option in valueOptions && '=' in argument -> {
                require(argument.substringAfter('=').isNotBlank()) { "Custom option $option requires a value." }
                result += argument
            }
            option in valueOptions -> {
                result += option
                expectsValue = true
            }
            argument in flagOptions -> result += argument
            else -> throw IllegalArgumentException("Custom option is not allowed: $option")
        }
    }
    require(!expectsValue) { "The last custom option is missing its value." }
    return result
}

internal fun validateSubtitleCustomArguments(source: String, arguments: List<String>) {
    if (source == "off") return
    val subtitleOptions = setOf("--sub-langs", "--write-subs", "--write-auto-subs", "--embed-subs")
    require(arguments.none { it.substringBefore('=') in subtitleOptions }) {
        "Remove subtitle options from the advanced arguments while dedicated subtitle settings are enabled."
    }
}

internal fun isValidSubtitleLanguages(languages: String): Boolean {
    val values = languages.split(',').map(String::trim)
    return languages.length <= 200 && values.isNotEmpty() &&
        values.all { it.isNotEmpty() && Regex("""^[A-Za-z0-9_*.-]+$""").matches(it) } &&
        (values.size == 1 || values.none { it.equals("all", ignoreCase = true) })
}

internal fun subtitleArguments(
    source: String,
    languages: String,
    embed: Boolean,
    format: String,
    audioOnly: Boolean,
    autoTranslatedSubtitles: Boolean = true,
): List<String> {
    require(source in setOf("off", "manual", "auto", "both")) { "Choose a supported subtitle source." }
    if (source == "off") return emptyList()
    require(isValidSubtitleLanguages(languages)) {
        "Enter subtitle language codes such as en, ar, fr, or all."
    }
    require(format in setOf("srt", "vtt")) { "Choose SRT or WebVTT as the subtitle format." }

    return buildList {
        if (source == "manual" || source == "both") add("--write-subs")
        if (source == "auto" || source == "both") {
            add("--write-auto-subs")
            // Keep only the original-language automatic captions when translations are not wanted.
            if (!autoTranslatedSubtitles) {
                addAll(listOf("--extractor-args", "youtube:skip=translated_subs"))
            }
        }
        addAll(listOf("--sub-langs", languages.split(',').joinToString(",") { it.trim() }))
        if (embed && !audioOnly) add("--embed-subs")
        addAll(listOf("--convert-subs", format))
    }
}

internal fun subtitleOnlyDownloadArguments(
    source: String,
    languages: String,
    format: String,
    autoTranslatedSubtitles: Boolean = true,
): List<String> =
    listOf("--skip-download") +
        subtitleArguments(
            source = source,
            languages = languages,
            embed = false,
            format = format,
            audioOnly = false,
            autoTranslatedSubtitles = autoTranslatedSubtitles,
        ) +
        subtitleThrottleArguments(source)

internal fun isSubtitleDownloadFailure(message: String): Boolean {
    val normalized = message.lowercase()
    return "subtitle" in normalized &&
        ("error:" in normalized || "unable to" in normalized || "failed" in normalized)
}

internal fun withoutSubtitleArguments(arguments: List<String>): List<String> {
    val flags = setOf("--write-subs", "--write-auto-subs", "--embed-subs")
    val optionsWithValues =
        setOf("--sub-langs", "--convert-subs", "--sleep-requests", "--sleep-subtitles", "--retry-sleep")
    val result = mutableListOf<String>()
    var index = 0
    while (index < arguments.size) {
        when {
            arguments[index] in flags -> index++
            arguments[index] in optionsWithValues -> index += 2
            else -> result += arguments[index++]
        }
    }
    return result
}

internal fun subtitleThrottleArguments(source: String): List<String> =
    if (source == "off") {
        emptyList()
    } else {
        listOf(
            "--sleep-requests",
            "1",
            "--sleep-subtitles",
            "5",
            "--retry-sleep",
            "http:exp=2:60",
        )
    }

internal fun isNetscapeCookieFile(file: File): Boolean =
    file.isFile && file.canRead() && runCatching {
        file.useLines { lines ->
            lines.take(5).any { it.trim() in setOf("# Netscape HTTP Cookie File", "# HTTP Cookie File") }
        }
    }.getOrDefault(false)

internal fun isValidAria2cExecutable(executable: String): Boolean {
    val file = File(executable)
    return file.isFile && file.canRead() &&
        (file.name.equals("aria2c", ignoreCase = true) || file.name.equals("aria2c.exe", ignoreCase = true))
}

internal fun isYouTubePlaylistUrl(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    val host = uri.host?.lowercase().orEmpty()
    return uri.scheme?.lowercase() in setOf("http", "https") &&
        (host == "youtube.com" || host.endsWith(".youtube.com") || host == "youtu.be") &&
        uri.rawQuery.orEmpty().split('&')
            .any { it.substringBefore('=') == "list" && it.substringAfter('=', "").isNotBlank() }
}

internal fun isYouTubeVideoUrl(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    if (uri.scheme?.lowercase() !in setOf("http", "https")) return false
    val host = uri.host?.lowercase().orEmpty()
    val videoIdPattern = Regex("""^[A-Za-z0-9_-]{6,}$""")
    if (host == "youtu.be") return uri.path.orEmpty().removePrefix("/").let(videoIdPattern::matches)
    if (host != "youtube.com" && !host.endsWith(".youtube.com")) return false
    val path = uri.path.orEmpty()
    return when {
        path == "/watch" ->
            uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.substringBefore('=') == "v" }
                ?.substringAfter('=', "")
                ?.let(videoIdPattern::matches) == true
        path.startsWith("/shorts/") || path.startsWith("/embed/") || path.startsWith("/live/") ->
            path.substringAfterLast('/').let(videoIdPattern::matches)
        else -> false
    }
}

internal fun playlistEntryUrl(entry: PlaylistEntry): String? {
    entry.webpage_url?.takeIf(::isYouTubeVideoUrl)?.let { return it }
    entry.url?.takeIf(::isYouTubeVideoUrl)?.let { return it }
    val videoId =
        sequenceOf(entry.id, entry.url).filterNotNull()
            .firstOrNull { Regex("""^[A-Za-z0-9_-]{6,}$""").matches(it) } ?: return null
    return "https://www.youtube.com/watch?v=$videoId"
}

internal fun isValidOutputTemplate(template: String): Boolean {
    if (
        template.isBlank() || template.startsWith('/') || template.contains('\\') ||
        template.contains("..") || Regex("""^[A-Za-z]:|[:*?"<>|]""").containsMatchIn(template)
    ) return false
    val placeholderPattern = Regex("""%\(([^)]+)\)(?:\.\d+)?[B]?[sdif]?""")
    val withoutPlaceholders = placeholderPattern.replace(template, "")
    if ('%' in withoutPlaceholders) return false
    val allowed =
        setOf("title", "id", "uploader", "upload_date", "resolution", "ext", "extractor", "extractor_key")
    return placeholderPattern.findAll(template).all { it.groupValues[1] in allowed }
}

internal fun videoFormatSelector(format: VideoFormat): String? {
    val id = format.format_id?.takeIf { Regex("""^[A-Za-z0-9._-]+$""").matches(it) } ?: return null
    return if (format.hasAudio()) id else "$id+bestaudio/best"
}

internal fun isValidProxyUrl(proxy: String): Boolean {
    val uri =
        try {
            URI(proxy)
        } catch (_: Exception) {
            return false
        }
    return uri.scheme in setOf("http", "https", "socks4", "socks4a", "socks5") &&
        !uri.host.isNullOrBlank() && uri.userInfo == null
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

internal fun isValidHttpUrl(url: String): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
}

internal fun parseDroppedUrl(text: String?): String? =
    text?.lineSequence()
        ?.map(String::trim)
        ?.firstOrNull { line ->
            line.isNotEmpty() && !line.startsWith("#") && isValidHttpUrl(line)
        }

internal fun requireHttpUrl(url: String) {
    require(isValidHttpUrl(url)) {
        "Enter a valid HTTP or HTTPS URL. yt-dlp will determine whether the site is supported."
    }
}

internal fun userFriendlyYtDlpError(message: String): String =
    if (Regex("""unsupported url|no suitable extractor|not a valid url""", RegexOption.IGNORE_CASE)
            .containsMatchIn(message)
    ) {
        "This URL is invalid or this site is not supported by yt-dlp.\n$message"
    } else {
        message
    }
