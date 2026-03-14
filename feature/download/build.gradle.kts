plugins {
    id("svd.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.socialvideodownloader.feature.download"
}

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
