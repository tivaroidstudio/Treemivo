package studio.tivaroid.treemivo.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AdvancedSearchTest {
    private fun record(path: String, bytes: Long, mime: String = "application/octet-stream",
        known: Boolean = true, modified: Long = 0L) = FileRecord(
        path.substringAfterLast('/'), path, bytes, mime,
        Uri.parse("content://example/" + path),
        modifiedAtMillis = modified, sizeKnown = known
    )

    @Test fun intersectsVideoAndLargeSizeCriteria() {
        val files = listOf(
            record("DCIM/short.mp4", 3_000, "video/mp4"),
            record("DCIM/long.mp4", 200_000_000, "video/mp4"),
            record("DCIM/huge.pdf", 300_000_000, "application/pdf")
        )
        val filtered = filterFiles(files,
            AdvancedFilter(query = "type:video size>100MB"))
        assertEquals(listOf("long.mp4"), filtered.map { it.name })
    }

    @Test fun quotedNamesAndNegativeExtensionsWorkTogether() {
        val files = listOf(
            record("Download/summer holiday.zip", 100),
            record("Download/summer holiday.tmp", 100),
            record("Pictures/summer vacation.zip", 100)
        )
        val result = filterFiles(files,
            AdvancedFilter(query = "name:\"summer holiday\" -ext:tmp"))
        assertEquals(listOf("summer holiday.zip"), result.map { it.name })
    }

    @Test fun appliesLocalDateAndFolderCriteria() {
        val date = LocalDate.of(2026, 5, 1).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val files = listOf(
            record("Download/now.zip", 20, modified = date),
            record("Download/old.zip", 20, modified = 1000L),
            record("Other/now.zip", 20, modified = date)
        )
        assertEquals("now.zip", filterFiles(files,
            AdvancedFilter(query = "in:Download after:2026-01-01")).single().name)
    }

    @Test fun invalidSizeTermReportsErrorInsteadOfThrowing() {
        val plan = parseSearchQuery("type:video size>banana")
        assertFalse(plan.errors.isEmpty())
        assertEquals(1, plan.rules.size)
    }

    @Test fun unknownSizeDoesNotSatisfySizeOperators() {
        val file = record("unknown.bin", 0, known = false)
        assertFalse(parseSearchQuery("size<1GB").matches(file))
    }

    @Test fun extensionListsMatchEitherAlternative() {
        val files = listOf(record("a.zip", 1), record("b.7z", 1), record("c.txt", 1))
        assertEquals(2, filterFiles(files,
            AdvancedFilter(query = "ext:zip,7z")).size)
    }
}
