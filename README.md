# LoopingVid Android Studio

A native Android mobile application designed for video looping, audio mastering, video composition/editing, job history management, and 24/7 RTMP live streaming with infinite seamless repeat.

---

## Architecture & Technology Stack

- **Platform:** Native Android (Kotlin)
- **UI Framework:** Jetpack Compose with Material Design 3 Studio Theme
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence:** Room Database (`RenderJobEntity`, `LiveSessionEntity`, `AppSettingEntity`)
- **Media Engine:** Custom Coroutines MediaProcessor & DSP Audio Mastering Engine (5-Band Equalizer, Dynamics Compressor, Peak Limiter, LUFS Normalizer)
- **Audio Visualizers:** Custom Canvas-rendered reactive audio spectrum bars, wave, and circular visualizers
- **Background Live Streaming:** Foreground Service (`LiveStreamService`) with persistent notifications and direct pipeline integration
- **Testing:** Robolectric & JUnit 4 JVM tests for ViewModel, Repository, and Media DSP engines

---

## Package Structure

```
com.example/
├── MainActivity.kt
├── core/
│   ├── database/         # Room Database, DAOs, Entities, Repository
│   ├── media/            # Audio Spectrum, Waveform Analyzer, Mastering DSP
│   ├── ffmpeg/           # Coroutine MediaProcessor & Render Engine
│   └── ui/               # Common theme, components, colors, typography
├── feature/
│   ├── loop/             # Video Loop Studio (Normal, Crossfade, Ping-Pong)
│   ├── mastering/        # Audio Mastering Studio (5-Band EQ, Compression, LUFS)
│   ├── editor/           # Video Editor & Composition (Text overlays, Spectrum)
│   ├── live/             # Go Live Studio & Foreground Service (YouTube, TikTok, Custom RTMP)
│   ├── history/          # Render Jobs & Live Sessions History
│   └── settings/         # Storage paths, stream keys, preview settings
```

---

## Feature Parity & Roadmap

### Phase 1 Features (Implemented & Parity)
| Feature | Parity Status | Details |
|---|---|---|
| **Loop Tool** | ✅ Complete | Input picker, Target duration slider, Normal / Crossfade / Ping-Pong styles, Mute audio, Quality presets, Async render progress & cancel. |
| **Audio Mastering** | ✅ Complete | Waveform preview, 5-Band EQ, Dynamics Compressor, Peak Limiter, LUFS meter, Presets (Clear, Deep Bass, Vocal, Neutral), WAV/MP3/M4A export. |
| **Video Editor** | ✅ Complete | Merge video/image + audio track, Title & Watermark text overlays, Audio spectrum overlay (Bars, Wave, Circle), Preview canvas. |
| **Live Streaming (MVP Phase 1)** | ✅ Complete | Direct "Go Live with this" pipeline, YouTube / TikTok RTMP / Custom RTMP, Infinite loop streaming without gap, Live telemetry (Bitrate, RTT, Loop #N counter), Foreground Service. |
| **History** | ✅ Complete | Filterable history for Render Jobs and Live Streaming sessions, delete items, direct Go Live button. |
| **Settings** | ✅ Complete | Output directory config, High-Quality Preview toggle, Encrypted Stream Key manager, System hardware info. |

### Phase 2 Roadmap
- **Gallery Slideshow + Transitions:** Multi-image video generation.
- **Multi-Track Audio Mixer:** Ducking, BGM + Mic live mixing.
- **AI Transcription:** Gemini API integration for automated subtitles & LRC lyrics sync.
- **Simulcast Streaming:** Multi-platform simultaneous RTMP push.

---

## How to Build and Run

1. Open the project in **Android Studio** (Koala / Ladybug or newer).
2. Sync Gradle dependencies: `gradle :app:assembleDebug`
3. Run Unit Tests: `gradle :app:testDebugUnitTest`
4. Deploy to device or streaming emulator.

---

## Technical Notes

- **TikTok Live Disclaimer:** TikTok RTMP key input requires an eligible TikTok account with Live Studio / RTMP access.
- **Foreground Service:** Continuous live streaming runs in `LiveStreamService` with `mediaProjection|microphone` foreground type as required by Android 14+.
