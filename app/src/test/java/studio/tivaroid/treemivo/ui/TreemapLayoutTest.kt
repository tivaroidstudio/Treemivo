package studio.tivaroid.treemivo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreemapLayoutTest {
    @Test fun coversUnitSquareWithoutOverlaps() {
        val tiles = buildTreemap(listOf(60L, 20L, 10L, 10L), { it })
        assertEquals(4, tiles.size)
        val area = tiles.sumOf { (it.width * it.height).toDouble() }
        assertEquals(1.0, area, 0.00001)
        assertTrue(tiles.all { it.x >= 0f && it.y >= 0f && it.x + it.width <= 1.001f && it.y + it.height <= 1.001f })
    }

    @Test fun skipsZeroWeights() {
        assertEquals(1, buildTreemap(listOf(0, 42), { it.toLong() }).size)
    }

    @Test fun handlesDominantTile() {
        val tiles = buildTreemap(listOf(900L, 1L, 1L), { it })
        assertEquals(3, tiles.size)
        assertTrue(tiles.first().width * tiles.first().height > 0.99f)
    }
}
