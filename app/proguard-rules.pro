# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Moshi
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# Keep generated JsonAdapters
-keep class *JsonAdapter { *; }
-keep class **JsonAdapter { *; }

# Keep members of any class annotated with @JsonClass
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep @com.squareup.moshi.JsonClass class ** { *; }

# Keep data classes that Moshi will serialize/deserialize
-keep class prasad.vennam.moneypilot.data.entity.** { *; }
-keep class prasad.vennam.moneypilot.data.model.** { *; }
-keep class prasad.vennam.moneypilot.util.FinancePriceFetcher$** { *; }
-keep class prasad.vennam.moneypilot.feature.ai.model.** { *; }
-keep class prasad.vennam.moneypilot.feature.ai.service.** { *; }
-keep class prasad.vennam.moneypilot.util.QuotesManager$Quote { *; }
-keep class prasad.vennam.moneypilot.util.ParsedReceipt { *; }

# SQLCipher for Android
-keep class net.sqlcipher.** { *; }
-keep interface net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
-keep class net.zetetic.** { *; }
-keep interface net.zetetic.** { *; }
-dontwarn net.zetetic.**

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

# Google Sheets Sync DTOs
-keep class prasad.vennam.moneypilot.util.DriveFilesResponse { *; }
-keep class prasad.vennam.moneypilot.util.DriveFile { *; }
-keep class prasad.vennam.moneypilot.util.BatchGetSpreadsheetResponse { *; }
-keep class prasad.vennam.moneypilot.util.ValueRange { *; }

# Keep Kotlin Metadata for Moshi reflection
-keep class kotlin.Metadata { *; }