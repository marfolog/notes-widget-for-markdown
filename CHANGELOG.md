# Changelog

All notable public changes will be documented in this file.

## v1.1.0 - 2026-09-04

- New notes are created directly in the selected folder instead of being asked of Obsidian — a note could previously land in the vault root when the vault couldn't be located, reported on Reddit and reproduced by granting access to a subfolder only.
- Choose how a tapped note opens: Obsidian, the device's default Markdown app, or ask every time.
- Fixed: deleting a note that no longer existed kept failing with the same error on every retry instead of just disappearing from the list.
- Fixed: swiping a note to delete it could remove the wrong file after the list changed.
- Fixed: the widget could stop redrawing after its first refresh until something restarted the app process.
- Tagged releases now publish as drafts, so a signed build can be installed and checked before anyone else sees it.

## v1.0.0 - 2026-09-02

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
- GitHub Actions release pipeline for signed APK releases.
