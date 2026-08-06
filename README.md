# LoopingVid — Professional Creator Studio

Native Android application for video looping, audio mastering, visualizer studio,
slideshow creation, project management, and 24/7 RTMP live streaming.

---

## Aplikasi Utama

Kode sumber utama berada di **`app/`** (Kotlin / Jetpack Compose).

> Folder `flutter_app/` adalah eksperimen lama dan **bukan** aplikasi resmi.
> Jangan gunakan `flutter_app/` untuk build produksi.

---

## Arsitektur & Stack

| Layer | Teknologi |
|-------|-----------|
| Platform | Android native (Kotlin) |
| UI | Jetpack Compose + Material Design 3 (dark theme) |
| Arsitektur | MVVM + Repository Pattern |
| Database | Room v4 (migrasi non-destruktif v3→v4) |
| Media | Media3 / ExoPlayer, custom AudioProcessor |
| Render | FFmpeg via native library |
| Live | RTMP foreground service |
| AI | Firebase AI (Gemini) |
| Background | WorkManager (export queue) |
| Test | JUnit 4, Robolectric, Roborazzi |

---

## Fitur Stabil

- **Video Loop** — Normal, Crossfade, Ping-Pong dengan durasi target dan progress render.
- **Audio Mastering** — 5-Band EQ, Dynamics Compressor, Peak Limiter, LUFS Normalizer.
- **Video Editor** — Merge video/image + audio, text overlay, color grading, undo/redo.
- **Live Streaming** — YouTube / TikTok / Custom RTMP, foreground service, thermal monitoring.
- **History** — Render jobs dan live sessions tersimpan di Room.
- **Settings** — Output directory, stream key terenkripsi, preview toggle.
- **Project Manager** — Simpan, buka, rename, duplikasi, arsip, dan hapus proyek.

## Fitur Eksperimental

- **Visualizer Studio** — Preview audio-reactive 6 mode, beat detection, export ke video.
- **Slideshow** — Multi-image dengan transisi dan audio. Masih dalam pengembangan.
- **Beat Sync** — Deteksi BPM, Tap BPM, marker manual, efek reaktif.

---

## Build

Prasyarat:
- JDK 17 (Temurin)
- Android SDK (compileSdk 36)
- Gradle 9.3.1 (wrapper sudah tersedia)

```bash
# Debug build
./gradlew :app:assembleDebug

# Unit tests
./gradlew :app:testDebugUnitTest

# Compile check
./gradlew :app:compileDebugKotlin
```

Lokasi APK output: `app/build/outputs/apk/debug/app-debug.apk`

> **Catatan:** Host dengan RAM < 4 GB mungkin mengalami OOM saat Gradle daemon.
> Gunakan `--no-daemon --max-workers=1` atau CI (GitHub Actions) untuk full build.

---

## CI / GitHub Actions

Workflow native: `.github/workflows/build-native-apk.yml`
- Trigger: push ke `main` dan `feature/pro-ui-visualizer`
- Steps: unit test → assembleDebug → upload APK artifact

---

## Navigasi Utama

| Tab | Konten |
|-----|--------|
| Beranda | Dashboard proyek dan shortcut |
| Studio | Video Loop, Video Editor, Audio Mastering, Visualizer Studio, Slideshow |
| Live | RTMP streaming studio |
| Proyek | Project Manager |

Menu tambahan: Pengaturan, Panduan, Riwayat, Tentang, Kebijakan Privasi, Dukungan.

---

## Struktur Folder

```
app/src/main/java/com/example/
├── MainActivity.kt
├── core/
│   ├── database/       # Room DB, DAOs, Entities, Migrations, Repository
│   ├── media/          # AudioProcessor, Waveform, AutoSave, Templates
│   ├── ffmpeg/         # FFmpegWrapper, MediaProcessor, VisualizerProcessor
│   ├── work/           # VideoExportWorker, ExportQueueViewModel
│   ├── utils/          # MediaStoreExporter, ThermalMonitor, etc.
│   └── ui/             # Shared UI components
├── feature/
│   ├── loop/           # Video Loop
│   ├── mastering/      # Audio Mastering
│   ├── editor/         # Video Editor
│   ├── visualizer/     # Visualizer Studio + Beat Sync
│   ├── slideshow/      # Slideshow creator
│   ├── live/           # Live Streaming + foreground service
│   ├── project/        # Project Manager
│   ├── history/        # Render/Live history
│   ├── settings/       # App settings
│   └── about/          # About, Privacy, Support
└── ui/
    ├── navigation/     # NavDestination, MainScreen, AppNavHost
    └── theme/          # Color, Theme, Type
```

---

## Catatan Teknis

- Database menggunakan migrasi eksplisit dari v3 ke atas. Versi 1-2 masih memakai destructive fallback.
- Schema JSON Room (`app/schemas/`) di-export oleh KSP saat build.
- Debug keystore di-generate oleh CI; tidak disimpan di repository.
- ABI native saat ini: `armeabi-v7a`, `x86`. Penambahan `arm64-v8a` menunggu verifikasi library FFmpeg.
