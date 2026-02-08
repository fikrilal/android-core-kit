package dev.fikril.androidcorekit.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<CommonExtension> {
                buildFeatures.compose = true
            }

            val composeBom = libs.findLibrary("androidx-compose-bom").get()
            val composeUi = libs.findLibrary("androidx-compose-ui").get()
            val composeUiToolingPreview = libs.findLibrary("androidx-compose-ui-tooling-preview").get()

            dependencies {
                add("implementation", dependencies.platform(composeBom))
                add("implementation", composeUi)
                add("implementation", composeUiToolingPreview)
            }
        }
    }
}
