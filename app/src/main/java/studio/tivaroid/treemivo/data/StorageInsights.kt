package studio.tivaroid.treemivo.data

data class ExtensionUsage(
    val extension: String,
    val bytes: Long,
    val files: Int
)

data class AgeUsage(
    val recentBytes: Long,
    val lastMonthBytes: Long,
    val olderBytes: Long,
    val unknownBytes: Long
)

data class StorageInsights(
    val indexedFiles: Int,
    val indexedBytes: Long,
    val zeroByteFiles: Int,
    val unknownDateFiles: Int,
    val veryLargeFiles: Int,
    val hiddenFiles: Int,
    val extensions: List<ExtensionUsage>,
    val age: AgeUsage,
    val largestFiles: List<FileRecord>
)

/**
 * Analysis only, never infers inaccessible files or total device size.
 * Extension names are based on file names, not trusted MIME metadata.
 */
fun analyzeStorageInsights(
    files: List<FileRecord>,
    nowMillis: Long = System.currentTimeMillis()
): StorageInsights {
    data class Counter(var bytes: Long = 0, var count: Int = 0)
    val extensionTotals = HashMap<String, Counter>()
    var totalBytes = 0L
    var zeroSize = 0
    var unknownDates = 0
    var oneGbFiles = 0
    var hiddenFiles = 0
    var week = 0L
    var month = 0L
    var old = 0L
    var unknown = 0L
    val weekAgo = nowMillis - 7L * 24 * 60 * 60 * 1000
    val monthAgo = nowMillis - 30L * 24 * 60 * 60 * 1000

    for (file in files) {
        val size = file.sizeBytes.coerceAtLeast(0L)
        totalBytes += size
        if (size == 0L) zeroSize++
        if (size >= 1024L * 1024 * 1024) oneGbFiles++
        if (file.name.startsWith('.')) hiddenFiles++
        val extension = file.name.substringAfterLast('.', "")
            .lowercase().take(16).ifBlank { "(none)" }
        val entry = extensionTotals.getOrPut(extension) { Counter() }
        entry.bytes += size
        entry.count++

        when {
            file.modifiedAtMillis <= 0L -> {
                unknownDates++
                unknown += size
            }
            file.modifiedAtMillis >= weekAgo -> week += size
            file.modifiedAtMillis >= monthAgo -> month += size
            else -> old += size
        }
    }
    return StorageInsights(
        indexedFiles = files.size,
        indexedBytes = totalBytes,
        zeroByteFiles = zeroSize,
        unknownDateFiles = unknownDates,
        veryLargeFiles = oneGbFiles,
        hiddenFiles = hiddenFiles,
        extensions = extensionTotals.map { (ext, entry) ->
            ExtensionUsage(ext, entry.bytes, entry.count)
        }.sortedByDescending { it.bytes }.take(12),
        age = AgeUsage(week, month, old, unknown),
        largestFiles = files.sortedByDescending { it.sizeBytes }.take(8)
    )
}
