package studio.tivaroid.treemivo.data

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class CollectionMode { FOLDERS, EXTENSIONS, YEARS, NAME_FAMILIES, FILE_SIZES }

data class SmartCollection(
    val id: String,
    val label: String,
    val count: Int,
    val bytes: Long,
    val members: List<FileRecord>
)

/**
 * Rule-based, local organization of already indexed files.
 * A "name family" is never a duplicate claim or a deletion recommendation.
 */
fun makeCollections(files: List<FileRecord>, mode: CollectionMode): List<SmartCollection> {
    val groups = LinkedHashMap<String, MutableList<FileRecord>>()

    fun key(file: FileRecord): String? = when (mode) {
        CollectionMode.FOLDERS ->
            file.parentPath.substringBefore('/').ifBlank { "(root)" }
        CollectionMode.EXTENSIONS -> file.name.substringAfterLast('.', "")
            .lowercase(Locale.ROOT).ifBlank { "(none)" }
        CollectionMode.YEARS -> if (file.modifiedAtMillis <= 0) "(unknown)"
            else try {
                Instant.ofEpochMilli(file.modifiedAtMillis).atZone(ZoneId.systemDefault())
                    .year.toString()
            } catch (_: Exception) { "(unknown)" }
        CollectionMode.NAME_FAMILIES -> {
            val stem = file.name.substringBeforeLast('.', file.name)
            val base = stem.replace(Regex("[-_\\s]*[0-9]{3,}$"), "")
                .trim('-', '_', ' ').lowercase(Locale.ROOT)
            if (base.length < 3 || base.length > 45 || base == stem.lowercase(Locale.ROOT)) null
            else file.parentPath + "|" + base
        }
        CollectionMode.FILE_SIZES -> when {
            !file.sizeKnown -> "unknown"
            file.sizeBytes == 0L -> "empty"
            file.sizeBytes < 10L * 1024 * 1024 -> "under_10mb"
            file.sizeBytes < 100L * 1024 * 1024 -> "10_100mb"
            file.sizeBytes < 1024L * 1024 * 1024 -> "100mb_1gb"
            else -> "over_1gb"
        }
    }

    for (file in files) {
        val groupKey = key(file) ?: continue
        groups.getOrPut(groupKey) { ArrayList() }.add(file)
    }
    return groups.entries.asSequence()
        .filter { (key, members) ->
            mode != CollectionMode.NAME_FAMILIES || members.size >= 2
        }
        .map { (key, members) ->
            val label = if (mode == CollectionMode.NAME_FAMILIES)
                key.substringAfterLast('|') else key
            SmartCollection(
                id = key, label = label,
                count = members.size,
                bytes = members.sumOf { it.sizeBytes.coerceAtLeast(0L) },
                members = members
            )
        }
        .sortedByDescending { it.bytes }
        .take(120)
        .toList()
}
