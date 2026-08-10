# core/ffmpeg Module Documentation

The `core/ffmpeg` module provides video seamless looping, audio LUFS mastering, spectrum rendering, and command execution facilities for **LoopingVid Studio**.

---

## 1. Module Overview

- **`FFmpegWrapper`**: Clean Kotlin interface exposing async command execution (`execute`), status flow (`logFlow`), version checks, and cancellation support.
- **`FFmpegWrapperImpl`**: Production implementation backed by FFmpegKit (maintained). Delegates all commands via FFmpegKit's async API (`executeWithArgumentsAsync`). Native engine initialization is fail-safe — absence is reported through `isNativeSupported()`.
- **`FFmpegCommandBuilder`**: High-level utility to construct FFmpeg argument lists for:
  - Video seamless looping (`-stream_loop`, `xfade` transitions)
  - Audio EBU R128 (`loudnorm`) mastering
  - Spectrum visualizer overlays and filtergraphs
- **`MediaProcessor`**: Coroutine engine orchestrating async rendering jobs, database persistence, and progress state.

---

## 2. FFmpegKit Integration

### Dependency

```kotlin
// gradle/libs.versions.toml
[libraries]
ffmpegkit = { module = "dev.ffmpegkit-maintained:ffmpeg-kit-full", version = "8.1.7" }

// app/build.gradle.kts
dependencies {
    implementation(libs.ffmpegkit)
}
```

- **Package**: `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7`
- **License**: LGPL-3.0 (NOT full-gpl)
- **API package**: `com.arthenica.ffmpegkit`
- **minSdk requirement**: 24

### ABI Policy

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
}
```

- **Shipped ABI**: `arm64-v8a` only (Google Play requirement).
- Legacy ABIs (`armeabi-v7a`, `x86`, `x86_64`) are excluded unless a concrete product requirement appears.

### Native Libraries in APK

The debug APK contains these native objects under `lib/arm64-v8a/`:

- `libffmpegkit.so`
- `libavcodec.so`
- `libavformat.so`
- `libavutil.so`
- `libavfilter.so`
- `libavdevice.so`
- `libswresample.so`
- `libswscale.so`
- `libc++_shared.so`
- Plus codec-specific libraries (openh264, etc.)

---

## 3. Architecture Notes

### No Custom JNI Bridge

This module does **not** use a custom NDK/CMake build or JNI bridge. All FFmpeg interaction goes through FFmpegKit's Java API:

```kotlin
// Async execution with argument array (no shell parsing, injection-safe)
FFmpegKit.executeWithArgumentsAsync(arguments, completeCallback, logCallback, statisticsCallback)

// Synchronous (used in instrumented smoke tests only)
FFmpegKit.executeWithArguments(arguments)

// Version query
FFmpegKitConfig.getFFmpegVersion()
```

### Command Safety

Commands are always passed as argument arrays, never as concatenated strings. This prevents path-space breakage and shell injection:

```kotlin
val args = listOf("-i", inputPath, "-c:v", encoder, "-o", outputPath)
FFmpegKit.executeWithArgumentsAsync(args.toTypedArray(), ...)
```

---

## 4. Usage Example

```kotlin
val ffmpegWrapper: FFmpegWrapper = FFmpegWrapperImpl(context)

// Execute with progress callback
val exitCode = ffmpegWrapper.execute(
    commandArgs = listOf("-i", inputPath, "-c:v", "libx264", "-preset", "ultrafast", outputPath),
    onProgress = { percent -> Log.d("FFmpeg", "Progress: $percent%") }
)
```

---

## 5. Instrumented Smoke Test

`FFmpegNativeSmokeTest` (androidTest) verifies on a real ARM64 device/emulator:

1. Native engine loads without `UnsatisfiedLinkError`
2. `-version` command executes successfully
3. `-encoders` inventory is queryable and contains at least one H.264 encoder
4. Synthetic encode (`lavfi testsrc`) produces a playable output file

The test writes `encoder-inventory.txt` to app cache for evidence capture.

---

## 6. Known Limitations

- **Runtime encoder set unknown**: The H.264 encoder used in command builders (`libx264`) has not been confirmed present in the shipped binary via actual `-encoders` output. Migration to the proven encoder is pending device/emulator execution.
- **No runtime proof yet**: Build host lacks an ARM64 execution target. CI instrumented test job is planned but not yet committed.
