plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // javax.inject for @Qualifier annotations
    implementation(libs.javax.inject)

    // Coroutines for Flow types in repository interfaces
    implementation(libs.kotlinx.coroutines.core)

    // Serialization for domain models passed via Intent
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
