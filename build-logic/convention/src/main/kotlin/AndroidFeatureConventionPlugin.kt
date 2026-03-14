import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("videograb.android.library")
                apply("videograb.android.compose")
                apply("videograb.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:ui"))
                add("implementation", project(":core:domain"))
            }
        }
    }
}
