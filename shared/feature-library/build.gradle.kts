plugins {
    id("svd.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:data"))
        }
        iosMain.dependencies {
            implementation(project(":shared:feature-download"))
            implementation(project(":shared:feature-history"))
            implementation(project(":shared:network"))
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.feature.library"
}
