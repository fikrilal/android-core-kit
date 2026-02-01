package dev.fikril.androidcorekit.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidcorekit.android.library")
            pluginManager.apply("androidcorekit.compose")
        }
    }
}
