# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# App models
-keep class com.mrbaigsdownloader.data.** { *; }

# youtubedl-android
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
