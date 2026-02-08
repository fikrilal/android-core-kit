plugins {
    id("androidcorekit.android.library")
    id("androidcorekit.compose")
}

android {
    namespace = "dev.fikril.androidcorekit.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material3)
}
