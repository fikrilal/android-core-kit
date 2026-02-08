plugins {
    id("androidcorekit.android.library")
}

android {
    namespace = "dev.fikril.androidcorekit.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
