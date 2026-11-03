package studio.tivaroid.treemivo.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FileAnalyticsTest {
    private fun record(path: String, bytes: Long, mime: String = "text/plain", modified: Long = 0L) =
        FileRecord(
            name = path.substringAfterLast('/'),
            relativePath = path,
            sizeBytes = bytes,
            mimeType = mime,
            uri = Uri.parse("content://example/files/" + path.replace('/', '_')),
            modifiedAtMillis = modified
        )

    @Test fun totalsAreRolledUpToAncestors() {
        val items = listOf(
            record("DCIM/Camera/a.jpg", 100, "image/jpeg"),
            record("DCIM/Camera/b.jpg", 70, "image/jpeg"),
            record("DCIM/Screen/c.jpg", 30, "image/jpeg")
        )
        val folders = folderBreakdown(items).associateBy { it.relativePath }
        assertEquals(200L, folders.getValue("DCIM").sizeBytes)
        assertEquals(3, folders.getValue("DCIM").fileCount)
        assertEquals(170L, folders.getValue("DCIM/Camera").sizeBytes)
        assertEquals(2, folders.getValue("DCIM/Camera").fileCount)
        assertEquals(30L, folders.getValue("DCIM/Screen").sizeBytes)
    }

    @Test fun emptyDirectoryAggregationRemainsEmpty() {
        assertTrue(folderBreakdown(emptyList()).isEmpty())
    }

    @Test fun caseInsensitiveSearchMatchesFileAndPath() {
        val list = listOf(record("DCIM/Camera/Photo.PNG", 2), record("docs/notes.txt", 3))
        assertEquals(1, filterFiles(list, AdvancedFilter(query = "photo")).size)
        assertEquals("Photo.PNG", filterFiles(list, AdvancedFilter(query = "CAMERA")).single().name)
    }

    @Test fun exactSizeWindowUsesInclusiveBounds() {
        val files = listOf(record("a", 99), record("b", 100), record("c", 101))
        assertEquals(1, filterFiles(files,
            AdvancedFilter(minSizeBytes = 100, maxSizeBytes = 100)).size)
    }

    @Test fun fileTypeFilterDetectsVideo() {
        val list = listOf(record("a.mp4", 10, "video/mp4"), record("b.txt", 40))
        assertEquals("a.mp4", filterFiles(list, AdvancedFilter(kind = FileKind.VIDEO)).single().name)
    }

    @Test fun changedAfterAndSortAreApplied() {
        val list = listOf(record("old.txt", 100, modified = 1000),
            record("new.txt", 10, modified = 3000),
            record("mid.txt", 30, modified = 2000))
        val recent = filterFiles(list,
            AdvancedFilter(modifiedAfterMillis = 2000, sort = FileSort.MODIFIED_DESC))
        assertEquals(listOf("new.txt", "mid.txt"), recent.map { it.name })
    }

    @Test fun alphabeticalSortingIgnoresCase() {
        val items = listOf(record("z.txt", 1), record("Apple.txt", 1), record("banana.txt", 1))
        val sorted = filterFiles(items, AdvancedFilter(sort = FileSort.NAME))
        assertEquals(listOf("Apple.txt", "banana.txt", "z.txt"), sorted.map { it.name })
    }

    @Test fun largeCollectionCanBeFilteredWithoutOpeningFiles() {
        val list = (1..10_000).map { record("files/file_$it", it.toLong()) }
        val filtered = filterFiles(list, AdvancedFilter(minSizeBytes = 9_900))
        assertEquals(101, filtered.size)
        assertEquals(10_000L, filtered.first().sizeBytes)
    }
}
