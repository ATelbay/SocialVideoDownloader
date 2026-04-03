import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for KMP library modules (svd.kmp.library).
 *
 * Uses com.android.library + org.jetbrains.kotlin.multiplatform combination enabled via
 * android.builtInKotlin=false and android.newDsl=false in gradle.properties.
 * This is required for KSP compatibility until KSP supports AGP 9.x com.android.kotlin.multiplatform.library.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                val javaVersion = libs.findVersion("javaVersion").get().toString().toInt()
                jvmToolchain(javaVersion)

                androidTarget()
                val frameworkBaseName = project.path.removePrefix(":").replace(":", "_").replace("-", "_")
                iosArm64 {
                    binaries.framework {
                        baseName = frameworkBaseName
                        isStatic = true
                    }
                }
                iosSimulatorArm64 {
                    binaries.framework {
                        baseName = frameworkBaseName
                        isStatic = true
                    }
                }

                sourceSets.apply {
                    getByName("commonMain") {
                        dependencies {
                            implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                        }
                    }
                    getByName("commonTest") {
                        dependencies {
                            implementation(kotlin("test"))
                        }
                    }
                }
            }

            extensions.configure<LibraryExtension> {
                compileSdk = libs.findVersion("compileSdk").get().toString().toInt()

                defaultConfig {
                    minSdk = libs.findVersion("minSdk").get().toString().toInt()
                }

                compileOptions {
                    val javaVer = JavaVersion.toVersion(libs.findVersion("javaVersion").get().toString().toInt())
                    sourceCompatibility = javaVer
                    targetCompatibility = javaVer
                }
            }
        }
    }
}
