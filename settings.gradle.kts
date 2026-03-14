pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tracey"

include(":tracey")
include(":tracey-navigation")
include(":tracey-navigation3")

// Sample app uses published Maven Central artifacts — not local project references.
// Remove the two includes above when developing the sample in isolation.
include(":sample:androidApp")
