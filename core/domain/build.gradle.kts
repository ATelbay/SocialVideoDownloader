plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // javax.inject for @Qualifier annotations
    implementation(libs.javax.inject)

    // Coroutines for Flow types in repository interfaces
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
