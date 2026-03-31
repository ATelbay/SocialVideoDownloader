import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.gradle.kotlin.dsl.configure

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("svd.kmp.library")
                apply("svd.kmp.compose")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.apply {
                    getByName("commonMain") {
                        dependencies {
                            implementation(project(":core:domain"))
                            implementation(libs.findLibrary("koin-core").get())
                            implementation(libs.findLibrary("coil3-compose").get())
                            implementation(libs.findLibrary("coil3-network-ktor3").get())
                            implementation(libs.findLibrary("navigation-compose-multiplatform").get())
                        }
                    }
                }
            }
        }
    }
}
