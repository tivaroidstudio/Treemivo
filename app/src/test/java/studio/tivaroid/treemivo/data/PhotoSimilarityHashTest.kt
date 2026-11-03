package studio.tivaroid.treemivo.data

import org.junit.Assert.*
import org.junit.Test

class PhotoSimilarityHashTest {
    @Test fun flatImageHashIsZero() {
        assertEquals(0L, DifferenceHash.fromLuminance(IntArray(72) { 128 }))
    }

    @Test fun reversingHorizontalGradientChangesAllBits() {
        val lightToDark = IntArray(72) { idx -> 255 - (idx % 9) * 20 }
        val darkToLight = IntArray(72) { idx -> (idx % 9) * 20 }
        assertEquals(-1L, DifferenceHash.fromLuminance(lightToDark))
        assertEquals(0L, DifferenceHash.fromLuminance(darkToLight))
        assertEquals(64, DifferenceHash.distance(
            DifferenceHash.fromLuminance(lightToDark),
            DifferenceHash.fromLuminance(darkToLight)
        ))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidGridIsRejected() {
        DifferenceHash.fromLuminance(IntArray(64))
    }
}
