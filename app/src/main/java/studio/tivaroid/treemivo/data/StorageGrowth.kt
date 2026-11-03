package studio.tivaroid.treemivo.data

/**
 * Comparison of indexed, accessible data from *the same stable scanning scope*.
 * This must never be presented as whole-device growth or as a list of new files.
 */
data class KindGrowth(
    val kind: FileKind,
    val beforeBytes: Long,
    val afterBytes: Long
) {
    val deltaBytes: Long get() = afterBytes - beforeBytes
}

data class StorageGrowth(
    val startedAt: Long,
    val endedAt: Long,
    val beforeBytes: Long,
    val afterBytes: Long,
    val beforeFiles: Int,
    val afterFiles: Int,
    val kindChanges: List<KindGrowth>
) {
    val deltaBytes: Long get() = afterBytes - beforeBytes
    val deltaFiles: Int get() = afterFiles - beforeFiles
    val elapsedDays: Double get() = (endedAt - startedAt).coerceAtLeast(0L) / 86_400_000.0
}

/** Returns null if scope, source or result completeness cannot be compared safely. */
fun compareGrowth(
    previous: ScanSnapshot,
    latest: ScanSnapshot
): StorageGrowth? {
    if (previous.time >= latest.time ||
        previous.source != latest.source ||
        previous.partial || latest.partial ||
        previous.scopeKey.isBlank() ||
        previous.scopeKey != latest.scopeKey
    ) return null

    return StorageGrowth(
        previous.time, latest.time,
        previous.bytes, latest.bytes,
        previous.count, latest.count,
        FileKind.entries.map { kind ->
            KindGrowth(
                kind,
                previous.typeBytes[kind.name] ?: 0L,
                latest.typeBytes[kind.name] ?: 0L
            )
        }.filter { it.beforeBytes != it.afterBytes }
            .sortedByDescending { kotlin.math.abs(it.deltaBytes) }
    )
}

fun matchingPrevious(
    snapshots: List<ScanSnapshot>,
    current: ScanSnapshot
): ScanSnapshot? =
    snapshots.filter {
        it.time < current.time &&
            it.source == current.source &&
            it.scopeKey.isNotBlank() && it.scopeKey == current.scopeKey &&
            !it.partial && !current.partial
    }.maxByOrNull { it.time }
