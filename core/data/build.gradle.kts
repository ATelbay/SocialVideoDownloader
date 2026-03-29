plugins {
    id("svd.android.library")
    id("svd.android.hilt")
    id("svd.android.room")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.socialvideodownloader.core.data"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "YTDLP_SERVER_URL", "\"${project.findProperty("ytdlp.server.url") ?: "http://13.50.106.77:8000"}\"")
        buildConfigField("String", "YTDLP_API_KEY", "\"${project.findProperty("ytdlp.api.key") ?: ""}\"")

    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":shared:data"))
    implementation(project(":shared:network"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.youtubedl.android.library)
    implementation(libs.youtubedl.android.ffmpeg)
    implementation(libs.youtubedl.android.aria2c)

    testImplementation(libs.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
