package com.junkfood.seal.desktop

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        val connection = URI(RELEASES_API).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = API_READ_TIMEOUT_MILLIS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        val releaseJson =
            try {
                val statusCode = connection.responseCode
                if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null
                check(statusCode in 200..299) {
                    "GitHub update check failed (HTTP $statusCode)."
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

        val release = json.decodeFromString<GitHubRelease>(releaseJson)
        val newestVersion =
            release.tagName.removePrefix("v")
                .also { require(parseVersion(it) != null) { "GitHub returned an invalid release version." } }
        if (!isNewerVersion(currentVersion, newestVersion)) return null

        val installer =
            release.assets.firstOrNull {
                it.name.endsWith(".msi", ignoreCase = true)
            } ?: throw IllegalStateException("The latest release does not include a Windows MSI installer.")

        validateInstallerUrl(installer.downloadUrl)
        return DesktopUpdate(newestVersion, installer.downloadUrl)
    }

    fun downloadInstaller(update: DesktopUpdate, onProgress: (Float?) -> Unit): File {
        validateInstallerUrl(update.installerUrl)
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
        val connection = URI(update.installerUrl).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = INSTALLER_READ_TIMEOUT_MILLIS
        connection.instanceFollowRedirects = true

        try {
            val statusCode = connection.responseCode
            check(statusCode in 200..299) {
                "Installer download failed (HTTP $statusCode)."
            }
            val totalBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
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
        } finally {
            connection.disconnect()
        }
    }

    private fun validateInstallerUrl(url: String) {
        val uri = URI(url)
        require(uri.scheme == "https" && uri.host == "github.com") {
            "The latest release contains an invalid installer download link."
        }
    }
}

private const val CONNECT_TIMEOUT_MILLIS = 20_000
private const val API_READ_TIMEOUT_MILLIS = 30_000
private const val INSTALLER_READ_TIMEOUT_MILLIS = 30 * 60 * 1000

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
