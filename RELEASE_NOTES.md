# v0.1.0

First public release. An Android home screen widget for Markdown notes kept in a folder you pick.

## What it does

- **Two widgets** over the same folder: cards with a Markdown preview, or a compact list of names.
- **A sync chip** that reads the vault's `.git` and says `synced 17:51`, `sync stuck` when a merge
  or rebase was left half-finished, or `not pushed` when the branch drifted from `origin`.
- **Refreshes in seconds** when the folder changes, instead of waiting for the next update cycle.
- **Per-note height, text size and colour**, drag to reorder, delete a file from settings.
- Tapping a note opens it in Obsidian; everything else works without it.

## Two builds

`foss` declares no `INTERNET` permission — verify with `aapt dump permissions`.
`play` adds Firebase analytics and crash reporting, switchable off in settings.

Take `foss` unless you want to send crash reports.

## Known limits

- Not an Obsidian plugin, and not affiliated with Obsidian.
- Reports sync state, never performs it.
- No editing in the widget; opening and creating go through Obsidian.
- Sync status only for git. Obsidian Sync and Syncthing keep their state where no app can read it.
- English only for now.
- Android 11 or newer.

Free and MIT-licensed, provided as is, without warranty — see the
[privacy policy](https://folmbuild.cz/notes-widget-privacy/) and the README.
