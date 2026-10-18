package com.junkfood.seal.desktop

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class AppSettingsBackup(
    val language: String,
    val theme: String,
    val executable: String,
    val downloadDirectory: String,
    val audioOnly: Boolean,
    val audioFormat: String,
    val audioQuality: String,
    val outputTemplate: String,
    val downloadConcurrency: Int,
    val rateLimit: String,
    val proxy: String,
    val writeThumbnail: Boolean,
    val embedThumbnail: Boolean,
    val embedMetadata: Boolean,
    val embedChapters: Boolean,
    val clipboardMonitoring: Boolean,
    val aria2cExecutable: String = "",
    val sponsorBlockCategories: List<String> = emptyList(),
    val customArguments: String = "",
    val subtitleSource: String = "off",
    val subtitleLanguages: String = "en",
    val embedSubtitles: Boolean = false,
    val subtitleFormat: String = "srt",
    val subtitlesOnly: Boolean = false,
    val autoTranslatedSubtitles: Boolean = true,
) {
    fun validate() {
        require(language in setOf("en", "ar")) { "Settings backup has an unsupported language." }
        require(theme in setOf("system", "light", "dark", "high-contrast")) {
            "Settings backup has an unsupported theme."
        }
        require(audioFormat in setOf("mp3", "m4a", "opus", "wav")) {
            "Settings backup has an unsupported audio format."
        }
        require(audioQuality in setOf("Best", "320 kbps", "256 kbps", "192 kbps", "128 kbps")) {
            "Settings backup has an unsupported audio quality."
        }
        require(downloadConcurrency in 1..4) { "Settings backup has an invalid concurrency value." }
        require(isValidOutputTemplate(outputTemplate)) { "Settings backup has an invalid output template." }
        require(
            rateLimit.isBlank() ||
                Regex("""^\d+(?:\.\d+)?[KMG]?$""", RegexOption.IGNORE_CASE).matches(rateLimit),
        ) { "Settings backup has an invalid download speed limit." }
        require(proxy.isBlank() || isValidProxyUrl(proxy)) { "Settings backup has an invalid proxy URL." }
        require(aria2cExecutable.none(Char::isISOControl)) { "Settings backup has an invalid aria2c path." }
        require(
            sponsorBlockCategories.isEmpty() ||
                isValidSponsorBlockCategories(sponsorBlockCategories),
        ) { "Settings backup has invalid SponsorBlock categories." }
        validateSubtitleCustomArguments(subtitleSource, parseCustomYtDlpArguments(customArguments))
        require(subtitleSource in setOf("off", "manual", "auto", "both")) {
            "Settings backup has an unsupported subtitle source."
        }
        require(isValidSubtitleLanguages(subtitleLanguages)) {
            "Settings backup has invalid subtitle language codes."
        }
        require(subtitleFormat in setOf("srt", "vtt")) {
            "Settings backup has an unsupported subtitle format."
        }
    }

    fun write(file: File) {
        validate()
        file.writeText(Json { prettyPrint = true }.encodeToString(this))
    }

    companion object {
        fun read(file: File): AppSettingsBackup {
            val backup = Json.decodeFromString<AppSettingsBackup>(file.readText())
            backup.validate()
            return backup
        }
    }
}
