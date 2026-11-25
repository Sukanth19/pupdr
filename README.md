<p align="center"><img src="docs/icon.png" width="360" alt="pupdr icon"></p>

# pupdr

A Kotlin / Jetpack Compose Android app for tracking daily gym workouts. The name comes from the STM32 GPIO pull-up/pull-down register (PUPDR); the icon is a minimal pull-up bar over ⇅ arrows.

## Features

- **870+ exercise library, offline.** The full [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (public domain) is bundled. Search it, filter by muscle and equipment, and read step-by-step instructions. You can also add your own exercises, and delete them again.
- **Routines.** Tap *New routine* and the exercise library opens straight away. Pick your exercises, name the routine inline, and tap any exercise to set its sets × reps or reorder it. If you back out of a new routine without adding anything, it's discarded. You can also start a new routine from any exercise in the library.
- **Daily tracking.** Start a routine or add exercises one at a time, log weight and reps per set, and tick sets as done. New sets are pre-filled with the weight you used last time. You can move to any date. The app also shows your streak and how many sessions you did in the last 7 days.
- **Reports.** Pick Day, Week, Month, Year or All, step back through earlier periods, or choose a custom range. Each report shows:
  - days trained, sets, reps and volume
  - a per-day chart
  - sets per muscle
  - per-exercise best weight and estimated 1RM
  
  **Export** a report as CSV or a readable text summary, using the share sheet or saving it to your device.
- **Material 3, in black and white**: a monochrome ink theme (true black in dark mode), with optional Material You wallpaper colours in the ⋮ menu. Large collapsing app bars, a swipeable week strip, swipe-to-delete sets, A–Z library headers, and bottom-sheet menus.
- **Motion** follows Material's emphasized easing: fade-through between tabs, shared-axis for detail screens, animated list insert/remove/reorder, rolling numbers, growing charts, and FABs that collapse on scroll.
- kg / lb, light and dark themes, and an adaptive launcher icon that supports Android 13+ themed icons.

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
