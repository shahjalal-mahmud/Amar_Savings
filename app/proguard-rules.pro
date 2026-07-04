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

# ─── kotlinx.serialization ──────────────────────────────────────────────────
# Keep generated serializers so minification does not break JSON encoding/decoding.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.appriyo.amarsavings.**$$serializer { *; }
-keepclassmembers class com.appriyo.amarsavings.** {
    *** Companion;
}
-keepclasseswithmembers class com.appriyo.amarsavings.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── Google Sign-In (play-services-auth) ──────────────────────────────────
# The library reflects on several Identity / AuthorizationClient / Tasks
# classes at runtime. Without these keep rules R8 obfuscates or strips them
# in the release build and the One Tap / AuthorizationClient calls silently
# fail — the sign-in button stays in "Connecting…" forever.
-keep class com.google.android.gms.auth.api.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.auth.**
-dontwarn com.google.android.gms.common.**