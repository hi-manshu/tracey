# Contributing to Tracey

Thank you for your interest in contributing!

## Getting started

1. Fork the repository and clone your fork.
2. Open the project in Android Studio or IntelliJ IDEA with the Kotlin Multiplatform plugin.
3. Run the tests: `./gradlew desktopTest` (fastest) or `./gradlew test`.

## Making changes

- Keep changes focused — one logical change per pull request.
- Add or update tests for any new behaviour.
- Run `./gradlew apiCheck` before opening a PR to ensure no accidental binary incompatibilities.
- Follow the existing code style (no formatter config yet — just match the surrounding code).

## Pull request process

1. Open a PR against the `main` branch.
2. Fill in the PR template.
3. A maintainer will review and merge when everything looks good.

## Reporting bugs

Use the **Bug report** issue template. Include:
- Tracey version
- Platform (Android / iOS) and OS version
- A minimal reproducer or steps to reproduce
- Logcat / console output if relevant

## Requesting features

Use the **Feature request** issue template. Describe the problem you're trying to solve — not just the solution you have in mind.

## Code of conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md).
