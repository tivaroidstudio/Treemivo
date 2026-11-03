# Treemivo — release readiness

## Before Play production
- [ ] Confirm debug and release builds with Android SDK 36 / JDK 17.
- [ ] Run manual tests on Android 10, 13, 15 and 16.
- [ ] Test Samsung DeX or another external display at 900dp+ and with window resizing.
- [ ] Measure scans of 1k / 10k / 40k files and provider cancellation/error handling.
- [ ] Verify reported sizes on multiple document providers.
- [ ] Check all five locales, screen readers, font scale 200%, contrast and RTL.
- [ ] Check document opening, duplicate SHA-256 groups and CSV in spreadsheet apps.
- [ ] Create actual production icon, screenshots, store description and privacy policy URL.
- [ ] Review Google Play data safety and permissions declarations.
- [ ] Choose Play Billing product IDs and implement genuine entitlement verification BEFORE offering Pro.
- [ ] Evaluate whether broad storage access is essential and eligible for Play policy. It is NOT used by this version.
- [ ] Add performance/robustness tests, crash testing, and independent security review where needed.

## Commercial approach
Free: visual map, folder analysis, basic breakdown.
Potential Pro (future only): saved scan history, comparison between scans, advanced filters, batch reports, custom export.
No billing, deletion, automatic cleanup, ads or tracking exist in v0.1.0. Never advertise unimplemented functions.

## Product scope warning
Android 11+ SAF blocks full internal-storage root, root Downloads and private app directories. The screenshot-like DEVICE storage bar is device-wide; the map is limited to the user-granted tree. Never report the scan total as full-device usage or suggest that every file is accessible.
