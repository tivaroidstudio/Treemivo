package studio.tivaroid.treemivo.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SmartCollectionsTest {
    private fun file(path: String, bytes: Long, known: Boolean = true) =
        FileRecord(path.substringAfterLast('/'), path, bytes, "image/jpeg",
            Uri.parse("content://example/" + path), sizeKnown = known)

    @Test fun groupsImmediateTopFolderWithoutCountingOtherFolders() {
        val items = listOf(
            file("DCIM/Camera/a.jpg", 100),
            file("DCIM/Camera/b.jpg", 200),
            file("Downloads/c.jpg", 50)
        )
        val groups = makeCollections(items, CollectionMode.FOLDERS)
        assertEquals(300L, groups.first { it.id == "DCIM" }.bytes)
        assertEquals(2, groups.first { it.id == "DCIM" }.count)
    }

    @Test fun numberSuffixCreatesNameFamilyButNotDuplicateGuarantee() {
        val items = listOf(
            file("DCIM/IMG_0001.jpg", 100),
            file("DCIM/IMG_0002.jpg", 150),
            file("DCIM/holiday.jpg", 130)
        )
        val families = makeCollections(items, CollectionMode.NAME_FAMILIES)
        assertEquals(1, families.size)
        assertEquals(2, families.single().count)
    }

    @Test fun fileSizeCollectionsPreserveUnknownSize() {
        val items = listOf(file("a.jpg", 0, known = false),
            file("b.jpg", 0, known = true))
        val groups = makeCollections(items, CollectionMode.FILE_SIZES)
        assertEquals(1, groups.first { it.id == "unknown" }.count)
        assertEquals(1, groups.first { it.id == "empty" }.count)
    }
}
