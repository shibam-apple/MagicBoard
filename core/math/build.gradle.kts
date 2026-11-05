plugins {
    id("doubtboard.android.library")
}

android {
    namespace = "com.doubtboard.core.math"
}

dependencies {
    api(project(":core:board"))
}
