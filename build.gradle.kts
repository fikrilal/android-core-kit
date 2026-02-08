import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
}

val checkFeatureModuleDependencies =
    tasks.register("checkFeatureModuleDependencies") {
        group = "verification"
        description = "Fails if any :feature:* module depends on another :feature:* module (directly or transitively)."

        doLast {
            val allProjects = rootProject.subprojects.associateBy { it.path }
            val featureProjects = allProjects.keys.filter { it.startsWith(":feature:") }.toSet()

            fun directProjectDependencies(projectPath: String): Set<String> {
                val project = allProjects[projectPath] ?: return emptySet()
                val deps = linkedSetOf<String>()
                project.configurations.forEach { configuration ->
                    configuration.dependencies.forEach { dependency ->
                        if (dependency is ProjectDependency) {
                            deps.add(dependency.path)
                        }
                    }
                }
                return deps
            }

            val adjacency = allProjects.keys.associateWith { directProjectDependencies(it) }
            val violations = mutableListOf<String>()

            featureProjects.forEach { startFeature ->
                val reachableFeatures = linkedSetOf<String>()
                val visited = mutableSetOf<String>()
                val queue = ArrayDeque<String>()

                visited.add(startFeature)
                queue.add(startFeature)

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    adjacency[current].orEmpty().forEach { next ->
                        if (visited.add(next)) {
                            queue.add(next)
                        }
                        if (next in featureProjects && next != startFeature) {
                            reachableFeatures.add(next)
                        }
                    }
                }

                if (reachableFeatures.isNotEmpty()) {
                    violations.add("$startFeature -> ${reachableFeatures.joinToString()}")
                }
            }

            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Feature module dependency violations detected:\n" + violations.joinToString(separator = "\n"),
                )
            }
        }
    }

tasks.named("check") {
    dependsOn(checkFeatureModuleDependencies)
}

extensions.configure<SpotlessExtension> {
    val ktlintVersion = libs.versions.ktlint.get()

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", ".gradle/**")
        ktlint(ktlintVersion)
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", ".gradle/**")
        ktlint(ktlintVersion)
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            xml.required.set(true)
            html.required.set(true)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
        }
    }
}

val verify =
    tasks.register("verify") {
        group = "verification"
        description = "Runs the baseline verification suite (builds, tests, lint, formatting, static analysis)."

        dependsOn(checkFeatureModuleDependencies)
        dependsOn(":app:assembleDevDebug", ":app:assembleProdDebug")
        dependsOn("spotlessCheck")
    }

gradle.projectsEvaluated {
    val checkTasks =
        rootProject.allprojects
            .mapNotNull { project -> project.tasks.findByName("check")?.path }
    val detektTasks =
        rootProject.subprojects
            .mapNotNull { project -> project.tasks.findByName("detekt")?.path }

    verify.configure {
        dependsOn(checkTasks)
        dependsOn(detektTasks)
    }
}
