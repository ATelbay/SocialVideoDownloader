plugins {
    id("svd.kmp.library")
    id("svd.kmp.compose")
}

compose.resources {
    packageOfResClass = "com.socialvideodownloader.shared.di.generated.resources"
}

kotlin {
    sourceSets {
        // This module's only source set is iosMain (the Compose Multiplatform iOS shell
        // + Koin init). The Android app wires its own navigation/DI and does not depend on
        // :shared:di, so these dependencies live in iosMain rather than leaking to the
        // (empty) Android target.
        iosMain.dependencies {
            implementation(project(":shared:ui"))
            implementation(project(":shared:feature-download"))
            implementation(project(":shared:feature-history"))
            implementation(project(":shared:feature-library"))
            implementation(project(":shared:network"))
            implementation(project(":shared:data"))
            implementation(libs.koin.core)
            implementation(libs.navigation.compose.multiplatform)
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.di"
}
