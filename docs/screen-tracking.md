# Screen tracking

## Automatic — Compose Navigation (`tracey-navigation`, KMP)

Swap `rememberNavController()` for the Tracey drop-in replacement:

```kotlin
val navController = rememberTraceyNavController()
```

Every navigation event is recorded automatically as a `ScreenView`.

---

## Automatic — Navigation 3 (`tracey-navigation3`, Android only)

Swap `remember { mutableStateListOf<Any>(...) }` for:

```kotlin
val backStack = rememberTraceyNavBackStack(Home)

NavDisplay(
    backStack = backStack,
    onBack    = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
        entry<Home>    { HomeScreen() }
        entry<Profile> { ProfileScreen() }
    }
)
```

Screen names are derived automatically from the key's class name (`data object Home` → `"Home"`). Supply a `nameSelector` lambda for full control:

```kotlin
val backStack = rememberTraceyNavBackStack(Home) { key ->
    when (key) {
        is Home    -> "Home Feed"
        is Profile -> "User Profile"
        else       -> key::class.simpleName ?: key.toString()
    }
}
```

Or attach tracking to an existing back stack:

```kotlin
val backStack = remember { mutableStateListOf<Any>(Home) }
backStack.trackWithTracey()
```

---

## Manual

From anywhere in your code:

```kotlin
Tracey.route("HomeScreen")   // from a nav listener or ViewModel
Tracey.screen("HomeScreen")  // from a composable (suspends briefly)
```

Or use the composable helper to record the screen name for the current composition:

```kotlin
@Composable
fun HomeScreen() {
    TraceyScreen("HomeScreen")
    // ...
}
```
