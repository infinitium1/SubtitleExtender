# Subtitle Extender

A native Android app that makes `.srt` subtitle files easier to read by
extending how long each line stays on screen - without ever letting one
subtitle overlap the next.

## Purpose

People who read slowly often lose subtitles before they finish reading them.
Subtitle Extender fixes this by pushing every subtitle's **end time** later
by a user-chosen amount (0.5s / 1s / 1.5s / 2s). It never touches **start**
times, and it never lets two subtitles overlap: if the full extension would
collide with the next line, the extension is automatically shortened so the
subtitle ends exactly one millisecond before the next one begins. The very
last subtitle in a file has nothing after it, so it always gets the full
extension.

The original file is never modified. The app writes a new file (default
name `<original>_extended.srt`) to a location you choose.

## Build requirements

- Android Studio (a recent version that bundles/supports AGP 8.13.x and JDK 17)
- JDK 17 (Android Studio's bundled JDK works)
- Internet access on first sync (Gradle needs to download the wrapper
  distribution and dependencies - see the note below)

| Item | Value |
|---|---|
| Language | Kotlin (2.4.0) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + Repository) |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 36 |
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.13 |

### A note on the Gradle wrapper

This project ships `gradlew`, `gradlew.bat`, and `gradle-wrapper.properties`,
but **not** the `gradle-wrapper.jar` binary, since it can't be produced
without a live connection to Gradle's distribution servers. The first time
you open the project, Android Studio will fetch it automatically during
"Sync Project with Gradle Files" (or offer to repair the wrapper if it
doesn't). This requires an internet connection just for that first sync;
after that, everything else builds normally offline.

## How to run

1. Open the `SubtitleExtender/` folder in Android Studio.
2. Let Gradle sync (see the wrapper note above).
3. Run the `app` configuration on an emulator or a real device (API 26+).
4. In the app: tap **Select SRT File**, pick a duration, tap
   **Extend Subtitle Duration**, then choose where to save the result.

To run the unit tests: right-click `app/src/test` in Android Studio and
choose **Run Tests**, or from a terminal:

```bash
./gradlew test
```

To build a release APK:

```bash
./gradlew assembleRelease
```

(The release build type has no signing config configured, matching a
fresh Android Studio project - add your own keystore in
`app/build.gradle.kts` before distributing a signed release build. Debug
builds run on-device out of the box with Android Studio's auto-generated
debug signing key.)

## How the extension algorithm works

For every subtitle, in order:

1. Compute the desired end time: `currentEnd + extension`.
2. If it's the **last** subtitle in the file, there's nothing after it, so
   it always gets the full requested extension.
3. Otherwise, look at the **next** subtitle's start time (start times are
   never modified, so this is always a stable reference). The latest this
   subtitle may end without overlapping is exactly `nextStart - 1` ms.
4. Apply `min(desiredEnd, nextStart - 1)` - i.e. extend by the full amount
   if there's room, or shorten the extension just enough to avoid an
   overlap if there isn't. The result is also never allowed to fall below
   the subtitle's *original* end time, so an already-overlapping input file
   can never cause a subtitle to end up shorter than it started.

This logic lives in one pure, dependency-free function -
`SubtitleExtensionProcessor.extend()` - which is exhaustively unit tested
(see `app/src/test/.../SubtitleExtensionProcessorTest.kt`) independently of
any Android UI or I/O code.

## Project structure

```
app/src/main/java/com/subtitleextender/
├── MainActivity.kt                     Single Activity hosting Compose UI
├── data/
│   ├── model/                          SubtitleEntry, ExtensionDuration
│   ├── parser/                         SrtParser, SrtWriter, SrtParseException
│   ├── processor/                      SubtitleExtensionProcessor (core algorithm)
│   └── repository/                     SubtitleRepository (SAF-based file I/O)
├── util/                               SrtTimeFormatter, LineEndingDetector, SubtitleFileNaming
└── ui/
    ├── main/                           MainViewModel, MainUiState, MainUiEvent, MainScreen
    └── theme/                          Color.kt, Type.kt, Theme.kt (Material 3)

app/src/test/java/com/subtitleextender/  JVM unit tests (parser, writer, processor, utils)
```

All file access goes through the Storage Access Framework
(`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`), so the app requests no
storage permissions at all.
