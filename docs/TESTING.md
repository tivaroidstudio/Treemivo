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


## 0.3 feature-focused device checks (not yet executed)
| Scenario | Expected result |
| --- | --- |
| Pinch/drag/double-tap on phone and DeX | Treemap zooms, pans, resets; selected tile remains accurate after transform |
| Very large directory (100k files) | UI remains responsive during filter and treemap calculation |
| Select 3 MediaStore videos | Single in-app confirmation followed by one system trash dialog |
| Cancel system trash approval | No files are changed |
| Attempt bulk deletion of a SAF document | No bulk action is offered; single-file irreversible warning remains |
| Preview a large photo, 4K video, PDF and text | Bounded local thumbnail or safe fallback; no uploads or OOM |
| Pro compare two visual near-duplicates | Suggestions only, with manual review; never auto-delete |
| Pro on 500+ images | At most 400 analyzed per user request, with progress/cancel |
| Open malicious ZIP, huge ZIP or corrupted ZIP | No extraction; reject/cap metadata preview, delete temporary cached file |
| Pro save filter and reopen app | Filter remains available; free users cannot create new paid presets |
| Pro export PDF | User chooses output URI, correct metadata, no file upload |
| 2 scans with same folder changed | HMAC-pseudonymized saved snapshots enable size differences |
| Mixed MediaStore and SAF history | Changes are compared only within matching source |
| Android 14 select-only photos | Only authorized media considered; total file map is partial |

**Regression**: verify main Material3 screen, MediaStore consent, page layout, persistent SAF folders and billing setup; no test here guarantees Google Play release readiness.


## Deferred 0.4 validation scenarios — DO NOT mark done yet

These scenarios belong to the planned final Android QA phase.

1. Directory treemap at root: blocks represent immediate children and sum to scanned indexed bytes; tapping a folder focuses its descendants and back/breadcrumb returns to root.
2. Rapid folder navigation with 100,000 indexed records: background treemap building must not freeze the UI; cancel/refresh must not display stale results.
3. Bookmarks: pin nested folder, change source, return, remove folder grant, and verify stale bookmarks are not offered as accessible files.
4. File extension filtering: open Types/Insights, tap an extension, verify map/list; clear chip resets filtering without altering scan results.
5. Hidden and zero-byte filters: verify combinations with size, type, age and existing saved presets; migrate older saved presets correctly.
6. Insights: compare extension totals and file-age buckets to independently inspected test data; handle missing modified dates and empty scans.
7. JSON export: verify parsing, Unicode, quotes/newlines in filenames, no truncation at large inventory size and no unexpected network access.
8. File sharing: external receiving apps should open shared media and SAF URIs with temporary read permission, then revoke access when appropriate.
9. Desktop mode: Ctrl+F focuses search, Ctrl+R rescans, Esc closes dialogs; try mouse/touch and DeX resizing.
10. Long path breadcrumbs and 24 pinned folders: check narrow phone, 200% font scaling, TalkBack and all five locales.
11. Regression: default map, duplicate SHA-256, system trash, billing purchase restoration, PDF report, photo similarity and SAF permissions.

Automated instrumented tests, emulator QA and full builds have **not** been executed for this development batch at the user's request.


## Deferred v0.5 scenarios — not yet executed

- [ ] Two complete MediaStore scans with unchanged permissions: display indexed-byte, file-count and file-type deltas.
- [ ] MediaStore access reduced from full library to selected photos: old snapshot must NOT be compared.
- [ ] Add/remove a SAF folder grant and rescan: old snapshot must NOT be compared as if same source.
- [ ] Partial or incomplete scan must not drive growth or file-type delta conclusions.
- [ ] Read legacy scan summaries that lack scope fingerprint or type breakdown without crash.
- [ ] One scan only, two scans with the same timestamp, or multiple scopes mixed in history: no fabricated growth.
- [ ] Smart Cleanup screenshot/backup/empty suggestions: only metadata-based review prompts and no auto-delete.
- [ ] Unknown size from a SAF provider: never classify the file as confirmed empty.
- [ ] Ignore a suggested item, rescan and reopen; it should remain hidden until restored or file metadata changes.
- [ ] Restore ignored suggestions; ensure no filenames or raw URIs are saved in ignore preferences.
- [ ] Test desktop resize via touch/mouse drag and on-screen +/− across 900–2000 dp, large fonts and rotation.
- [ ] Ensure right panel scrolling does not prevent folder/list navigation, and narrow DeX windows revert to phone layout.
- [ ] Retest all prior confirmation dialogs and system trash cancellation paths after adding review categories.

These scenarios are queued for the user's final verification phase; no build or tests were run in this feature batch.


## Deferred v0.6 scenarios — intentionally not run yet

1. Parse and combine `type:`, `ext:`, `name:`, `path:`, `in:`, `size>`, `size<`, `before:` and `after:` expressions, quoted phrases and exclusion; display errors for invalid input.
2. Verify unknown-size files do not satisfy size comparison expressions.
3. Verify advanced searches work on the phone map and DeX sidebar without losing the focused query.
4. Save Pro filters that contain quoted search expressions; restart and reapply; legacy presets should still load with empty query.
5. Group files by folders, extensions, modification years, size bands and numbered-name families; ensure a name family is never labeled a confirmed duplicate.
6. Switch collection modes rapidly and open/preview/share selected files; handle disconnected document providers safely.
7. Browse 100,000 indexed records on budget Android phones: background grouping, scrolling performance, bounded UI memory and cancellation.
8. Compare top-200 large-file tracking across matching complete snapshots; verify differing source permission scopes, partial scans and unknown sizes do not yield fabricated file changes.
9. Verify no raw file paths, filenames or content URIs are written to scan history; verify saved user-entered search text is stored only privately.
10. Complete Android SDK 36 build, unit tests, lint and device/manual destructive-operation regressions at the final testing stage.

New Robolectric/JUnit test source files have been prepared for advanced search, collections and tracked-file comparisons, but have not been executed.
