# Changelog

All notable changes to this project will be documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] — 2026-03-14

### Added
- Core SDK: gesture recording (click, long press, scroll, swipe, pinch) via a single `Modifier` at the root composable
- `TraceyHost` composable — zero-config setup with gesture recording and debug overlay
- `RingBuffer` — thread-safe, time-and-count bounded circular buffer
- `CrashHandler` — saves a ring-buffer snapshot to disk on crash; replays it on next launch
- `LogcatReporter` — built-in reporter that prints formatted timelines with local time to Logcat / stdout
- `TraceyConfig` and `rememberTraceyConfig` for Compose-idiomatic configuration
- `Tracey.log()`, `Tracey.screen()`, `Tracey.route()` for manual instrumentation
- `Tracey.capture()`, `captureAndExportTest()`, `captureAndExportFile()`, `captureAndExportHtml()`
- `TraceyScreen` composable helper for manual screen tracking
- `tracey-navigation` module — `rememberTraceyNavController()` and `NavController.trackWithTracey()` for automatic screen tracking with Compose Multiplatform Navigation
- Android and iOS platform support
- Lifecycle observer: `AppForeground` / `AppBackground` events via `ActivityLifecycleCallbacks` (Android) and `NSNotificationCenter` (iOS)
- Debug overlay: draggable capture button, live event log panel, gesture trail canvas
- `ReplayHtmlExporter` — self-contained HTML report with visual user flow diagram
- `TestCaseExporter` — generates runnable Compose UI tests from a captured session
- `TraceyReporter` interface for custom reporters
- `Modifier.traceyMask()` — redacts sensitive composable regions (PII, payment fields) with a solid colour in captured screenshots; renders normally on screen
