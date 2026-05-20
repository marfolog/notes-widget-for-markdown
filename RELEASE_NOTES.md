# Notes Widget for Markdown v0.1.0

Initial public MVP release.

Notes Widget for Markdown is an Android home screen widget for local Markdown notes. It reads `.md` files from a selected folder and displays them as customizable note cards.

## Highlights

- Local-first Markdown folder access through Android Storage Access Framework.
- Scrollable Android home screen widget.
- Simplified Markdown preview with headings, links, lists, task checkboxes, and strikethrough.
- Per-note card height, text size, color, and manual order.
- Fast note dialog for appending quick notes to a selected Markdown file.
- Obsidian URI integration for opening existing notes and creating new notes.

## Known Limitations

- This is not an official Obsidian plugin.
- Obsidian is currently required for tap-to-open and add-new-note actions.
- No native Git sync; sync remains handled by your existing notes setup.
- No in-widget Markdown editing.
- Markdown preview is simplified for Android widget constraints.
- Manual APK installation may require allowing installs from unknown sources.
