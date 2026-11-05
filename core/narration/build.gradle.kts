plugins {
    id("doubtboard.android.library")
}

android {
    namespace = "com.doubtboard.core.narration"
}

dependencies {
    api(project(":core:board"))
}
