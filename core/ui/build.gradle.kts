plugins {
    id("svd.android.library")
    id("svd.android.compose")
}

android {
    namespace = "com.socialvideodownloader.core.ui"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
