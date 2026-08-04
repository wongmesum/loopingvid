# Flutter
-keep class io.flutter.** { *; }
-keep class io.flutter.plugins.** { *; }
-dontwarn io.flutter.embedding.**

# FFmpeg Kit
-keep class com.antonkarpenko.ffmpegkit.** { *; }
-keep class com.antonkarpenko.smartexception.** { *; }
-dontwarn com.antonkarpenko.**

# Video Player / ExoPlayer / Media3
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Just Audio
-keep class com.ryanheise.just_audio.** { *; }
-dontwarn com.ryanheise.**

# SQLite / sqflite
-keep class com.tekartik.sqflite.** { *; }
-dontwarn com.tekartik.**

# File Picker
-keep class com.mr.flutter.plugin.filepicker.** { *; }
-dontwarn com.mr.flutter.plugin.**

# Permission Handler
-keep class com.baseflow.permissionhandler.** { *; }
-dontwarn com.baseflow.**

# Google Fonts (OkHttp)
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# Kotlin
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# General
-keep class androidx.** { *; }
-dontwarn androidx.**
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
