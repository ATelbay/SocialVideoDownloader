plugins {
    id("svd.android.library")
    id("svd.android.hilt")
}

android {
    namespace = "com.socialvideodownloader.core.billing"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        // Base64 RSA public key from the Play Console used to verify purchase
        // signatures. Set `play.billing.public.key` in ~/.gradle/gradle.properties
        // (never commit it). Empty by default => verification is skipped (opt-in),
        // so the upgrade flow still works in debug/CI before the key is wired in.
        buildConfigField(
            "String",
            "BASE64_PUBLIC_KEY",
            "\"${project.findProperty("play.billing.public.key") ?: ""}\"",
        )
    }
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
