/*
 * Tracey Navigation 3 — build.gradle.kts
 * Navigation 3 (androidx.navigation3) integration for Tracey.
 * Android-only: Navigation 3 does not yet support Kotlin Multiplatform.
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.vanniktech.publish)
}

group   = "com.himanshoe"
version = providers.gradleProperty("tracey.navigation3.version").getOrElse("0.0.1-SNAPSHOT")

android {
    namespace  = "com.himanshoe.tracey.navigation3"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":tracey"))
    implementation(libs.navigation3.runtime)
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    pom {
        name.set("Tracey Navigation 3")
        description.set("Navigation 3 (androidx.navigation3) integration for Tracey SDK.")
        url.set("https://github.com/himanshoe/tracey")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("himanshoe")
                name.set("Himanshu Singh")
                url.set("https://github.com/hi-manshu")
            }
        }
        scm {
            url.set("https://github.com/himanshoe/tracey")
            connection.set("scm:git:git://github.com/himanshoe/tracey.git")
            developerConnection.set("scm:git:ssh://git@github.com/himanshoe/tracey.git")
        }
    }
}
