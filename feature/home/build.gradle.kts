plugins {
    id("androidcorekit.android.feature")
}

android {
    namespace = "dev.fikril.androidcorekit.feature.home"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(libs.androidx.compose.material3)
}
