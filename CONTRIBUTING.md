# Contributing

Notes Widget for Markdown is early. Small, focused issues and pull requests are easiest to review.

## Useful Feedback

- Android launcher and widget compatibility reports.
- Real Markdown or Obsidian vault edge cases.
- Markdown preview differences that matter in a widget.
- Accessibility issues with contrast, text size, or touch targets.
- Setup flow problems on specific Android versions.

Please include device model, Android version, launcher name, and a short reproduction path when reporting bugs.

## Pull Requests

- Open an issue first for large behavior changes.
- Keep changes scoped to one problem.
- Do not commit signing keys, passwords, tokens, personal notes, screenshots with private content, or local IDE state.
- Run unit tests before submitting:

```bash
./gradlew testDebugUnitTest
```

## Dialogs, banners and other prompts

Every prompt (a dialog, Google's review card, a banner) is one entry in the list in
`util/prompt/Prompts.kt`; one coordinator decides which of them may show, so that no prompt has to
know about another. To add one: write its "is it due" rule as a pure function with tests, add a field
to `PromptFacts` if it needs something new, add a `PromptDef` (kind, surface, priority, what it keeps
away from), and show it only when its id comes back from `PromptSurfaces.decide`. A prompt only one
build may have goes into that build's `FlavorPrompts`. `PromptTimelineTest` shows whole days of use.

In a debug build, settings end with a "Debug: time travel" section, and the same can be driven
from adb (the answer is in `adb logcat -s NWPrompts`):

```bash
adb shell am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.TIME --el addDays 7
adb shell am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.TIME --ez reset true
adb shell am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.PROMPTS_RESET
adb shell am broadcast -p com.marfolog.noteswidgetformarkdown -a com.marfolog.noteswidgetformarkdown.debug.PROMPTS_DUMP
```

It moves only the clock the prompts go by. The release build has neither the section nor the receiver.
A debug build also never writes to Firestore; it logs what it would have sent.

## Project Direction

The app is local-first and Markdown-first. Obsidian integration is useful, but note storage and sync should remain outside the app unless a future feature is explicitly designed around that boundary.
