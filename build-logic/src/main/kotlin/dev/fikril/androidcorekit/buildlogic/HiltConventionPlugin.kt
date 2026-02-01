package dev.fikril.androidcorekit.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.plugin.KaptExtension

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.dagger.hilt.android")

        // KSP-first policy.
        //
        // If KSP is blocked for any reason in a specific app clone, set:
        //   androidcorekit.hilt.processor=kapt
        // and document why + the exit plan back to KSP in the app's `_WIP/*`.
        val processor = providers
            .gradleProperty("androidcorekit.hilt.processor")
            .orNull
            ?.trim()
            ?.lowercase()
            ?: "ksp"

        val hiltAndroid = libs.findLibrary("hilt-android").get()
        val hiltCompiler = libs.findLibrary("hilt-compiler").get()

        dependencies {
            add("implementation", hiltAndroid)
        }

        when (processor) {
            "kapt" -> {
                pluginManager.apply("org.jetbrains.kotlin.kapt")
                extensions.configure(KaptExtension::class.java) {
                    correctErrorTypes = true
                }
                dependencies {
                    add("kapt", hiltCompiler)
                }
            }
            "ksp" -> {
                pluginManager.apply("com.google.devtools.ksp")
                dependencies {
                    add("ksp", hiltCompiler)
                }
            }
            else -> error(
                "Unknown value for androidcorekit.hilt.processor='$processor'. Expected 'ksp' (default) or 'kapt'."
            )
        }
    }
}
