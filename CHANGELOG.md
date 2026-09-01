# Changelog

All notable public changes will be documented in this file.

## v0.1.0 - Unreleased

Initial public MVP.

- Local-first Android widget for selected Markdown folders.
- Markdown preview for headings, links, lists, task checkboxes, and strikethrough.
- Per-note card height, text size, color, and manual order.
- Fast note append flow for a selected Markdown file.
- Obsidian URI integration for opening existing notes and creating new notes.
- Bottom action bar in the widget: add note and fast note buttons no longer overlap the note cards.
- Git sync indicator in the bottom bar, read from the vault's `.git` metadata: last pull/commit, stuck merge or rebase, and unpushed work. Client-agnostic (CLI git, obsidian-git, Git Sync); tap opens settings with the full status. Can be switched off for non-git sync setups.
- Second widget: a compact list of note names, sharing the data, actions and sync chip with the card widget.
- Automatic refresh when the notes folder changes, instead of waiting for the periodic update.
- Note management in settings: rows collapse to one line, notes can be deleted (with confirmation) and reordered by dragging.
- Notes are opened in the correct Obsidian vault: the vault is detected from `.obsidian` and the path is resolved relative to its root. Previously a subfolder name was passed as the vault, so Obsidian silently opened the last note instead.
- Newly added notes appear at the top of the widget rather than below the fold.
- GitLab CI release pipeline for signed APK releases.
