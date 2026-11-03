package studio.tivaroid.treemivo.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReviewSuggestionsTest {
    private fun file(
        path: String, bytes: Long = 5L,
        type: String = "application/octet-stream",
        known: Boolean = true
    ) = FileRecord(
        path.substringAfterLast('/'), path, bytes, type,
        Uri.parse("content://example/" + path),
        modifiedAtMillis = 1000L,
        sizeKnown = known
    )

    @Test fun screenshotCandidatesAreOnlySuggestions() {
        val list = listOf(
            file("Pictures/Screenshots/a.png", 20, "image/png"),
            file("DCIM/Camera/photo.png", 20, "image/png")
        )
        assertEquals(1, reviewCandidates(list, ReviewCategory.SCREENSHOTS).size)
    }

    @Test fun unknownZeroSizeIsNotListedAsEmpty() {
        val list = listOf(
            file("docs/unknown.dat", 0L, known = false),
            file("docs/real-empty.txt", 0L)
        )
        val found = reviewCandidates(list, ReviewCategory.EMPTY_FILES)
        assertEquals(1, found.size)
        assertEquals("real-empty.txt", found.first().file.name)
    }

    @Test fun extensionBackupDoesNotIncludeNormalPictures() {
        val list = listOf(file("data/config.bak"), file("DCIM/IMG_1.jpg"))
        assertEquals(1, reviewCandidates(list, ReviewCategory.BACKUPS).size)
    }

    @Test fun downloadFolderIsMatchedBySegmentNotSubstring() {
        val list = listOf(
            file("Download/old.zip"),
            file("my-download-archive/old.zip"),
            file("Documents/daily.zip")
        )
        assertEquals(1,
            reviewCandidates(list, ReviewCategory.OLD_DOWNLOAD,
                nowMillis = 60L * 24 * 60 * 60 * 1000).size)
    }
}
