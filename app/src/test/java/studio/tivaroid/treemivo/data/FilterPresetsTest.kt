package studio.tivaroid.treemivo.data

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FilterPresetsTest {
    @Test fun retainsTenNewestPresetsAndRemovesByName() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = FilterPresets(context)
        store.load().forEach { store.remove(it.name) }
        repeat(12) {
            store.save(SavedFilter("preset-$it", it.toLong(), Long.MAX_VALUE,
                FileKind.IMAGE, false, FileSort.SIZE_DESC))
        }
        assertEquals(10, store.load().size)
        assertFalse(store.load().any { it.name == "preset-0" })
        store.save(SavedFilter("preset-11", 999, 12345, FileKind.VIDEO, true, FileSort.NAME))
        assertEquals(10, store.load().size)
        assertEquals(999, store.load().last().minSize)
        store.remove("preset-11")
        assertFalse(store.load().any { it.name == "preset-11" })
    }
}
