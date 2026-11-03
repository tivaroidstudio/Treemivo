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
class ScanHistoryTest {
    @Test fun recordsAggregatesButNeverPersistsFileNames() {
        val context = RuntimeEnvironment.getApplication()
        val history = ScanHistory(context)
        history.clear()
        val file = FileRecord(
            "secret-personal-file.txt", "Private/secret-personal-file.txt",
            1234L, "text/plain", Uri.parse("content://example/private/secret")
        )
        history.record("FOLDERS", StorageReport("Private", listOf(file),
            folderBreakdown(listOf(file)), false))
        val entries = history.entries()
        assertEquals(1, entries.size)
        assertEquals(1, entries.single().count)
        assertEquals(1234L, entries.single().bytes)
        val serialized = context.getSharedPreferences("treemivo_history", 0)
            .getString("entries", "") ?: ""
        assertFalse(serialized.contains("secret-personal-file"))
        history.clear()
        assertTrue(history.entries().isEmpty())
    }

    @Test fun retainsAtMostThirtySummaries() {
        val history = ScanHistory(RuntimeEnvironment.getApplication())
        history.clear()
        val report = StorageReport("Empty", emptyList(), emptyList(), false)
        repeat(40) { history.record("MEDIA", report) }
        assertEquals(30, history.entries().size)
        history.clear()
    }
}
