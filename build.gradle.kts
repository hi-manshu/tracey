/*
 * Tracey SDK – Root build.gradle.kts
 */
plugins {
    alias(libs.plugins.android.application)                  apply false
    alias(libs.plugins.android.library)                      apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform)    apply false
    alias(libs.plugins.kotlin.android)          apply false
    alias(libs.plugins.kotlin.compose)          apply false
    alias(libs.plugins.kotlin.serialization)    apply false
    alias(libs.plugins.compose.multiplatform)   apply false
    alias(libs.plugins.binary.compatibility)    apply false
    alias(libs.plugins.nexus.publish)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/snapshots/"))
            username.set(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
            password.set(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
        }
    }
}
