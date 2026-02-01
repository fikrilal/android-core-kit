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
                defaultConfig.targetSdk = AndroidConfig.targetSdk
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }
    }
}
