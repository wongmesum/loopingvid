# LoopingVid Flutter

A Flutter-based Android application for video looping, audio mastering, video composition/editing, job history management, and 24/7 RTMP live streaming with infinite seamless repeat.

> **No Android Studio required!** This project builds entirely via GitHub Actions CI/CD.

---

## Features

| Feature | Description |
|---------|-------------|
| **Loop Studio** | Video looping with Normal, Crossfade, and Ping-Pong styles. Configurable target duration, quality presets (480p-4K), mute audio option. |
| **Audio Mastering** | 5-Band Equalizer, Dynamics Compressor, Peak Limiter, LUFS Normalizer. Presets: Neutral, Clear Vocal, Deep Bass, Bright Pop, Warm Jazz. Export as WAV/MP3/M4A/FLAC. |
| **Video Editor** | Merge video/image + audio, title & watermark text overlays, audio spectrum visualizer (Bars, Wave, Circle), live preview canvas. |
| **Go Live** | RTMP streaming to YouTube, TikTok, or custom server. Infinite loop without gaps. Live telemetry: bitrate, uptime, loop counter. |
| **History** | Filterable job history (render jobs + live sessions). Delete items, re-stream from history. |
| **Settings** | Output directory, high-quality preview toggle, Firestore cloud sync, encrypted API key management, system info. |

---

## Tech Stack

- **Framework:** Flutter (Dart) - Stable channel
- **UI:** Material Design 3 with adaptive theming (light/dark)
- **Architecture:** Stateful Widgets with separation of concerns
- **Navigation:** Bottom NavigationBar with 5 tabs + push navigation for Settings
- **Target Platform:** Android (minSdk 24, targetSdk 34)
- **Build System:** Gradle 8.3, AGP 8.2.2, Kotlin 1.9.22, Java 17
- **CI/CD:** GitHub Actions (automated APK builds)

---

## Project Structure

```
flutter_app/
├── lib/
│   ├── main.dart                          # App entry point
│   ├── app.dart                           # MaterialApp configuration
│   ├── core/
│   │   ├── theme/app_theme.dart           # Material 3 theme (light & dark)
│   │   └── navigation/app_router.dart     # Bottom nav + screen routing
│   └── features/
│       ├── loop/loop_screen.dart           # Video Loop Studio
│       ├── mastering/mastering_screen.dart # Audio Mastering Studio
│       ├── editor/editor_screen.dart       # Video Editor & Composition
│       ├── live/live_screen.dart           # Go Live RTMP Streaming
│       ├── history/history_screen.dart     # Job History
│       └── settings/settings_screen.dart   # App Settings
├── test/
│   ├── unit_test.dart                     # Unit tests
│   └── widget_test.dart                   # Widget tests
├── android/                               # Android platform files
│   ├── app/build.gradle                   # App-level Gradle config
│   ├── build.gradle                       # Project-level Gradle config
│   ├── settings.gradle                    # Gradle settings
│   └── gradle.properties                  # Gradle JVM config
├── assets/images/                         # App assets
├── pubspec.yaml                           # Flutter dependencies
├── analysis_options.yaml                  # Dart linter rules
└── .gitignore                             # Git ignore rules
```

---

## How to Build the APK (via GitHub Actions)

This project is designed to build **without Android Studio** using GitHub Actions.

### Option 1: Automatic Build (on push)

1. Push code to the `main` or `flutter-app` branch
2. The workflow triggers automatically
3. Wait for the build to complete (~5-8 minutes)
4. Download the APK from the workflow artifacts

### Option 2: Manual Trigger (workflow_dispatch)

1. Go to the repository on GitHub
2. Navigate to **Actions** tab
3. Select **"Build Flutter APK"** workflow
4. Click **"Run workflow"** button
5. Select branch and click **"Run workflow"**
6. Wait for completion, then download the artifact

### How to Download the APK

1. Go to **Actions** tab in your GitHub repository
2. Click on the completed workflow run (green checkmark)
3. Scroll down to the **Artifacts** section
4. Click **`app-release-apk`** to download the ZIP file
5. Extract the ZIP to get `app-release.apk`
6. Transfer the APK to your Android device and install

> **Note:** You may need to enable "Install from Unknown Sources" on your Android device.

---

## GitHub Actions Workflow

The CI/CD pipeline (`.github/workflows/build-apk.yml`) performs:

| Step | Command | Purpose |
|------|---------|---------|
| 1 | `actions/checkout@v4` | Clone repository |
| 2 | `actions/setup-java@v4` | Install Java 17 (Temurin) |
| 3 | `subosito/flutter-action@v2` | Install Flutter SDK (stable) |
| 4 | `flutter pub get` | Install Dart dependencies |
| 5 | `flutter analyze` | Static code analysis |
| 6 | `flutter test` | Run unit & widget tests |
| 7 | `flutter build apk --release` | Build release APK |
| 8 | `actions/upload-artifact@v4` | Upload APK as artifact (14-day retention) |

---

## Local Development (Optional)

If you want to build locally (without Android Studio), you only need:

### Prerequisites

- Flutter SDK (stable): https://docs.flutter.dev/get-started/install
- Java 17 (e.g., Temurin/Adoptium)
- Android SDK command-line tools (optional, for device deployment)

### Commands

```bash
# Navigate to flutter project
cd flutter_app

# Install dependencies
flutter pub get

# Run static analysis
flutter analyze

# Run tests
flutter test

# Build release APK
flutter build apk --release

# The APK will be at:
# build/app/outputs/flutter-apk/app-release.apk
```

---

## Bug Fixes from Original Project

The original native Kotlin/Jetpack Compose project had several critical issues that have been resolved in this Flutter rewrite:

| Issue | Original (Kotlin) | Fixed (Flutter) |
|-------|-------------------|-----------------|
| Fictional Kotlin version | `2.2.10` (doesn't exist) | Using Flutter stable Dart SDK |
| Fictional AGP version | `9.1.1` (doesn't exist) | AGP `8.2.2` (verified release) |
| Fictional KSP version | `2.3.5` (doesn't exist) | Not needed in Flutter |
| Deprecated JCenter | Used `jcenter.bintray.com` | Only Google + Maven Central |
| No Dependency Injection | Manual ViewModel creation | Clean stateful widget architecture |
| FFmpeg library mismatch | Declared vs imported mismatched | Using standard Flutter packages |
| Missing foreground service types | Android 14+ compliance issue | Proper manifest declarations |
| No CI/CD pipeline | No automated builds | Full GitHub Actions workflow |

---

## References

- [Flutter Documentation](https://docs.flutter.dev/)
- [Flutter GitHub Actions Setup](https://github.com/subosito/flutter-action)
- [Material Design 3 for Flutter](https://m3.material.io/develop/flutter)
- [GitHub Actions Artifacts](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts)
- [Android APK Signing](https://docs.flutter.dev/deployment/android)
- [Dart Analysis Options](https://dart.dev/tools/analysis)

---

## License

This project is provided as-is for educational and personal use.
