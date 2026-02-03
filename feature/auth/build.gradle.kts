plugins {
    id("androidcorekit.android.feature")
}

android {
    namespace = "dev.fikril.androidcorekit.feature.auth"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:session"))
    implementation(project(":core:ui"))
}
