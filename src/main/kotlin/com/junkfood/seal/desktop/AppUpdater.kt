package com.junkfood.seal.desktop

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.Properties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val RELEASES_API = "https://api.github.com/repos/Youcef-fareh/seal-pc/releases/latest"

internal data class DesktopUpdate(
    val version: String,
    val installerUrl: String,
)

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<ReleaseAsset> = emptyList(),
)

@Serializable
private data class ReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

internal object AppUpdater {
    private val httpClient =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build()
    private val json = Json { ignoreUnknownKeys = true }

    val currentVersion: String by lazy {
        val properties = Properties()
        val stream =
            checkNotNull(AppUpdater::class.java.getResourceAsStream("/version.properties")) {
                "The application version is missing."
            }
        stream.use { properties.load(it) }
        checkNotNull(properties.getProperty("version")) { "The application version is missing." }
    }

    fun checkForUpdate(): DesktopUpdate? {
        val request =
            HttpRequest.newBuilder(URI(RELEASES_API))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 404) return null
        check(response.statusCode() in 200..299) {
            "GitHub update check failed (HTTP ${response.statusCode()})."
        }

        val release = json.decodeFromString<GitHubRelease>(response.body())
        val newestVersion =
            release.tagName.removePrefix("v")
                .also { require(parseVersion(it) != null) { "GitHub returned an invalid release version." } }
        if (!isNewerVersion(currentVersion, newestVersion)) return null

        val installer =
            release.assets.firstOrNull {
                it.name.endsWith(".msi", ignoreCase = true)
            } ?: throw IllegalStateException("The latest release does not include a Windows MSI installer.")

        val installerUri = URI(installer.downloadUrl)
        require(installerUri.scheme == "https" && installerUri.host == "github.com") {
            "The latest release contains an invalid installer download link."
        }
        return DesktopUpdate(newestVersion, installer.downloadUrl)
    }

    fun downloadInstaller(update: DesktopUpdate, onProgress: (Float?) -> Unit): File {
        val directory =
            File(
                System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
                "SealDesktop/updates",
            )
        check(directory.mkdirs() || directory.isDirectory) {
            "Could not create the update download folder."
        }
        val destination = File(directory, "Seal-Desktop-${update.version}.msi")
        val partial = File(directory, "${destination.name}.part")
        val request =
            HttpRequest.newBuilder(URI(update.installerUrl))
                .timeout(Duration.ofMinutes(30))
                .GET()
                .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
            check(response.statusCode() in 200..299) {
                response.body().close()
                "Installer download failed (HTTP ${response.statusCode()})."
            }
            val totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1)
            response.body().use { input ->
                Files.newOutputStream(partial.toPath()).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloadedBytes = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead < 0) break
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        onProgress(
                            if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else null,
                        )
                    }
                }
            }
            check(partial.length() > 0L) { "The downloaded installer is empty." }
            Files.move(partial.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return destination
        } catch (exception: Exception) {
            Files.deleteIfExists(partial.toPath())
            throw exception
        }
    }
}

internal fun isNewerVersion(currentVersion: String, availableVersion: String): Boolean {
    val current = parseVersion(currentVersion) ?: return false
    val available = parseVersion(availableVersion) ?: return false
    val size = maxOf(current.size, available.size)
    for (index in 0 until size) {
        val currentPart = current.getOrElse(index) { 0 }
        val availablePart = available.getOrElse(index) { 0 }
        if (availablePart != currentPart) return availablePart > currentPart
    }
    return false
}

private fun parseVersion(version: String): List<Int>? {
    val parts = version.split('.')
    if (parts.isEmpty()) return null
    val numbers = parts.map { it.toIntOrNull() ?: return null }
    return numbers.takeIf { it.all { number -> number >= 0 } }
}
