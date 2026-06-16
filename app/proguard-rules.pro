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
#-renamesourcefileattribute SourceFile

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
# Keep data classes that Moshi will serialize/deserialize
-keep class prasad.vennam.moneypilot.data.entity.** { *; }
-keep class prasad.vennam.moneypilot.util.FinancePriceFetcher$** { *; }

# SQLCipher for Android
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class * implements kotlinx.serialization.KSerializer {
    *;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    *** write$Self$*(...);
}

# Keep all Navigation Destinations
-keep class prasad.vennam.moneypilot.ui.navigation.Destination** { *; }

# LiteRT-LM (formerly TensorFlow Lite Edge AI)
-keep class com.google.ai.edge.litertlm.** { *; }
-keep class org.tensorflow.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
-dontwarn org.tensorflow.**

# Google ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Google Tink / AndroidX Security Crypto
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Google Play Services Auth & Credentials Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**