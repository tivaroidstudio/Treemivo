package studio.tivaroid.treemivo.data

/**
 * Metadata-based suggestions: not proof of redundancy, obsolescence or recoverable space.
 * The user must inspect each candidate before any move to system trash.
 */
enum class ReviewCategory {
    LARGE_VIDEO, OLD_DOWNLOAD, INSTALLERS, DUPLICATES, SIMILAR,
    SCREENSHOTS, BACKUPS, EMPTY_FILES
}

data class ReviewCandidate(
    val file: FileRecord,
    val category: ReviewCategory
)

fun reviewCandidates(
    files: List<FileRecord>,
    category: ReviewCategory,
    nowMillis: Long = System.currentTimeMillis()
): List<ReviewCandidate> {
    val selected = when (category) {
        ReviewCategory.LARGE_VIDEO ->
            files.filter { it.kind == FileKind.VIDEO && it.sizeBytes >= 100L * 1024 * 1024 }
        ReviewCategory.OLD_DOWNLOAD ->
            files.filter {
                it.relativePath.split('/').any { part ->
                    part.equals("Download", true) || part.equals("Downloads", true)
                } && it.modifiedAtMillis in 1 until
                    (nowMillis - 30L * 24 * 60 * 60 * 1000)
            }
        ReviewCategory.INSTALLERS ->
            files.filter { it.kind == FileKind.INSTALLER }
        ReviewCategory.SCREENSHOTS ->
            files.filter {
                it.kind == FileKind.IMAGE &&
                    (it.relativePath.split('/').any { segment ->
                        segment.equals("Screenshots", true) ||
                            segment.equals("Screenshot", true) ||
                            segment.equals("Zrzuty ekranu", true)
                    } || it.name.startsWith("Screenshot", true))
            }
        ReviewCategory.BACKUPS ->
            files.filter {
                val ext = it.name.substringAfterLast('.', "").lowercase()
                ext in setOf("bak", "backup", "old", "orig", "tmp") &&
                    it.sizeBytes > 0L
            }
        ReviewCategory.EMPTY_FILES -> files.filter { it.sizeBytes == 0L }
        // These two require stronger, explicitly requested analysis and are prepared separately.
        ReviewCategory.DUPLICATES, ReviewCategory.SIMILAR -> emptyList()
    }
    return selected.distinctBy { it.uri.toString() }
        .sortedByDescending { it.sizeBytes }
        .map { ReviewCandidate(it, category) }
}
