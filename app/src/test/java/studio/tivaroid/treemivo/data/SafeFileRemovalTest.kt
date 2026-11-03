package studio.tivaroid.treemivo.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SafeFileRemovalTest {
    @Test fun mediaUsesSystemTrashButSafDoesNot() {
        val media = FileRecord("a.jpg", "DCIM/a.jpg", 50L, "image/jpeg",
            Uri.parse("content://media/external/images/media/21"), source = FileSource.MEDIA)
        val saf = media.copy(
            uri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADCIM"),
            source = FileSource.SAF
        )
        assertTrue(SafeFileRemoval.supportsTrash(media))
        assertFalse(SafeFileRemoval.supportsTrash(saf))
    }

    @Test fun arbitraryContentUriCannotBecomeSystemTrashTarget() {
        val invalid = FileRecord("a.jpg", "a.jpg", 10L, "image/jpeg",
            Uri.parse("content://untrusted.provider/object/1"), source = FileSource.MEDIA)
        assertFalse(SafeFileRemoval.supportsTrash(invalid))
    }
}
