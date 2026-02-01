import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("checkFeatureModuleDependencies") {
    group = "verification"
    description = "Fails if any :feature:* module depends on another :feature:* module."

    doLast {
        val violations = mutableListOf<String>()

        rootProject.subprojects
            .filter { it.path.startsWith(":feature:") }
            .forEach { featureProject ->
                val illegalFeatureDeps = linkedSetOf<String>()

                featureProject.configurations.forEach { configuration ->
                    configuration.dependencies.forEach { dependency ->
                        if (dependency is ProjectDependency) {
                            val dependencyPath = dependency.path
                            if (dependencyPath.startsWith(":feature:") && dependencyPath != featureProject.path) {
                                illegalFeatureDeps.add(dependencyPath)
                            }
                        }
                    }
                }

                if (illegalFeatureDeps.isNotEmpty()) {
                    violations.add("${featureProject.path} -> ${illegalFeatureDeps.joinToString()}")
                }
            }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Feature module dependency violations detected:\n" + violations.joinToString(separator = "\n")
            )
        }
    }
}
