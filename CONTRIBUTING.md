# Contributing

Notes Widget for Markdown is early. Small, focused issues and pull requests are easiest to review.

## Useful Feedback

- Android launcher and widget compatibility reports.
- Real Markdown or Obsidian vault edge cases.
- Markdown preview differences that matter in a widget.
- Accessibility issues with contrast, text size, or touch targets.
- Setup flow problems on specific Android versions.

Please include device model, Android version, launcher name, and a short reproduction path when reporting bugs.

## Pull Requests

- Open an issue first for large behavior changes.
- Keep changes scoped to one problem.
- Do not commit signing keys, passwords, tokens, personal notes, screenshots with private content, or local IDE state.
- Run unit tests before submitting:

```bash
./gradlew testDebugUnitTest
```

## Project Direction

The app is local-first and Markdown-first. Obsidian integration is useful, but note storage and sync should remain outside the app unless a future feature is explicitly designed around that boundary.
