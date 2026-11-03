package studio.tivaroid.treemivo.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FolderHistoryComparisonTest {
    private fun file(n: String, s: Long) =
        FileRecord(n.substringAfterLast('/'), n, s, "image/jpeg",
            Uri.parse("content://example/" + n))

    @Test fun comparesFolderWithoutPersistingRawPath() {
        val app = RuntimeEnvironment.getApplication()
        val history = ScanHistory(app)
        history.clear()
        val before = listOf(file("DCIM/Camera/secret.jpg", 100))
        val after = listOf(file("DCIM/Camera/secret.jpg", 150))
        history.record("MEDIA", StorageReport("MediaStore", before, folderBreakdown(before), false),
            listOf("READ_MEDIA_IMAGES"))
        val old = history.entries().first()
        val comparison = history.compare(old,
            StorageReport("MediaStore", after, folderBreakdown(after), false),
            history.scopeKey("MEDIA", listOf("READ_MEDIA_IMAGES")))
        assertTrue(comparison.any { it.currentPath == "DCIM/Camera" && it.delta == 50L })
        val json = app.getSharedPreferences("treemivo_history", 0)
            .getString("entries", "") ?: ""
        assertFalse(json.contains("DCIM"))
        assertFalse(json.contains("secret.jpg"))
        history.clear()
    }
}
