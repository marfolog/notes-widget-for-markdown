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
- GitLab CI release pipeline for signed APK releases.
