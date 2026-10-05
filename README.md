# Seal Desktop

A Windows desktop companion to Seal for downloading videos and audio from YouTube. Choose a video resolution or audio format, select where files are saved, and follow download progress in the app.

> Download only videos and audio that you have the right to save. You are responsible for complying with YouTube's terms and applicable copyright laws.

## Requirements

- Windows 10 or later (64-bit)
- Java 21 for running from source
- [yt-dlp](https://github.com/yt-dlp/yt-dlp#installation) available on `PATH`, or the path to `yt-dlp.exe`
- [FFmpeg](https://ffmpeg.org/download.html) available on `PATH` for combining separate video/audio streams and converting audio

The app does not bundle yt-dlp or FFmpeg. Install and update these tools from their official sources. YouTube may change its service at any time; if downloads stop working, update yt-dlp.

## Run from source

1. Install a Java 21 JDK and yt-dlp. Install FFmpeg for format merging and audio conversion.
2. Open PowerShell in this folder.
3. Run:

   ```powershell
   .\gradlew.bat run
   ```

If you already have Gradle installed, `gradle run` also works.

The first launch can take a few minutes while Gradle downloads its dependencies.

## Build Windows installers

```powershell
.\gradlew.bat packageExe packageMsi
```

The `.exe` and `.msi` installers are created under `build/compose/binaries/main/exe/` and `build/compose/binaries/main/msi/`.

## Download the Windows app

The GitHub Actions workflow tests the project and builds both Windows installers. Open the repository's **Actions** tab and download the `seal-desktop-windows-installers` artifact from a successful run. To publish both installers on a GitHub Release, push a version tag such as `v1.0.0`; the workflow attaches the installers to that release automatically.

Seal Desktop checks GitHub Releases for updates at startup and offers **Download & install** or **Remind me later**. It downloads the full Windows MSI only after you choose to install. Windows starts the MSI installer so you can review and confirm the installation. Reminders are snoozed for 24 hours.

Updates use the complete installer rather than incremental patches. This keeps updates reliable and avoids downloading anything until you approve; delta updates would require a separate patching and recovery system.

## Downloading

1. Paste a YouTube video link and select **Load formats**.
2. Choose video resolution or audio format/quality and a download folder, then select **Add to download queue**.
3. Repeat for each video. Choose **Start downloads** when you are ready; queued videos download one at a time.
4. Pause and resume individual active downloads, or remove a video from the queue.

Video and audio streams are merged into one MKV file using FFmpeg. Audio-only downloads are converted by yt-dlp/FFmpeg to the selected format and quality. Paused downloads can resume from their partial files. The app supports individual YouTube videos; playlist downloads are not included in this desktop version. Choose English or Arabic from the language menu; the Arabic interface uses right-to-left layout.

## Standalone repository

This folder is its own Git repository. Its `.gitignore` and `.github/workflows/build.yml` are scoped to this desktop app.

## License

Seal Desktop is distributed under the GNU General Public License, version 3 or later. See [LICENSE](LICENSE).
