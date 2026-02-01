plugins {
    id("androidcorekit.android.library")
}

android {
    namespace = "dev.fikril.androidcorekit.core.testing"
}

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
