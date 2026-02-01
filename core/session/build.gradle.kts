plugins {
    id("androidcorekit.android.library")
}

android {
    namespace = "dev.fikril.androidcorekit.core.session"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

