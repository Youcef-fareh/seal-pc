package com.junkfood.seal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val accent = Color(0xFFE53935)
private val audioFormats = listOf("mp3", "m4a", "opus", "wav")
private val audioQualities = listOf("Best", "320 kbps", "256 kbps", "192 kbps", "128 kbps")

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Seal Desktop",
    ) {
        SealDesktopApp()
    }
}

@Composable
private fun SealDesktopApp() {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var executable by remember { mutableStateOf("yt-dlp") }
    var downloadDirectory by remember {
        mutableStateOf(File(System.getProperty("user.home"), "Downloads").absolutePath)
    }
    var details by remember { mutableStateOf<VideoDetails?>(null) }
    var maxHeight by remember { mutableStateOf<Int?>(null) }
    var audioOnly by remember { mutableStateOf(false) }
    var audioFormat by remember { mutableStateOf("mp3") }
    var audioQuality by remember { mutableStateOf("Best") }
    var status by remember { mutableStateOf("Paste a YouTube link to get started.") }
    var error by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) }

    MaterialTheme(
        colorScheme =
            if (UIManager.getLookAndFeel()?.name?.contains("dark", ignoreCase = true) == true) {
                darkColorScheme(primary = accent)
            } else {
                lightColorScheme(primary = accent)
            },
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 36.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.background(accent, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text("S", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Seal Desktop", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Video and audio downloads, made simple.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("1. Video link", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("YouTube URL") },
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            singleLine = true,
                            enabled = !busy,
                        )
                        Button(
                            onClick = {
                                busy = true
                                error = false
                                progress = null
                                status = "Loading video formats…"
                                scope.launch {
                                    try {
                                        val result =
                                            withContext(Dispatchers.IO) {
                                                YtDlpClient(executable.trim()).fetchVideoDetails(url)
                                            }
                                        details = result
                                        maxHeight =
                                            result.formats
                                                .filter { it.hasVideo() }
                                                .mapNotNull { it.height }
                                                .distinct()
                                                .maxOrNull()
                                        status = "Formats loaded. Choose a quality and download folder."
                                    } catch (exception: Exception) {
                                        details = null
                                        error = true
                                        status = exception.message ?: "Could not load video formats."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy && url.isNotBlank(),
                        ) {
                            Text(if (busy) "Please wait…" else "Load formats")
                        }
                    }
                }

                details?.let { video ->
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("2. Download options", style = MaterialTheme.typography.titleMedium)
                            Text(video.title, style = MaterialTheme.typography.titleLarge)
                            video.uploader?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Type:", modifier = Modifier.width(84.dp))
                                ChoiceMenu(
                                    selected = if (audioOnly) "Audio only" else "Video + audio",
                                    options = listOf("Video + audio", "Audio only"),
                                    enabled = !busy,
                                    onSelected = { audioOnly = it == "Audio only" },
                                )
                            }
                            if (audioOnly) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Audio:", modifier = Modifier.width(84.dp))
                                    ChoiceMenu(
                                        selected = audioFormat.uppercase(),
                                        options = audioFormats.map(String::uppercase),
                                        enabled = !busy,
                                        onSelected = {
                                            audioFormat = it.lowercase()
                                        },
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    ChoiceMenu(
                                        selected = audioQuality,
                                        options = audioQualities,
                                        enabled = !busy,
                                        onSelected = { audioQuality = it },
                                    )
                                }
                            } else {
                                val heights =
                                    video.formats
                                        .filter { it.hasVideo() }
                                        .mapNotNull { it.height }
                                        .distinct()
                                        .sortedDescending()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Quality:", modifier = Modifier.width(84.dp))
                                    ChoiceMenu(
                                        selected = maxHeight?.let { "$it p or lower" } ?: "Best available",
                                        options = listOf("Best available") + heights.map { "$it p or lower" },
                                        enabled = !busy,
                                        onSelected = {
                                            maxHeight =
                                                if (it == "Best available") null
                                                else it.substringBefore(" p").toIntOrNull()
                                        },
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Save to:", modifier = Modifier.width(84.dp))
                                Text(
                                    downloadDirectory,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    enabled = !busy,
                                    onClick = {
                                        chooseDirectory(downloadDirectory)?.let {
                                            downloadDirectory = it.absolutePath
                                        }
                                    },
                                ) {
                                    Text("Browse…")
                                }
                            }
                            Button(
                                enabled = !busy && video.title.isNotBlank(),
                                onClick = {
                                    busy = true
                                    error = false
                                    progress = 0f
                                    status = "Starting download…"
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                YtDlpClient(executable.trim())
                                                    .download(
                                                        url = url,
                                                        outputDirectory = File(downloadDirectory),
                                                        maxHeight = if (audioOnly) null else maxHeight,
                                                        audioFormat = if (audioOnly) audioFormat else null,
                                                        audioQuality =
                                                            if (audioQuality == "Best") {
                                                                "0"
                                                            } else {
                                                                audioQuality.substringBefore(" kbps") + "K"
                                                            },
                                                    ) { line ->
                                                        val percent =
                                                            Regex("""(\d+(?:\.\d+)?)%""")
                                                                .find(line)
                                                                ?.groupValues
                                                                ?.getOrNull(1)
                                                                ?.toFloatOrNull()
                                                        if (percent != null) progress = percent / 100f
                                                        if (line.isNotBlank()) status = line.take(180)
                                                    }
                                            }
                                            progress = 1f
                                            status = "Download complete. Saved to $downloadDirectory"
                                        } catch (exception: Exception) {
                                            error = true
                                            status = exception.message ?: "Download failed."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                            ) {
                                Text(if (busy) "Downloading…" else "Download")
                            }
                        }
                    }
                }

                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Downloader", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Seal Desktop uses yt-dlp and FFmpeg installed on your PC.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = executable,
                                onValueChange = { executable = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("yt-dlp command or executable path") },
                                placeholder = { Text("yt-dlp or C:\\tools\\yt-dlp.exe") },
                                singleLine = true,
                                enabled = !busy,
                            )
                            Spacer(Modifier.width(10.dp))
                            TextButton(
                                enabled = !busy,
                                onClick = {
                                    chooseExecutable(executable)?.let { executable = it.absolutePath }
                                },
                            ) {
                                Text("Browse…")
                            }
                        }
                    }
                }

                Card {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            status,
                            color =
                                if (error) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                        )
                        progress?.let {
                            LinearProgressIndicator(
                                progress = { it.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Text(
                    "Only download content you have permission to save. You are responsible for complying with applicable laws and YouTube’s terms.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ChoiceMenu(
    selected: String,
    options: List<String>,
    enabled: Boolean,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }, enabled = enabled) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun chooseDirectory(currentPath: String): File? {
    val chooser = JFileChooser(currentPath).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose download folder"
        isAcceptAllFileFilterUsed = false
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}

private fun chooseExecutable(currentPath: String): File? {
    val initialDirectory = File(currentPath).takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))
    val chooser = JFileChooser(initialDirectory).apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        dialogTitle = "Select yt-dlp executable"
        fileFilter = FileNameExtensionFilter("yt-dlp executable (*.exe)", "exe")
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}
