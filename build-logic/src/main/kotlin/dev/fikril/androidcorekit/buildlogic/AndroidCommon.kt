package dev.fikril.androidcorekit.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure

internal fun Project.configureAndroidCommon() {
    extensions.configure<CommonExtension> {
        compileSdk = AndroidConfig.compileSdk
        defaultConfig.minSdk = AndroidConfig.minSdk

        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
    }
}
