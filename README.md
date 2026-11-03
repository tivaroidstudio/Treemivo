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

## Treemivo 0.4 additions — development branch

**Free features**
- **Folder-first treemap:** switch between aggregated immediate-folder tiles and individual file tiles. Tap a folder to drill down; breadcrumbs provide a route back to the indexed-source root.
- **Bookmarked folders:** pin up to 24 folder paths already present in an authorized scan. Bookmarks are navigation shortcuts, not additional Android storage permissions.
- **Storage insights:** view top filename extensions, distribution by modification age, counts of zero-byte/hidden/very large files, and the largest accessible files. Tap an extension to filter the active file map.
- **Advanced view filters:** show only zero-byte files or only dot-prefixed hidden files. Filters change the current view, not the underlying disk or scan. The three biggest-size filters are still available.
- **File details:** copy a relative file path, share an accessible file through Android's share sheet, and inspect the available modification date.
- **External display workflow:** Ctrl+F focuses search, Ctrl+R rescans the selected scope, and Esc closes common overlays.

**Pro features**
- **JSON export:** write a detailed structured scan inventory to a user-selected document, including paths and filenames, using streaming output. Exports are local and explicit.
- **Expanded saved filters:** retain empty/hidden/extension criteria as well as the previous size, file-type, age and sort options.

**Product limitations**
- Source indexes are scoped to MediaStore access or folders granted through SAF. Neither bookmarks nor folder drilldown can bypass Android restrictions.
- Extension, age and file-size insights count **accessible indexed files only**. Modification dates can be missing or inaccurate depending on the document provider.
- Folder mode displays at most 260 largest immediate child tiles at a time, while the existing indexed file lists are not reduced to those tiles.
- JSON reports may contain sensitive filenames and paths; only export to destinations you trust.
- This is development code. Build and device testing are intentionally deferred until the later testing phase.

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


## Treemivo v0.5 — growth insights, review intelligence and DeX controls

- **Scope-aware growth insights (Free basic / Pro detailed):** compare consecutive complete scans only if the authorized SAF folder set or MediaStore permission scope is identical. Show indexed-byte and file-count deltas; Pro breaks changes down by file type. Old/partial/incompatible history is deliberately not compared.
- **Privacy-preserving history:** persist aggregate file-type bytes, scan completeness and a keyed fingerprint of the scan scope, in addition to previously hashed folder totals. Existing history is read safely but is not treated as comparable until new compatible scans exist.
- **Smarter Smart Cleanup suggestions (Free):** optional review groups for screenshots, backup-like file extensions, and truly zero-byte files, in addition to the existing large-video and installer views. Metadata suggestions are not duplicate detection or an instruction to delete anything.
- **Ignore and restore suggestions:** per-file user choice stored locally as a keyed HMAC of URI, size and modification timestamp, without retaining raw paths. Changed files can reappear. Users can restore ignored suggestions.
- **Provider size reliability:** unknown file sizes are now distinguished from confirmed zero-byte files. A 0-byte placeholder with no size metadata must not be advertised as an empty file.
- **DeX inspector:** adjustable 260–520 dp (depending on available window width) details pane with a draggable splitter and on-screen +/− width controls, keeping a minimum width for the treemap.
- **Translations:** new views and actions translated to Polish, English, German, Spanish and French.

No background scanning, forecasting, automatic removal or unrestricted filesystem access is implemented. Charts show only user-authorized indexed data and changes in that same scope. Keep the PR in draft until final build, safety and device testing.


## Treemivo v0.6 — advanced search, smart collections, tracked changes

**Advanced offline search (Free)**

Use search operators directly from the map or file screens:
- `type:video size>500MB` — videos larger than 500 MiB
- `ext:zip,7z path:Download` — archives in matching paths
- `name:"holiday photos" -ext:tmp` — quoted phrase and exclusion
- `after:2026-01-01 before:2026-11-01` — local-calendar modification date range
- Plain text and up to 30 combined rules are supported. The search help panel provides examples; invalid terms are shown rather than deleting or modifying any files.

**Smart Collections (Free)**

A dedicated responsive browser groups only the current accessible indexed files by their first folder, extension, modification year, file-size band and numbered filename families such as `IMG_0001`, `IMG_0002`. Open a group to inspect filenames, preview or open an individual file through Android. Name families are **not** duplicate detection or advice to delete anything. Up to 120 collections and 300 listed members per group are displayed for usability.

**Additional Pro value**

- Saved advanced filter presets retain the user's query expressions along with existing size, date, type, extension, empty/hidden switches. Search phrases may contain sensitive filenames and are stored locally in app-private preferences.
- The History panel shows verified size changes for previously tracked URI identities among the 200 largest accessible files in matching complete scans. Histories store only keyed HMAC digests and sizes, **not** filenames, paths or file contents. Changes only appear when the same URI is tracked in both samples; other changes are not claimed to be new or deleted files.
- The sampling limit reduces local history storage overhead; it is not intended to be a complete file change journal.

**Not yet tested:** Android/Gradle build, UI interactions, large inventories, photo/SAF permissions, billed Pro purchases and device-specific providers. These remain in the deferred release verification checklist.
