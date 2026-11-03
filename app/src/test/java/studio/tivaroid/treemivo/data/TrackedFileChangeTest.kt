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
class TrackedFileChangeTest {
    private fun report(size: Long) = StorageReport(
        "MediaStore",
        listOf(
            FileRecord("sensitive-photo.jpg", "DCIM/sensitive-photo.jpg",
                size, "image/jpeg",
                Uri.parse("content://media/external/images/media/99"))
        ),
        emptyList(),
        partial = false
    )

    @Test fun comparesOnlySameHashedFileInSameAuthorizedScope() {
        val context = RuntimeEnvironment.getApplication()
        val history = ScanHistory(context)
        history.clear()
        val before = report(1024)
        history.record("MEDIA", before, listOf("READ_MEDIA_IMAGES"))
        val previous = history.entries().first()
        val changes = history.changedTrackedFiles(previous, report(2048),
            history.scopeKey("MEDIA", listOf("READ_MEDIA_IMAGES")))
        assertEquals(1, changes.size)
        assertEquals(1024L, changes.single().deltaBytes)
        assertTrue(history.changedTrackedFiles(previous, report(2048),
            history.scopeKey("MEDIA", listOf("READ_MEDIA_AUDIO"))).isEmpty())
        val stored = context.getSharedPreferences("treemivo_history", 0)
            .getString("entries", "") ?: ""
        assertFalse(stored.contains("sensitive-photo"))
        assertFalse(stored.contains("content://media"))
        history.clear()
    }
}
