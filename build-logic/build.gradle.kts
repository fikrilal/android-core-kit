plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "dev.fikril.androidcorekit.buildlogic"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Convention plugins configure Android/Kotlin via their public DSL types.
    // Keep these as compileOnly so the main build controls plugin versions.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "androidcorekit.android.application"
            implementationClass = "dev.fikril.androidcorekit.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "androidcorekit.android.library"
            implementationClass = "dev.fikril.androidcorekit.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "androidcorekit.android.feature"
            implementationClass = "dev.fikril.androidcorekit.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("compose") {
            id = "androidcorekit.compose"
            implementationClass = "dev.fikril.androidcorekit.buildlogic.ComposeConventionPlugin"
        }
        register("hilt") {
            id = "androidcorekit.hilt"
            implementationClass = "dev.fikril.androidcorekit.buildlogic.HiltConventionPlugin"
        }
    }
}
