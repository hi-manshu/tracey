# Privacy masking

Mark sensitive composables so their screen region is replaced with a solid rectangle in captures. The composable **renders normally on screen** — only captures are affected.

## Usage

```kotlin
TextField(
    value = cardNumber,
    onValueChange = { cardNumber = it },
    modifier = Modifier
        .fillMaxWidth()
        .traceyMask(),           // black by default
)
```

## Custom mask colour

```kotlin
Text(
    text = ssn,
    modifier = Modifier.traceyMask(maskColor = Color.Red),
)
```

## How it works

`Modifier.traceyMask()` registers the composable's layout bounds with `TraceyMaskRegistry`. When Tracey takes a screenshot for a `ReplayPayload`, any registered regions are painted over before the PNG is encoded. No sensitive pixels are ever written to the payload.

## Redacting by test tag

Alternatively, suppress events from specific composables entirely using `redactedTags` in `TraceyConfig`. Events whose semantic path matches a redacted tag are dropped from the buffer:

```kotlin
TraceyConfig(
    redactedTags = listOf("PasswordField", "CreditCardInput"),
)
```
