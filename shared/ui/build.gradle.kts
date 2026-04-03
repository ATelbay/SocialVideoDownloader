plugins {
    id("svd.kmp.library")
    id("svd.kmp.compose")
}

val composeExt = the<org.jetbrains.compose.ComposeExtension>()

compose.resources {
    packageOfResClass = "com.socialvideodownloader.shared.ui.generated.resources"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(composeExt.dependencies.materialIconsExtended)
            implementation(libs.coil3.compose)
            implementation(libs.coil3.network.ktor3)
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.ui"
}
