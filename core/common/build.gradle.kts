plugins {
    id("androidcorekit.android.library")
}

android {
    namespace = "dev.fikril.androidcorekit.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

