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
}

tasks.matching { it.name == "packageExe" || it.name == "packageMsi" }.configureEach {
    dependsOn(prepareFfmpeg)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filesMatching("version.properties") {
        expand(mapOf("version" to desktopVersion))
    }
}
