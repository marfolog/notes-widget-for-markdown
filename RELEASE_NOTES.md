# v1.1.0

- New notes are created directly in the selected folder instead of being asked of Obsidian — a
  note could previously land in the vault root when the vault couldn't be located, reported on
  Reddit and reproduced by granting access to a subfolder only
- Choose how a tapped note opens: Obsidian, the device's default Markdown app, or ask every time
- Fixed: deleting a note that no longer existed (e.g. removed some other way) kept failing with
  the same error on every retry instead of just disappearing from the list
- Fixed: swiping a note to delete it could remove the wrong file after the list changed
- Fixed: the widget could stop redrawing after its first refresh until something restarted the
  app process — refresh now reliably shows the current state, with a small spinner while it loads
- Widget refresh diagnostics are logged under one filterable tag for easier bug reports

Two builds: `foss` has no `INTERNET` permission, `play` adds analytics and crash reporting that can
be switched off. Take `foss` unless you want to send crash reports.

Updates are not instant — Android decides when to tell the widget a folder changed. See Limitations
in the README.

Android 11+. GPL-3.0, as is, without warranty.
