package studio.tivaroid.treemivo.data

import android.net.Uri

enum class FileSource { SAF, MEDIA }

/** Each record refers to an explicitly authorized SAF document or accessible indexed media. */
data class FileRecord(
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val mimeType: String,
    val uri: Uri,
    val modifiedAtMillis: Long = 0L,
    val source: FileSource = FileSource.SAF
) {
    val parentPath: String get() = relativePath.substringBeforeLast('/', "")
    val kind: FileKind get() = FileKind.detect(name, mimeType)
}

data class FolderRecord(
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val fileCount: Int
)

data class StorageReport(
    val rootName: String,
    val files: List<FileRecord>,
    val folders: List<FolderRecord>,
    val partial: Boolean
) {
    val totalBytes: Long get() = files.sumOf { it.sizeBytes }
}

data class DuplicateGroup(val files: List<FileRecord>) {
    val reclaimableBytes: Long get() = files.firstOrNull()?.sizeBytes?.times(files.size - 1) ?: 0
}

enum class FileKind {
    IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, INSTALLER, OTHER;

    companion object {
        fun detect(name: String, mime: String): FileKind {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                mime.startsWith("image/") || ext in setOf("jpg","jpeg","png","gif","webp","heic","bmp","svg") -> IMAGE
                mime.startsWith("video/") || ext in setOf("mp4","mov","mkv","webm","avi","3gp") -> VIDEO
                mime.startsWith("audio/") || ext in setOf("mp3","m4a","ogg","flac","wav","aac") -> AUDIO
                ext in setOf("zip","rar","7z","tar","gz","bz2","xz") -> ARCHIVE
                ext in setOf("apk","aab","apks","xapk") -> INSTALLER
                mime.startsWith("text/") || ext in setOf("pdf","doc","docx","xls","xlsx","ppt","pptx","txt","md","csv","json","xml","html","js","kt","py") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
