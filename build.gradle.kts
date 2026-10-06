import java.net.URI
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.1"
}

group = "com.junkfood.seal.desktop"
val desktopVersion = providers.gradleProperty("appVersion").orElse("1.0.0").get().removePrefix("v")
version = desktopVersion
val ffmpegResourcesDir = layout.buildDirectory.dir("ffmpeg-resources")
val ffmpegArchive = layout.buildDirectory.file("ffmpeg/ffmpeg-N-127203-ga35c879992-win64-lgpl-shared.zip")
val ffmpegUrl =
    "https://github.com/BtbN/FFmpeg-Builds/releases/download/autobuild-2026-10-05-13-07/ffmpeg-N-127203-ga35c879992-win64-lgpl-shared.zip"
val ffmpegSha256 = "17c80fdc5c8f59f24c2275b3f33ef9cded8fd789120dbd36d0ca91b30403ed4f"
val ytDlpUrl = "https://github.com/yt-dlp/yt-dlp/releases/download/2026.08.19/yt-dlp.exe"
val ytDlpSha256 = "66674953fe251b89f4d08c5f0e35e0728679bd67ab3d7d05c0562af101dd3e7a"
val denoArchive = layout.buildDirectory.file("deno/deno-x86_64-pc-windows-msvc.zip")
val denoUrl =
    "https://github.com/denoland/deno/releases/download/v2.9.7/deno-x86_64-pc-windows-msvc.zip"
val denoSha256 = "a0c3101b4158d1dfb7d6a78a7bf0f3de80c96bb423c152beec8beb22786f2238"

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) break
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val downloadFfmpeg = tasks.register("downloadFfmpeg") {
    val archiveFile = ffmpegArchive.get().asFile
    outputs.file(archiveFile)

    doLast {
        if (!archiveFile.isFile) {
            archiveFile.parentFile.mkdirs()
            val partialFile = archiveFile.resolveSibling("${archiveFile.name}.part")
            partialFile.delete()
            URI(ffmpegUrl).toURL().openStream().use { input ->
                partialFile.outputStream().use(input::copyTo)
            }
            check(sha256(partialFile) == ffmpegSha256) {
                "FFmpeg archive checksum mismatch. Update the pinned checksum before packaging."
            }
            Files.move(partialFile.toPath(), archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

val prepareFfmpeg = tasks.register("prepareFfmpeg") {
    val archiveFile = ffmpegArchive.get().asFile
    val resourceDirectory = ffmpegResourcesDir.get().dir("windows/ffmpeg").asFile
    dependsOn(downloadFfmpeg)
    inputs.file(archiveFile)
    outputs.dir(resourceDirectory)

    doLast {
        check(sha256(archiveFile) == ffmpegSha256) {
            "FFmpeg archive checksum mismatch. Update the pinned checksum before packaging."
        }

        resourceDirectory.deleteRecursively()
        check(resourceDirectory.mkdirs()) { "Could not create the FFmpeg resources directory." }
        var ffmpegFound = false
        var ffprobeFound = false
        ZipFile(archiveFile).use { zip ->
            val runtimeFiles =
                zip.entries().asSequence()
                    .filter { entry ->
                        !entry.isDirectory &&
                            Regex("""/bin/(ffmpeg\.exe|ffprobe\.exe|[^/]+\.dll)$""", RegexOption.IGNORE_CASE)
                                .containsMatchIn(entry.name)
                    }
            runtimeFiles.forEach { entry ->
                val output = resourceDirectory.resolve(entry.name.substringAfterLast('/'))
                zip.getInputStream(entry).use { input ->
                    output.outputStream().use(input::copyTo)
                }
                if (entry.name.endsWith("/ffmpeg.exe", ignoreCase = true)) ffmpegFound = true
                if (entry.name.endsWith("/ffprobe.exe", ignoreCase = true)) ffprobeFound = true
            }
        }
        check(ffmpegFound && ffprobeFound) {
            "The FFmpeg archive did not contain the expected Windows executables."
        }
    }
}

val prepareYtDlp = tasks.register("prepareYtDlp") {
    val executableFile = ffmpegResourcesDir.get().file("windows/yt-dlp.exe").asFile
    outputs.file(executableFile)

    doLast {
        if (!executableFile.isFile || sha256(executableFile) != ytDlpSha256) {
            executableFile.parentFile.mkdirs()
            val partialFile = executableFile.resolveSibling("${executableFile.name}.part")
            partialFile.delete()
            URI(ytDlpUrl).toURL().openStream().use { input ->
                partialFile.outputStream().use(input::copyTo)
            }
            check(sha256(partialFile) == ytDlpSha256) {
                "yt-dlp checksum mismatch. Update the pinned checksum before packaging."
            }
            Files.move(partialFile.toPath(), executableFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

val downloadDeno = tasks.register("downloadDeno") {
    val archiveFile = denoArchive.get().asFile
    outputs.file(archiveFile)

    doLast {
        if (!archiveFile.isFile) {
            archiveFile.parentFile.mkdirs()
            val partialFile = archiveFile.resolveSibling("${archiveFile.name}.part")
            partialFile.delete()
            URI(denoUrl).toURL().openStream().use { input ->
                partialFile.outputStream().use(input::copyTo)
            }
            check(sha256(partialFile) == denoSha256) {
                "Deno archive checksum mismatch. Update the pinned checksum before packaging."
            }
            Files.move(partialFile.toPath(), archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

val prepareDeno = tasks.register("prepareDeno") {
    val archiveFile = denoArchive.get().asFile
    val executableFile = ffmpegResourcesDir.get().file("windows/deno.exe").asFile
    dependsOn(downloadDeno)
    inputs.file(archiveFile)
    outputs.file(executableFile)

    doLast {
        check(sha256(archiveFile) == denoSha256) {
            "Deno archive checksum mismatch. Update the pinned checksum before packaging."
        }
        executableFile.parentFile.mkdirs()
        ZipFile(archiveFile).use { zip ->
            val entry =
                zip.entries().asSequence()
                    .firstOrNull {
                        !it.isDirectory && it.name.substringAfterLast('/').equals("deno.exe", ignoreCase = true)
                    }
                    ?: error("The Deno archive did not contain deno.exe.")
            zip.getInputStream(entry).use { input ->
                executableFile.outputStream().use(input::copyTo)
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

compose.desktop {
    application {
        mainClass = "com.junkfood.seal.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            packageName = "Seal Desktop"
            packageVersion = desktopVersion
            description = "A Windows desktop video and audio downloader"
            vendor = "Seal Desktop"
            appResourcesRootDir.set(ffmpegResourcesDir)
        }
    }
}

tasks.matching { it.name == "prepareAppResources" }.configureEach {
    dependsOn(prepareFfmpeg)
    dependsOn(prepareYtDlp)
    dependsOn(prepareDeno)
}

tasks.matching { it.name == "packageExe" || it.name == "packageMsi" }.configureEach {
    dependsOn(prepareFfmpeg)
    dependsOn(prepareYtDlp)
    dependsOn(prepareDeno)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("version.properties") {
        expand(mapOf("version" to desktopVersion))
    }
}
