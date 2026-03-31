plugins {
    id("svd.kmp.library")
    id("svd.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:ui"))
            implementation(project(":shared:feature-download"))
            implementation(project(":shared:feature-history"))
            implementation(project(":shared:feature-library"))
            implementation(libs.koin.core)
            implementation(libs.navigation.compose.multiplatform)
        }
        iosMain.dependencies {
            implementation(project(":shared:network"))
            implementation(project(":shared:data"))
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.di"
}
