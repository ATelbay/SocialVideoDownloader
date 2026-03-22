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

    testImplementation(libs.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
