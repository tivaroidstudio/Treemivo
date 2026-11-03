# Treemivo — visual storage analyzer

Native Android (Kotlin + Jetpack Compose) by **Tivaroid Studio**.

## Product vision
A fast, private visual storage explorer inspired by treemaps. The phone UI follows the supplied dark mockup; a wide window (tablet / DeX / connected display) shows navigation, a large treemap, and a detail panel at once.

## Current prototype
- Read-only analysis of a **user-selected folder** using Android Storage Access Framework (SAF).
- Real file-size scan and color-coded interactive treemap; folder and file-type breakdown.
- Duplicate detection by **size and SHA-256 contents** on demand.
- CSV export, tap a file to open it in another app.
- Responsive layout: phone bottom navigation, desktop sidebar, two content panes.
- Localized Android string resources: English, Polish, German, Spanish, French; language follows system or Android 13+ app language settings.
- All analysis is **on-device**, with no internet permission, telemetry or account.

## Development
Open this repository in Android Studio with JDK 17 and Android SDK 36.
Gradle 8.11.1 / AGP 8.9.2 / Kotlin 2.1.20. Run `./gradlew :app:assembleDebug` on Linux/macOS, or `gradlew.bat :app:assembleDebug` on Windows. The Gradle wrapper scripts and JAR are included.

## Important Android limitation
On Android 11+, SAF cannot select the storage root, the Downloads folder root, or Android/data and Android/obb. The storage bar shows **device-wide** free/used space; the treemap shows **only files in the user-selected folder**. Do not confuse these statistics. The app intentionally does **not** request `MANAGE_EXTERNAL_STORAGE` or delete files. A later, justified file-management edition can evaluate Play policy and alternative MediaStore access before promising full-device scan.

## Next production milestones
1. Instrumented QA on Android 10–16 and Samsung DeX / desktop windowing.
2. Incremental scans, caching, progress and cancellation for large trees.
3. Safe bulk clean-up with per-file confirmation and undo where feasible.
4. UX polish, accessibility, dark/light themes, performance tests, store listing, privacy policy.
5. Optional Pro unlock (advanced filters, saved reports, compare scans); core access and truthful basic analysis stay free.

No binaries, personal user files or secrets belong in this repository.
