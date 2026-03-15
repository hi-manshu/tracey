/*
 * Tracey Navigation – build.gradle.kts
 * Compose Multiplatform Navigation integration for Tracey.
 * Provides automatic screen tracking via NavController.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.vanniktech.publish)
}

group   = "com.himanshoe"
version = "0.0.1-alpha01"

kotlin {
    androidLibrary {
        namespace  = "com.himanshoe.tracey.navigation"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk     = libs.versions.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(project(":tracey"))
            implementation(libs.navigation.compose)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    pom {
        name.set("Tracey Navigation")
        description.set("Compose Multiplatform Navigation integration for Tracey.")
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
