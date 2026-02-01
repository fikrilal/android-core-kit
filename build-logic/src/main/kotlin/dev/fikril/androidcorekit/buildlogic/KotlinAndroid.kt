package dev.fikril.androidcorekit.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.configureKotlinAndroid() {
    val configure: KotlinAndroidProjectExtension.() -> Unit = {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    fun configureIfPresent() {
        extensions.findByType<KotlinAndroidProjectExtension>()?.apply(configure)
    }

    // Configure immediately when possible (for plugin stacks that register the extension early).
    configureIfPresent()

    // Configure safely once the Kotlin Android plugin is applied (order-independent).
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        extensions.configure(configure)
    }
}
