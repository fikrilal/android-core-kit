package dev.fikril.androidcorekit.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            configureAndroidCommon()
            configureKotlinAndroid()

            extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
                buildFeatures.buildConfig = true

                defaultConfig.targetSdk = AndroidConfig.targetSdk
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

                // Placeholder until Phase 4 (dev/prod flavors) overrides this field.
                defaultConfig.buildConfigField("String", "BASE_URL", "\"https://example.invalid\"")
            }
        }
    }
}
