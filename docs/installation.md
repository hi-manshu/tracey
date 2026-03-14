# Installation

## Core library

Add the dependency to your shared module's `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation("com.himanshoe:tracey:<version>")
}
```

---

## With Compose Navigation (KMP — Android + iOS)

```kotlin
commonMain.dependencies {
    implementation("com.himanshoe:tracey:<version>")
    implementation("com.himanshoe:tracey-navigation:<version>")
    implementation("org.jetbrains.androidx.navigation:navigation-compose:<version>")
}
```

> `tracey-navigation` does not expose `navigation-compose` transitively — declare it explicitly.

---

## With Navigation 3 (Android only)

```kotlin
// Android module build.gradle.kts
dependencies {
    implementation("com.himanshoe:tracey:<version>")
    implementation("com.himanshoe:tracey-navigation3:<version>")
    implementation("androidx.navigation3:navigation3-runtime:<version>")
    implementation("androidx.navigation3:navigation3-ui:<version>")
}
```

---

## Artifact coordinates

| Artifact | Description |
|---|---|
| `com.himanshoe:tracey` | Core SDK — gestures, breadcrumbs, crash recovery, reporters |
| `com.himanshoe:tracey-navigation` | Compose Navigation integration (KMP) |
| `com.himanshoe:tracey-navigation3` | Navigation 3 integration (Android only) |
