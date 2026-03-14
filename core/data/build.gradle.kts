plugins {
    id("videograb.android.library")
    id("videograb.android.hilt")
    id("videograb.android.room")
}

android {
    namespace = "com.videograb.core.data"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
    implementation(libs.youtubedl.android.aria2c)

    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
