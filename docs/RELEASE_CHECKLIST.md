# Release readiness: Treemivo

These checks are not automatically satisfied by a successful code commit.

## Build/test
- [ ] Assemble debug and release variants with Android SDK 36 (JDK 17+).
- [ ] Pass all Robolectric unit tests and Android Lint.
- [ ] Launch on Android 8/10/12/13/14/15/16. Validate permissions on each OS.
- [ ] Test Android 14+ full photo access, selected-only access, audio-only grant, deny and revoke.
- [ ] Test images, videos and audio from MediaStore against device file manager.
- [ ] Test SAF 1 folder, 2 overlapping folders, removed permission, disconnected SD/USB drives, large and cloud-backed folders.
- [ ] Run 1,000 / 10,000 / 100,000 entry benchmarks; observe memory, ANR, cancel, and provider exceptions.
- [ ] Verify scans stop, no stale UI state after quickly switching sources.
- [ ] Verify duplicate hashing by content (same-size different files must not match), and cancellation.
- [ ] Confirm metadata sorting/filtering including unknown sizes and missing modification dates.
- [ ] Test MediaStore system trash approval and cancellation; SAF permanent delete with consent and provider denial.
- [ ] Test CSV encoding and spreadsheet formula injection hardening.
- [ ] Test portrait, landscape, tablet and Samsung DeX 900dp+ resizing; keyboard/mouse focus.
- [ ] Validate all 5 locales, 200% fonts, TalkBack/VoiceOver equivalent and accessible contrast.

## Commercial and legal
- [ ] Create lifetime INAPP product `treemivo_pro_lifetime` in Play Console; test ownership and restore.
- [ ] Validate purchase states and tokens on a secure backend before relying on Pro sales at scale.
- [ ] Confirm Play policies for persistent image/video read permissions; consider Photo Picker where suitable.
- [ ] No MANAGE_EXTERNAL_STORAGE permission is included.
- [ ] Host a public privacy policy and correctly fill the Google Play Data Safety form.
- [ ] Supply original app icon, screenshots, feature graphic, ASO copy and store categories.
- [ ] QA backup/restore behavior; no confidential file names in scan history.
- [ ] Set versionCode/versionName for release; test with internal/closed track before production.

## Intentional limits
- Device space usage is global. Treemap sizes are *only granted indexed media or selected SAF folders*.
- Tree displays 260 largest file rectangles; all scanned records remain available in lists and CSV.
- Provider may return unknown file size, zero, inaccessible URI or refuse deletion.
- Media trash is recoverable through a compatible media manager; SAF direct deletion may be permanent.
- No automatic deletion, bulk destructive actions, or on-device root file access.


## 0.3 additions requiring hands-on verification
- [ ] Validate Zoom/pan hit-testing and double-tap reset in different orientations and external display window sizes.
- [ ] Verify Smart Cleanup candidates, individual previews, 500-item media trash cap, system cancel/error paths and file restoration.
- [ ] Stress-test visual photo similarity on varied lighting, blank photos, rotations and screenshots; assess false positives.
- [ ] Inspect ZIPs with malicious paths, compression bombs and broken central directories; no data should be extracted.
- [ ] Verify locale behavior and layout of new dialogs, charts, filter presets and PDF export.
- [ ] Test snapshot HMAC comparisons across app relaunches and after clearing scan history.
- [ ] Validate Pro billing product in Google Play internal testing and verify entitlements securely before public paid launch.
