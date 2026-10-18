# Seal Desktop

A Windows desktop companion to Seal for downloading videos and audio from sites supported by yt-dlp. Choose a video resolution or audio format, select where files are saved, and follow download progress in the app.

> Download only videos and audio that you have the right to save. You are responsible for complying with YouTube's terms and applicable copyright laws.

## Requirements

- Windows 10 or later (64-bit)
- Java 21 for running from source
- [yt-dlp](https://github.com/yt-dlp/yt-dlp#installation) available on `PATH`, or the path to `yt-dlp.exe` (not required for the Windows installer)
- FFmpeg available on `PATH` for running from source; the Windows installers bundle FFmpeg for format merging and audio conversion

The Windows installers bundle yt-dlp, Deno (the JavaScript runtime yt-dlp uses for YouTube extraction), and an LGPL shared build of FFmpeg, so users do not need to install these tools or add them to `PATH`. Running from source still requires yt-dlp, Deno, and FFmpeg installed separately. Bundled tools are downloaded and checksum-verified during packaging. YouTube may change its service at any time; if downloads stop working, update yt-dlp.

## Run from source

1. Install a Java 21 JDK, yt-dlp, Deno, and FFmpeg for format loading, merging, and audio conversion.
2. Open PowerShell in this folder.
3. Run:

   ```powershell
   .\gradlew.bat run
   ```

If you already have Gradle installed, `gradle run` also works.

The first launch can take a few minutes while Gradle downloads its dependencies.

## Build Windows installers

```powershell
.\gradlew.bat packageMsi
```

The standalone `.msi` installer is created under `build/compose/binaries/main/msi/`. It bundles the Java runtime, yt-dlp, Deno, and FFmpeg; users do not need to download or install additional components after setup.

## Download the Windows app

The GitHub Actions workflow tests the project and builds the standalone MSI. Open the repository's **Actions** tab and download the `seal-desktop-windows-installer` artifact from a successful run. To publish the installer on a GitHub Release, push a version tag such as `v1.0.0`; the workflow attaches the MSI to that release automatically.

Seal Desktop checks GitHub Releases for updates at startup and offers **Download & install** or **Remind me later**. It downloads the full Windows MSI only after you choose to install. Windows starts the MSI installer so you can review and confirm the installation. Reminders are snoozed for 24 hours.

Updates use the complete installer rather than incremental patches. This keeps updates reliable and avoids downloading anything until you approve; delta updates would require a separate patching and recovery system.

## Downloading

1. Paste, drag, or drop a valid HTTP(S) URL from a yt-dlp-supported site and select **Load formats**. The app delegates site detection to yt-dlp and reports unsupported URLs with the backend error details. YouTube playlists have a selection panel; other URLs are loaded as individual downloads. Optional clipboard monitoring is off by default; when enabled, the app prompts before using a detected link. `Ctrl+Shift+V` reads a URL from the clipboard, `Ctrl+Enter` starts queued downloads, `Ctrl+Shift+P` pauses/resumes a download, and `Ctrl+Shift+O` scrolls to settings.
2. Choose video resolution or audio format/quality and a download folder, then select **Add to download queue**.
3. Repeat for each video. Reorder waiting items with **Move up** and **Move down**. Choose **Start downloads** when you are ready; the concurrency setting supports one to four simultaneous downloads.
4. Pause and resume individual active downloads, or remove a video from the queue. Partial files are retained and yt-dlp resumes them with `--continue`.

Video downloads select either a paired video-and-audio stream or a progressive stream that already contains audio. They are merged into one playable file using FFmpeg when necessary. The app prefers MP4 video with M4A/AAC audio for Windows-player compatibility, ignores personal yt-dlp config that could override the selected format, and reports download/post-processing errors instead of ignoring them. The optional format menu can select a specific available video stream. Audio-only downloads are converted by yt-dlp/FFmpeg to the selected format and quality. Playlist entries are queued as individual video downloads, so each item retains normal progress, retry, and history behavior.

Download settings include a speed limit, an HTTP/SOCKS proxy URL (without embedded credentials), thumbnail saving and audio thumbnail embedding, metadata/chapter embedding, and an output template. Subtitle controls support uploaded captions, automatic captions, or both; select English (`en`), Arabic (`ar`), French (`fr`), all languages, or enter comma-separated language codes. Captions can be converted to SRT or WebVTT and embedded into video files, or saved as sidecars; audio-only downloads always save subtitles as sidecars. YouTube automatic captions are included when **Automatic captions** or **Uploaded + automatic** is selected and matching captions are available. Templates accept `title`, `id`, `uploader`, `upload_date`, `resolution`, `ext`, `extractor`, and `extractor_key`; use slash-separated components for folders, for example `%(extractor_key)s/%(uploader)s/%(upload_date)s/%(title)s [%(id)s].%(ext)s`. SponsorBlock can remove the selected segment categories (the default selection removes sponsor, intro, outro, and self-promotion segments). The optional aria2c integration uses a user-installed `aria2c` executable for protocols supported by yt-dlp's external downloader; aria2c is not bundled. The scheduler accepts local time in `YYYY-MM-DD HH:MM` format; keep the app running until the scheduled time.

For authenticated downloads, **Import cookies file** selects a browser-exported Netscape `cookies.txt` file. The app passes its path to yt-dlp; it does not copy, encrypt, or include cookie contents in diagnostics or settings backups. Cookie files can grant account access, so protect the selected file and clear its path in settings when it is no longer needed.

The advanced yt-dlp argument template is saved with app preferences and settings backups. Put one supported argument per line; values go on the following line or can use `--option=value`. Templates are limited to 32 arguments, 1,000 characters per argument, and 7,000 characters overall. The app passes arguments directly to yt-dlp without a shell and only accepts an allowlist of format, subtitle, date/size/title-filter, playlist-item, extractor, and compatibility options. When dedicated subtitle controls are enabled, configure subtitle options there rather than duplicating them in the advanced template. The app intentionally blocks options that could replace managed output paths, execute commands, or change authentication and network settings.

## Browser extension

The optional Chrome/Edge Manifest V3 extension is in [`browser-extension/`](./browser-extension/). To set it up:

1. Open `chrome://extensions` (Chrome) or `edge://extensions` (Edge), turn on **Developer mode**, and choose **Load unpacked**. Select the `browser-extension` folder.
2. In Seal Desktop, open **Settings** → **Network and authentication**, turn on **Enable authenticated browser extension bridge**, and choose **Copy token**.
3. Open the browser's **Extensions** menu, select **Seal Desktop Link Sender** → **Extension options**, paste the token, and choose **Save token**. You can also open these settings from the extension popup.
4. Pin **Seal Desktop Link Sender** from the browser's Extensions menu so its toolbar button stays visible. Keep Seal Desktop open with the bridge enabled.
5. Open a single YouTube video, click the pinned toolbar button, then choose **Add current video to Seal Desktop**. This queues the video in Seal Desktop; choose **Start downloads** in the app to begin downloading.

The extension does not show an icon over the video page and currently supports single YouTube videos only, not Instagram. For other yt-dlp-supported sites, paste the video URL into Seal Desktop directly. The bridge binds only to `127.0.0.1`, accepts only extension-origin requests, requires the private bearer token, rejects playlists, and is stopped when the app closes or the setting is disabled. The token is stored in local app preferences and is not included in settings backups.

Choose **System**, **Light**, **Dark**, or **High contrast** appearance from the theme menu. The layout is scrollable and constrained to a readable maximum width, and interactive controls expose their labels to assistive technology. English and Arabic are supported, with right-to-left layout for Arabic. yt-dlp-supported sites can be loaded as individual downloads; YouTube playlists additionally support selecting individual entries.

Completed, failed, and cancelled downloads are saved in the **Download history** section. Failed entries can be added back to the queue with **Retry**. The history file is stored at `%LOCALAPPDATA%\SealDesktop\download-history.json`. The **Open diagnostics log** button opens the log in the default text editor; yt-dlp output and application errors are recorded at `%LOCALAPPDATA%\SealDesktop\logs\seal-desktop.log`, with the previous log retained as `seal-desktop.log.1`. Download completion and failure notifications use the Windows notification area when system-tray notifications are available.

The storage panel reports history counts, estimated completed-download size, and scanned disk usage for current and historical download folders. It can identify `.part` files older than 30 days; deletion requires confirmation and is restricted to the recorded download folders. **Export settings** and **Import settings** back up or restore the app’s preferences and output template as JSON; they do not include download history, browser bridge tokens, or cookie contents.

## Standalone repository

This folder is its own Git repository. Its `.gitignore` and `.github/workflows/build.yml` are scoped to this desktop app.

## License

Seal Desktop is distributed under the GNU General Public License, version 3 or later. See [LICENSE](LICENSE).
