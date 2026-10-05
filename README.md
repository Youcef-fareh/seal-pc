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

## Downloading

1. Paste a YouTube video link and select **Load formats**.
2. Choose a resolution for video, or select audio-only and choose an audio format.
3. Pick a download folder and select **Download**.

Video downloads use yt-dlp's best available video and audio streams at or below the selected resolution, then merge them when necessary. Audio downloads are converted by yt-dlp/FFmpeg to the selected format and quality. The app supports individual YouTube videos; playlist downloads are not included in this desktop version.

## Standalone repository

This folder is its own Git repository. Its `.gitignore` and `.github/workflows/build.yml` are scoped to this desktop app.

## License

Seal Desktop is distributed under the GNU General Public License, version 3 or later. See [LICENSE](LICENSE).
