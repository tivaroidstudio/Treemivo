# Treemivo — visual storage analyzer (Android)

**Tivaroid Studio** · Kotlin · Jetpack Compose · Android 8–16

Treemivo is a storage explorer with a real file-size treemap, source-aware reporting, filters, optional duplicate detection and a responsive Android desktop-window layout. The dark UI follows the supplied phone mockup. Samsung DeX / connected monitor windowing uses sidebar + treemap + detail pane at 900dp width or more.

## Available functionality

- **Scoped media library:** uses indexed Android MediaStore Images, Video and Audio collections when permissions are granted. Android 14+ may expose *only user-selected photos/videos*.
- **Selected folders:** pick and persist multiple folders using Android Storage Access Framework (SAF); revoke a folder grant from app settings.
- **Fast, interruptible scans:** background I/O, single-pass size enumeration, batched progress, cancel controls and a 100,000-entry safety cap per scan.
- **Map and reports:** real-size interactive treemap (260 largest tiles for legibility), directory breakdown, file types and CSV export.
- **Advanced filters:** name/path search, 100MB/1GB lower bounds, 1GB upper bound, file types, last 30 days, sort by size, filename or modified date. Heavy sorting and folder summaries run off the UI thread.
- **Exact duplicates:** candidates grouped by size and checked with SHA-256 on demand, cancelable with progress.
- **Safe disposal:** Android 11+ indexed media goes to the system trash only after in-app confirmation AND Android's own system approval. SAF documents may be permanently deleted only after an explicit irreversible-action warning; providers may refuse the action. MediaStore supports a bounded batch trash flow (up to 500 selected files), always with explicit user and system confirmation. There is no automatic deletion or bulk SAF deletion.
- **History:** aggregate-only scan summaries plus HMAC-pseudonymized top-folder sizes, most recent 3 free / last 30 with Pro. Pro also displays comparisons to older scans of the same source.
- **Pro billing:** Google Play Billing 9.1.0 one-time product ID `treemivo_pro_lifetime`. No hardcoded Pro toggle. A real product must be configured in Play Console and tested before launch.
- **Localization:** English, Polish, German, Spanish and French through `res/values-*/strings.xml`; locale selection uses Android's app language settings on Android 13+.
- No app network permission, telemetry, advertising SDK, or automatic uploads.

## Additional features in v0.3 (development)

- **Treemap gestures:** pinch to zoom, drag to pan and double tap to reset.
- **Local previews:** images, video thumbnails, the first page of PDFs and small text files.
- **Smart Cleanup:** free large-video, installer and exact-duplicate suggestions; Pro adds old-download and visual-similarity suggestions. Suggested bytes are *not* guaranteed savings.
- **Batch media trash:** checkbox-selected MediaStore files on Android 11+ via one Android system consent prompt (max 500 per transaction); never silent removal. SAF deletion remains single-file and can be permanent.
- **Photo similarity (Pro):** on-demand local difference hash for up to 400 accessible photos. False positives are possible; users must inspect every image and choose manually.
- **Pro reports and history:** offline PDF summary, history trend plot and HMAC-pseudonymized folder-by-folder comparisons.
- **Pro utilities:** up to 10 locally saved filter presets and limited ZIP metadata inspection (up to 32 MB; no extraction).
- **SD/USB OTG:** users may grant compatible removable directories through the Android folder picker.

**Not implemented:** scheduled background scans, unrestricted/root scanning, AI quality ranking of photos, automatic cleanup, trusted backend purchase-token validation and a production-tested APK. Do not advertise these capabilities.

## Critical storage limitation

This is **NOT** a full unrestricted scan of all phone data. On modern Android the SAF picker cannot grant the entire storage root, root Downloads, or other apps' private data folders. MediaStore can read **only accessible indexed media**, according to Android permissions. Other files require separate user-authorized folder grants. The device-wide used/free indicator is independent of the selected source's treemap total, so the values need not add up. Do not market Treemivo as an all-files/root filesystem scanner.

The app deliberately does not declare `MANAGE_EXTERNAL_STORAGE`. Google Play tightly restricts that permission; it is not a shortcut for indexing a user media library. The `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` permissions also require a justified core use case under current Play policy. Get approval as needed before production.

## Build and tests

Requirements: Android SDK 36, Android Studio, JDK 17+ (JDK 21 recommended for Robolectric SDK 36 tests).

- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lintDebug`
- `./scripts/verify.sh` — Linux/macOS full local verification.

Windows: use `gradlew.bat` instead of `./gradlew`.

Build stack: Gradle 8.13, Android Gradle Plugin 8.12.3, Kotlin 2.3.21, compile/target SDK 36, minimum SDK 26.

## Monetization checklist

1. Create and activate a **non-consumable** product `treemivo_pro_lifetime` in Play Console.
2. Configure localized price, internal testing license accounts and billing test tracks.
3. Verify new, pending, completed, refunded and restored purchase states.
4. Before a production launch, move purchase-token verification and acknowledgment to a trusted backend using Google Play Developer API if strong anti-fraud assurance is needed. This prototype only queries purchases via BillingClient.
5. Do not sell features until backend/payment QA and Google Play compliance checks are complete.

Read `docs/RELEASE_CHECKLIST.md` and `docs/TESTING.md` before release.
