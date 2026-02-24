# Notes Widget for Markdown

A beautiful, Material Design 3 (Material You) Android home screen widget that brings a Google Keep-style grid/list experience to your local Markdown notes.

Perfectly designed as a companion for mobile Obsidian vaults. It acts as a lightweight, native, scrollable viewer for your `.md` files, delegating creation and editing directly to your Markdown editor via deep links.

## ✨ Features (MVP)
* **Scrollable Grid:** View all your notes directly on the home screen via Jetpack Glance widget.
* **Material You Design:** Dynamic colors that adapt to the user's wallpaper and rounded corners (16dp).
* **Local First:** Reads directly from your file system via Storage Access Framework (SAF). No databases, no cloud syncing.
* **Folder Selection:** Pick your Obsidian vault folder with persistent URI permissions (survives reboots).
* **Markdown Preview:** Automatically strips markdown formatting (#, *, -, links, code) for clean previews.
* **Deep Integration:** *(planned)*
    * Tap a note -> Opens the file in Obsidian (via `obsidian://open`).
    * Tap '+' -> Opens Obsidian to create a new note (via `obsidian://new`).
* **Abstracted Analytics:** *(planned)* Clean architecture for integrating Firebase Crashlytics & AppCenter.

## 🗺️ Project Roadmap

### Phase 1: Project Setup ✅
- [x] Android project with Gradle KTS + Version Catalogs
- [x] Dependencies: Glance, Koin, Coroutines, Compose, DocumentFile

### Phase 2: Domain Layer ✅
- [x] `NoteSummary` data class (pure Kotlin)
- [x] `FileRepository` interface (Flow-based)
- [x] `GetNotesUseCase` (sorts by lastModified descending)
- [x] `CreateNoteUseCase`

### Phase 3: Data Layer & SAF ✅
- [x] `FileRepositoryImpl` with SAF (`DocumentFile` + `ContentResolver`)
- [x] Markdown stripping for preview (headings, lists, bold, italic, links, code, blockquotes)
- [x] Efficient preview: reads only first 5 non-blank lines
- [x] Koin DI module (`AppModule.kt`)

### Phase 4: Presentation Layer ✅
- [x] `NotesViewModel` with sealed `NotesUiState` (Loading, Empty, Success, Error)
- [x] `NotesWidget` — Glance `LazyVerticalGrid` with Material You colors
- [x] `NotesWidgetReceiver`
- [x] Widget metadata (`notes_widget_info.xml`)

### Phase 5: Entry Point & Manifest ✅
- [x] `NotesWidgetApp` (Application class with Koin init)
- [x] `SetupActivity` — Compose UI with folder picker + persistable URI permission
- [x] SharedPreferences for vault URI (`app_prefs` / `vault_uri`)
- [x] Full AndroidManifest registration

### Phase 6: Polish & Integration (Next)
- [ ] Obsidian URI deep link routing (`obsidian://open`, `obsidian://new`)
- [ ] Widget refresh after folder selection
- [ ] Grid/List toggle in widget
- [ ] Analytics abstraction (Firebase, AppCenter)
- [ ] CI/CD setup

### Future Phases
- [ ] **Interactive Widget:** Actionable checkboxes (`- [ ]` markdown syntax)
- [ ] **Quick Capture:** Floating dialog to write notes without opening Obsidian
- [ ] **Rich Media:** Camera integration, voice memos

## 🛠️ Tech Stack
* **Language:** Kotlin 2.0.21
* **Min SDK:** 30 (Android 11) / **Target SDK:** 36
* **UI (Widget):** Jetpack Glance 1.1.1
* **UI (Setup):** Jetpack Compose (Material 3)
* **Dependency Injection:** Koin 4.1.1
* **Asynchronous:** Kotlin Coroutines 1.10.2 & Flow
* **Storage:** SAF via DocumentFile + ContentResolver

## 📦 Package Structure (`com.marfolog.noteswidgetformarkdown`)

```
├── NotesWidgetApp.kt              # Application (Koin init)
├── di/
│   └── AppModule.kt               # Koin module definitions
├── analytics/                     # (planned) Analytics abstraction
├── data/
│   ├── repository/
│   │   └── FileRepositoryImpl.kt  # SAF-based file reading & creation
│   └── parser/                    # (planned) Dedicated markdown parser
├── domain/
│   ├── model/
│   │   └── NoteSummary.kt         # Pure Kotlin data class
│   ├── repository/
│   │   └── FileRepository.kt      # Interface (Flow-based)
│   └── usecase/
│       ├── GetNotesUseCase.kt     # Fetch + sort by date
│       └── CreateNoteUseCase.kt   # Create new .md file
├── presentation/
│   ├── setup/
│   │   └── SetupActivity.kt      # Launcher: folder picker UI
│   ├── viewmodel/
│   │   └── NotesViewModel.kt     # MVVM with sealed UiState
│   └── widget/
│       ├── NotesWidget.kt        # GlanceAppWidget (LazyVerticalGrid)
│       ├── NotesWidgetReceiver.kt # GlanceAppWidgetReceiver
│       └── components/            # (planned) NoteCard, FabButton
└── ui/theme/                      # Compose theme (Material You)
```
