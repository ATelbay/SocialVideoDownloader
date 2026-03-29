plugins {
    id("svd.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:data"))
        }
    }
}

android {
    namespace = "com.socialvideodownloader.shared.feature.library"
}
