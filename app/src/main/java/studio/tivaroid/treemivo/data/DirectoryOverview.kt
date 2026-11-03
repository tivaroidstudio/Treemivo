package studio.tivaroid.treemivo.data

/**
 * Visible children of a single logical directory. Unlike the file-level treemap,
 * directory tiles aggregate all descendant files before drawing.
 */
data class DirectoryTile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val isFolder: Boolean,
    val file: FileRecord? = null,
    val dominantKind: FileKind = FileKind.OTHER
)

/** Pure, single-pass aggregation; does not reopen files or need filesystem permissions. */
fun directoryTiles(files: List<FileRecord>, directory: String): List<DirectoryTile> {
    data class FolderTotals(
        var bytes: Long = 0,
        var count: Int = 0,
        val kinds: LongArray = LongArray(FileKind.entries.size)
    )
    val prefix = if (directory.isBlank()) "" else "$directory/"
    val folders = linkedMapOf<String, FolderTotals>()
    val output = ArrayList<DirectoryTile>()
    for (file in files) {
        if (!file.relativePath.startsWith(prefix)) continue
        val child = file.relativePath.removePrefix(prefix)
        if (child.isBlank()) continue
        val first = child.substringBefore('/')
        if (first.isBlank()) continue
        if (child.contains('/')) {
            val aggregate = folders.getOrPut(first) { FolderTotals() }
            aggregate.bytes += file.sizeBytes.coerceAtLeast(0L)
            aggregate.count++
            aggregate.kinds[file.kind.ordinal] += file.sizeBytes.coerceAtLeast(0L)
        } else {
            output.add(
                DirectoryTile(
                    name = file.name,
                    path = file.relativePath,
                    sizeBytes = file.sizeBytes.coerceAtLeast(0L),
                    fileCount = 1,
                    isFolder = false,
                    file = file,
                    dominantKind = file.kind
                )
            )
        }
    }
    for ((name, totals) in folders) {
        val dominantIndex = totals.kinds.indices.maxByOrNull { totals.kinds[it] } ?: 0
        output.add(
            DirectoryTile(
                name = name,
                path = prefix + name,
                sizeBytes = totals.bytes,
                fileCount = totals.count,
                isFolder = true,
                dominantKind = FileKind.entries[dominantIndex]
            )
        )
    }
    return output.sortedByDescending { it.sizeBytes }
}

fun directoryAncestors(path: String): List<String> {
    if (path.isBlank()) return listOf("")
    val parts = path.split('/').filter { it.isNotBlank() }
    val ancestors = mutableListOf("")
    for (i in parts.indices) ancestors.add(parts.take(i + 1).joinToString("/"))
    return ancestors
}
