# Treemivo — draft privacy policy

Treemivo is a local Android file-size analyzer by Tivaroid Studio.

### Data processed
The application reads names, file sizes, file types, relative paths, modification dates and, **only when the user starts duplicate verification**, file contents to calculate SHA-256 hashes for comparison. Files are analyzed in memory on the device. Scan history stores only timestamp, source type, number of files, total bytes and largest file size in private local storage.

### Permissions
Users choose SAF folders explicitly; grants can be removed from settings. Optional shared-media permissions allow scanning images, video and audio visible through Android MediaStore. On Android 14+ the user may allow only selected images/videos. Treemivo does not request unrestricted all-files access.

### Sharing and purchases
Treemivo has no app network permission, advertisement SDK or analytic SDK. Users may export reports explicitly through Android's document picker and open files in another installed application. Optional purchases are handled by Google Play Billing, whose own terms/privacy practices apply. A selected SAF provider (such as a cloud documents provider) may retrieve its own data when the user grants access.

### Deletion
Files are never deleted automatically. Media removal uses Android system trash confirmation where available; SAF document removal, if supported by the provider, requires explicit confirmation and may be irreversible.

### Contact and updates
Before publishing, replace this draft with a public policy including support contact, revision date, rights requests and any actual additional services or SDKs enabled at release.


### New offline processing (development)
With a deliberate user action, Treemivo may load bounded photo thumbnails to calculate perceptual similarity hashes, preview a media/PDF/text file, or temporarily copy a ZIP file (up to 32 MB) into app-private cache to read its index without extracting contents. Temporary ZIP files are deleted after inspection. These are local operations. Pro history may retain truncated HMAC-protected folder identifiers and aggregate sizes in private app storage to compare scans; no raw folder paths are persisted in history. Users can clear scan summaries from the app.

### Growth trends and ignored cleanup suggestions (development)

The app locally stores aggregate bytes by file type and a pseudonymous, keyed fingerprint of granted scan sources to avoid comparing unrelated or incomplete scans. It does not store raw scanned folder URIs in scan history. Ignore decisions for Smart Cleanup are stored as keyed digests of document URIs plus size and modification metadata; raw file paths are not persisted for these ignore choices. These values are not uploaded. Users can clear scan history or restore ignored cleanup suggestions from within the app. Different source permissions may make earlier scan snapshots incomparable.


### Advanced search, collections and tracked files (development)

Advanced searches and Smart Collections operate on local metadata of files already available to Treemivo. Search expressions stored intentionally as Pro filter presets may contain user-entered filenames and path terms; these are kept only in private app preferences. The history feature also saves keyed, truncated HMAC identifiers and size measurements for a bounded sample of 200 large files in order to identify size changes across comparable scans. No raw filenames, file paths, file contents or complete content URIs are stored in this history index, and the app does not upload this data. Users may clear scan history at any time.
