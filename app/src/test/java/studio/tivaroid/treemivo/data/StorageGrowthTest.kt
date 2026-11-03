package studio.tivaroid.treemivo.data

import org.junit.Assert.*
import org.junit.Test

class StorageGrowthTest {
    private fun snap(
        time: Long, bytes: Long, source: String = "MEDIA",
        scope: String = "stable", partial: Boolean = false,
        types: Map<String, Long> = emptyMap()
    ) = ScanSnapshot(
        time = time, source = source, count = 20, bytes = bytes,
        largest = 100L, scopeKey = scope, partial = partial,
        typeBytes = types
    )

    @Test fun comparableScopesReturnGrowthByType() {
        val before = snap(1_000L, 100L, types = mapOf("IMAGE" to 80, "VIDEO" to 20))
        val after = snap(100_000L, 160L, types = mapOf("IMAGE" to 90, "VIDEO" to 70))
        val growth = compareGrowth(before, after)!!
        assertEquals(60L, growth.deltaBytes)
        assertEquals(FileKind.VIDEO, growth.kindChanges.first().kind)
        assertEquals(50L, growth.kindChanges.first().deltaBytes)
    }

    @Test fun rejectsDifferentSourcesAndFolderGrants() {
        assertNull(compareGrowth(snap(1, 100), snap(2, 150, scope = "new-folder")))
        assertNull(compareGrowth(snap(1, 100), snap(2, 150, source = "FOLDERS")))
    }

    @Test fun rejectsPartialAndLegacySnapshots() {
        assertNull(compareGrowth(snap(1, 100, partial = true), snap(2, 150)))
        assertNull(compareGrowth(snap(1, 100, scope = ""), snap(2, 150, scope = "")))
    }

    @Test fun rejectsSameTimestamp() {
        assertNull(compareGrowth(snap(1, 100), snap(1, 200)))
    }
}
