pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("build-logic")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "SocialVideoDownloader"

include(":app")
include(":feature:download")
include(":feature:history")
include(":feature:library")
include(":core:domain")
include(":core:data")
include(":core:ui")
