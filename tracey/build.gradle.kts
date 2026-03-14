/*
 * Tracey Core – build.gradle.kts
 * Kotlin Multiplatform library targeting Android and iOS.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.vanniktech.publish)
}

group   = "com.himanshoe"
version = providers.gradleProperty("tracey.version").getOrElse("0.0.1-SNAPSHOT")

kotlin {
    androidLibrary {
        namespace  = "com.himanshoe.tracey"
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
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.activity.compose)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    pom {
        name.set("Tracey")
        description.set("Kotlin Multiplatform SDK for recording, replaying, and reporting user interactions.")
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
