import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val commonExtension = extensions.findByType(CommonExtension::class.java)
            commonExtension?.apply {
                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
