/*
 * Tracey Core – build.gradle.kts
 * Kotlin Multiplatform library targeting Android and iOS.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.binary.compatibility)
    `maven-publish`
    signing
}

group   = "com.himanshoe"
version = "0.1.0"

kotlin {
    androidTarget {
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
            implementation(compose.preview)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace  = "com.himanshoe.tracey"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

publishing {
    publications.withType<MavenPublication>().configureEach {
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
                    url.set("https://github.com/himanshoe")
                }
            }
            scm {
                url.set("https://github.com/himanshoe/tracey")
                connection.set("scm:git:git://github.com/himanshoe/tracey.git")
                developerConnection.set("scm:git:ssh://git@github.com/himanshoe/tracey.git")
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT"))
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                else
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            )
            credentials {
                username = providers.gradleProperty("ossrhUsername").orNull
                    ?: System.getenv("OSSRH_USERNAME")
                password = providers.gradleProperty("ossrhPassword").orNull
                    ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val signingKey = providers.gradleProperty("signingKey").orNull
        ?: System.getenv("SIGNING_KEY")
    val signingPassword = providers.gradleProperty("signingPassword").orNull
        ?: System.getenv("SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}
