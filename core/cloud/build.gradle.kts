plugins {
    id("svd.android.library")
    id("svd.android.hilt")
}

android {
    namespace = "com.socialvideodownloader.core.cloud"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.datastore.preferences)
}
