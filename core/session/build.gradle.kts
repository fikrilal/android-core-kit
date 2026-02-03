plugins {
    id("androidcorekit.android.library")
}

android {
    namespace = "dev.fikril.androidcorekit.core.session"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
}
