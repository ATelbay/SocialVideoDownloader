plugins {
    id("svd.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:data"))
            implementation(project(":shared:ui"))
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.feature.history"
}
