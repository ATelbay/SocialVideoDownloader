plugins {
    id("svd.kmp.feature")
}

val composeExt = the<org.jetbrains.compose.ComposeExtension>()

compose.resources {
    packageOfResClass = "com.socialvideodownloader.shared.feature.download.generated.resources"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(composeExt.dependencies.materialIconsExtended)
            implementation(project(":shared:ui"))
            implementation(project(":shared:data"))
            implementation(project(":shared:network"))
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.feature.download"
}
