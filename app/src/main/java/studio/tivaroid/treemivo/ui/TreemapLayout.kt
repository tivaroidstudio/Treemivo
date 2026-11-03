package studio.tivaroid.treemivo.ui

/**
 * Deterministic binary treemap. Rectangle coordinates are normalized to 0..1.
 * Splitting close to half the total weight prevents extreme long strips.
 */
data class TreemapRect<T>(
    val item: T, val x: Float, val y: Float, val width: Float, val height: Float
)

fun <T> buildTreemap(
    items: List<T>, weight: (T) -> Long, maxTiles: Int = 260
): List<TreemapRect<T>> {
    val sorted = items.filter { weight(it) > 0L }
        .sortedByDescending(weight)
        .take(maxTiles)
    if (sorted.isEmpty()) return emptyList()
    val result = ArrayList<TreemapRect<T>>(sorted.size)

    fun layout(part: List<T>, x: Float, y: Float, w: Float, h: Float) {
        if (part.size == 1) {
            result.add(TreemapRect(part[0], x, y, w, h))
            return
        }
        val sum = part.sumOf { weight(it).toDouble() }
        val target = sum / 2.0
        var cumulative = 0.0
        var split = 0
        while (split < part.lastIndex && cumulative + weight(part[split]) <= target) {
            cumulative += weight(part[split])
            split++
        }
        if (split == 0) {
            split = 1
            cumulative = weight(part[0]).toDouble()
        }
        val fraction = (cumulative / sum).toFloat().coerceIn(0.001f, 0.999f)
        if (w >= h) {
            val leftWidth = w * fraction
            layout(part.subList(0, split), x, y, leftWidth, h)
            layout(part.subList(split, part.size), x + leftWidth, y, w - leftWidth, h)
        } else {
            val topHeight = h * fraction
            layout(part.subList(0, split), x, y, w, topHeight)
            layout(part.subList(split, part.size), x, y + topHeight, w, h - topHeight)
        }
    }
    layout(sorted, 0f, 0f, 1f, 1f)
    return result
}
