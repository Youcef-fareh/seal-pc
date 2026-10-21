package com.junkfood.seal.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.Window as AwtWindow
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private val accent = Color(0xFFE53935)
private val lightColors =
    lightColorScheme(
        primary = Color(0xFFC62828),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF5F5E5E),
        onSecondary = Color.White,
        background = Color(0xFFF6F7F9),
        surface = Color.White,
        surfaceVariant = Color(0xFFEBEEF2),
        onSurface = Color(0xFF191B1F),
        onSurfaceVariant = Color(0xFF41464D),
        outline = Color(0xFF73777F),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
    )
private val highContrastColors =
    darkColorScheme(
        primary = Color(0xFFFFFF00),
        onPrimary = Color.Black,
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF101010),
        onSurface = Color.White,
        onSurfaceVariant = Color.White,
        error = Color(0xFFFF8080),
        onError = Color.Black,
    )
private val audioFormats = listOf("mp3", "m4a", "opus", "wav")
private val audioQualities = listOf("Best", "320 kbps", "256 kbps", "192 kbps", "128 kbps")
private const val UPDATE_REMINDER_MILLIS = 24L * 60 * 60 * 1000

private val english = mapOf(
    "tagline" to "Video and audio downloads, made simple.",
    "language" to "Language",
    "english" to "English",
    "arabic" to "العربية",
    "theme" to "Theme",
    "system" to "System",
    "light" to "Light",
    "dark" to "Dark",
    "highContrast" to "High contrast",
    "settings" to "Settings",
    "home" to "Downloads",
    "appearance" to "Appearance and language",
    "downloadSettings" to "Download preferences",
    "downloadOptions" to "Download behavior and file naming",
    "subtitleSettings" to "Subtitle options",
    "networkSettings" to "Network and authentication",
    "additionalSettings" to "Other download options",
    "automationSettings" to "Automation and scheduling",
    "concurrentDownloads" to "Simultaneous downloads",
    "outputTemplate" to "Output template",
    "rateLimit" to "Speed limit (e.g. 1M)",
    "proxy" to "Proxy URL (optional)",
    "thumbnail" to "Save thumbnail",
    "embedThumbnail" to "Embed thumbnail in audio files",
    "metadata" to "Embed metadata",
    "chapters" to "Embed chapters",
    "clipboardMonitoring" to "Watch clipboard for video links",
    "browserBridge" to "Enable authenticated browser extension bridge",
    "browserBridgeToken" to "Extension bridge token",
    "copyToken" to "Copy token",
    "tokenCopied" to "Bridge token copied to clipboard.",
    "bridgeStopped" to "Browser extension bridge stopped.",
    "cookiesImported" to "Browser cookies imported.",
    "invalidCookies" to "Choose a readable Netscape-format cookies.txt file.",
    "playlistVideos" to "Playlist videos",
    "selectAll" to "Select all",
    "selectNone" to "Select none",
    "addSelected" to "Add selected videos to queue",
    "aria2cPath" to "aria2c executable (optional)",
    "cookiesFile" to "Browser cookies file (Netscape format)",
    "chooseCookies" to "Import cookies file",
    "clearCookies" to "Clear cookies",
    "cookieWarning" to "Cookie files can grant account access. Keep the file private and do not share it.",
    "sponsorBlock" to "Remove SponsorBlock segments",
    "sponsorBlockCategories" to "Categories (comma-separated)",
    "customArguments" to "Advanced yt-dlp arguments (one argument per line)",
    "customArgumentsHelp" to "Allowed options: format, date/size/title filters, playlist items, extractor args. Manage subtitle options above.",
    "useClipboardLink" to "Use detected link",
    "invalidDrop" to "Drop a valid HTTP or HTTPS video URL.",
    "subtitleSource" to "Subtitles",
    "subtitleOff" to "Off",
    "subtitleManual" to "Uploaded",
    "subtitleAuto" to "Automatic captions",
    "subtitleBoth" to "Uploaded + automatic",
    "subtitleLanguages" to "Subtitle languages",
    "subtitleLanguageHelp" to "Choose a preset or enter comma-separated codes: en, ar, fr, or all.",
    "subtitleLanguageEnglish" to "English (en)",
    "subtitleLanguageArabic" to "Arabic (ar)",
    "subtitleLanguageFrench" to "French (fr)",
    "subtitleLanguageAll" to "All languages",
    "subtitleLanguageCustom" to "Custom codes",
    "subtitleEmbed" to "Embed subtitles in video",
    "subtitleFormat" to "Subtitle format",
    "subtitleAutoTranslated" to "Include auto-translated captions",
    "subtitleAutoTranslatedHelp" to "Download platform machine-translated captions (for example YouTube translations). Turn off to keep only the original-language automatic captions.",
    "subtitleHelp" to "Automatic captions are downloaded when the platform provides them. Audio-only downloads save subtitles as sidecar files.",
    "schedule" to "Schedule queue (YYYY-MM-DD HH:MM)",
    "scheduleQueue" to "Schedule",
    "cancelSchedule" to "Cancel schedule",
    "queueScheduled" to "Queue scheduled.",
    "storage" to "Storage and statistics",
    "scanStorage" to "Scan download folders",
    "cleanupTemp" to "Find old partial files",
    "cleanupConfirm" to "Delete old partial files found in your download folders?",
    "schedulerNote" to "Keep the app running until the scheduled time.",
    "cleanup" to "Delete selected temporary files",
    "cancel" to "Cancel",
    "backup" to "Export settings",
    "restore" to "Import settings",
    "historyCount" to "History items",
    "downloadsSize" to "Downloaded size (estimate)",
    "moveUp" to "Move up",
    "moveDown" to "Move down",
    "videoLink" to "1. Video link",
    "youtubeUrl" to "Video or playlist URL",
    "loadFormats" to "Load formats",
    "loading" to "Loading…",
    "videoOptions" to "2. Download options",
    "type" to "Type:",
    "audioOnly" to "Audio only",
    "videoAudio" to "Video + audio",
    "subtitlesOnly" to "Subtitles only",
    "subtitlesOnlyHelp" to "Downloads subtitles only, using the source, languages, and format chosen below.",
    "audio" to "Audio:",
    "quality" to "Quality:",
    "automaticFormat" to "Automatic format",
    "estimatedSize" to "Estimated size:",
    "sizeUnavailable" to "Unavailable",
    "best" to "Best available",
    "orLower" to "or lower",
    "saveTo" to "Save to:",
    "browse" to "Browse…",
    "addToQueue" to "Add to download queue",
    "queueTitle" to "Download queue",
    "queueEmpty" to "Your download queue is empty. Add video links and choose their settings above.",
    "historyTitle" to "Download history",
    "historyEmpty" to "Completed, failed, and cancelled downloads will appear here.",
    "retry" to "Retry",
    "retryAdded" to "Download added to the queue to retry.",
    "openDiagnostics" to "Open diagnostics log",
    "historyLoadFailed" to "Could not load download history.",
    "notificationComplete" to "Download complete",
    "notificationFailed" to "Download failed",
    "notificationCancelled" to "Download cancelled",
    "notificationUnavailable" to "System tray notifications are unavailable on this system.",
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
    "downloaderDescription" to "Windows installers include yt-dlp, Deno, and FFmpeg. Running from source requires them installed.",
    "executable" to "yt-dlp command or executable path",
    "appUpdates" to "App updates",
    "checking" to "Checking…",
    "checkUpdates" to "Check for updates",
    "pasteLink" to "Paste a YouTube link to get started.",
    "formatsLoaded" to "Formats loaded. Choose the download settings, then add this video to the queue.",
    "startQueued" to "Downloading queued videos…",
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
    "theme" to "المظهر",
    "system" to "النظام",
    "light" to "فاتح",
    "dark" to "داكن",
    "highContrast" to "تباين عالٍ",
    "settings" to "الإعدادات",
    "home" to "التنزيلات",
    "appearance" to "المظهر واللغة",
    "downloadSettings" to "تفضيلات التنزيل",
    "downloadOptions" to "سلوك التنزيل وأسماء الملفات",
    "subtitleSettings" to "خيارات الترجمة",
    "networkSettings" to "الشبكة والمصادقة",
    "additionalSettings" to "خيارات تنزيل أخرى",
    "automationSettings" to "الأتمتة والجدولة",
    "concurrentDownloads" to "التنزيلات المتزامنة",
    "outputTemplate" to "قالب اسم الملف",
    "rateLimit" to "حد السرعة (مثل 1M)",
    "proxy" to "عنوان الوكيل (اختياري)",
    "thumbnail" to "حفظ الصورة المصغرة",
    "embedThumbnail" to "تضمين الصورة المصغرة في الملفات الصوتية",
    "metadata" to "تضمين البيانات الوصفية",
    "chapters" to "تضمين الفصول",
    "clipboardMonitoring" to "مراقبة الحافظة لروابط الفيديو",
    "browserBridge" to "تفعيل جسر إضافة المتصفح الموثق",
    "browserBridgeToken" to "رمز جسر الإضافة",
    "copyToken" to "نسخ الرمز",
    "tokenCopied" to "تم نسخ رمز الجسر إلى الحافظة.",
    "bridgeStopped" to "تم إيقاف جسر إضافة المتصفح.",
    "cookiesImported" to "تم استيراد ملفات تعريف الارتباط.",
    "invalidCookies" to "اختر ملف cookies.txt صالحًا بتنسيق Netscape.",
    "playlistVideos" to "فيديوهات قائمة التشغيل",
    "selectAll" to "تحديد الكل",
    "selectNone" to "إلغاء التحديد",
    "addSelected" to "إضافة الفيديوهات المحددة إلى القائمة",
    "aria2cPath" to "مسار aria2c (اختياري)",
    "cookiesFile" to "ملف ملفات تعريف الارتباط (تنسيق Netscape)",
    "chooseCookies" to "استيراد ملف ملفات الارتباط",
    "clearCookies" to "مسح ملفات الارتباط",
    "cookieWarning" to "قد يتيح ملف ملفات الارتباط الوصول إلى حسابك. احتفظ به خاصًا ولا تشاركه.",
    "sponsorBlock" to "إزالة مقاطع SponsorBlock",
    "sponsorBlockCategories" to "الفئات (مفصولة بفواصل)",
    "customArguments" to "وسائط yt-dlp متقدمة (وسيط واحد في كل سطر)",
    "customArgumentsHelp" to "الخيارات المسموحة: التنسيق، مرشحات التاريخ/الحجم/العنوان، عناصر القائمة، ووسائط المستخرج. اضبط خيارات الترجمة أعلاه.",
    "useClipboardLink" to "استخدام الرابط المكتشف",
    "invalidDrop" to "أفلت رابط فيديو صالحًا يبدأ بـ HTTP أو HTTPS.",
    "subtitleSource" to "الترجمة",
    "subtitleOff" to "إيقاف",
    "subtitleManual" to "مرفوعة",
    "subtitleAuto" to "تلقائية",
    "subtitleBoth" to "مرفوعة وتلقائية",
    "subtitleLanguages" to "لغات الترجمة",
    "subtitleLanguageHelp" to "اختر إعدادًا أو أدخل رموزًا مفصولة بفواصل: en أو ar أو fr أو all.",
    "subtitleLanguageEnglish" to "الإنجليزية (en)",
    "subtitleLanguageArabic" to "العربية (ar)",
    "subtitleLanguageFrench" to "الفرنسية (fr)",
    "subtitleLanguageAll" to "كل اللغات",
    "subtitleLanguageCustom" to "رموز مخصصة",
    "subtitleEmbed" to "تضمين الترجمة في الفيديو",
    "subtitleFormat" to "تنسيق الترجمة",
    "subtitleAutoTranslated" to "تضمين الترجمات المترجمة آليًا",
    "subtitleAutoTranslatedHelp" to "ينزّل الترجمات التي تترجمها المنصة آليًا (مثل ترجمات يوتيوب). أوقفه للاحتفاظ بالترجمات التلقائية بلغة الأصل فقط.",
    "subtitleHelp" to "يتم تنزيل الترجمة التلقائية عند توفرها. تُحفظ الترجمات كملفات جانبية عند تنزيل الصوت فقط.",
    "schedule" to "جدولة القائمة (YYYY-MM-DD HH:MM)",
    "scheduleQueue" to "جدولة",
    "cancelSchedule" to "إلغاء الجدولة",
    "queueScheduled" to "تمت جدولة القائمة.",
    "storage" to "التخزين والإحصاءات",
    "scanStorage" to "فحص مجلدات التنزيل",
    "cleanupTemp" to "البحث عن الملفات الجزئية القديمة",
    "cleanupConfirm" to "هل تريد حذف الملفات الجزئية القديمة الموجودة في مجلدات التنزيل؟",
    "schedulerNote" to "اترك التطبيق قيد التشغيل حتى موعد الجدولة.",
    "cleanup" to "حذف الملفات المؤقتة المحددة",
    "cancel" to "إلغاء",
    "backup" to "تصدير الإعدادات",
    "restore" to "استيراد الإعدادات",
    "historyCount" to "عناصر السجل",
    "downloadsSize" to "حجم التنزيلات (تقديري)",
    "moveUp" to "تحريك لأعلى",
    "moveDown" to "تحريك لأسفل",
    "videoLink" to "١. رابط الفيديو",
    "youtubeUrl" to "رابط فيديو أو قائمة تشغيل",
    "loadFormats" to "تحميل الجودات",
    "loading" to "جارٍ التحميل…",
    "videoOptions" to "٢. إعدادات التنزيل",
    "type" to "النوع:",
    "audioOnly" to "صوت فقط",
    "videoAudio" to "فيديو وصوت",
    "subtitlesOnly" to "الترجمات فقط",
    "subtitlesOnlyHelp" to "ينزّل الترجمات فقط، باستخدام المصدر واللغات والتنسيق المحددين أدناه.",
    "audio" to "الصوت:",
    "quality" to "الجودة:",
    "automaticFormat" to "التنسيق التلقائي",
    "estimatedSize" to "الحجم التقديري:",
    "sizeUnavailable" to "غير متاح",
    "best" to "أفضل جودة متاحة",
    "orLower" to "أو أقل",
    "saveTo" to "الحفظ في:",
    "browse" to "استعراض…",
    "addToQueue" to "إضافة إلى قائمة التنزيل",
    "queueTitle" to "قائمة التنزيل",
    "queueEmpty" to "قائمة التنزيل فارغة. أضف روابط الفيديو واختر إعداداتها أعلاه.",
    "historyTitle" to "سجل التنزيلات",
    "historyEmpty" to "ستظهر هنا التنزيلات المكتملة والفاشلة والملغاة.",
    "retry" to "إعادة المحاولة",
    "retryAdded" to "تمت إضافة التنزيل إلى قائمة الانتظار لإعادة المحاولة.",
    "openDiagnostics" to "فتح سجل التشخيص",
    "historyLoadFailed" to "تعذر تحميل سجل التنزيلات.",
    "notificationComplete" to "اكتمل التنزيل",
    "notificationFailed" to "فشل التنزيل",
    "notificationCancelled" to "تم إلغاء التنزيل",
    "notificationUnavailable" to "إشعارات منطقة النظام غير متاحة على هذا النظام.",
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
    "downloaderDescription" to "تتضمن نسخة ويندوز المثبتة أدوات yt-dlp وDeno وFFmpeg. يتطلب التشغيل من المصدر تثبيتها.",
    "executable" to "أمر yt-dlp أو مسار البرنامج",
    "appUpdates" to "تحديثات التطبيق",
    "checking" to "جارٍ التحقق…",
    "checkUpdates" to "التحقق من التحديثات",
    "pasteLink" to "الصق رابط يوتيوب للبدء.",
    "formatsLoaded" to "تم تحميل الجودات. اختر الإعدادات ثم أضف الفيديو إلى القائمة.",
    "startQueued" to "جارٍ تنزيل الفيديوهات في القائمة…",
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
    val exit = {
        TrayNotifications.remove()
        exitApplication()
    }
    Window(onCloseRequest = exit, title = "Seal Desktop") {
        SealDesktopApp(onExit = exit)
    }
}

@Composable
private fun SealDesktopApp(onExit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val preferences = remember { Preferences.userNodeForPackage(AppUpdater::class.java) }
    val historyStore = remember { DownloadHistoryStore() }
    val queue = remember { mutableStateListOf<DownloadQueueItem>() }
    val history = remember { mutableStateListOf<DownloadHistoryEntry>() }
    val controls = remember { ConcurrentHashMap<String, DownloadControl>() }
    var queueWorkerActive by remember { mutableStateOf(false) }
    var language by remember {
        mutableStateOf(if (preferences.get("language", "en") == "ar") "ar" else "en")
    }
    var theme by remember {
        mutableStateOf(preferences.get("theme", "system").takeIf { it in setOf("system", "light", "dark", "high-contrast") } ?: "system")
    }
    var showSettings by remember { mutableStateOf(false) }
    var downloadConcurrency by remember { mutableStateOf(preferences.getInt("downloadConcurrency", 1).coerceIn(1, 4)) }
    var outputTemplate by remember {
        mutableStateOf(preferences.get("outputTemplate", "%(title).200B [%(id)s].%(ext)s"))
    }
    var rateLimit by remember { mutableStateOf(preferences.get("rateLimit", "")) }
    var proxy by remember { mutableStateOf(preferences.get("proxy", "")) }
    var writeThumbnail by remember { mutableStateOf(preferences.getBoolean("writeThumbnail", false)) }
    var embedThumbnail by remember { mutableStateOf(preferences.getBoolean("embedThumbnail", false)) }
    var embedMetadata by remember { mutableStateOf(preferences.getBoolean("embedMetadata", false)) }
    var embedChapters by remember { mutableStateOf(preferences.getBoolean("embedChapters", false)) }
    var clipboardMonitoring by remember { mutableStateOf(preferences.getBoolean("clipboardMonitoring", false)) }
    var aria2cExecutable by remember { mutableStateOf(preferences.get("aria2cExecutable", "")) }
    var cookiesFile by remember { mutableStateOf(preferences.get("cookiesFile", "")) }
    var sponsorBlockCategories by remember {
        mutableStateOf(preferences.get("sponsorBlockCategories", "").split(',').map(String::trim).filter(String::isNotEmpty))
    }
    var customArguments by remember { mutableStateOf(preferences.get("customArguments", "")) }
    var subtitleSource by remember {
        mutableStateOf(preferences.get("subtitleSource", "off").takeIf { it in setOf("off", "manual", "auto", "both") } ?: "off")
    }
    var subtitleLanguages by remember { mutableStateOf(preferences.get("subtitleLanguages", "en")) }
    var embedSubtitles by remember { mutableStateOf(preferences.getBoolean("embedSubtitles", false)) }
    var subtitleFormat by remember {
        mutableStateOf(preferences.get("subtitleFormat", "srt").takeIf { it in setOf("srt", "vtt") } ?: "srt")
    }
    var autoTranslatedSubtitles by remember {
        mutableStateOf(preferences.getBoolean("autoTranslatedSubtitles", true))
    }
    var browserBridgeEnabled by remember { mutableStateOf(false) }
    var browserBridgeToken by remember {
        mutableStateOf(preferences.get("browserBridgeToken", "").ifBlank { createBrowserBridgeToken() })
    }
    var browserBridge by remember { mutableStateOf<BrowserExtensionBridge?>(null) }
    val browserRequests = remember { Channel<Pair<String, CompletableDeferred<Unit>>>(Channel.UNLIMITED) }
    var scheduleAt by remember { mutableStateOf("") }
    var scheduledJob by remember { mutableStateOf<Job?>(null) }
    var scheduledStartMillis by remember { mutableStateOf(0L) }
    val strings = if (language == "ar") arabic else english
    fun tr(key: String) = strings[key] ?: english.getValue(key)
    fun subtitleSourceLabel(source: String) =
        when (source) {
            "manual" -> tr("subtitleManual")
            "auto" -> tr("subtitleAuto")
            "both" -> tr("subtitleBoth")
            else -> tr("subtitleOff")
        }

    var url by remember { mutableStateOf("") }
    var executable by remember { mutableStateOf(preferences.get("executable", "yt-dlp")) }
    var downloadDirectory by remember {
        mutableStateOf(
            preferences.get(
                "downloadDirectory",
                File(System.getProperty("user.home"), "Downloads").absolutePath,
            ),
        )
    }
    var details by remember { mutableStateOf<VideoDetails?>(null) }
    var playlist by remember { mutableStateOf<PlaylistDetails?>(null) }
    val selectedPlaylistEntries = remember { mutableStateListOf<String>() }
    var maxHeight by remember { mutableStateOf<Int?>(null) }
    var selectedFormatSelector by remember { mutableStateOf<String?>(null) }
    var detectedClipboardUrl by remember { mutableStateOf("") }
    var lastClipboardText by remember { mutableStateOf("") }
    var audioOnly by remember { mutableStateOf(preferences.getBoolean("audioOnly", false)) }
    var subtitlesOnly by remember { mutableStateOf(preferences.getBoolean("subtitlesOnly", false)) }
    var audioFormat by remember {
        mutableStateOf(preferences.get("audioFormat", "mp3").takeIf { it in audioFormats } ?: "mp3")
    }
    var audioQuality by remember {
        mutableStateOf(preferences.get("audioQuality", "Best").takeIf { it in audioQualities } ?: "Best")
    }
    var status by remember { mutableStateOf(tr("pasteLink")) }
    var error by remember { mutableStateOf(false) }
    var loadingFormats by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf("") }
    var diagnosticsError by remember { mutableStateOf("") }
    var historyLoaded by remember { mutableStateOf(false) }
    var historyReady by remember { mutableStateOf(false) }
    var storageSizeBytes by remember { mutableStateOf<Long?>(null) }
    var removableTempFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showCleanupConfirmation by remember { mutableStateOf(false) }

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
                try {
                    DiagnosticLog.append("Update check failed", updateStatus)
                } catch (logException: Exception) {
                    updateStatus += " Diagnostics log failed: ${logException.message.orEmpty()}"
                }
            } finally {
                checkingUpdates = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            history.addAll(withContext(Dispatchers.IO) { historyStore.load() })
            historyLoaded = true
            historyReady = true
        } catch (exception: Exception) {
            historyError = exception.message ?: tr("historyLoadFailed")
            status = "${tr("historyLoadFailed")} $historyError"
            error = true
            try {
                DiagnosticLog.append("History load failed", historyError)
            } catch (logException: Exception) {
                diagnosticsError = "Diagnostics log also failed: ${logException.message.orEmpty()}"
            }
            historyReady = true
        }
        try {
            DiagnosticLog.append("Application started")
        } catch (exception: Exception) {
            diagnosticsError = "Could not write diagnostics log: ${exception.message.orEmpty()}"
            status = diagnosticsError
            error = true
        }
        checkForUpdates(manual = false)
    }

    LaunchedEffect(clipboardMonitoring) {
        if (!clipboardMonitoring) return@LaunchedEffect
        while (true) {
            try {
                val clipboardText =
                    withContext(Dispatchers.IO) {
                        Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
                    }?.trim()
                if (!clipboardText.isNullOrBlank() && clipboardText != lastClipboardText) {
                    lastClipboardText = clipboardText
                    try {
                        requireHttpUrl(clipboardText)
                        detectedClipboardUrl = clipboardText
                    } catch (_: IllegalArgumentException) {
                        detectedClipboardUrl = ""
                    }
                }
            } catch (exception: Exception) {
                status = "Could not read the clipboard: ${exception.message.orEmpty()}"
                error = true
                return@LaunchedEffect
            }
            delay(1500)
        }
    }

    DisposableEffect(Unit) {
        val awtWindow = AwtWindow.getWindows().firstOrNull { it.isShowing }
        val previousDropTarget = awtWindow?.dropTarget
        val dropTarget =
            awtWindow?.let { window ->
                DropTarget(
                    window,
                    java.awt.dnd.DnDConstants.ACTION_COPY,
                    object : DropTargetAdapter() {
                        override fun drop(event: DropTargetDropEvent) {
                            val flavors = event.currentDataFlavors
                            val flavor =
                                flavors.firstOrNull { it.isMimeTypeEqual("text/uri-list") }
                                    ?: flavors.firstOrNull { it == DataFlavor.stringFlavor }
                                    ?: flavors.firstOrNull { it.isMimeTypeEqual("text/plain") }
                                    ?: flavors.firstOrNull {
                                        it.isFlavorTextType && !it.isMimeTypeEqual("text/html")
                                    }
                            if (flavor == null) {
                                event.rejectDrop()
                                return
                            }
                            event.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                            try {
                                val droppedText =
                                    flavor.getReaderForText(event.transferable).buffered().use { it.readText() }
                                val droppedUrl = parseDroppedUrl(droppedText)
                                if (droppedUrl != null) {
                                    SwingUtilities.invokeLater {
                                        url = droppedUrl
                                        details = null
                                        playlist = null
                                        selectedPlaylistEntries.clear()
                                        selectedFormatSelector = null
                                        detectedClipboardUrl = ""
                                        status = tr("pasteLink")
                                        error = false
                                    }
                                    event.dropComplete(true)
                                } else {
                                    SwingUtilities.invokeLater {
                                        status = tr("invalidDrop")
                                        error = true
                                    }
                                    event.dropComplete(false)
                                }
                            } catch (exception: Exception) {
                                SwingUtilities.invokeLater {
                                    status = "Could not read the dropped URL: ${exception.message.orEmpty()}"
                                    error = true
                                }
                                DiagnosticLog.append("URL drop failed", exception.message)
                                event.dropComplete(false)
                            }
                        }
                    },
                    true,
                )
            }
        if (awtWindow == null) {
            status = "URL drag-and-drop is unavailable on this window."
        }
        onDispose {
            if (awtWindow != null && dropTarget != null) {
                awtWindow.dropTarget = previousDropTarget
            }
        }
    }

    DisposableEffect(browserBridge) {
        val activeBridge = browserBridge
        onDispose { activeBridge?.close() }
    }

    DisposableEffect(browserRequests) {
        onDispose { browserRequests.close() }
    }

    fun setQueueItem(id: String, transform: (DownloadQueueItem) -> DownloadQueueItem) {
        val index = queue.indexOfFirst { it.id == id }
        if (index >= 0) queue[index] = transform(queue[index])
    }

    fun openDiagnosticsLog() {
        try {
            DiagnosticLog.append("Diagnostics log opened")
            check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                "Opening files is not supported on this system."
            }
            Desktop.getDesktop().open(DiagnosticLog.file)
        } catch (exception: Exception) {
            diagnosticsError = "Could not open diagnostics log: ${exception.message.orEmpty()}"
            status = diagnosticsError
            error = true
            try {
                DiagnosticLog.append("Could not open diagnostics log", exception.message)
            } catch (logException: Exception) {
                diagnosticsError += " Log write also failed: ${logException.message.orEmpty()}"
                status = diagnosticsError
            }
        }
    }

    fun setBrowserBridge(enabled: Boolean) {
        if (enabled == browserBridgeEnabled) return
        if (!enabled) {
            browserBridgeEnabled = false
            browserBridge = null
            status = tr("bridgeStopped")
            error = false
            return
        }
        try {
            preferences.put("browserBridgeToken", browserBridgeToken)
            val bridge =
                BrowserExtensionBridge(
                    token = browserBridgeToken,
                    onEnqueue = { requestedUrl ->
                        runBlocking {
                            val completed = CompletableDeferred<Unit>()
                            check(browserRequests.trySend(requestedUrl to completed).isSuccess) {
                                "The browser bridge is no longer accepting requests."
                            }
                            withTimeout(5000) { completed.await() }
                        }
                    },
                ).apply { start() }
            browserBridge = bridge
            browserBridgeEnabled = true
            status = "Browser extension bridge listening at ${bridge.address}."
            error = false
        } catch (exception: Exception) {
            browserBridge = null
            browserBridgeEnabled = false
            status = "Could not start the browser extension bridge: ${exception.message.orEmpty()}"
            error = true
        }
    }

    fun currentSettingsBackup() =
        AppSettingsBackup(
            language = language,
            theme = theme,
            executable = executable,
            downloadDirectory = downloadDirectory,
            audioOnly = audioOnly,
            audioFormat = audioFormat,
            audioQuality = audioQuality,
            outputTemplate = outputTemplate,
            downloadConcurrency = downloadConcurrency,
            rateLimit = rateLimit,
            proxy = proxy,
            writeThumbnail = writeThumbnail,
            embedThumbnail = embedThumbnail,
            embedMetadata = embedMetadata,
            embedChapters = embedChapters,
            clipboardMonitoring = clipboardMonitoring,
            aria2cExecutable = aria2cExecutable,
            sponsorBlockCategories = sponsorBlockCategories,
            customArguments = customArguments,
            subtitleSource = subtitleSource,
            subtitleLanguages = subtitleLanguages,
            embedSubtitles = embedSubtitles,
            subtitleFormat = subtitleFormat,
            subtitlesOnly = subtitlesOnly,
            autoTranslatedSubtitles = autoTranslatedSubtitles,
        )

    fun exportSettings() {
        val destination = chooseSettingsFile(save = true) ?: return
        try {
            currentSettingsBackup().write(destination)
            status = "Settings exported to ${destination.absolutePath}"
            error = false
        } catch (exception: Exception) {
            status = "Could not export settings: ${exception.message.orEmpty()}"
            error = true
            DiagnosticLog.append("Settings export failed", exception.message)
        }
    }

    fun importSettings() {
        val source = chooseSettingsFile(save = false) ?: return
        try {
            val backup = AppSettingsBackup.read(source)
            language = backup.language
            theme = backup.theme
            executable = backup.executable
            downloadDirectory = backup.downloadDirectory
            audioOnly = backup.audioOnly
            subtitlesOnly = backup.subtitlesOnly
            audioFormat = backup.audioFormat
            audioQuality = backup.audioQuality
            outputTemplate = backup.outputTemplate
            downloadConcurrency = backup.downloadConcurrency
            rateLimit = backup.rateLimit
            proxy = backup.proxy
            writeThumbnail = backup.writeThumbnail
            embedThumbnail = backup.embedThumbnail
            embedMetadata = backup.embedMetadata
            embedChapters = backup.embedChapters
            clipboardMonitoring = backup.clipboardMonitoring
            aria2cExecutable = backup.aria2cExecutable
            sponsorBlockCategories = backup.sponsorBlockCategories
            customArguments = backup.customArguments
            subtitleSource = backup.subtitleSource
            subtitleLanguages = backup.subtitleLanguages
            embedSubtitles = backup.embedSubtitles
            subtitleFormat = backup.subtitleFormat
            autoTranslatedSubtitles = backup.autoTranslatedSubtitles
            preferences.put("language", language)
            preferences.put("theme", theme)
            preferences.put("executable", executable)
            preferences.put("downloadDirectory", downloadDirectory)
            preferences.putBoolean("audioOnly", audioOnly)
            preferences.putBoolean("subtitlesOnly", subtitlesOnly)
            preferences.put("audioFormat", audioFormat)
            preferences.put("audioQuality", audioQuality)
            preferences.put("outputTemplate", outputTemplate)
            preferences.putInt("downloadConcurrency", downloadConcurrency)
            preferences.put("rateLimit", rateLimit)
            preferences.put("proxy", proxy)
            preferences.putBoolean("writeThumbnail", writeThumbnail)
            preferences.putBoolean("embedThumbnail", embedThumbnail)
            preferences.putBoolean("embedMetadata", embedMetadata)
            preferences.putBoolean("embedChapters", embedChapters)
            preferences.putBoolean("clipboardMonitoring", clipboardMonitoring)
            preferences.put("aria2cExecutable", aria2cExecutable)
            preferences.put("sponsorBlockCategories", sponsorBlockCategories.joinToString(","))
            preferences.put("customArguments", customArguments)
            preferences.put("subtitleSource", subtitleSource)
            preferences.put("subtitleLanguages", subtitleLanguages)
            preferences.putBoolean("embedSubtitles", embedSubtitles)
            preferences.put("subtitleFormat", subtitleFormat)
            preferences.putBoolean("autoTranslatedSubtitles", autoTranslatedSubtitles)
            status = "Settings imported."
            error = false
        } catch (exception: Exception) {
            status = "Could not import settings: ${exception.message.orEmpty()}"
            error = true
            DiagnosticLog.append("Settings import failed", exception.message)
        }
    }

    fun scanStorage() {
        scope.launch {
            try {
                val roots =
                    (history.map { File(it.outputDirectory) } + queue.map { it.outputDirectory } + File(downloadDirectory))
                        .distinctBy { it.absolutePath }
                val summary = withContext(Dispatchers.IO) { scanDownloadStorage(roots) }
                storageSizeBytes = summary.bytes
                removableTempFiles = summary.oldPartialFiles
            } catch (exception: Exception) {
                status = "Could not scan download storage: ${exception.message.orEmpty()}"
                error = true
                DiagnosticLog.append("Storage scan failed", exception.message)
            }
        }
    }

    fun cleanupOldPartials() {
        scope.launch {
            try {
                val roots =
                    (history.map { File(it.outputDirectory) } + queue.map { it.outputDirectory } + File(downloadDirectory))
                        .distinctBy { it.absolutePath }
                val deleted =
                    withContext(Dispatchers.IO) {
                        deleteOldPartialFiles(removableTempFiles, roots)
                    }
                removableTempFiles = emptyList()
                showCleanupConfirmation = false
                status = "Deleted $deleted old partial files."
                scanStorage()
            } catch (exception: Exception) {
                status = "Could not clean temporary files: ${exception.message.orEmpty()}"
                error = true
                DiagnosticLog.append("Temporary-file cleanup failed", exception.message)
            }
        }
    }

    fun startQueue() {
        if (queueWorkerActive) return
        if (!historyReady) {
            status = "Download history is still loading. Try again shortly."
            error = true
            return
        }
        if (queue.none { it.status == QueueStatus.WAITING }) {
            status = tr("nothingToStart")
            error = true
            return
        }
        queueWorkerActive = true
        scheduledJob?.cancel()
        scheduledJob = null
        scheduledStartMillis = 0L
        status = tr("startQueued")
        error = false
        scope.launch {
            try {
                coroutineScope {
                    repeat(downloadConcurrency.coerceIn(1, 4)) {
                        launch {
                while (true) {
                    val item = queue.firstOrNull { it.status == QueueStatus.WAITING } ?: break
                    val control = DownloadControl()
                    controls[item.id] = control
                    var terminalStatus: QueueStatus? = null
                    var terminalMessage = ""
                    val logFailureReported = AtomicBoolean(false)
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
                                formatSelector = item.formatSelector,
                                outputTemplate = item.outputTemplate,
                                rateLimit = item.rateLimit,
                                proxy = item.proxy,
                                writeThumbnail = item.writeThumbnail,
                                embedThumbnail = item.embedThumbnail,
                                embedMetadata = item.embedMetadata,
                                embedChapters = item.embedChapters,
                                aria2cExecutable = item.aria2cExecutable,
                                cookiesFile = item.cookiesFile,
                                sponsorBlockCategories = item.sponsorBlockCategories,
                                customArguments = item.customArguments,
                                subtitleSource = item.subtitleSource,
                                subtitleLanguages = item.subtitleLanguages,
                                embedSubtitles = item.embedSubtitles,
                                subtitleFormat = item.subtitleFormat,
                                subtitlesOnly = item.subtitlesOnly,
                                autoTranslatedSubtitles = item.autoTranslatedSubtitles,
                            ) { line ->
                                try {
                                    DiagnosticLog.append("yt-dlp output", "${item.title}: $line")
                                } catch (exception: Exception) {
                                    if (logFailureReported.compareAndSet(false, true)) {
                                        scope.launch(Dispatchers.Main) {
                                            diagnosticsError = "Could not write diagnostics log: ${exception.message.orEmpty()}"
                                            status = diagnosticsError
                                            error = true
                                        }
                                    }
                                }
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
                        terminalStatus = QueueStatus.COMPLETE
                    } catch (_: DownloadPausedException) {
                        setQueueItem(item.id) {
                            if (it.status == QueueStatus.DOWNLOADING) {
                                it.copy(status = QueueStatus.PAUSED, message = "")
                            } else it
                        }
                    } catch (_: DownloadCancelledException) {
                        terminalStatus = QueueStatus.CANCELLED
                        setQueueItem(item.id) {
                            it.copy(status = QueueStatus.CANCELLED, message = "")
                        }
                    } catch (exception: Exception) {
                        terminalStatus = QueueStatus.FAILED
                        terminalMessage = userFriendlyYtDlpError(exception.message.orEmpty())
                        setQueueItem(item.id) {
                            it.copy(status = QueueStatus.FAILED, message = terminalMessage)
                        }
                    } finally {
                        controls.remove(item.id)
                    }
                    terminalStatus?.let { result ->
                        val entry = item.toHistoryEntry(result, terminalMessage)
                        history.add(0, entry)
                        while (history.size > 500) history.removeAt(history.lastIndex)
                        try {
                            DiagnosticLog.append("Download $result", "${item.title}: $terminalMessage")
                        } catch (exception: Exception) {
                            diagnosticsError = "Could not write diagnostics log: ${exception.message.orEmpty()}"
                            status = diagnosticsError
                            error = true
                        }
                        if (historyLoaded) {
                            try {
                                withContext(Dispatchers.IO) { historyStore.append(entry) }
                                historyError = ""
                            } catch (exception: Exception) {
                                historyError = exception.message ?: "Could not save download history."
                                status = "Could not save download history: $historyError"
                                error = true
                                try {
                                    DiagnosticLog.append("History save failed", historyError)
                                } catch (logException: Exception) {
                                    diagnosticsError = "Could not write diagnostics log: ${logException.message.orEmpty()}"
                                    status = diagnosticsError
                                }
                            }
                        } else {
                            historyError = "History could not be loaded, so it was not overwritten."
                            status = historyError
                            error = true
                        }
                        val notificationTitle =
                            when (result) {
                                QueueStatus.COMPLETE -> tr("notificationComplete")
                                QueueStatus.FAILED -> tr("notificationFailed")
                                else -> tr("notificationCancelled")
                            }
                        try {
                            if (!TrayNotifications.show(notificationTitle, item.title, result == QueueStatus.FAILED)) {
                                status = tr("notificationUnavailable")
                                error = false
                                DiagnosticLog.append(
                                    "Notification unavailable",
                                    "System tray notifications are not supported.",
                                )
                            }
                        } catch (exception: Exception) {
                            diagnosticsError = "Could not show desktop notification: ${exception.message.orEmpty()}"
                            status = diagnosticsError
                            error = true
                            try {
                                DiagnosticLog.append("Notification failed", exception.message)
                            } catch (logException: Exception) {
                                diagnosticsError += " Diagnostics log failed: ${logException.message.orEmpty()}"
                                status = diagnosticsError
                            }
                        }
                    }
                }
                        }
                    }
                }
            } finally {
                queueWorkerActive = false
            }
        }
    }

    fun scheduleQueue() {
        val target =
            try {
                java.time.LocalDateTime.parse(
                    scheduleAt.trim(),
                    java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm"),
                )
            } catch (exception: Exception) {
                status = "Enter a schedule time in YYYY-MM-DD HH:MM format."
                error = true
                return
            }
        val delayMillis =
            java.time.Duration.between(java.time.LocalDateTime.now(), target).toMillis()
        if (delayMillis <= 0) {
            status = "Schedule time must be in the future."
            error = true
            return
        }
        if (queue.none { it.status == QueueStatus.WAITING }) {
            status = tr("nothingToStart")
            error = true
            return
        }
        scheduledJob?.cancel()
        scheduledJob =
            scope.launch {
                delay(delayMillis)
                startQueue()
            }
        scheduledStartMillis = System.currentTimeMillis() + delayMillis
        status = tr("queueScheduled")
        error = false
    }

    fun addToQueue(videoUrl: String, title: String, sourcePlatform: String?, video: VideoDetails?): Boolean {
        val outputFolder = File(downloadDirectory)
        if (!outputFolder.isDirectory) {
            status = "Choose an existing download folder."
            error = true
            return false
        }
        if (!isValidOutputTemplate(outputTemplate)) {
            status = "Use supported output placeholders: title, id, uploader, upload_date, resolution, ext."
            error = true
            return false
        }
        try {
            val parsedCustomArguments = parseCustomYtDlpArguments(customArguments)
            // Subtitles-only falls back to both sources when the user left subtitles off.
            val effectiveSubtitleSource =
                if (subtitlesOnly && subtitleSource == "off") "both" else subtitleSource
            validateSubtitleCustomArguments(effectiveSubtitleSource, parsedCustomArguments)
            require(sponsorBlockCategories.isEmpty() || isValidSponsorBlockCategories(sponsorBlockCategories)) {
                "Select supported SponsorBlock categories."
            }
            require(cookiesFile.isBlank() || isNetscapeCookieFile(File(cookiesFile))) {
                "Select a readable Netscape-format cookie file."
            }
            require(aria2cExecutable.isBlank() || isValidAria2cExecutable(aria2cExecutable)) {
                "Select an existing aria2c executable."
            }
            require(!isYouTubePlaylistUrl(videoUrl)) { "Select individual videos from the playlist before queueing." }
            requireHttpUrl(videoUrl)
            subtitleArguments(
                source = effectiveSubtitleSource,
                languages = subtitleLanguages,
                embed = embedSubtitles && !subtitlesOnly,
                format = subtitleFormat,
                audioOnly = audioOnly && !subtitlesOnly,
                autoTranslatedSubtitles = autoTranslatedSubtitles,
            )
        } catch (exception: IllegalArgumentException) {
            status = exception.message.orEmpty()
            error = true
            return false
        }
        val selectedAudioFormat = if (audioOnly && !subtitlesOnly) audioFormat else null
        val selectedAudioQuality =
            if (audioQuality == "Best") "0"
            else audioQuality.substringBefore(" kbps") + "K"
        val selectedVideoFormat =
            if (audioOnly || subtitlesOnly || video == null) null
            else video.formats.firstOrNull { videoFormatSelector(it) == selectedFormatSelector }
        queue.add(
            DownloadQueueItem(
                id = UUID.randomUUID().toString(),
                url = videoUrl.trim(),
                title = title.ifBlank { videoUrl.trim() },
                sourcePlatform = sourcePlatform,
                outputDirectory = outputFolder,
                maxHeight = if (audioOnly || subtitlesOnly) null else maxHeight,
                audioFormat = selectedAudioFormat,
                audioQuality = selectedAudioQuality,
                qualityLabel =
                    if (subtitlesOnly)
                        "${subtitleSourceLabel(if (subtitleSource == "off") "both" else subtitleSource)} · " +
                            "${subtitleFormat.uppercase()} · $subtitleLanguages"
                    else if (audioOnly) "$audioFormat · $audioQuality"
                    else selectedVideoFormat?.let {
                        listOfNotNull(it.format_id, it.height?.let { height -> "${height}p" }, it.ext?.uppercase())
                            .joinToString(" · ")
                    } ?: maxHeight?.let { "$it p" } ?: tr("best"),
                estimatedSizeBytes =
                    if (subtitlesOnly) null else video?.let {
                        estimateDownloadSizeBytes(
                                video = video,
                                maxHeight = if (audioOnly || subtitlesOnly) null else maxHeight,
                                audioFormat = selectedAudioFormat,
                                audioQuality = selectedAudioQuality,
                        )
                    },
                formatSelector = if (audioOnly || subtitlesOnly) null else selectedFormatSelector,
                outputTemplate = outputTemplate,
                rateLimit = rateLimit.trim().ifBlank { null },
                proxy = proxy.trim().ifBlank { null },
                writeThumbnail = writeThumbnail,
                embedThumbnail = embedThumbnail,
                embedMetadata = embedMetadata,
                embedChapters = embedChapters,
                aria2cExecutable = aria2cExecutable.trim().ifBlank { null },
                cookiesFile = cookiesFile.trim().ifBlank { null },
                sponsorBlockCategories = sponsorBlockCategories,
                customArguments = customArguments,
                subtitleSource = if (subtitlesOnly && subtitleSource == "off") "both" else subtitleSource,
                subtitleLanguages = subtitleLanguages.trim().split(',').joinToString(",") { it.trim() },
                embedSubtitles = embedSubtitles,
                subtitleFormat = subtitleFormat,
                subtitlesOnly = subtitlesOnly,
                autoTranslatedSubtitles = autoTranslatedSubtitles,
            ),
        )
        status =
            if (subtitlesOnly) {
                if (language == "ar") "تمت إضافة الترجمات إلى قائمة التنزيل." else "Subtitles added to the download queue."
            } else if (language == "ar") {
                "تمت إضافة الفيديو إلى قائمة التنزيل."
            } else {
                "Video added to the download queue."
            }
        error = false
        return true
    }

    fun addCurrentVideoToQueue() {
        val video = details ?: return
        if (!addToQueue(url, video.title, video.extractor_key ?: video.extractor, video)) return
        url = ""
        details = null
    }

    LaunchedEffect(browserRequests) {
        for ((requestedUrl, completed) in browserRequests) {
            try {
                if (addToQueue(requestedUrl, requestedUrl, "YouTube", null)) {
                    completed.complete(Unit)
                } else {
                    completed.completeExceptionally(IllegalArgumentException(status))
                }
            } catch (exception: Exception) {
                completed.completeExceptionally(exception)
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides
            if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialTheme(
            colorScheme =
                if (theme == "high-contrast") {
                    highContrastColors
                } else if (
                    theme == "dark" ||
                        (theme == "system" && isSystemInDarkTheme())
                ) {
                    darkColorScheme(primary = accent)
                } else {
                    lightColors
                },
        ) {
            Surface(
                modifier =
                    Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) {
                            false
                        } else {
                            when (event.key) {
                                Key.V -> if (event.isShiftPressed) {
                                    try {
                                        url = readClipboardText()
                                        details = null
                                        playlist = null
                                        selectedPlaylistEntries.clear()
                                        selectedFormatSelector = null
                                    } catch (exception: Exception) {
                                        status = "Could not read the clipboard: ${exception.message.orEmpty()}"
                                        error = true
                                    }
                                    true
                                } else false
                                Key.Enter -> {
                                    startQueue()
                                    true
                                }
                                Key.O -> if (event.isShiftPressed) {
                                    showSettings = true
                                    true
                                } else false
                                Key.P -> if (event.isShiftPressed) {
                                    val active = queue.firstOrNull { it.status == QueueStatus.DOWNLOADING }
                                    if (active != null) {
                                        controls[active.id]?.pause()
                                        setQueueItem(active.id) {
                                            it.copy(status = QueueStatus.PAUSED, message = "")
                                        }
                                    } else {
                                        queue.firstOrNull { it.status == QueueStatus.PAUSED }?.let { paused ->
                                            setQueueItem(paused.id) {
                                                it.copy(status = QueueStatus.WAITING, message = "")
                                            }
                                            startQueue()
                                        }
                                    }
                                    true
                                } else false
                                else -> false
                            }
                        }
                    },
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth().widthIn(max = 1100.dp)
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
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showSettings) {
                            TextButton(onClick = { showSettings = false }) { Text(tr("home")) }
                            Button(onClick = { showSettings = true }) { Text(tr("settings")) }
                        } else {
                            Button(onClick = { showSettings = false }) { Text(tr("home")) }
                            TextButton(onClick = { showSettings = true }) { Text(tr("settings")) }
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
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

                    if (!showSettings) {
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
                                onValueChange = {
                                    url = it
                                    details = null
                                    playlist = null
                                    selectedPlaylistEntries.clear()
                                    selectedFormatSelector = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("youtubeUrl")) },
                                placeholder = { Text("https://www.youtube.com/watch?v=…") },
                                singleLine = true,
                                enabled = !loadingFormats,
                            )
                            if (detectedClipboardUrl.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        url = detectedClipboardUrl
                                        details = null
                                        playlist = null
                                        selectedPlaylistEntries.clear()
                                        selectedFormatSelector = null
                                        detectedClipboardUrl = ""
                                    },
                                ) {
                                    Text(tr("useClipboardLink"))
                                }
                            }
                            Button(
                                onClick = {
                                    loadingFormats = true
                                    error = false
                                    status = tr("loading")
                                    details = null
                                    playlist = null
                                    selectedPlaylistEntries.clear()
                                    scope.launch {
                                        try {
                                            if (isYouTubePlaylistUrl(url)) {
                                                val result =
                                                    withContext(Dispatchers.IO) {
                                                        YtDlpClient(executable.trim()).fetchPlaylistDetails(url)
                                                    }
                                                playlist = result
                                                details = null
                                                maxHeight = null
                                                selectedFormatSelector = null
                                                selectedPlaylistEntries.clear()
                                                selectedPlaylistEntries.addAll(
                                                    result.entries.mapNotNull { it?.let(::playlistEntryUrl) },
                                                )
                                                status = "${tr("playlistVideos")}: ${selectedPlaylistEntries.size}"
                                            } else {
                                                val result =
                                                    withContext(Dispatchers.IO) {
                                                        YtDlpClient(executable.trim()).fetchVideoDetails(url)
                                                    }
                                                details = result
                                                playlist = null
                                                selectedPlaylistEntries.clear()
                                                maxHeight =
                                                    result.formats.filter { it.hasVideo() }
                                                        .mapNotNull { it.height }.distinct().maxOrNull()
                                                selectedFormatSelector = null
                                                status = tr("formatsLoaded")
                                            }
                                        } catch (exception: Exception) {
                                            details = null
                                            playlist = null
                                            selectedPlaylistEntries.clear()
                                            error = true
                                            status = userFriendlyYtDlpError(exception.message ?: tr("updateCheckFailed"))
                                            try {
                                                DiagnosticLog.append("Format loading failed", status)
                                            } catch (logException: Exception) {
                                                status += " Diagnostics log failed: ${logException.message.orEmpty()}"
                                            }
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

                    playlist?.let { listing ->
                        val selectableEntries =
                            listing.entries.mapNotNull { entry ->
                                entry ?: return@mapNotNull null
                                val entryUrl = playlistEntryUrl(entry) ?: return@mapNotNull null
                                entryUrl to entry
                            }
                        Card {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(listing.title.ifBlank { tr("playlistVideos") }, style = MaterialTheme.typography.titleMedium)
                                Text("${tr("playlistVideos")} (${selectableEntries.size})")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("type"), modifier = Modifier.width(100.dp))
                                    ChoiceMenu(
                                        selected =
                                            when {
                                                subtitlesOnly -> tr("subtitlesOnly")
                                                audioOnly -> tr("audioOnly")
                                                else -> tr("videoAudio")
                                            },
                                        options = listOf(tr("videoAudio"), tr("audioOnly"), tr("subtitlesOnly")),
                                        enabled = true,
                                        onSelected = {
                                            subtitlesOnly = it == tr("subtitlesOnly")
                                            audioOnly = it == tr("audioOnly")
                                            preferences.putBoolean("audioOnly", audioOnly)
                                            preferences.putBoolean("subtitlesOnly", subtitlesOnly)
                                        },
                                    )
                                }
                                if (subtitlesOnly) {
                                    Text(tr("subtitlesOnlyHelp"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("subtitleSource"), modifier = Modifier.width(100.dp))
                                        val sourceOptions =
                                            listOf(
                                                "manual" to tr("subtitleManual"),
                                                "auto" to tr("subtitleAuto"),
                                                "both" to tr("subtitleBoth"),
                                            )
                                        ChoiceMenu(
                                            selected =
                                                sourceOptions.firstOrNull { it.first == subtitleSource }?.second
                                                    ?: tr("subtitleBoth"),
                                            options = sourceOptions.map { it.second },
                                            enabled = true,
                                            onSelected = { selected ->
                                                subtitleSource = sourceOptions.first { it.second == selected }.first
                                                preferences.put("subtitleSource", subtitleSource)
                                            },
                                        )
                                    }
                                } else if (audioOnly) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("audio"), modifier = Modifier.width(100.dp))
                                        ChoiceMenu(
                                            selected = audioFormat.uppercase(),
                                            options = audioFormats.map(String::uppercase),
                                            enabled = true,
                                            onSelected = {
                                                audioFormat = it.lowercase()
                                                preferences.put("audioFormat", audioFormat)
                                            },
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        ChoiceMenu(
                                            selected = audioQuality,
                                            options = audioQualities,
                                            enabled = true,
                                            onSelected = {
                                                audioQuality = it
                                                preferences.put("audioQuality", audioQuality)
                                            },
                                        )
                                    }
                                } else {
                                    val playlistHeights = listOf(2160, 1440, 1080, 720, 480, 360)
                                    ChoiceMenu(
                                        selected = maxHeight?.let { "$it p ${tr("orLower")}" } ?: tr("best"),
                                        options = listOf(tr("best")) + playlistHeights.map { "$it p ${tr("orLower")}" },
                                        enabled = true,
                                        onSelected = {
                                            maxHeight = if (it == tr("best")) null else it.substringBefore(" p").toIntOrNull()
                                        },
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("saveTo"), modifier = Modifier.width(100.dp))
                                    Text(downloadDirectory, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        chooseDirectory(downloadDirectory)?.let {
                                            downloadDirectory = it.absolutePath
                                            preferences.put("downloadDirectory", downloadDirectory)
                                        }
                                    }) { Text(tr("browse")) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        selectedPlaylistEntries.clear()
                                        selectedPlaylistEntries.addAll(selectableEntries.map { it.first })
                                    }) { Text(tr("selectAll")) }
                                    TextButton(onClick = { selectedPlaylistEntries.clear() }) {
                                        Text(tr("selectNone"))
                                    }
                                }
                                selectableEntries.forEach { (entryUrl, entry) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = entryUrl in selectedPlaylistEntries,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedPlaylistEntries.add(entryUrl)
                                                else selectedPlaylistEntries.remove(entryUrl)
                                            },
                                        )
                                        Text(
                                            entry.title?.ifBlank { null } ?: entryUrl,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                Button(
                                    enabled = selectedPlaylistEntries.isNotEmpty(),
                                    onClick = {
                                        var added = 0
                                        selectableEntries
                                            .filter { it.first in selectedPlaylistEntries }
                                            .forEach { (entryUrl, entry) ->
                                                if (addToQueue(entryUrl, entry.title.orEmpty(), "YouTube", null)) added++
                                                else return@Button
                                            }
                                        status = "$added ${tr("addSelected").lowercase()}."
                                        error = false
                                        url = ""
                                        playlist = null
                                        selectedPlaylistEntries.clear()
                                    },
                                ) {
                                    Text("${tr("addSelected")} (${selectedPlaylistEntries.size})")
                                }
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
                                        selected =
                                            when {
                                                subtitlesOnly -> tr("subtitlesOnly")
                                                audioOnly -> tr("audioOnly")
                                                else -> tr("videoAudio")
                                            },
                                        options = listOf(tr("videoAudio"), tr("audioOnly"), tr("subtitlesOnly")),
                                        enabled = true,
                                        onSelected = {
                                            subtitlesOnly = it == tr("subtitlesOnly")
                                            audioOnly = it == tr("audioOnly")
                                            preferences.putBoolean("audioOnly", audioOnly)
                                            preferences.putBoolean("subtitlesOnly", subtitlesOnly)
                                        },
                                    )
                                }
                                if (subtitlesOnly) {
                                    Text(tr("subtitlesOnlyHelp"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("subtitleSource"), modifier = Modifier.width(100.dp))
                                        val sourceOptions =
                                            listOf(
                                                "manual" to tr("subtitleManual"),
                                                "auto" to tr("subtitleAuto"),
                                                "both" to tr("subtitleBoth"),
                                            )
                                        ChoiceMenu(
                                            selected =
                                                sourceOptions.firstOrNull { it.first == subtitleSource }?.second
                                                    ?: tr("subtitleBoth"),
                                            options = sourceOptions.map { it.second },
                                            enabled = true,
                                            onSelected = { selected ->
                                                subtitleSource = sourceOptions.first { it.second == selected }.first
                                                preferences.put("subtitleSource", subtitleSource)
                                            },
                                        )
                                    }
                                } else if (audioOnly) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tr("audio"), modifier = Modifier.width(100.dp))
                                        ChoiceMenu(
                                            selected = audioFormat.uppercase(),
                                            options = audioFormats.map(String::uppercase),
                                            enabled = true,
                                            onSelected = {
                                                audioFormat = it.lowercase()
                                                preferences.put("audioFormat", audioFormat)
                                            },
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        ChoiceMenu(
                                            selected = audioQuality,
                                            options = audioQualities,
                                            enabled = true,
                                            onSelected = {
                                                audioQuality = it
                                                preferences.put("audioQuality", audioQuality)
                                            },
                                        )
                                    }
                                } else {
                                    val heights =
                                        video.formats.filter { it.hasVideo() }
                                            .mapNotNull { it.height }.distinct().sortedDescending()
                                    val exactFormats =
                                        video.formats.filter { it.hasVideo() }.mapNotNull { format ->
                                            val selector = videoFormatSelector(format) ?: return@mapNotNull null
                                            val label =
                                                listOfNotNull(
                                                    format.format_id,
                                                    format.height?.let { "${it}p" },
                                                    format.ext?.uppercase(),
                                                    format.vcodec?.takeIf { it != "none" },
                                                    format.fps?.let { "${it.toInt()}fps" },
                                                    format.tbr?.let { "${it.toInt()} kbps" },
                                                ).joinToString(" · ")
                                            label to selector
                                        }.distinctBy { it.second }
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
                                    if (exactFormats.isNotEmpty()) {
                                        ChoiceMenu(
                                            selected =
                                                exactFormats.firstOrNull { it.second == selectedFormatSelector }?.first
                                                    ?: tr("automaticFormat"),
                                            options = listOf(tr("automaticFormat")) + exactFormats.map { it.first },
                                            enabled = true,
                                            onSelected = { selected ->
                                                selectedFormatSelector =
                                                    exactFormats.firstOrNull { it.first == selected }?.second
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
                                                preferences.put("downloadDirectory", downloadDirectory)
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
                            queue.forEachIndexed { index, item ->
                                QueueItemRow(
                                    item = item,
                                    strings = strings,
                                    canMoveUp =
                                        index > 0 &&
                                            item.status == QueueStatus.WAITING &&
                                            queue[index - 1].status == QueueStatus.WAITING,
                                    canMoveDown =
                                        index < queue.lastIndex &&
                                            item.status == QueueStatus.WAITING &&
                                            queue[index + 1].status == QueueStatus.WAITING,
                                    onMoveUp = {
                                        val moved = queue.removeAt(index)
                                        queue.add(index - 1, moved)
                                    },
                                    onMoveDown = {
                                        val moved = queue.removeAt(index)
                                        queue.add(index + 1, moved)
                                    },
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
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${tr("historyTitle")} (${history.size})",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                TextButton(onClick = ::openDiagnosticsLog) {
                                    Text(tr("openDiagnostics"))
                                }
                            }
                            if (historyError.isNotBlank()) {
                                Text(historyError, color = MaterialTheme.colorScheme.error)
                            }
                            if (diagnosticsError.isNotBlank()) {
                                Text(diagnosticsError, color = MaterialTheme.colorScheme.error)
                            }
                            if (history.isEmpty() && historyError.isBlank()) {
                                Text(tr("historyEmpty"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            history.forEach { entry ->
                                HistoryItemRow(
                                    entry = entry,
                                    strings = strings,
                                    onRetry = {
                                        queue.add(
                                            entry.toQueueItem().copy(
                                                id = UUID.randomUUID().toString(),
                                                status = QueueStatus.WAITING,
                                                progress = 0f,
                                                message = "",
                                            ),
                                        )
                                        status =
                                            tr("retryAdded")
                                        error = false
                                    },
                                )
                            }
                        }
                    }

                    if (showSettings) {
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(tr("appearance"), style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("language"), modifier = Modifier.weight(1f))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("theme"), modifier = Modifier.weight(1f))
                                ChoiceMenu(
                                    selected =
                                        when (theme) {
                                            "light" -> tr("light")
                                            "dark" -> tr("dark")
                                            "high-contrast" -> tr("highContrast")
                                            else -> tr("system")
                                        },
                                    options = listOf(tr("system"), tr("light"), tr("dark"), tr("highContrast")),
                                    enabled = true,
                                    onSelected = {
                                        theme = when (it) {
                                            tr("light") -> "light"
                                            tr("dark") -> "dark"
                                            tr("highContrast") -> "high-contrast"
                                            else -> "system"
                                        }
                                        preferences.put("theme", theme)
                                    },
                                )
                            }
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(tr("downloadSettings"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                tr("downloadOptions"),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("concurrentDownloads"), modifier = Modifier.weight(1f))
                                ChoiceMenu(
                                    selected = downloadConcurrency.toString(),
                                    options = listOf("1", "2", "3", "4"),
                                    enabled = !queueWorkerActive,
                                    onSelected = {
                                        downloadConcurrency = it.toInt()
                                        preferences.putInt("downloadConcurrency", downloadConcurrency)
                                    },
                                )
                            }
                            OutlinedTextField(
                                value = outputTemplate,
                                onValueChange = {
                                    outputTemplate = it
                                    preferences.put("outputTemplate", it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("outputTemplate")) },
                                supportingText = {
                                    Text(
                                        "%(extractor_key)s/%(uploader)s/%(upload_date)s/%(title).200B [%(id)s].%(ext)s",
                                    )
                                },
                                singleLine = true,
                            )
                            Text(
                                tr("subtitleSettings"),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("subtitleSource"), modifier = Modifier.weight(1f))
                                val sourceOptions =
                                    listOf(
                                        "off" to tr("subtitleOff"),
                                        "manual" to tr("subtitleManual"),
                                        "auto" to tr("subtitleAuto"),
                                        "both" to tr("subtitleBoth"),
                                    )
                                ChoiceMenu(
                                    selected = sourceOptions.first { it.first == subtitleSource }.second,
                                    options = sourceOptions.map { it.second },
                                    enabled = true,
                                    onSelected = { selected ->
                                        subtitleSource = sourceOptions.first { it.second == selected }.first
                                        preferences.put("subtitleSource", subtitleSource)
                                    },
                                )
                            }
                            if (subtitleSource != "off" || subtitlesOnly) {
                                Text(tr("subtitleHelp"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val languageChoices =
                                    listOf(
                                        "en" to tr("subtitleLanguageEnglish"),
                                        "ar" to tr("subtitleLanguageArabic"),
                                        "fr" to tr("subtitleLanguageFrench"),
                                        "all" to tr("subtitleLanguageAll"),
                                        "custom" to tr("subtitleLanguageCustom"),
                                    )
                                val selectedLanguageChoice =
                                    languageChoices.firstOrNull { it.first == subtitleLanguages.trim() }?.second
                                        ?: languageChoices.last().second
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("subtitleLanguages"), modifier = Modifier.weight(1f))
                                    ChoiceMenu(
                                        selected = selectedLanguageChoice,
                                        options = languageChoices.map { it.second },
                                        enabled = true,
                                        onSelected = { selected ->
                                            val selectedCode = languageChoices.first { it.second == selected }.first
                                            if (selectedCode != "custom") {
                                                subtitleLanguages = selectedCode
                                                preferences.put("subtitleLanguages", subtitleLanguages)
                                            }
                                        },
                                    )
                                }
                                OutlinedTextField(
                                    value = subtitleLanguages,
                                    onValueChange = {
                                        subtitleLanguages = it
                                        preferences.put("subtitleLanguages", it)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(tr("subtitleLanguages")) },
                                    supportingText = { Text(tr("subtitleLanguageHelp")) },
                                    singleLine = true,
                                )
                                val includesAutomaticCaptions =
                                    when {
                                        subtitlesOnly -> subtitleSource != "manual"
                                        else -> subtitleSource == "auto" || subtitleSource == "both"
                                    }
                                if (includesAutomaticCaptions) {
                                    SettingSwitch(
                                        label = tr("subtitleAutoTranslated"),
                                        checked = autoTranslatedSubtitles,
                                        onCheckedChange = {
                                            autoTranslatedSubtitles = it
                                            preferences.putBoolean("autoTranslatedSubtitles", it)
                                        },
                                    )
                                    Text(
                                        tr("subtitleAutoTranslatedHelp"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                SettingSwitch(
                                    label = tr("subtitleEmbed"),
                                    checked = embedSubtitles,
                                    onCheckedChange = {
                                        embedSubtitles = it
                                        preferences.putBoolean("embedSubtitles", it)
                                    },
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tr("subtitleFormat"), modifier = Modifier.weight(1f))
                                    ChoiceMenu(
                                        selected = subtitleFormat.uppercase(),
                                        options = listOf("SRT", "VTT"),
                                        enabled = true,
                                        onSelected = {
                                            subtitleFormat = it.lowercase()
                                            preferences.put("subtitleFormat", subtitleFormat)
                                        },
                                    )
                                }
                            }
                            Text(
                                tr("networkSettings"),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            OutlinedTextField(
                                value = rateLimit,
                                onValueChange = {
                                    rateLimit = it
                                    preferences.put("rateLimit", it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("rateLimit")) },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = proxy,
                                onValueChange = {
                                    proxy = it
                                    preferences.put("proxy", it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("proxy")) },
                                placeholder = { Text("socks5://127.0.0.1:1080") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = aria2cExecutable,
                                onValueChange = {
                                    aria2cExecutable = it
                                    preferences.put("aria2cExecutable", it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("aria2cPath")) },
                                placeholder = { Text("C:\\tools\\aria2c.exe") },
                                singleLine = true,
                            )
                            TextButton(onClick = {
                                chooseAria2cExecutable(aria2cExecutable)?.let {
                                    aria2cExecutable = it.absolutePath
                                    preferences.put("aria2cExecutable", aria2cExecutable)
                                }
                            }) {
                                Text(tr("browse"))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    chooseCookiesFile()?.let { file ->
                                        if (isNetscapeCookieFile(file)) {
                                            cookiesFile = file.absolutePath
                                            preferences.put("cookiesFile", cookiesFile)
                                            status = tr("cookiesImported")
                                            error = false
                                        } else {
                                            status = tr("invalidCookies")
                                            error = true
                                        }
                                    }
                                }) { Text(tr("chooseCookies")) }
                                TextButton(
                                    enabled = cookiesFile.isNotBlank(),
                                    onClick = {
                                        cookiesFile = ""
                                        preferences.remove("cookiesFile")
                                    },
                                ) { Text(tr("clearCookies")) }
                            }
                            if (cookiesFile.isNotBlank()) {
                                Text(
                                    "${tr("cookiesFile")}: ${File(cookiesFile).name}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(tr("cookieWarning"), color = MaterialTheme.colorScheme.error)
                            }
                            Text(
                                tr("additionalSettings"),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            SettingSwitch(
                                label = tr("sponsorBlock"),
                                checked = sponsorBlockCategories.isNotEmpty(),
                                onCheckedChange = {
                                    sponsorBlockCategories =
                                        if (it) listOf("sponsor", "intro", "outro", "selfpromo") else emptyList()
                                    preferences.put("sponsorBlockCategories", sponsorBlockCategories.joinToString(","))
                                },
                            )
                            if (sponsorBlockCategories.isNotEmpty()) {
                                OutlinedTextField(
                                    value = sponsorBlockCategories.joinToString(","),
                                    onValueChange = { value ->
                                        sponsorBlockCategories =
                                            value.split(',').map(String::trim).filter(String::isNotEmpty)
                                        preferences.put("sponsorBlockCategories", value)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(tr("sponsorBlockCategories")) },
                                    supportingText = {
                                        Text("sponsor, intro, outro, selfpromo, preview, filler, interaction, music_offtopic, poi_highlight, exclusive_access, chapter")
                                    },
                                    singleLine = true,
                                )
                            }
                            OutlinedTextField(
                                value = customArguments,
                                onValueChange = {
                                    customArguments = it
                                    if (it.length <= 7000) {
                                        preferences.put("customArguments", it)
                                    } else {
                                        status = "Custom templates may contain at most 7000 characters."
                                        error = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("customArguments")) },
                                supportingText = { Text(tr("customArgumentsHelp")) },
                                minLines = 3,
                                maxLines = 8,
                            )
                            SettingSwitch(
                                label = tr("browserBridge"),
                                checked = browserBridgeEnabled,
                                onCheckedChange = ::setBrowserBridge,
                            )
                            if (browserBridgeEnabled) {
                                Text(tr("browserBridgeToken"), style = MaterialTheme.typography.labelMedium)
                                Text(browserBridgeToken, modifier = Modifier.semantics { contentDescription = tr("browserBridgeToken") })
                                TextButton(onClick = {
                                    try {
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                            java.awt.datatransfer.StringSelection(browserBridgeToken),
                                            null,
                                        )
                                        status = tr("tokenCopied")
                                        error = false
                                    } catch (exception: Exception) {
                                        status = "Could not copy the bridge token: ${exception.message.orEmpty()}"
                                        error = true
                                    }
                                }) {
                                    Text(tr("copyToken"))
                                }
                            }
                            SettingSwitch(
                                label = tr("thumbnail"),
                                checked = writeThumbnail,
                                onCheckedChange = {
                                    writeThumbnail = it
                                    preferences.putBoolean("writeThumbnail", it)
                                },
                            )
                            SettingSwitch(
                                label = tr("embedThumbnail"),
                                checked = embedThumbnail,
                                onCheckedChange = {
                                    embedThumbnail = it
                                    preferences.putBoolean("embedThumbnail", it)
                                },
                            )
                            SettingSwitch(
                                label = tr("metadata"),
                                checked = embedMetadata,
                                onCheckedChange = {
                                    embedMetadata = it
                                    preferences.putBoolean("embedMetadata", it)
                                },
                            )
                            SettingSwitch(
                                label = tr("chapters"),
                                checked = embedChapters,
                                onCheckedChange = {
                                    embedChapters = it
                                    preferences.putBoolean("embedChapters", it)
                                },
                            )
                            SettingSwitch(
                                label = tr("clipboardMonitoring"),
                                checked = clipboardMonitoring,
                                onCheckedChange = {
                                    clipboardMonitoring = it
                                    preferences.putBoolean("clipboardMonitoring", it)
                                    detectedClipboardUrl = ""
                                    lastClipboardText = ""
                                },
                            )
                            Text(
                                tr("automationSettings"),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(tr("schedulerNote"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = scheduleAt,
                                onValueChange = { scheduleAt = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(tr("schedule")) },
                                placeholder = { Text("2026-10-11 23:30") },
                                singleLine = true,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = !queueWorkerActive, onClick = ::scheduleQueue) {
                                    Text(tr("scheduleQueue"))
                                }
                                if (scheduledJob?.isActive == true) {
                                    TextButton(
                                        onClick = {
                                            scheduledJob?.cancel()
                                            scheduledJob = null
                                            scheduledStartMillis = 0L
                                        },
                                    ) {
                                        Text(tr("cancelSchedule"))
                                    }
                                    Text(
                                        java.text.DateFormat.getDateTimeInstance().format(java.util.Date(scheduledStartMillis)),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = ::exportSettings) { Text(tr("backup")) }
                                TextButton(onClick = ::importSettings) { Text(tr("restore")) }
                            }
                        }
                    }

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(tr("storage"), style = MaterialTheme.typography.titleMedium)
                            Text("${tr("historyCount")}: ${history.size} · ${tr("complete")}: ${history.count { it.status == QueueStatus.COMPLETE }} · ${tr("failed")}: ${history.count { it.status == QueueStatus.FAILED }}")
                            val completedEstimate =
                                history.filter { it.status == QueueStatus.COMPLETE }
                                    .mapNotNull { it.estimatedSizeBytes }
                                    .fold(0L) { total, bytes ->
                                        if (Long.MAX_VALUE - total < bytes) Long.MAX_VALUE else total + bytes
                                    }
                            Text("${tr("downloadsSize")}: ${formatFileSize(completedEstimate)}")
                            history.mapNotNull { it.sourcePlatform }
                                .groupingBy { it }
                                .eachCount()
                                .maxByOrNull { it.value }
                                ?.let { (platform, count) ->
                                    Text("Most-used platform: $platform ($count)")
                                }
                            storageSizeBytes?.let {
                                Text("Files in download folders: ${formatFileSize(it)}")
                            }
                            removableTempFiles.takeIf { it.isNotEmpty() }?.let {
                                Text("${tr("cleanupTemp")}: ${it.size}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = ::scanStorage) { Text(tr("scanStorage")) }
                                TextButton(
                                    enabled = removableTempFiles.isNotEmpty(),
                                    onClick = { showCleanupConfirmation = true },
                                ) {
                                    Text(tr("cleanup"))
                                }
                            }
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
                                    onValueChange = {
                                        executable = it
                                        preferences.put("executable", it)
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(tr("executable")) },
                                    placeholder = { Text("yt-dlp or C:\\tools\\yt-dlp.exe") },
                                    singleLine = true,
                                )
                                Spacer(Modifier.width(10.dp))
                                TextButton(
                                    onClick = {
                                        chooseExecutable(executable)?.let {
                                            executable = it.absolutePath
                                            preferences.put("executable", executable)
                                        }
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
                                        try {
                                            DiagnosticLog.append("Update installation failed", updateStatus)
                                        } catch (logException: Exception) {
                                            updateStatus += " Diagnostics log failed: ${logException.message.orEmpty()}"
                                        }
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
            if (showCleanupConfirmation) {
                AlertDialog(
                    onDismissRequest = { showCleanupConfirmation = false },
                    title = { Text(tr("cleanupTemp")) },
                    text = { Text(tr("cleanupConfirm")) },
                    confirmButton = {
                        Button(onClick = ::cleanupOldPartials) { Text(tr("cleanup")) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCleanupConfirmation = false }) {
                            Text(tr("cancel"))
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
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
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
                "$statusText · ${
                    when {
                        item.subtitlesOnly -> strings.getValue("subtitlesOnly")
                        item.audioFormat != null -> strings.getValue("audioOnly")
                        else -> strings.getValue("videoAudio")
                    }
                }",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${strings.getValue("quality")} ${item.qualityLabel}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${strings.getValue("estimatedSize")} ${item.estimatedSizeBytes?.let(::formatFileSize) ?: strings.getValue("sizeUnavailable")}",
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
                if (item.status == QueueStatus.WAITING) {
                    TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Text(strings.getValue("moveUp"))
                    }
                    TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Text(strings.getValue("moveDown"))
                    }
                }
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
private fun HistoryItemRow(
    entry: DownloadHistoryEntry,
    strings: Map<String, String>,
    onRetry: () -> Unit,
) {
    val statusText =
        when (entry.status) {
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
            Text(entry.title, style = MaterialTheme.typography.titleSmall)
            entry.sourcePlatform?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "$statusText · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.completedAtMillis))}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.status == QueueStatus.FAILED && entry.message.isNotBlank()) {
                Text(entry.message, color = MaterialTheme.colorScheme.error)
            }
            if (entry.status == QueueStatus.FAILED) {
                TextButton(onClick = onRetry) { Text(strings.getValue("retry")) }
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

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = label },
        )
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

private fun chooseSettingsFile(save: Boolean): File? {
    val chooser =
        JFileChooser(System.getProperty("user.home")).apply {
            dialogTitle = if (save) "Export Seal Desktop settings" else "Import Seal Desktop settings"
            fileSelectionMode = JFileChooser.FILES_ONLY
            fileFilter = FileNameExtensionFilter("Seal Desktop settings (*.json)", "json")
            if (save) selectedFile = File("seal-desktop-settings.json")
        }
    val result =
        if (save) chooser.showSaveDialog(null)
        else chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    val selected = chooser.selectedFile
    return if (save && !selected.name.endsWith(".json", ignoreCase = true)) File(selected.parentFile, "${selected.name}.json")
    else selected
}

private fun readClipboardText(): String =
    Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
        ?: throw IllegalStateException("The clipboard does not contain text.")

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

private fun chooseCookiesFile(): File? {
    val chooser =
        JFileChooser(System.getProperty("user.home")).apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Import Netscape-format browser cookies"
            fileFilter = FileNameExtensionFilter("Cookie files (*.txt)", "txt")
        }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun chooseAria2cExecutable(currentPath: String): File? {
    val initialDirectory =
        File(currentPath).takeIf { it.isDirectory } ?: File(currentPath).parentFile ?: File(System.getProperty("user.home"))
    val chooser =
        JFileChooser(initialDirectory).apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            dialogTitle = "Select aria2c executable"
            fileFilter = FileNameExtensionFilter("aria2c executable", "exe")
        }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

private fun createBrowserBridgeToken(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
