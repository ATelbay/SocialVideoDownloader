import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val android = extensions.findByType(CommonExtension::class.java)
                ?: error("Android plugin must be applied before svd.android.compose")
            android.buildFeatures.compose = true
        }
    }
}
