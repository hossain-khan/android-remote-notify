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


# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Circuit
-keep class * extends com.slack.circuit.foundation.Circuit { *; }
-keep @com.slack.circuit.codegen.annotations.CircuitInject class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Retrofit/OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# Moshi
-keep class * extends com.squareup.moshi.JsonAdapter
-keep class dev.hossain.remotenotify.** { *; }
-keepclassmembers class dev.hossain.remotenotify.** { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }

# WorkManager
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# https://issuetracker.google.com/issues/413078297?pli=1
# Thanks for the quick fix! Confirming the issue is resolved in DataStore 1.1.7.
# DataStore Preferences specific rules
#-keep class androidx.datastore.preferences.protobuf.** { *; }
#-keep class androidx.datastore.preferences.internal.** { *; }

# Generic rules for Protocol Buffers often used by DataStore
#-dontwarn com.google.protobuf.**
#
#-keepclassmembers class com.google.protobuf.** {
#    <fields>;
#    <methods>;
#}