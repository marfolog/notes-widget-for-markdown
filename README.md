# Notes Widget for Markdown

An Android home screen widget for local Markdown notes.

Notes Widget for Markdown is built for people who keep their notes as plain `.md` files and want a fast, glanceable, Google Keep-like view on their Android home screen. It works especially well as a companion for an Obsidian vault, but it is not tied to Obsidian storage or sync. Your files stay in your folder; the app reads them directly.

> Early project status: usable prototype / MVP. APIs, settings, and visual design may still change.

## What It Does

- Shows Markdown notes in a scrollable Android home screen widget.
- Reads notes directly from a user-selected folder using Android Storage Access Framework.
- Keeps the file system as the source of truth. No database, account, cloud backend, or proprietary note format.
- Generates a clean preview from Markdown, including headings, links, lists, and task checkboxes.
- Opens notes in Obsidian when tapped, using Obsidian deep links.
- Creates a new note through Obsidian from the widget add button.
- Lets you customize each note card:
  - card height
  - text size
  - preset or custom color
  - manual order
- Automatically keeps unknown/new files safe: newly synced or newly created Markdown files appear with default styling until configured.

## Screenshots

Screenshots are planned before the first public release.

Suggested screenshots:

- configured home screen widget
- setup screen with vault/folder selection
- card appearance settings
- Markdown task-list preview

## Why

Obsidian and many other note apps are great for writing, linking, and organizing Markdown. Android widgets are great for quickly seeing what matters without opening an app.

This project sits between those worlds:

- Obsidian or another Markdown editor remains the writing tool.
- Your local Markdown folder remains the source of truth.
- The Android widget becomes a lightweight visual layer for quick access.

The design goal is a practical note board, not a full Markdown editor.

## Current Limitations

- This is not an official Obsidian plugin.
- It does not implement native Git sync.
- It does not edit Markdown content inside the widget.
- Android widgets cannot render full Obsidian/Markdown layout like the app itself can.
- Only local folders accessible through Android Storage Access Framework are supported.
- The current release flow is manual; there is no Play Store or F-Droid release yet.

## Setup

1. Install the app on Android 11 or newer.
2. Open the app.
3. Select the root of your Obsidian vault or Markdown folder.
4. Select the folder that contains the `.md` files you want in the widget.
5. Add the widget to your Android home screen.
6. Optional: open the app settings again to customize card size, color, text size, and order.

## Obsidian Integration

The app delegates note opening and note creation to Obsidian using URI links:

- tapping an existing note opens it in Obsidian
- tapping the add button starts a new note in Obsidian

If you use another Markdown editor, the file preview still works, but editor-specific deep links may need future support.

## Build From Source

Requirements:

- Android Studio
- JDK 17 or newer
- Android SDK with API 36

Build a debug APK:

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

Install a debug build on a connected device:

```bash
./gradlew installDebug
```

## Tech Stack

- Kotlin
- Gradle Kotlin DSL
- Jetpack Compose for the setup UI
- Jetpack Glance for the home screen widget
- Koin for dependency injection
- Kotlin Coroutines and Flow
- Android Storage Access Framework via `DocumentFile`
- CommonMark for Markdown preview parsing
- WorkManager for periodic widget refresh

## Project Principles

- Local-first by default.
- Plain Markdown files remain the source of truth.
- No Room database for note content.
- No account system.
- No cloud backend.
- Android platform APIs over custom file access hacks.
- Obsidian integration should be helpful but optional where possible.

## Roadmap

Near-term:

- Add real screenshots and a demo GIF.
- Clean up release signing and public APK publishing.
- Improve Markdown preview fidelity within widget limits.
- Add better empty/error states.
- Make non-Obsidian editor integration configurable.
- Add GitLab/GitHub release automation.

Later:

- F-Droid metadata and reproducible release setup.
- Optional quick-capture flow.
- More layout modes.
- Better support for nested folders.
- Accessibility review for widget contrast and touch targets.

## Contributing

Issues, ideas, and pull requests are welcome once the first public release is published.

Useful contributions:

- testing with real Obsidian vaults
- testing with non-Obsidian Markdown folders
- Markdown preview edge cases
- Android launcher/widget compatibility reports
- UI and accessibility improvements
- documentation and screenshots

Before opening a large pull request, please create an issue with the proposed change.

## License

MIT. See [LICENSE](LICENSE).

