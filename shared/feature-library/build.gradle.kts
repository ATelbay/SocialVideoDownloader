plugins {
    id("svd.kmp.feature")
}

val composeExt = the<org.jetbrains.compose.ComposeExtension>()

compose.resources {
    packageOfResClass = "com.socialvideodownloader.shared.feature.library.generated.resources"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(composeExt.dependencies.materialIconsExtended)
            implementation(project(":shared:data"))
            implementation(project(":shared:ui"))
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.feature.library"
}
