import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

object DoubtBoard {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
    val JAVA = JavaVersion.VERSION_17
    val JVM_TARGET = JvmTarget.JVM_17
}

/** JUnit 5 + Kotest assertions for every module's unit tests. */
internal fun Project.configureUnitTests() {
    val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    dependencies {
        add("testImplementation", platform(libs.findLibrary("junit5-bom").get()))
        add("testImplementation", libs.findLibrary("junit5-jupiter").get())
        add("testImplementation", libs.findLibrary("kotest-assertions").get())
        add("testRuntimeOnly", libs.findLibrary("junit5-launcher").get())
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    }
}

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<LibraryExtension> {
            compileSdk = DoubtBoard.COMPILE_SDK
            defaultConfig {
                minSdk = DoubtBoard.MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = DoubtBoard.JAVA
                targetCompatibility = DoubtBoard.JAVA
            }
            testOptions.unitTests.isReturnDefaultValues = true
        }
        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions.jvmTarget.set(DoubtBoard.JVM_TARGET)
        }
        configureUnitTests()
    }
}

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = DoubtBoard.JAVA
            targetCompatibility = DoubtBoard.JAVA
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions.jvmTarget.set(DoubtBoard.JVM_TARGET)
        }
        configureUnitTests()
    }
}
