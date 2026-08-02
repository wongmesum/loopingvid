# core/ffmpeg Module Documentation

The `core/ffmpeg` module provides video seamless looping, audio LUFS mastering, spectrum rendering, and command execution facilities for **LoopingVid Studio**.

---

## 1. Module Overview

- **`FFmpegWrapper`**: Clean Kotlin interface exposing async command execution (`execute`), status flow (`logFlow`), version checks, and cancellation support.
- **`FFmpegWrapperImpl`**: Production-ready wrapper implementation. Attempts to load native NDK JNI shared libraries (`libffmpeg_wrapper.so`), falling back gracefully to high-performance soft emulation when native binaries are absent in pure Android environments.
- **`FFmpegCommandBuilder`**: High-level utility to construct FFmpeg argument lists for:
  - Video seamless seamless looping (`-stream_loop`, `xfade` transitions)
  - Audio EBU R128 (`loudnorm`) mastering
  - Spectrum visualizer overlays and filtergraphs
- **`MediaProcessor`**: Coroutine engine orchestrating async rendering jobs, database persistence, and progress state.

---

## 2. NDK Build & `ffmpeg-kit` Integration Requirements

To compile and link full native `ffmpeg-kit` or custom FFmpeg C/C++ builds on Android, satisfy the following configuration steps:

### A. Android NDK Requirements
- **Recommended NDK Version**: `25.2.9519653` (or NDK r25+)
- **Minimum CMake Version**: `3.22.1`
- **Target ABIs**: `arm64-v8a`, `armeabi-v7a`, `x86_64`

### B. `app/build.gradle.kts` Configuration
Add the NDK toolchain and CMake reference in `app/build.gradle.kts`:

```kotlin
android {
    ndkVersion = "25.2.9519653"

    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -O3")
                arguments("-DANDROID_STL=c++_shared")
                abiFilters("arm64-v8a", "armeabi-v7a", "x86_64")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            pickFirsts += setOf("**/libffmpeg.so", "**/libavcodec.so", "**/libavformat.so", "**/libavutil.so")
        }
    }
}
```

### C. `CMakeLists.txt` Structure (`app/src/main/cpp/CMakeLists.txt`)

```cmake
cmake_minimum_required(VERSION 3.22.1)
project("ffmpeg_wrapper")

# Import FFmpeg prebuilt shared libraries or ffmpeg-kit
add_library(ffmpeg_wrapper SHARED
    native-ffmpeg.cpp
)

find_library(log-lib log)

target_link_libraries(
    ffmpeg_wrapper
    ${log-lib}
)
```

### D. JNI Bridge Signature (`native-ffmpeg.cpp`)

Implement the native C/C++ bridge matching `com.example.core.ffmpeg.FFmpegWrapperImpl`:

```cpp
#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "FFmpegNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_example_core_ffmpeg_FFmpegWrapperImpl_executeCommandNative(
        JNIEnv* env,
        jobject thiz,
        jobjectArray args) {
    int argc = env->GetArrayLength(args);
    LOGI("Executing native FFmpeg command with %d arguments", argc);
    // Invoke ffmpeg_execute_cmd(argc, argv) from ffmpeg-kit C library
    return 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_core_ffmpeg_FFmpegWrapperImpl_cancelExecutionNative(
        JNIEnv* env,
        jobject thiz) {
    LOGI("Cancelling native FFmpeg execution");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_core_ffmpeg_FFmpegWrapperImpl_getFFmpegVersionNative(
        JNIEnv* env,
        jobject thiz) {
    return env->NewStringUTF("FFmpeg v6.1-native (ffmpeg-kit gpl)");
}
```

### E. Option: Direct Dependency via `ffmpeg-kit` Maven
Alternatively, add the precompiled `ffmpeg-kit` dependency in `app/build.gradle.kts`:

```kotlin
dependencies {
    // FFmpeg-Kit Full GPL package with x264 & mp3lame support
    implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0-2")
}
```

---

## 3. Usage Example in ViewModels / Services

```kotlin
val ffmpegWrapper: FFmpegWrapper = FFmpegWrapperImpl(context)

val command = FFmpegCommandBuilder.buildNormalLoopCommand(
    inputPath = "/path/to/input.mp4",
    outputPath = "/path/to/output.mp4",
    loopCount = 4,
    presetQuality = "1080p"
)

ffmpegWrapper.execute(command) { progressPercent ->
    Log.d("FFmpeg", "Rendering progress: $progressPercent%")
}
```
