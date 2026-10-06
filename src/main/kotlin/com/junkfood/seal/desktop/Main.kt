package com.junkfood.seal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val accent = Color(0xFFE53935)
private val audioFormats = listOf("mp3", "m4a", "opus", "wav")
private val audioQualities = listOf("Best", "320 kbps", "256 kbps", "192 kbps", "128 kbps")
private const val UPDATE_REMINDER_MILLIS = 24L * 60 * 60 * 1000

private val english = mapOf(
    "tagline" to "Video and audio downloads, made simple.",
    "language" to "Language",
    "english" to "English",
    "arabic" to "العربية",
    "videoLink" to "1. Video link",
    "youtubeUrl" to "YouTube URL",
    "loadFormats" to "Load formats",
    "loading" to "Loading…",
    "videoOptions" to "2. Download options",
    "type" to "Type:",
    "audioOnly" to "Audio only",
    "videoAudio" to "Video + audio",
    "audio" to "Audio:",
    "quality" to "Quality:",
    "best" to "Best available",
    "orLower" to "or lower",
    "saveTo" to "Save to:",
    "browse" to "Browse…",
    "addToQueue" to "Add to download queue",
    "queueTitle" to "Download queue",
    "queueEmpty" to "Your download queue is empty. Add video links and choose their settings above.",
    "startDownloads" to "Start downloads",
    "waiting" to "Waiting",
    "downloading" to "Downloading",
    "paused" to "Paused",
    "complete" to "Complete",
    "failed" to "Failed",
    "cancelled" to "Cancelled",
    "pause" to "Pause",
    "resume" to "Resume",
    "remove" to "Remove",
    "downloader" to "Downloader",
    "downloaderDescription" to "Windows installers include yt-dlp and FFmpeg. Running from source requires both installed.",
    "executable" to "yt-dlp command or executable path",
    "appUpdates" to "App updates",
    "checking" to "Checking…",
    "checkUpdates" to "Check for updates",
    "pasteLink" to "Paste a YouTube link to get started.",
    "formatsLoaded" to "Formats loaded. Choose the download settings, then add this video to the queue.",
    "startQueued" to "Downloading the queued videos one at a time…",
    "nothingToStart" to "There are no videos waiting in the queue.",
    "upToDate" to "You’re up to date",
    "updateAvailable" to "An update is available.",
    "updateCheckFailed" to "Could not check for updates.",
    "updateDialog" to "Download and open the Windows installer now, or be reminded about this update later.",
    "install" to "Download & install",
    "remindLater" to "Remind me later",
    "downloadingUpdate" to "Downloading the update installer…",
    "openingInstaller" to "Opening the Windows installer…",
    "updateFailed" to "Could not download or open the update installer.",
    "legal" to "Only download content you have permission to save. You are responsible for complying with applicable laws and YouTube’s terms.",
)

private val arabic = mapOf(
    "tagline" to "تنزيل الفيديوهات والصوت بسهولة.",
    "language" to "اللغة",
    "english" to "English",
    "arabic" to "العربية",
    "videoLink" to "١. رابط الفيديو",
    "youtubeUrl" to "رابط يوتيوب",
    "loadFormats" to "تحميل الجودات",
    "loading" to "جارٍ التحميل…",
    "videoOptions" to "٢. إعدادات التنزيل",
    "type" to "النوع:",
    "audioOnly" to "صوت فقط",
    "videoAudio" to "فيديو وصوت",
    "audio" to "الصوت:",
    "quality" to "الجودة:",
    "best" to "أفضل جودة متاحة",
    "orLower" to "أو أقل",
    "saveTo" to "الحفظ في:",
    "browse" to "استعراض…",
    "addToQueue" to "إضافة إلى قائمة التنزيل",
    "queueTitle" to "قائمة التنزيل",
    "queueEmpty" to "قائمة التنزيل فارغة. أضف روابط الفيديو واختر إعداداتها أعلاه.",
    "startDownloads" to "بدء التنزيل",
    "waiting" to "في الانتظار",
    "downloading" to "جارٍ التنزيل",
    "paused" to "متوقف مؤقتًا",
    "complete" to "مكتمل",
    "failed" to "فشل",
    "cancelled" to "أُلغي",
    "pause" to "إيقاف مؤقت",
    "resume" to "استئناف",
    "remove" to "إزالة",
    "downloader" to "أداة التنزيل",
    "downloaderDescription" to "تتضمن نسخة ويندوز المثبتة أداتي yt-dlp وFFmpeg. يتطلب التشغيل من المصدر تثبيتهما.",
    "executable" to "أمر yt-dlp أو مسار البرنامج",
    "appUpdates" to "تحديثات التطبيق",
    "checking" to "جارٍ التحقق…",
    "checkUpdates" to "التحقق من التحديثات",
    "pasteLink" to "الصق رابط يوتيوب للبدء.",
    "formatsLoaded" to "تم تحميل الجودات. اختر الإعدادات ثم أضف الفيديو إلى القائمة.",
    "startQueued" to "جارٍ تنزيل الفيديوهات في القائمة واحدًا تلو الآخر…",
    "nothingToStart" to "لا توجد فيديوهات بانتظار التنزيل.",
    "upToDate" to "التطبيق محدّث",
    "updateAvailable" to "يتوفر تحديث جديد.",
    "updateCheckFailed" to "تعذر التحقق من التحديثات.",
    "updateDialog" to "نزّل مثبّت ويندوز وافتحه الآن، أو ذكّرني بهذا التحديث لاحقًا.",
    "install" to "تنزيل وتثبيت",
    "remindLater" to "ذكّرني لاحقًا",
    "downloadingUpdate" to "جارٍ تنزيل ملف التحديث…",
    "openingInstaller" to "جارٍ فتح مثبّت ويندوز…",
    "updateFailed" to "تعذر تنزيل التحديث أو فتح مثبّته.",
    "legal" to "نزّل فقط المحتوى المسموح لك بحفظه. أنت مسؤول عن الالتزام بالقوانين وشروط يوتيوب.",
)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Seal Desktop") {
        SealDesktopApp(onExit = ::exitApplication)
    }
}

@Composable
private fun SealDesktopApp(onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val preferences = remember { Preferences.userNodeForPackage(AppUpdater::class.java) }
    val queue = remember { mutableStateListOf<DownloadQueueItem>() }
    val controls = remember { ConcurrentHashMap<String, DownloadControl>() }
    var queueWorkerActive by remember { mutableStateOf(false) }
    var language by remember {
        mutableStateOf(if (preferences.get("language", "en") == "ar") "ar" else "en")
    }
    val strings = if (language == "ar") arabic else english
    fun tr(key: String) = strings[key] ?: english.getValue(key)

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
    var status by remember { mutableStateOf(tr("pasteLink")) }
    var error by remember { mutableStateOf(false) }
    var loadingFormats by remember { mutableStateOf(false) }

    var update by remember { mutableStateOf<DesktopUpdate?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var checkingUpdates by remember { mutableStateOf(false) }
    var installingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableStateOf<Float?>(null) }
    var updateStatus by remember { mutableStateOf("Checking for updates…") }
    var updateError by remember { mutableStateOf(false) }

    fun checkForUpdates(manual: Boolean) {
        if (checkingUpdates) return
        checkingUpdates = true
        updateError = false
        updateStatus = "Checking for updates…"
        scope.launch {
            try {
                val available = withContext(Dispatchers.IO) { AppUpdater.checkForUpdate() }
                update = available
                if (available == null) {
                    updateStatus = "${tr("upToDate")} (v${AppUpdater.currentVersion})."
                } else {
                    updateStatus = "${tr("updateAvailable")} (${available.version})"
                    val snoozedUntil = preferences.getLong("updateReminderUntil", 0L)
                    if (manual || System.currentTimeMillis() >= snoozedUntil) {
                        showUpdateDialog = true
                    } else {
                        delay(snoozedUntil - System.currentTimeMillis())
                        showUpdateDialog = true
                    }
                }
            } catch (exception: Exception) {
                updateError = true
                updateStatus = exception.message ?: tr("updateCheckFailed")
            } finally {
                checkingUpdates = false
            }
        }
    }

    fun setQueueItem(id: String, transform: (DownloadQueueItem) -> DownloadQueueItem) {
        val index = queue.indexOfFirst { it.id == id }
        if (index >= 0) queue[index] = transform(queue[index])
    }

    fun startQueue() {
        if (queueWorkerActive) return
        if (queue.none { it.status == QueueStatus.WAITING }) {
            status = tr("nothingToStart")
            error = true
            return
        }
        queueWorkerActive = true
        status = tr("startQueued")
        error = false
        scope.launch {
            try {
                while (true) {
                    val item = queue.firstOrNull { it.status == QueueStatus.WAITING } ?: break
                    val control = DownloadControl()
                    controls[item.id] = control
                    setQueueItem(item.id) {
                        it.copy(status = QueueStatus.DOWNLOADING, progress = 0f, message = "")
                    }
                    try {
                        withContext(Dispatchers.IO) {
                            YtDlpClient(executable.trim()).download(
                                url = item.url,
                                outputDirectory = item.outputDirectory,
                                maxHeight = item.maxHeight,
                                audioFormat = item.audioFormat,
                                audioQuality = item.audioQuality,
                                control = control,
                            ) { line ->
                                val percent =
                                    Regex("""(\d+(?:\.\d+)?)%""").find(line)
                                        ?.groupValues?.getOrNull(1)?.toFloatOrNull()
                                scope.launch(Dispatchers.Main) {
                                    setQueueItem(item.id) { current ->
                                        if (current.status == QueueStatus.DOWNLOADING) {
                                            current.copy(
                                                progress = percent?.div(100f) ?: current.progress,
                                                message = line.take(180),
                                            )
                                        } else current
                                    }
                                }
                            }
                        }
                        setQueueItem(item.id) {
                            it.copy(status = QueueStatus.COMPLETE, progress = 1f, message = "")
                        }
                    } catch (_: DownloadPausedException) {
                        setQueueItem(item.id) {
                            if (it.status == QueueStatus.DOWNLOADING) {
                                it.copy(status = QueueStatus.PAUSED, message = "")
                            } else it
                        }
                    } catch (_: DownloadCancelledException) {
                        setQueueItem(item.id) {
                            it.copy(status = QueueStatus.CANCELLED, message = "")
                        }
                    } catch (exception: Exception) {
                        setQueueItem(item.id) {
                            it.copy(status = QueueStatus.FAILED, message = exception.message.orEmpty())
                        }
                    } finally {
                        controls.remove(item.id)
                    }
                }
            } finally {
                queueWorkerActive = false
            }
        }
    }

    fun addCurrentVideoToQueue() {
        val video = details ?: return
        val outputFolder = File(downloadDirectory)
        if (!outputFolder.isDirectory) {
            status = "Choose an existing download folder."
            error = true
            return
        }
        queue.add(
            DownloadQueueItem(
                id = UUID.randomUUID().toString(),
                url = url.trim(),
                title = video.title.ifBlank { url.trim() },
                outputDirectory = outputFolder,
                maxHeight = if (audioOnly) null else maxHeight,
                audioFormat = if (audioOnly) audioFormat else null,
                audioQuality =
                    if (audioQuality == "Best") "0"
                    else audioQuality.substringBefore(" kbps") + "K",
            ),
        )
        status = if (language == "ar") "تمت إضافة الفيديو إلى قائمة التنزيل." else "Video added to the download queue."
        error = false
        url = ""
        details = null
    }

    LaunchedEffect(Unit) { checkForUpdates(manual = false) }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides
            if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
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
                        Column(Modifier.weight(1f)) {
                            Text("Seal Desktop", style = MaterialTheme.typography.headlineMedium)
                            Text(tr("tagline"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(tr("language"))
                        Spacer(Modifier.width(8.dp))
                        ChoiceMenu(
                            selected = if (language == "ar") "العربية" else "English",
                            options = listOf("English", "العربية"),
                            enabled = true,
                            onSelected = {
                                language = if (it == "العربية") "ar" else "en"
                                preferences.put("language", language)
                                status = if (details == null) tr("pasteLink") else tr("formatsLoaded")
                            },
                        )
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
                            Text(tr("videoLink"), style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("youtubeUrl")) },
                                placeholder = { Text("https://www.youtube.com/watch?v=…") },
                                singleLine = true,
                                enabled = !loadingFormats,
                            )
                            Button(
                                onClick = {
                                    loadingFormats = true
                                    error = false
                                    status = tr("loading")
                                    scope.launch {
                                        try {
                                            val result =
                                                withContext(Dispatchers.IO) {
                                                    YtDlpClient(executable.trim()).fetchVideoDetails(url)
                                                }
                                            details = result
                                            maxHeight =
                                                result.formats.filter { it.hasVideo() }
                                                    .mapNotNull { it.height }.distinct().maxOrNull()
                                            status = tr("formatsLoaded")
                                        } catch (exception: Exception) {
                                            details = null
                                            error = true
                                            status = exception.message ?: tr("updateCheckFailed")
                                        } finally {
                                            loadingFormats = false
                                        }
                                    }
                                },
                                enabled = !loadingFormats && url.isNotBlank(),
                            ) {
                                Text(if (loadingFormats) tr("loading") else tr("loadFormats"))
                            }
                        }
                    }

                    details?.let { video ->
                        Card {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(tr("videoOptions"), style = MaterialTheme.typography.titleMedium)
                                Text(video.title, style = MaterialTheme.typography.titleLarge)
                                video.uploader?.let {
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("type"), modifier = Modifier.width(100.dp))
                                    ChoiceMenu(
                                        selected = if (audioOnly) tr("audioOnly") else tr("videoAudio"),
                                        options = listOf(tr("videoAudio"), tr("audioOnly")),
                                        enabled = true,
                                        onSelected = { audioOnly = it == tr("audioOnly") },
                                    )
                                }
                                if (audioOnly) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("audio"), modifier = Modifier.width(100.dp))
                                        ChoiceMenu(
                                            selected = audioFormat.uppercase(),
                                            options = audioFormats.map(String::uppercase),
                                            enabled = true,
                                            onSelected = { audioFormat = it.lowercase() },
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        ChoiceMenu(
                                            selected = audioQuality,
                                            options = audioQualities,
                                            enabled = true,
                                            onSelected = { audioQuality = it },
                                        )
                                    }
                                } else {
                                    val heights =
                                        video.formats.filter { it.hasVideo() }
                                            .mapNotNull { it.height }.distinct().sortedDescending()
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("quality"), modifier = Modifier.width(100.dp))
                                        ChoiceMenu(
                                            selected =
                                                maxHeight?.let { "$it p ${tr("orLower")}" } ?: tr("best"),
                                            options = listOf(tr("best")) + heights.map { "$it p ${tr("orLower")}" },
                                            enabled = true,
                                            onSelected = {
                                                maxHeight =
                                                    if (it == tr("best")) null
                                                    else it.substringBefore(" p").toIntOrNull()
                                            },
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("saveTo"), modifier = Modifier.width(100.dp))
                                    Text(
                                        downloadDirectory,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    TextButton(
                                        onClick = {
                                            chooseDirectory(downloadDirectory)?.let {
                                                downloadDirectory = it.absolutePath
                                            }
                                        },
                                    ) {
                                        Text(tr("browse"))
                                    }
                                }
                                Button(
                                    enabled = url.isNotBlank() && video.title.isNotBlank(),
                                    onClick = ::addCurrentVideoToQueue,
                                ) {
                                    Text(tr("addToQueue"))
                                }
                            }
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${tr("queueTitle")} (${queue.size})",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Button(
                                    enabled = !queueWorkerActive && queue.any { it.status == QueueStatus.WAITING },
                                    onClick = ::startQueue,
                                ) {
                                    Text(tr("startDownloads"))
                                }
                            }
                            if (queue.isEmpty()) {
                                Text(tr("queueEmpty"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            queue.forEach { item ->
                                QueueItemRow(
                                    item = item,
                                    strings = strings,
                                    onPause = {
                                        controls[item.id]?.pause()
                                        setQueueItem(item.id) {
                                            it.copy(status = QueueStatus.PAUSED, message = "")
                                        }
                                    },
                                    onResume = {
                                        setQueueItem(item.id) {
                                            it.copy(status = QueueStatus.WAITING, message = "")
                                        }
                                        startQueue()
                                    },
                                    onRemove = {
                                        controls.remove(item.id)?.cancel()
                                        queue.removeAll { it.id == item.id }
                                    },
                                )
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
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(tr("downloader"), style = MaterialTheme.typography.titleMedium)
                            Text(tr("downloaderDescription"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = executable,
                                    onValueChange = { executable = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(tr("executable")) },
                                    placeholder = { Text("yt-dlp or C:\\tools\\yt-dlp.exe") },
                                    singleLine = true,
                                )
                                Spacer(Modifier.width(10.dp))
                                TextButton(
                                    onClick = {
                                        chooseExecutable(executable)?.let { executable = it.absolutePath }
                                    },
                                ) {
                                    Text(tr("browse"))
                                }
                            }
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(tr("appUpdates"), style = MaterialTheme.typography.titleMedium)
                                TextButton(
                                    enabled = !checkingUpdates && !installingUpdate,
                                    onClick = { checkForUpdates(manual = true) },
                                ) {
                                    Text(if (checkingUpdates) tr("checking") else tr("checkUpdates"))
                                }
                            }
                            Text(
                                updateStatus,
                                color =
                                    if (updateError) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            updateProgress?.let {
                                LinearProgressIndicator(
                                    progress = { it.coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    Text(
                        tr("legal"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = if (language == "ar") TextAlign.Right else TextAlign.Left,
                    )
                }
            }

            update?.takeIf { showUpdateDialog }?.let { availableUpdate ->
                AlertDialog(
                    onDismissRequest = {
                        preferences.putLong(
                            "updateReminderUntil",
                            System.currentTimeMillis() + UPDATE_REMINDER_MILLIS,
                        )
                        showUpdateDialog = false
                    },
                    title = { Text("Seal Desktop ${availableUpdate.version} is available") },
                    text = { Text(tr("updateDialog")) },
                    confirmButton = {
                        Button(
                            enabled = !installingUpdate,
                            onClick = {
                                installingUpdate = true
                                updateProgress = 0f
                                updateError = false
                                updateStatus = tr("downloadingUpdate")
                                scope.launch {
                                    try {
                                        val installer =
                                            withContext(Dispatchers.IO) {
                                                AppUpdater.downloadInstaller(availableUpdate) {
                                                    updateProgress = it
                                                }
                                            }
                                        updateStatus = tr("openingInstaller")
                                        ProcessBuilder("msiexec.exe", "/i", installer.absolutePath).start()
                                        onExit()
                                    } catch (exception: Exception) {
                                        installingUpdate = false
                                        updateError = true
                                        updateStatus = exception.message ?: tr("updateFailed")
                                        showUpdateDialog = false
                                    }
                                }
                            },
                        ) {
                            Text(if (installingUpdate) tr("loading") else tr("install"))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !installingUpdate,
                            onClick = {
                                preferences.putLong(
                                    "updateReminderUntil",
                                    System.currentTimeMillis() + UPDATE_REMINDER_MILLIS,
                                )
                                showUpdateDialog = false
                            },
                        ) {
                            Text(tr("remindLater"))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    item: DownloadQueueItem,
    strings: Map<String, String>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    val statusText =
        when (item.status) {
            QueueStatus.WAITING -> strings.getValue("waiting")
            QueueStatus.DOWNLOADING -> strings.getValue("downloading")
            QueueStatus.PAUSED -> strings.getValue("paused")
            QueueStatus.COMPLETE -> strings.getValue("complete")
            QueueStatus.FAILED -> strings.getValue("failed")
            QueueStatus.CANCELLED -> strings.getValue("cancelled")
        }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Text(
                "$statusText · ${if (item.audioFormat == null) strings.getValue("videoAudio") else strings.getValue("audioOnly")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.status == QueueStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { item.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (item.message.isNotBlank()) {
                    Text(item.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (item.status == QueueStatus.FAILED && item.message.isNotBlank()) {
                Text(item.message, color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.status) {
                    QueueStatus.DOWNLOADING -> TextButton(onClick = onPause) {
                        Text(strings.getValue("pause"))
                    }
                    QueueStatus.PAUSED -> TextButton(onClick = onResume) {
                        Text(strings.getValue("resume"))
                    }
                    else -> Unit
                }
                TextButton(onClick = onRemove) { Text(strings.getValue("remove")) }
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
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun chooseExecutable(currentPath: String): File? {
    val initialDirectory =
        File(currentPath).takeIf { it.isDirectory } ?: File(System.getProperty("user.home"))
    val chooser = JFileChooser(initialDirectory).apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        dialogTitle = "Select yt-dlp executable"
        fileFilter = FileNameExtensionFilter("yt-dlp executable (*.exe)", "exe")
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}
