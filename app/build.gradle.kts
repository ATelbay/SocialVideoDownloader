@file:Suppress("DSL_SCOPE_VIOLATION")

import java.util.Properties

plugins {
    id("svd.android.application")
    id("svd.android.compose")
    id("svd.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.reader().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.socialvideodownloader"

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile", "../svd.jks"))
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }

    defaultConfig {
        applicationId = "com.socialvideodownloader"
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += setOf("arm64-v8a", "x86_64") }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.keepDebugSymbols += "**/*.zip.so"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                abiFilters.clear()
                abiFilters += setOf("arm64-v8a")
            }
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":feature:download"))
    implementation(project(":feature:history"))
    implementation(project(":feature:library"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":core:cloud"))
    implementation(project(":core:billing"))
    implementation(project(":shared:data"))
    implementation(project(":shared:network"))
    implementation(project(":shared:feature-download"))
    implementation(project(":shared:feature-history"))
    implementation(project(":shared:feature-library"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
    implementation(libs.youtubedl.android.aria2c)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
