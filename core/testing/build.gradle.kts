plugins {
    id("androidcorekit.android.library")
}

android {
    namespace = "dev.fikril.androidcorekit.core.testing"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

