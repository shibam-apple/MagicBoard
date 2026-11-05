plugins {
    id("doubtboard.android.library")
}

android {
    namespace = "com.doubtboard.core.voice"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
