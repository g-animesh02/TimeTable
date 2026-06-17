# TimeTable — MBA Module 5 Class Planner (Android)

A native Android app (Kotlin + Jetpack Compose, Material 3) built from the
`final_TT_Mod_5.xlsx` master timetable. You pick the subjects you're enrolled
in, the app builds your personal week-wise timetable, lets you export it as an
image, track attendance, and add/modify classes on any given day.

## Features

- **Subject selection & config** — Pick your **batch (A / B)** and the subjects
  you're taking from the 34 Module-5 electives. Your choice is saved on-device
  (DataStore) so the app opens straight to your timetable next time.
- **Week-wise timetable** — A clean Monday–Saturday view of your classes with
  time, room and faculty, colour-coded per day.
- **Export as image** — One tap renders the whole timetable to a PNG, saves it
  to `Pictures/TimeTable`, and opens the share sheet.
- **Attendance tracking** — Navigate week by week and mark each class
  **Present / Absent / Cancelled**. The **Stats** tab shows your overall and
  per-subject attendance percentage.
- **Edit a particular day** — Add an extra class, change a class's timing or
  room, or cancel it — for one specific date only, without touching the
  recurring schedule.

## How the timetable data works

The spreadsheet organises classes into elective *baskets* (e.g. five subjects
sharing the 09:30–11:00 slot, each with its own faculty and room). The
`-A` / `-B` suffix on slot codes denotes the **batch division** — Batch A and
Batch B attend the same course on different days. The parser extracted every
`(subject, day, time, faculty, room, batch)` offering into
`app/src/main/assets/timetable.json` (82 offerings, 34 subjects). The app
filters these by your selected subjects and batch.

## Project structure

```
app/src/main/
├── assets/timetable.json          # parsed master timetable (bundled data)
├── java/com/animesh/timetable/
│   ├── MainActivity.kt            # Scaffold + bottom navigation
│   ├── data/
│   │   ├── model/                 # JSON models + ScheduleEntry
│   │   ├── local/                 # Room DB, DAOs, entities, DataStore
│   │   └── repo/                  # TimetableRepository (single source of truth)
│   └── ui/
│       ├── MainViewModel.kt       # state + scheduling/attendance logic
│       ├── screens/               # Setup, Schedule, Attendance, Stats, Settings
│       ├── components/            # shared composables
│       ├── export/ImageExporter   # render-to-PNG + share
│       └── theme/
```

## Build & run

Open the project in **Android Studio** (Giraffe or newer) and run, or from the
command line with an Android SDK installed:

```bash
./gradlew assembleDebug      # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # installs onto a connected device/emulator
```

- minSdk 26, targetSdk 35, Kotlin 2.1, AGP 8.7.3, Gradle 8.14.3.
- No internet permission required — everything runs offline on-device.

## Regenerating the data from a new spreadsheet

The bundled `timetable.json` was produced from `final_TT_Mod_5.xlsx`. If the
master timetable changes, re-run the parsing logic (see the project history)
to regenerate `app/src/main/assets/timetable.json`.
