# LoopingVid ProGuard/R8 Rules
# =====================================================

# --- General Android ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# --- Kotlin ---
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Retrofit ---
-dontwarn retrofit2.**
-keep,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# --- Moshi (KSP codegen handles adapters, but keep @Json annotations) ---
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * { @com.squareup.moshi.Json <fields>; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }
-keep @com.squareup.moshi.JsonQualifier @interface *

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- FFmpeg Kit ---
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# --- Compose (R8 full mode compatibility) ---
-dontwarn androidx.compose.**

# --- Vico charting ---
-keep class com.patrykandpatrick.vico.** { *; }

# --- Application data classes used by Moshi/Room ---
-keep class com.example.**.entity.** { *; }
-keep class com.example.**.model.** { *; }

# --- Timber ---
-dontwarn org.jetbrains.annotations.**
