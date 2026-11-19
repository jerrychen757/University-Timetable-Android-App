# 排課小幫手

排課小幫手是一個 Android 課表應用程式。你可以手動排課，或在設定裡填上學校的開課 API 網址後匯入課程；考試日曆會把同一門課相鄰節次合併成一段時間，也可以在上課前收到提醒，並把課表放到主畫面小工具或匯出成 PDF。課表存在這支手機上。

# Timetable Helper

Android app for building a weekly course timetable (`com.czy.timetable`). Course data stays on the device. Importing a catalog is optional: paste your school's course API URL in Settings. If that URL is empty, the app does not call a course endpoint.

## Features

- Weekly course timetable, including weekend days and early or evening periods when you turn them on.
- Exam calendar. Consecutive periods of the same course on the same day are merged into one exam, from the first period's start to the last period's end.
- Pre-class notifications. Each course can remind you ahead of time. Settings also has a unified default for new courses and for courses imported from the catalog, and you can apply that default to every course already on the timetable.
- Home-screen widget (Jetpack Glance) that lists today's classes.
- PDF export of the timetable, with optional school, name, and student ID in the header. JSON backup and restore are available from the same settings page.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Navigation Compose
- Room with KSP
- DataStore Preferences
- Glance app widgets
- Google AdMob (rewarded ad)

## Build

Requirements:

- JDK 17 or newer (the Gradle build is tested with JDK 21)
- Android SDK with platform `android-36` and Build-Tools `36.0.0`
- `sdk.dir` set in `local.properties` (this file is gitignored)

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## AdMob

Put your AdMob IDs in `local.properties`. Do not commit that file.

```properties
admob.app.id=YOUR_ADMOB_APP_ID
admob.rewarded.unit.id=YOUR_REWARDED_AD_UNIT_ID
```

If either property is missing, the build falls back to Google's public test ad IDs.

## School course API

Open **Settings → 從開課資訊匯入** (the same field is also under **Settings → 資料管理**, next to the school name). Paste a URL template. These placeholders are replaced when you import:

| Placeholder | Replaced with |
| --- | --- |
| `{year}` | The academic year you select (ROC year, for example `114`) |
| `{semester}` | The semester you select (`1` or `2`) |

Example:

```text
https://example.edu/api/courses?year={year}&semester={semester}
```

Leave the field empty if you do not want course import. An empty value is the default, including for existing installs that never saved a URL. The app then skips the network call and asks you to fill in the URL in Settings. The response parser still accepts the JSON shapes it already understood (an array of course objects, or an object with `items`, `data`, or a nested item list, including the field names used by the original catalog).

The Taiwan public-holiday calendar (`cdn.jsdelivr.net/gh/ruyut/TaiwanCalendar`) is separate and is not configured in Settings.

The school name used on the PDF header defaults to empty until you type one. A school name already saved on the device is left as is.

## License

GPL-3.0. See [LICENSE](LICENSE). Copyright (c) Jerry.czy.
