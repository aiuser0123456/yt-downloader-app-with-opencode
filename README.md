# YT Downloader

A modern Android app to download YouTube videos using yt-dlp and ffmpeg running directly on the device.

## Features

- Paste YouTube URL to fetch video info
- View thumbnail, title, views, duration, and description
- Select video quality (360p to 4K)
- Select audio quality (various bitrates)
- Parallel download of video and audio streams
- Automatic merging with ffmpeg
- Download history
- Material 3 modern UI with dark mode

## Requirements

Before building, you need to add two binary files:

### 1. yt-dlp binary

Download from: https://github.com/yt-dlp/yt-dlp/releases

Look for: `yt-dlp_linux_aarch64`

Rename the downloaded file to `libytdlp.so`

### 2. ffmpeg binary

Download from: https://github.com/nicoverbruggen/ffmpeg-binary-android/releases

Look for the `arm64-v8a` version in the releases.

Rename the downloaded file to `libffmpeg.so`

### 3. Place binaries

Put both files in:
```
app/src/main/jniLibs/arm64-v8a/
```

Your folder structure should look like:
```
app/
└── src/
    └── main/
        └── jniLibs/
            └── arm64-v8a/
                ├── libffmpeg.so
                └── libytdlp.so
```

## Building

### Option 1: Android Studio

1. Open the project in Android Studio
2. Sync Gradle
3. Run the app

### Option 2: Command Line

```bash
# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: GitHub Actions

1. Push to GitHub
2. Go to Actions tab
3. Run "Build Debug APK" workflow
4. Download the APK from Artifacts

## How It Works

1. User pastes a YouTube URL
2. App calls yt-dlp to fetch video metadata and available formats
3. User selects video quality and audio quality
4. App downloads both streams in parallel using yt-dlp
5. App merges video and audio using ffmpeg
6. Final file is saved to Downloads/yt-downloader/

## Troubleshooting

### "Binary not found" error

- Make sure the binaries are in `app/src/main/jniLibs/arm64-v8a/`
- Make sure they are named `libytdlp.so` and `libffmpeg.so`
- Restart the app after adding binaries

### App crashes on startup

- Check that you downloaded the ARM64 versions (not x86)
- Try uninstalling and reinstalling the app

### Diagnostics

Tap the wrench icon in the app to check binary status and versions.

## Technical Details

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Binaries**: yt-dlp (includes Python) + ffmpeg
