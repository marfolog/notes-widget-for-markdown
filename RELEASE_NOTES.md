# v1.0.0

First public release.

- Two widgets over a folder you pick: note cards with a Markdown preview, or a compact list of names
- Sync chip reading the vault's `.git`: last pull or commit, a stuck merge, an unpushed branch
- Not on git? Say so in settings and the chip falls back to when a note last changed
- Choose how long silence has to last before the chip turns amber
- Per-note height, text size and colour; drag to reorder; swipe a card aside to delete the file
- Fast note appends a line to a chosen file
- Tapping a note opens it in Obsidian
- Czech, German, Spanish, French, Chinese and Japanese
- Light and dark, forced or left to the system

Two builds: `foss` has no `INTERNET` permission, `play` adds analytics and crash reporting that can
be switched off. Take `foss` unless you want to send crash reports.

Updates are not instant — Android decides when to tell the widget a folder changed. See Limitations
in the README.

Android 11+. GPL-3.0, as is, without warranty.
