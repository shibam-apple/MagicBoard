plugins {
    id("doubtboard.android.library")
}

android {
    namespace = "com.doubtboard.core.data"
}

dependencies {
    implementation(project(":core:adapt"))
    implementation(libs.kotlinx.coroutines.core)
}
