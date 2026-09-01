# Screenshots

All screenshots use a throwaway demo vault, never personal notes. They were taken on a
Pixel 7 emulator (Android 16) with a folder of six example notes under `demo-vault/Quick`,
which is a real git repository — so the sync chip shows a genuine state, not a mockup.

Current set:

- `01-home-both-widgets.png`: both widget flavours on one home screen.
- `02-card-widget.png`: note cards with Markdown preview, action bar and sync chip.
- `03-list-widget.png`: compact list of note names.
- `04-settings-git-sync.png`: git sync section with the last repo change and branch.
- `05-settings-notes.png`: notes folder selection.

Still missing for the release: the sync chip in its `conflict` state, which is the clearest
illustration of what the widget is for. Reproduce it by leaving an unfinished merge in the
demo vault (`MERGE_HEAD` present) and refreshing the widget.

## Reproducing

1. Create a demo vault with a `.obsidian` folder, a notes subfolder and `git init` plus one commit.
2. Push it to the device or emulator under `/sdcard/Documents/`.
3. Point the app at it: root folder = vault, notes folder = subfolder.
4. Add both widgets to the home screen and screenshot.
