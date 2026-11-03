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
