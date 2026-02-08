plugins {
    id("androidcorekit.android.feature")
    id("androidcorekit.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.fikril.androidcorekit.feature.auth"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:navigation"))
    implementation(project(":core:session"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
