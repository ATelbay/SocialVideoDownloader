import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

                defaultConfig {
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(libs.findVersion("javaVersion").get().toString().toInt())
                    targetCompatibility = JavaVersion.toVersion(libs.findVersion("javaVersion").get().toString().toInt())
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(
                        org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(
                            libs.findVersion("javaVersion").get().toString()
                        )
                    )
                }
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
        }
    }
}
