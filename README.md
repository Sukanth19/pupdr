<p align="center"><img src="docs/icon.png" width="360" alt="pupdr icon"></p>

# pupdr

A Kotlin / Jetpack Compose Android app for tracking daily gym workouts.

## Features

- **870+ exercise library, offline.** The full [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (public domain) is bundled. Search it, filter by muscle and equipment, and read step-by-step instructions. You can also add your own custom exercises.
- **Routines.** Build splits with target sets × reps and reorder the exercises.
- **Import routines** from a file or pasted text. Accepted formats:
  - pupdr JSON (what the app exports)
  - plain text, for example:
    ```
    # Push Day
    Bench Press 4x8
    - Overhead press, 3 x 10-12
    Dips
    ```
    Exercise names are fuzzy-matched to the library. Anything that doesn't match becomes a custom exercise.
- **Daily tracking.** Start a routine or add exercises one at a time, log weight and reps per set, and tick sets as done. New sets are pre-filled with the weight you used last time. You can move to any date. The app also shows your streak and how many sessions you did in the last 7 days.
- **Reports.** Pick today, yesterday, this or last week, last 7 days, this or last month, last 30 days, this year, all time, or a custom date range. Each report shows:
  - days trained, sets, reps and volume
  - a per-day chart
  - sets per muscle
  - per-exercise best weight and estimated 1RM
  
  **Export** a report as CSV or a readable text summary, using the share sheet or saving it to your device.
- **kg / lb toggle**, light and dark themes, and an adaptive launcher icon that supports Android 13+ themed icons.

## Build

Requirements: JDK 17 and the Android SDK (compileSdk 37).

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest
```

`gradle.properties` caps build memory: a 1.5 GB Gradle heap, 2 workers, and the Kotlin compiler running in-process. Test JVMs are capped at 768 MB, one fork at a time. To put a hard limit on the whole build:

```
systemd-run --user --scope -p MemoryMax=3G ./gradlew assembleDebug
```
