# Open-source launch plán

Praktický plán pro zveřejnění projektu Notes Widget for Markdown a získání prvního feedbacku od lidí, kteří používají Obsidian nebo lokální Markdown poznámky.

## Pozice projektu

Krátká definice:

> Android home screen widget for local Markdown notes, inspired by the quick visual scanability of Google Keep.

Co zdůrazňovat:

- local-first
- žádný účet
- žádný cloud backend
- čte existující `.md` soubory
- vhodné pro Obsidian vaulty, ale není to oficiální Obsidian plugin
- widget je vizuální vrstva, ne editor

Co raději neslibovat:

- plný Obsidian render
- nativní Git sync
- editaci poznámek přímo ve widgetu
- Play Store dostupnost
- stabilní public API

## Cleanup před public v1

- Odstranit nebo schovat experimentální Obsidian Advanced URI sync flow.
  - Pro první veřejnou verzi je lepší jednoduchý příběh: appka čte lokální Markdown a refreshne widget.
  - Git/Obsidian sync je příliš specifický a může působit, že aplikace umí víc, než reálně dělá.
- Zkontrolovat, že v repu není veřejně citlivý release signing materiál.
- Vyčistit `app/build.gradle.kts` od lokálních hesel a nahradit je Gradle properties nebo lokálním necommitovaným configem.
- Přidat skutečné screenshoty do README.
- Rozhodnout název APK/release artefaktu, např. `notes-widget-for-markdown-v0.1.0.apk`.
- Založit tag `v0.1.0`.

## První release

Doporučený první kanál: GitHub/GitLab Releases s ručně instalovatelným APK.

Release checklist:

- Spustit testy:

```bash
./gradlew testDebugUnitTest
```

- Sestavit release APK:

```bash
./gradlew assembleRelease
```

- Ověřit instalaci na fyzickém zařízení.
- Připravit release notes:
  - co aplikace dělá
  - jak ji nastavit
  - známé limity
  - že jde o první MVP build
- Přiložit APK jako release asset.
- Přidat krátké varování: Android může při ruční instalaci vyžadovat povolení instalace z neznámých zdrojů.

## Doporučené release notes pro v0.1.0

```markdown
Initial public MVP release.

Notes Widget for Markdown is an Android home screen widget for local Markdown notes. It reads `.md` files from a selected folder and displays them as customizable note cards.

Highlights:
- local-first Markdown folder access
- home screen widget
- Markdown preview with task list support
- Obsidian note open/create deep links
- per-note card size, text size, color, and order

Known limitations:
- not an official Obsidian plugin
- no native Git sync
- no in-widget editing
- Markdown preview is simplified for Android widget constraints
```

## Promo kanály

Začít malým releasem a ptát se na feedback, ne tlačit agresivní promo.

Vhodné kanály:

- Obsidian Forum - Share & showcase  
  https://forum.obsidian.md/c/share-showcase
- Reddit `r/ObsidianMD`  
  https://www.reddit.com/r/ObsidianMD/
- Reddit `r/Markdown`  
  https://www.reddit.com/r/Markdown/
- Reddit `r/androidapps`  
  https://www.reddit.com/r/androidapps/
- Reddit `r/fossdroid` po doplnění čistého open-source release procesu  
  https://www.reddit.com/r/fossdroid/
- GitHub/GitLab topics:
  - `android`
  - `markdown`
  - `obsidian`
  - `widget`
  - `kotlin`
  - `local-first`

Později:

- F-Droid request až po vyčištění release signing a metadata.
- Show HN až ve chvíli, kdy jsou screenshoty, APK release, privacy story a jasná landing README sekce.

## Krátké promo texty

### Obsidian Forum

```markdown
Hi, I’m building Notes Widget for Markdown, an Android home screen widget for local Markdown notes.

The idea is simple: keep writing in Obsidian, but get a Google Keep-like glanceable widget for selected `.md` files on the Android home screen.

It reads files directly from a selected folder using Android Storage Access Framework. No account, no backend, no database for note content.

Current MVP:
- Markdown preview
- task list preview
- note cards with custom size/color/order
- open/create notes via Obsidian deep links

It is still early, and I’d appreciate feedback from people who use Obsidian on Android.
```

### Reddit

```markdown
I’m building an open-source Android widget for local Markdown notes.

It is aimed at people who use Obsidian or keep notes as plain `.md` files and want a Google Keep-like widget on the home screen.

Current MVP:
- reads a selected Markdown folder locally
- displays notes as customizable cards
- supports simplified Markdown/task-list preview
- opens/creates notes through Obsidian deep links
- no account, no backend, no proprietary note format

Still early, but I’d love feedback from Android + Markdown/Obsidian users.
```

### Krátký popisek pro repo

```text
Android home screen widget for local Markdown notes, inspired by Google Keep and built for Obsidian-style vaults.
```

## Co přidat do repa později

- `CONTRIBUTING.md`
- `CHANGELOG.md`
- GitLab/GitHub issue templates
- screenshots in `docs/screenshots/`
- basic privacy note
- release workflow
- F-Droid metadata

## F-Droid příprava

F-Droid řešit až po prvním ručním release. Před tím:

- odstranit lokální signing secrets z Gradle souborů
- ověřit, že build nevyžaduje privátní soubory
- přidat licenci
- mít tagované releasy
- popsat anti-features, pokud by nějaké existovaly
- držet závislosti z běžných Maven repozitářů

## Privacy story

Jednoduché veřejné tvrzení pro README/release:

> Notes Widget for Markdown reads only the folder you select through Android Storage Access Framework. It does not require an account and does not upload your notes to any server.

Před zveřejněním ověřit:

- žádné analytics SDK
- žádné síťové oprávnění, pokud není nutné
- žádný cloud sync v aplikaci

## Roadmap po v0.1.0

Priorita:

1. Screenshoty a lepší public README.
2. Vyčištěný release signing.
3. Stabilní ruční APK release.
4. Feedback z Obsidian/Markdown komunity.
5. Fixy podle reálných vaultů.
6. F-Droid příprava.

Funkční nápady:

- lepší preview pro Obsidian callouts
- volitelný grid/list layout
- výběr podsložek
- non-Obsidian editor deep-link nastavení
- rychlé vytvoření poznámky bez otevření editoru

