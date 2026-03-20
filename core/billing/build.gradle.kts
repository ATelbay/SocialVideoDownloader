plugins {
    id("svd.android.library")
    id("svd.android.hilt")
}

android {
    namespace = "com.socialvideodownloader.core.billing"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.play.billing)
}
