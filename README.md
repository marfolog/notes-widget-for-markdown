# Notes Widget for Markdown

An Android home screen widget for local Markdown notes.

Notes Widget for Markdown is built for people who keep their notes as plain `.md` files and want a fast, glanceable, Google Keep-like view on their Android home screen. It works especially well as a companion for an Obsidian vault, but the note preview itself is not tied to Obsidian storage or sync. Your files stay in your folder; the app reads them directly.

> Early project status: usable prototype / MVP. APIs, settings, and visual design may still change.

## What It Does

- Shows Markdown notes in a scrollable Android home screen widget.
- Reads notes directly from a user-selected folder using Android Storage Access Framework.
- Keeps the file system as the source of truth. No database, account, cloud backend, or proprietary note format.
- Generates a clean preview from Markdown, including headings, links, lists, and task checkboxes.
- Opens notes in Obsidian when tapped, using Obsidian deep links.
- Creates a new note through Obsidian from the widget add button.
- Appends quick Fast notes directly to a selected local Markdown file.
- Lets you customize each note card:
  - card height
  - text size
  - preset or custom color
  - manual order
- Automatically keeps unknown/new files safe: newly added Markdown files appear with default styling until configured.

## Screenshots

Screenshots are planned before the first public release. Keep public screenshots in `docs/screenshots/`.

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
- It does not implement native Git sync. It only *reads* `.git` metadata to show when the vault was last pulled or committed; pulling and pushing stays with your own client (Obsidian Git, GitSync, …).
- It does not edit Markdown content inside the widget.
- Android widgets cannot render full Obsidian/Markdown layout like the app itself can.
- Only local folders accessible through Android Storage Access Framework are supported.
- The current release flow is manual; there is no Play Store or F-Droid release yet.
- Obsidian is currently required for tap-to-open and add-new-note actions.

## Setup

1. Install the app on Android 11 or newer.
2. Open the app.
3. Select the root of your Obsidian vault or Markdown folder.
4. Select the folder that contains the `.md` files you want in the widget.
5. Add the widget to your Android home screen.
6. Optional: open the app settings again to customize card size, color, text size, and order.

## Works Without Obsidian

Yes, for the core preview flow:

- selecting a local Markdown folder
- reading `.md` files
- showing note cards in the widget
- customizing card size, text size, color, and order
- appending Fast notes to a selected Markdown file

Obsidian is currently used for editor actions:

- tapping an existing note opens it in Obsidian
- tapping the add button starts a new note in Obsidian

Support for configurable non-Obsidian editor actions is on the roadmap.

## Obsidian Integration

The app delegates note opening and note creation to Obsidian using URI links:

- tapping an existing note opens it in Obsidian
- tapping the add button starts a new note in Obsidian

If you use another Markdown editor, the file preview still works, but editor-specific deep links may need future support.

## Privacy

Notes Widget for Markdown reads only the folder you select through Android Storage Access Framework. It does not require an account and does not upload your notes to any server.

The app currently has no analytics SDK, no cloud backend, and no app-level sync. If you sync your notes with Obsidian Git, Syncthing, Git, cloud storage, or another tool, that sync happens outside this app.

## Supported Devices

- Minimum Android version: Android 11 (API 30).
- Target SDK: Android 16 / API 36.
- Tested by the maintainer on Pixel 10 running Android 16.

Please report launcher/widget compatibility issues with device model, Android version, launcher name, and whether battery optimization is enabled.

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

Build a release APK:

```bash
./gradlew assembleRelease
```

Unsigned release builds are expected when no release signing config is provided. For signed releases, provide these values through local Gradle properties or environment variables:

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Do not commit keystores, passwords, tokens, or local signing properties.

## Release Process

The first public release target is a signed APK attached to a GitLab Release.

For GitLab CI, configure these protected and masked variables:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Create a tag such as `v0.1.0`. The pipeline runs unit tests, builds the signed release APK, creates a SHA-256 checksum, uploads both files to GitLab Generic Packages, and creates a GitLab Release using `RELEASE_NOTES.md`.

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
- Publish the first signed public APK release.
- Improve Markdown preview fidelity within widget limits.
- Add better empty/error states.
- Make non-Obsidian editor integration configurable.
- Harden GitLab release automation after the first public tag.

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
