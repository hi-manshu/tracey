# Platform support

| Platform | Gestures | Lifecycle | Crash recovery | Event paths |
|----------|:--------:|:---------:|:--------------:|-------------|
| Android  | ✅ | ✅ | ✅ | Full composable path — `HomeScreen > AddToCartButton` |
| iOS      | ✅ | ✅ | ✅ | Screen + coordinates — `HomeScreen > Screen[x=150,y=300]` |

---

## iOS path resolution

On iOS, Compose does not expose the semantics tree at the Kotlin/Native layer, so event paths use the current screen name and touch coordinates rather than composable names.

Use `Tracey.screen()` or `TraceyScreen()` to supply the screen name so every path includes it:

```kotlin
@Composable
fun HomeScreen() {
    TraceyScreen("HomeScreen")
    // ...
}
```

Without a screen name, paths fall back to `"Screen[x=150,y=300]"`.
