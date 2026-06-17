import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "prasad.vennam.moneypilot"
    compileSdk = 37

    defaultConfig {
        applicationId = "prasad.vennam.moneypilot"
        minSdk = 24
        targetSdk = 37
        versionCode =
            libs.versions.versionCode
                .get()
                .toInt()
        versionName = libs.versions.versionName.get()

        val googleClientId = localProperties.getProperty("GOOGLE_CLIENT_ID", "\"\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", googleClientId)

        val bannerProdId = localProperties.getProperty("BANNER_PROD_ID", "\"\"")
        buildConfigField("String", "BANNER_PROD_ID", bannerProdId)

        val interstitialProdId = localProperties.getProperty("INTERSTITIAL_PROD_ID", "\"\"")
        buildConfigField("String", "INTERSTITIAL_PROD_ID", interstitialProdId)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713" // Default to Test ID
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("moneypilot.jks")
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5908710502730117~9956102404" // Production ID
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.adaptive.layout)
    implementation(libs.androidx.compose.adaptive.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.google.mlkit.text.recognition)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.auth)
    implementation(libs.googleid)
    implementation(libs.billing.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.kotlinx.metadata)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.logging.interceptor)
    implementation(libs.material)
    implementation(libs.androidx.biometric)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    implementation(libs.retrofit)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    "ksp"(libs.androidx.room.compiler)
    "ksp"(libs.moshi.kotlin.codegen)
    implementation(libs.androidx.work.runtime)
    implementation(libs.litert.lm.android)
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher.android)
}

tasks.withType<JavaCompile>().configureEach {
    doFirst {
        val path = options.annotationProcessorPath
        if (path != null) {
            options.annotationProcessorPath = path.filter { !it.name.contains("moshi-kotlin-codegen") }
        }
    }
}
