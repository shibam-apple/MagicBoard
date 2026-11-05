plugins {
    id("doubtboard.android.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.doubtboard.core.board"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
