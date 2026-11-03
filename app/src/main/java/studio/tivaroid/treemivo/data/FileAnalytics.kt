package studio.tivaroid.treemivo.data

/** Converts file paths to folder totals without opening any file contents. */
fun folderBreakdown(files: List<FileRecord>): List<FolderRecord> {
    val totals = HashMap<String, Pair<Long, Int>>()
    for (file in files) {
        var parent = file.parentPath
        while (parent.isNotBlank()) {
            val old = totals[parent] ?: (0L to 0)
            totals[parent] = (old.first + file.sizeBytes) to (old.second + 1)
            parent = parent.substringBeforeLast('/', "")
        }
    }
    return totals.map { (path, data) ->
        FolderRecord(path.substringAfterLast('/'), path, data.first, data.second)
    }.sortedByDescending { it.sizeBytes }
}

enum class FileSort { SIZE_DESC, SIZE_ASC, NAME, MODIFIED_DESC }

data class AdvancedFilter(
    val query: String = "",
    val minSizeBytes: Long = 0L,
    val maxSizeBytes: Long = Long.MAX_VALUE,
    val kind: FileKind? = null,
    val modifiedAfterMillis: Long = 0L,
    val sort: FileSort = FileSort.SIZE_DESC,
    val onlyEmpty: Boolean = false,
    val onlyHidden: Boolean = false
)

fun filterFiles(files: List<FileRecord>, filter: AdvancedFilter): List<FileRecord> {
    // Parse query only once, not once per file in a 100k-item scan.
    val plan = parseSearchQuery(filter.query)
    val result = files.asSequence()
        .filter { it.sizeBytes in filter.minSizeBytes..filter.maxSizeBytes }
        .filter { filter.kind == null || it.kind == filter.kind }
        .filter { !filter.onlyEmpty || (it.sizeKnown && it.sizeBytes == 0L) }
        .filter { !filter.onlyHidden || it.name.startsWith('.') }
        .filter { it.modifiedAtMillis >= filter.modifiedAfterMillis }
        .filter(plan::matches).toList()
    return when (filter.sort) {
        FileSort.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
        FileSort.SIZE_ASC -> result.sortedBy { it.sizeBytes }
        FileSort.NAME -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        FileSort.MODIFIED_DESC -> result.sortedByDescending { it.modifiedAtMillis }
    }
}
