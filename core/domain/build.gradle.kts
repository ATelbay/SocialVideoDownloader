plugins {
    id("svd.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.socialvideodownloader.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            // javax.inject for @Qualifier annotation (Hilt compatibility on Android)
            implementation(libs.javax.inject)
        }
    }
}
