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
    val sort: FileSort = FileSort.SIZE_DESC
)

fun filterFiles(files: List<FileRecord>, filter: AdvancedFilter): List<FileRecord> {
    val result = files.asSequence()
        .filter { it.sizeBytes in filter.minSizeBytes..filter.maxSizeBytes }
        .filter { filter.kind == null || it.kind == filter.kind }
        .filter { it.modifiedAtMillis >= filter.modifiedAfterMillis }
        .filter {
            filter.query.isBlank() ||
                it.name.contains(filter.query, ignoreCase = true) ||
                it.relativePath.contains(filter.query, ignoreCase = true)
        }.toList()
    return when (filter.sort) {
        FileSort.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
        FileSort.SIZE_ASC -> result.sortedBy { it.sizeBytes }
        FileSort.NAME -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        FileSort.MODIFIED_DESC -> result.sortedByDescending { it.modifiedAtMillis }
    }
}
