# Treemivo testing plan

## Automated tests in repository
- `TreemapLayoutTest`: normalized layout area and dominant-tile behavior.
- `FileAnalyticsTest`: ancestor folder totals, search, file types, size windows, time filter, sort and 10k-element list.
- `ScanHistoryTest`: aggregate-only history with a 30-entry cap and clear behavior.
- `SafeFileRemovalTest`: MediaStore-only trash eligibility and SAF/untrusted provider rejection.

From the repository root run `./scripts/verify.sh` after installing Android SDK 36.

## Manual end-to-end matrix
| Scenario | Expected result |
| --- | --- |
| Android 14: grant selected photos only | Only granted visual items appear; report is partial |
| Android 13: allow audio only | Scan returns accessible audio, not unauthorized pictures |
| Add 2 SAF folders | Both scanned; permission persists across app restart |
| Remove a remembered folder | It disappears from scope and grant is released |
| Scan folder with 100k+ documents | Shows progress then partial result and can cancel |
| Same-size files with different bytes | Not displayed as duplicates |
| Cancel SHA-256 check | Stops reading files and keeps UI usable |
| Filter >= 100MB + videos | Only matching media files displayed |
| Set screen width above 900 dp | Side navigation, treemap and details pane |
| Delete indexed media on API 30+ | App prompt followed by Android system trash prompt |
| Cancel Android trash prompt | Source file remains; scan reflects current state |
| SAF delete when provider denies write | File is not removed; error reported |
| Open CSV in spreadsheet | Values are quoted, leading formula markers neutralized |
| Play Billing purchase cancelled | Pro remains locked |
| Play Billing purchase pending | Pro remains locked |
| Play Billing purchased | Pro unlocked after ownership refresh |
| Play Billing restored after reinstall | Pro unlocked for authorized Play account |

## Minimum performance benchmark
Use comparable files and repeat each scan 3x on real hardware:
- 1k local SAF entries, 10k local SAF entries, 50k MediaStore indexed items
- Record wall-clock time, heap peak, app responsiveness, cancellation latency
- Compare Samsung A55 phone view and DeX or large Android display
- Avoid promising absolute speed until measured.

## Environment note
Android SDK/emulator and network package retrieval are unavailable in the authoring sandbox. Changes committed to GitHub do not count as a verified APK. The automated Android build and integration tests must run on an Android SDK-equipped machine before merging or publishing.
