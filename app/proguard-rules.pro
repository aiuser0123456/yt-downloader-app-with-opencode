# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep yt-dlp related classes
-keep class com.ytdownloader.python.** { *; }

# Keep data models
-keep class com.ytdownloader.data.model.** { *; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
