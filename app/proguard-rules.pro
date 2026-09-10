# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# FFmpegKit (io.github.maitrungduc1410 fork) uses JNI. Keep its classes and native
# methods so that enabling minification/R8 in the future does not break the engine.
-keep class com.arthenica.ffmpegkit.** { *; }
-keepclassmembers class com.arthenica.ffmpegkit.** {
    native <methods>;
}
-dontwarn com.arthenica.ffmpegkit.**

# RootEncoder (RTMP/RTSP/SRT streaming) uses native code and reflection-friendly classes.
-keep class com.pedro.** { *; }
-dontwarn com.pedro.**
